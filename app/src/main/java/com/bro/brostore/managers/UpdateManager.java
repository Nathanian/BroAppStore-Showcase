package com.example.appstoredemo.managers;
import android.Manifest;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.*;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.os.Environment;
import com.example.appstoredemo.utils.ApkInstaller;

import androidx.core.content.ContextCompat;
import androidx.core.app.ActivityCompat;

import com.example.appstoredemo.models.UpdateInfo;
import com.example.appstoredemo.utils.Constants;
import com.example.appstoredemo.models.RemoteAppInfo;
import com.example.appstoredemo.models.SignedUrlResponse;
import com.example.appstoredemo.network.BrostoreService;
import com.example.appstoredemo.utils.AppExecutors;
import com.example.appstoredemo.utils.Result;

import java.io.*;
import java.util.List;

import retrofit2.Response;

/**
 * Coordinates checking for app updates and downloading APKs through the system {@link DownloadManager}.
 */
public class UpdateManager {
    public static final int REQUEST_WRITE_PERMISSION = 1001;

    private final BrostoreService service;
    private static volatile BrostoreService sharedService;

    /**
     * Creates an instance backed by the supplied API service.
     *
     * @param service Retrofit API service used to resolve update metadata.
     * @throws IllegalArgumentException if {@code service} is {@code null}.
     */
    public UpdateManager(BrostoreService service) {
        if (service == null) {
            throw new IllegalArgumentException("BrostoreService must not be null");
        }
        this.service = service;
        sharedService = service;
    }

