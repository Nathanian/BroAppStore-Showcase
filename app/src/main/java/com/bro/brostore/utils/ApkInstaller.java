package com.example.appstoredemo.utils;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import androidx.core.content.FileProvider;

import java.io.File;

/**
 * Utility for launching APK installation intents on the main thread.
 */
public class ApkInstaller {

    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    /**
     * Requests installation of the provided APK file.
     *
     * @param context Android context used to start the installation activity.
     * @param apkFile file to install.
     * @return void.
     * @throws IllegalArgumentException if {@code context} or {@code apkFile} are {@code null}.
     */
    public static void installApk(Context context, File apkFile) {
        if (context == null || apkFile == null) {
            throw new IllegalArgumentException("Context and APK file must not be null");
        }

        Uri apkUri = FileProvider.getUriForFile(context, "com.example.appstoredemo.fileprovider", apkFile);
        Intent intent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
        intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        if (isPackageInstalled(context, "com.estrongs.android.pop")) {
            intent.setPackage("com.estrongs.android.pop");
        }
        runOnMain(() -> startInstallActivity(context, intent));
    }

    private static void startInstallActivity(Context context, Intent intent) {
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            intent.setPackage(null);
            try {
                context.startActivity(intent);
            } catch (ActivityNotFoundException ex) {
                showToast(context, "No application found to install APK");
            }
        }
    }

    private static void showToast(Context context, String message) {
        runOnMain(() -> Toast.makeText(context, message, Toast.LENGTH_SHORT).show());
    }

    private static void runOnMain(Runnable runnable) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            runnable.run();
        } else {
            MAIN_HANDLER.post(runnable);
        }
    }

    private static boolean isPackageInstalled(Context context, String packageName) {
        try {
            context.getPackageManager().getPackageInfo(packageName, 0);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}

