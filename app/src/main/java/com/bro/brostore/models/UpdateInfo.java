package com.example.appstoredemo.models;

/**
 * Represents metadata for a specific release of a remote application.
 */
public class UpdateInfo {
    public String versionName;
    public int versionCode;
    public String apk_url;
    public String changelog;
    public String date;
    // optional integrity information
    public String sha256;
    public long sizeBytes;
    public String signatureDigest;
}