    private static boolean hasWritePermission(Context context) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_GRANTED;
    }

    private static void requestWritePermission(Context context) {
        if (context instanceof Activity) {
            ActivityCompat.requestPermissions((Activity) context,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_PERMISSION);
        }
    }

    /**
     * Retrieves update metadata for the provided package by scanning the remote catalog.
     *
     * @param pkgName package name to search for.
     * @return a {@link Result} wrapping the latest {@link UpdateInfo} or an error message.
     * @throws IllegalArgumentException if {@code pkgName} is {@code null} or empty.
     */
    public Result<UpdateInfo> fetchUpdateInfo(String pkgName) {
        if (pkgName == null || pkgName.isEmpty()) {
            throw new IllegalArgumentException("Package name must not be empty");
        }
        try {
            Response<List<RemoteAppInfo>> response = service.getAppCatalog().execute();
            if (response.isSuccessful() && response.body() != null) {
                for (RemoteAppInfo info : response.body()) {
                    if (info != null && pkgName.equals(info.packageName)) {
                        UpdateInfo latest = info.getLatest();
                        if (latest != null) {
                            return Result.success(latest);
                        }
                        return Result.error("No update info available");
                    }
                }
                return Result.error("Package not found in catalog");
            } else {
                return Result.error("Server error: " + (response != null ? response.code() : "unknown"));
            }
        } catch (Exception e) {
            Log.e("UpdateManager", "UpdateInfo Fehler: " + e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * Starts downloading the specified APK file via the {@link DownloadManager}.
     *
     * @param context Android context used to access system services.
     * @param apkPath backend path of the APK within the storage bucket.
     * @return void.
     * @throws IllegalArgumentException if {@code context} or {@code apkPath} are invalid.
     */
    public static void startApkDownload(Context context, String apkPath) {
        if (context == null) {
            throw new IllegalArgumentException("Context must not be null");
        }
        if (apkPath == null || apkPath.isEmpty()) {
            throw new IllegalArgumentException("APK path must not be empty");
        }
        if (!hasWritePermission(context)) {
            requestWritePermission(context);
            return;
        }
        BrostoreService service = sharedService;
        if (service == null) {
            Log.e("UpdateManager", "BrostoreService not initialized");
            return;
        }
        AppExecutors.io().execute(() -> {
            try {
                Response<SignedUrlResponse> response = service.getSignedUrl(apkPath).execute();
                if (response.isSuccessful() && response.body() != null && response.body().signedUrl != null) {
                    Uri uri = Uri.parse(response.body().signedUrl);
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(
                            () -> enqueueDownload(context, uri));
                } else {
                    Log.e("UpdateManager", "Failed to get signed URL: " + (response != null ? response.code() : "unknown"));
                }
            } catch (Exception e) {
                Log.e("UpdateManager", "Download Fehler: " + e.getMessage());
            }
        });
    }
    /**
     * Downloads an APK and prompts installation when complete.
     *
     * @param context Android context used to access system services.
     * @param apkUrl backend path of the APK to download.
     * @return void.
     * @throws IllegalArgumentException if {@code context} or {@code apkUrl} are invalid.
     */
    public static void downloadAndInstall(Context context, String apkUrl) {
        if (context == null || apkUrl == null || apkUrl.isEmpty()) {
            throw new IllegalArgumentException("Context and APK path must not be null");
        }
        downloadAndInstall(context, apkUrl, null);
    }

    /**
     * Downloads an APK and triggers installation, invoking an optional callback when done.
     *
     * @param context Android context used to access system services.
     * @param apkPath backend path of the APK to download.
     * @param onFinished optional callback executed on the main thread after installation intent fires.
     * @return void.
     * @throws IllegalArgumentException if {@code context} or {@code apkPath} are invalid.
     */
    public static void downloadAndInstall(Context context, String apkPath, Runnable onFinished) {
        if (context == null) {
            throw new IllegalArgumentException("Context must not be null");
        }
        if (apkPath == null || apkPath.isEmpty()) {
            throw new IllegalArgumentException("APK path must not be empty");
        }
        if (!hasWritePermission(context)) {
            requestWritePermission(context);
            return;
        }
        BrostoreService service = sharedService;
        if (service == null) {
            Log.e("UpdateManager", "BrostoreService not initialized");
            return;
        }
        AppExecutors.io().execute(() -> {
            try {
                Response<SignedUrlResponse> response = service.getSignedUrl(apkPath).execute();
                if (response.isSuccessful() && response.body() != null && response.body().signedUrl != null) {
                    Uri uri = Uri.parse(response.body().signedUrl);
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(
                            () -> enqueueDownloadAndInstall(context, uri, onFinished));
                } else {
                    Log.e("UpdateManager", "Failed to get signed URL: " + (response != null ? response.code() : "unknown"));
                }
            } catch (Exception e) {
                Log.e("UpdateManager", "Download Fehler: " + e.getMessage());
            }
        });
    }
    /**
     * Resolves the directory used to store downloaded BroStore APKs.
     *
     * @return directory handle, creating the folder if necessary.
     * @throws SecurityException if external storage cannot be accessed.
     */
    public static File getDownloadDir() {
        File base = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File dir = new File(base, Constants.DOWNLOAD_APK_DIR);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new SecurityException("Unable to create download directory");
        }
        return dir;
    }

    private static void enqueueDownload(Context context, Uri uri) {
        try {
            String fileName = uri.getLastPathSegment();
            getDownloadDir();

            DownloadManager.Request request = new DownloadManager.Request(uri);
            request.setTitle("BroStore Update");
            request.setDescription("Lade Update herunter...");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,
                    Constants.DOWNLOAD_APK_DIR + "/" + fileName);

            DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            dm.enqueue(request);
        } catch (Exception e) {
            Log.e("UpdateManager", "Download Fehler: " + e.getMessage());
        }
    }

    private static void enqueueDownloadAndInstall(Context context, Uri uri, Runnable onFinished) {
        try {
            String fileName = uri.getLastPathSegment();
            File file = new File(getDownloadDir(), fileName);

            DownloadManager.Request request = new DownloadManager.Request(uri);
            request.setTitle("BroStore Update");
            request.setDescription("Lade Update herunter...");
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,
                    Constants.DOWNLOAD_APK_DIR + "/" + fileName);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            long downloadId = dm.enqueue(request);

            Context appContext = context.getApplicationContext();
            BroadcastReceiver receiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context ctx, Intent intent) {
                    long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                    if (id == downloadId) {
                        appContext.unregisterReceiver(this);
                        ApkInstaller.installApk(appContext, file);
                        if (onFinished != null) {
                            new android.os.Handler(android.os.Looper.getMainLooper()).post(onFinished);
                        }
                    }
                }
            };
            IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.registerReceiver(appContext, receiver, filter, ContextCompat.RECEIVER_EXPORTED);
            } else {
                appContext.registerReceiver(receiver, filter);
            }
        } catch (Exception e) {
            Log.e("UpdateManager", "Download Fehler: " + e.getMessage());
        }
    }
}