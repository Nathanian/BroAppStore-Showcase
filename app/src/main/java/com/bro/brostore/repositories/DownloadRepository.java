package com.example.appstoredemo.repositories;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.LongSparseArray;
import android.content.pm.PackageManager;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.appstoredemo.models.DownloadState;
import com.example.appstoredemo.models.SignedUrlResponse;
import com.example.appstoredemo.network.BrostoreService;
import com.example.appstoredemo.utils.ApkInstaller;
import com.example.appstoredemo.utils.AppExecutors;
import com.google.gson.Gson;

import javax.inject.Singleton;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

import retrofit2.Response;

/**
 * Handles downloads and exposes per app state changes.
 */
@Singleton
public class DownloadRepository {
    /**
     * Metadata describing a download request.
     */
    public static class DownloadEntry {
        public String packageName;
        public long versionCode;
        public String url;
        public String sha256;
        public long sizeBytes;
        public String signatureDigest;
    }

    private final Context context;
    private final DownloadManager downloadManager;
    private final MutableLiveData<Map<String, DownloadState>> states = new MutableLiveData<>(new HashMap<>());
    private final LongSparseArray<String> idToPackage = new LongSparseArray<>();
    private final Gson gson = new Gson();
    private final SharedPreferences prefs;
    private final BrostoreService brostoreService;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean observerRegistered = false;
    private boolean receiverRegistered = false;

