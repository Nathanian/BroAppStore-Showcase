package com.example.appstoredemo.models;

/**
 * Represents the per item state for downloads in BroStore.
 */
public enum DownloadState {
    IDLE,
    DOWNLOADING,
    VERIFYING,
    INSTALL_PROMPT,
    INSTALLED,
    FAILED
}
