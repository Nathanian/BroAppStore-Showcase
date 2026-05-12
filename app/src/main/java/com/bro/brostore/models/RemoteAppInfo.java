package com.example.appstoredemo.models;
import com.google.gson.annotations.SerializedName;
import com.example.appstoredemo.utils.VersionUtils;
import java.util.List;

/**
 * Describes a remotely hosted application, including available versions.
 */
public class RemoteAppInfo {
    @SerializedName("package")
    public String packageName;
    public String name;
    public List<UpdateInfo> versions;

    /**
     * Returns the latest update entry based on semantic version comparison.
     *
     * @return most recent {@link UpdateInfo} or {@code null} when unavailable.
     * @throws IllegalStateException if the version list is present but empty.
     */
    public UpdateInfo getLatest() {
        if (versions != null && versions.isEmpty()) {
            throw new IllegalStateException("Versions list must not be empty");
        }
        UpdateInfo latest = null;
        if (versions == null) return null;
        for (UpdateInfo v : versions) {
            if (latest == null || VersionUtils.compare(v.versionName, latest.versionName) > 0) {
                latest = v;
            }
        }
        return latest;
    }
}