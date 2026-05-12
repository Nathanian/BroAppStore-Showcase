package com.example.appstoredemo.models;

/**
 * Holds a pair of version strings for comparison.
 */
public class VersionInfo {
    public String currentVersion;
    public String latestVersion;
    /**
     * Indicates whether the current version differs from the latest known version.
     *
     * @return {@code true} when an update is available.
     * @throws IllegalStateException if either version string is {@code null}.
     */
    public boolean isOutdated() {
        if (currentVersion == null || latestVersion == null) {
            throw new IllegalStateException("Version strings must not be null");
        }
        return !currentVersion.equals(latestVersion);
    }
}