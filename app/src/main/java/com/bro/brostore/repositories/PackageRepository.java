package com.example.appstoredemo.repositories;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;

import java.security.MessageDigest;

/**
 * Provides information about installed packages such as version and signing digest.
 */
public class PackageRepository {
    private final PackageManager pm;

    /**
     * Creates a repository backed by the supplied context's {@link PackageManager}.
     *
     * @param context Android context used to resolve package information.
     * @throws IllegalArgumentException if {@code context} is {@code null}.
     */
    public PackageRepository(@NonNull Context context) {
        if (context == null) {
            throw new IllegalArgumentException("Context must not be null");
        }
        pm = context.getPackageManager();
    }

    /**
     * Determines whether the given package is installed on the device.
     *
     * @param pkg package name to inspect.
     * @return {@code true} if the package is installed, otherwise {@code false}.
     * @throws IllegalArgumentException if {@code pkg} is empty.
     */
    public boolean isInstalled(@NonNull String pkg) {
        if (pkg.isEmpty()) {
            throw new IllegalArgumentException("Package name must not be empty");
        }
        try {
            pm.getPackageInfo(pkg, 0);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Retrieves the installed version code of a package.
     *
     * @param pkg package name to inspect.
     * @return the version code or {@code -1} if unavailable.
     * @throws IllegalArgumentException if {@code pkg} is empty.
     */
    public long getVersionCode(@NonNull String pkg) {
        if (pkg.isEmpty()) {
            throw new IllegalArgumentException("Package name must not be empty");
        }
        try {
            PackageInfo info = pm.getPackageInfo(pkg, 0);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                return info.getLongVersionCode();
            }
            return info.versionCode;
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * Computes the SHA-256 digest of the first signing certificate for the package.
     *
     * @param pkg package name to inspect.
     * @return hexadecimal digest string or {@code null} if unavailable.
     * @throws IllegalArgumentException if {@code pkg} is empty.
     */
    public String getSignatureDigest(@NonNull String pkg) {
        if (pkg.isEmpty()) {
            throw new IllegalArgumentException("Package name must not be empty");
        }
        try {
            PackageInfo info;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                info = pm.getPackageInfo(pkg, PackageManager.GET_SIGNING_CERTIFICATES);
                if (info.signingInfo != null && info.signingInfo.getApkContentsSigners().length > 0) {
                    byte[] cert = info.signingInfo.getApkContentsSigners()[0].toByteArray();
                    MessageDigest md = MessageDigest.getInstance("SHA-256");
                    byte[] digest = md.digest(cert);
                    StringBuilder sb = new StringBuilder();
                    for (byte b : digest) sb.append(String.format("%02x", b));
                    return sb.toString();
                }
            } else {
                info = pm.getPackageInfo(pkg, PackageManager.GET_SIGNATURES);
                if (info.signatures != null && info.signatures.length > 0) {
                    byte[] cert = info.signatures[0].toByteArray();
                    MessageDigest md = MessageDigest.getInstance("SHA-256");
                    byte[] digest = md.digest(cert);
                    StringBuilder sb = new StringBuilder();
                    for (byte b : digest) sb.append(String.format("%02x", b));
                    return sb.toString();
                }
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }
}