    private final ContentObserver observer = new ContentObserver(mainHandler) {
        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            queryProgress();
        }
    };

    private final BroadcastReceiver completeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context ctx, Intent intent) {
            long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            String pkg = idToPackage.get(id);
            if (pkg == null) return;
            handleCompletion(pkg, id);
        }
    };

    /**
     * Creates a repository bound to the application context and backend service.
     *
     * @param ctx application context used to interact with {@link DownloadManager} and storage.
     * @param service Retrofit API service used to resolve signed download URLs.
     * @throws IllegalArgumentException if any dependency is {@code null} or the download manager
     *                                  cannot be obtained.
     */
    public DownloadRepository(@NonNull Context ctx, @NonNull BrostoreService service) {
        context = ctx.getApplicationContext();
        downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        if (downloadManager == null) {
            throw new IllegalArgumentException("DownloadManager not available");
        }
        prefs = context.getSharedPreferences("downloads", Context.MODE_PRIVATE);
        brostoreService = service;
        start();
    }

    /**
     * Registers observers and restores persisted download state.
     *
     * @return void.
     * @throws SecurityException if the app lacks permission to observe downloads.
     */
    public synchronized void start() {
        boolean changed = false;
        if (!observerRegistered) {
            context.getContentResolver().registerContentObserver(Uri.parse("content://downloads/my_downloads"), true, observer);
            observerRegistered = true;
            changed = true;
        }
        if (!receiverRegistered) {
            ContextCompat.registerReceiver(context, completeReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), ContextCompat.RECEIVER_EXPORTED);
            receiverRegistered = true;
            changed = true;
        }
        if (changed) {
            restoreState();
        }
    }

    private void restoreState() {
        Map<String, DownloadState> current = states.getValue();
        Map<String, DownloadState> map = current != null ? new HashMap<>(current) : new HashMap<>();
        Map<String, ?> all = prefs.getAll();
        SharedPreferences.Editor editor = prefs.edit();
        PackageManager pm = context.getPackageManager();
        Map<String, Long> activeDownloads = findActiveDownloads();

        for (String key : all.keySet()) {
            String json = prefs.getString(key, null);
            if (json == null) continue;
            DownloadEntry entry = gson.fromJson(json, DownloadEntry.class);
            if (entry == null || entry.packageName == null) continue;

            if (isPackageInstalled(pm, entry.packageName)) {
                map.remove(entry.packageName);
                editor.remove(entry.packageName);
                continue;
            }

            Long id = activeDownloads.get(entry.packageName);
            if (id != null) {
                idToPackage.put(id, entry.packageName);
                map.put(entry.packageName, DownloadState.DOWNLOADING);
            } else {
                map.remove(entry.packageName);
                editor.remove(entry.packageName);
            }
        }

        editor.apply();
        states.setValue(map);
    }

    /**
     * Exposes live download states keyed by package name.
     *
     * @return live map of package names to current {@link DownloadState} values.
     * @throws IllegalStateException if the repository has already been disposed.
     */
    public LiveData<Map<String, DownloadState>> getStates() {
        if (states == null) {
            throw new IllegalStateException("States LiveData not initialized");
        }
        return states;
    }

    @MainThread
    /**
     * Resolves a signed download URL and enqueues it via {@link DownloadManager}.
     *
     * @param entry metadata describing the APK to download.
     * @return void.
     * @throws IllegalArgumentException if {@code entry} is missing required fields.
     */
    public void startDownload(@NonNull DownloadEntry entry) {
        if (entry == null) {
            throw new IllegalArgumentException("Download entry is required");
        }
        if (entry.packageName == null || entry.packageName.isEmpty()) {
            throw new IllegalArgumentException("Package name is required");
        }
        if (entry.url == null || entry.url.isEmpty()) {
            throw new IllegalArgumentException("Download URL is required");
        }
        Map<String, DownloadState> map = states.getValue();
        if (map != null) {
            DownloadState current = map.get(entry.packageName);
            if (current != null && current != DownloadState.FAILED && current != DownloadState.IDLE) {
                return; // dedupe active downloads
            }
        }

        updateStateOnMainThread(entry.packageName, DownloadState.DOWNLOADING);

        AppExecutors.io().execute(() -> {
            try {
                Response<SignedUrlResponse> response = brostoreService.getSignedUrl(entry.url).execute();
                if (response.isSuccessful() && response.body() != null) {
                    String signedUrl = response.body().signedUrl;
                    if (signedUrl != null && !signedUrl.isEmpty()) {
                        mainHandler.post(() -> enqueueDownload(entry, signedUrl));
                        return;
                    }
                }
                mainHandler.post(() -> mapFailed(entry.packageName));
            } catch (Exception e) {
                mainHandler.post(() -> mapFailed(entry.packageName));
            }
        });
    }

    @MainThread
    /**
     * Cancels an active download and clears any persisted state for the package.
     *
     * @param packageName unique identifier of the package to cancel.
     * @return void.
     * @throws IllegalArgumentException if {@code packageName} is empty.
     */
    public void cancel(@NonNull String packageName) {
        if (packageName == null || packageName.isEmpty()) {
            throw new IllegalArgumentException("Package name is required");
        }
        for (int i = 0; i < idToPackage.size(); i++) {
            if (packageName.equals(idToPackage.valueAt(i))) {
                long id = idToPackage.keyAt(i);
                downloadManager.remove(id);
                idToPackage.removeAt(i);
                break;
            }
        }
        prefs.edit().remove(packageName).apply();
        Map<String, DownloadState> map = states.getValue();
        if (map != null) {
            map.put(packageName, DownloadState.IDLE);
            states.setValue(map);
        }
    }

    /**
     * Marks an app as installed, clearing any pending download state so UI can refresh.
     */
    @MainThread
    /**
     * Marks an app as installed, clearing any pending download state so UI can refresh.
     *
     * @param packageName unique identifier of the installed package.
     * @return void.
     * @throws IllegalArgumentException if {@code packageName} is empty.
     */
    public void markInstalled(@NonNull String packageName) {
        if (packageName == null || packageName.isEmpty()) {
            throw new IllegalArgumentException("Package name is required");
        }
        for (int i = 0; i < idToPackage.size(); i++) {
            if (packageName.equals(idToPackage.valueAt(i))) {
                idToPackage.removeAt(i);
                break;
            }
        }
        prefs.edit().remove(packageName).apply();
        Map<String, DownloadState> map = states.getValue();
        if (map != null) {
            map.remove(packageName);
            states.setValue(map);
        }
    }

    private void queryProgress() {
        DownloadManager.Query q = new DownloadManager.Query();
        for (int i = 0; i < idToPackage.size(); i++) {
            long id = idToPackage.keyAt(i);
            Cursor c = downloadManager.query(q.setFilterById(id));
            if (c != null) {
                if (c.moveToFirst()) {
                    int status = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
                    if (status == DownloadManager.STATUS_FAILED) {
                        String pkg = idToPackage.valueAt(i);
                        mapFailed(pkg);
                    }
                }
                c.close();
            }
        }
    }

    private void mapFailed(String pkg) {
        updateStateOnMainThread(pkg, DownloadState.FAILED);
        prefs.edit().remove(pkg).apply();
    }

    private void enqueueDownload(@NonNull DownloadEntry entry, @NonNull String signedUrl) {
        Uri uri = Uri.parse(signedUrl);
        String fileName = entry.packageName + "-" + entry.versionCode + ".apk";
        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File brostoreDir = new File(downloadsDir, "brostore");
        if (!brostoreDir.exists() && !brostoreDir.mkdirs()) {
            android.util.Log.w("DownloadRepository", "Unable to create brostore downloads directory: " + brostoreDir);
        }
        File dest = new File(brostoreDir, fileName);
        DownloadManager.Request req = new DownloadManager.Request(uri);
        req.setTitle(entry.packageName);
        req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
        req.setMimeType("application/vnd.android.package-archive");
        req.setDestinationUri(Uri.fromFile(dest));
        long id = downloadManager.enqueue(req);
        idToPackage.put(id, entry.packageName);
        prefs.edit().putString(entry.packageName, gson.toJson(entry)).apply();
        updateStateOnMainThread(entry.packageName, DownloadState.DOWNLOADING);
    }

    private Map<String, Long> findActiveDownloads() {
        Map<String, Long> result = new HashMap<>();
        DownloadManager.Query query = new DownloadManager.Query().setFilterByStatus(
                DownloadManager.STATUS_RUNNING
                        | DownloadManager.STATUS_PAUSED
                        | DownloadManager.STATUS_PENDING);
        Cursor cursor = downloadManager.query(query);
        if (cursor != null) {
            int idIndex = cursor.getColumnIndex(DownloadManager.COLUMN_ID);
            int titleIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TITLE);
            while (cursor.moveToNext()) {
                long id = cursor.getLong(idIndex);
                String title = cursor.getString(titleIndex);
                if (title != null) {
                    result.put(title, id);
                }
            }
            cursor.close();
        }
        return result;
    }

    private boolean isPackageInstalled(@NonNull PackageManager pm, @NonNull String packageName) {
        try {
            pm.getPackageInfo(packageName, 0);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void updateStateOnMainThread(@NonNull String packageName, @NonNull DownloadState state) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            Map<String, DownloadState> current = states.getValue();
            Map<String, DownloadState> updated = current != null ? new HashMap<>(current) : new HashMap<>();
            updated.put(packageName, state);
            states.setValue(updated);
        } else {
            mainHandler.post(() -> updateStateOnMainThread(packageName, state));
        }
    }

    private void handleCompletion(String pkg, long id) {
        Cursor c = downloadManager.query(new DownloadManager.Query().setFilterById(id));
        if (c != null && c.moveToFirst()) {
            int status = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
            String localUri = c.getString(c.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI));
            c.close();
            if (status == DownloadManager.STATUS_SUCCESSFUL) {
                Map<String, DownloadState> map = states.getValue();
                if (map == null) map = new HashMap<>();
                map.put(pkg, DownloadState.VERIFYING);
                states.setValue(map);
                verifyAndInstall(pkg, Uri.parse(localUri).getPath());
            } else {
                mapFailed(pkg);
            }
        }
    }


    private void verifyAndInstall(final String pkg, final String path) {
        AppExecutors.io().execute(() -> {
            final String DEBUG_TAG = "VERIFY_DEBUG";
            android.util.Log.d(DEBUG_TAG, "Verification started for pkg: " + pkg + " at path: " + path);

            String json = prefs.getString(pkg, null);
            if (json == null) {
                android.util.Log.e(DEBUG_TAG, "Failed: DownloadEntry JSON not found in SharedPreferences.");
                mapFailed(pkg);
                return;
            }

            DownloadEntry entry = gson.fromJson(json, DownloadEntry.class);
            File file = new File(path);

            // CHECK 1: Existiert die Datei?
            if (!file.exists()) {
                android.util.Log.e(DEBUG_TAG, "Failed: File does not exist at path: " + path);
                mapFailed(pkg);
                return;
            }
            android.util.Log.d(DEBUG_TAG, "Check 1 Passed: File exists.");

            // CHECK 2: Stimmt die Dateigröße?
            long expectedSize = entry.sizeBytes;
            long actualSize = file.length();
            android.util.Log.d(DEBUG_TAG, "Size Check: Expected=" + expectedSize + ", Actual=" + actualSize);
            if (expectedSize > 0 && actualSize != expectedSize) {
                android.util.Log.e(DEBUG_TAG, "Failed: File size mismatch.");
                mapFailed(pkg);
                return;
            }
            android.util.Log.d(DEBUG_TAG, "Check 2 Passed: File size is correct.");

            // CHECK 3: Stimmt der SHA256-Hash?
            if (entry.sha256 != null && !entry.sha256.isEmpty()) {
                try {
                    MessageDigest md = MessageDigest.getInstance("SHA-256");
                    try (InputStream in = new FileInputStream(file)) {
                        byte[] buf = new byte[8192];
                        int n;
                        while ((n = in.read(buf)) > 0) {
                            md.update(buf, 0, n);
                        }
                    }
                    StringBuilder sb = new StringBuilder();
                    for (byte b : md.digest()) {
                        sb.append(String.format("%02x", b));
                    }
                    String actualSha256 = sb.toString();
                    android.util.Log.d(DEBUG_TAG, "SHA256 Check: Expected=" + entry.sha256.toLowerCase() + ", Actual=" + actualSha256);
                    if (!actualSha256.equalsIgnoreCase(entry.sha256)) {
                        android.util.Log.e(DEBUG_TAG, "Failed: SHA256 checksum mismatch.");
                        mapFailed(pkg);
                        return;
                    }
                    android.util.Log.d(DEBUG_TAG, "Check 3 Passed: SHA256 is correct.");
                } catch (Exception e) {
                    android.util.Log.e(DEBUG_TAG, "Failed: Exception during SHA256 check.", e);
                    mapFailed(pkg);
                    return;
                }
            } else {
                android.util.Log.d(DEBUG_TAG, "Check 3 Skipped: No SHA256 provided.");
            }

            // Alle Checks bestanden, jetzt den Installer aufrufen!
            android.util.Log.d(DEBUG_TAG, "All checks passed! Triggering install prompt.");
            updateStateOnMainThread(pkg, DownloadState.INSTALL_PROMPT);
            mainHandler.post(() -> ApkInstaller.installApk(context, file));
        });
    }

    /**
     * Unregisters observers to avoid leaks when the repository is no longer needed.
     *
     * @return void.
     * @throws IllegalStateException if unregistering observers fails.
     */
    public synchronized void dispose() {
        if (observerRegistered) {
            try {
                context.getContentResolver().unregisterContentObserver(observer);
            } catch (IllegalStateException e) {
                throw new IllegalStateException("Failed to unregister content observer", e);
            }
            observerRegistered = false;
        }
        if (receiverRegistered) {
            try {
                context.unregisterReceiver(completeReceiver);
            } catch (IllegalArgumentException e) {
                throw new IllegalStateException("Failed to unregister receiver", e);
            }
            receiverRegistered = false;
        }
    }
}
