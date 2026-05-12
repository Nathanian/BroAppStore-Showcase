package com.example.appstoredemo.models;

import android.graphics.drawable.Drawable;

/**
 * Represents an installed application and optional update information.
 */
public class AppModel {
    public Drawable icon;
    public String name;
    public String packageName;
    public String version;
    public UpdateInfo updateInfo;

    /**
     * Creates a model populated with display metadata.
     *
     * @param icon drawable icon for the app.
     * @param name display name of the app.
     * @param packageName unique package identifier.
     * @param version currently installed version string.
     * @throws IllegalArgumentException if {@code packageName} is {@code null} or empty.
     */
    public AppModel(Drawable icon, String name, String packageName, String version) {
        if (packageName == null || packageName.isEmpty()) {
            throw new IllegalArgumentException("Package name must not be empty");
        }
        this.icon = icon;
        this.name = name;
        this.packageName = packageName;
        this.version = version;
    }
}