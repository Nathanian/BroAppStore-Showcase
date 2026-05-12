package com.example.appstoredemo.managers;

import android.util.Log;

import com.example.appstoredemo.models.RemoteAppInfo;
import com.example.appstoredemo.network.BrostoreService;
import com.example.appstoredemo.utils.Result;

import java.util.List;

import retrofit2.Response;

/**
 * Handles loading of remote app information using Retrofit.
 */
public class RemoteAppManager {
    private final BrostoreService service;

    /**
     * Creates a new manager wrapping the provided network service.
     *
     * @param service Retrofit implementation of the backend API.
     * @throws IllegalArgumentException if {@code service} is {@code null}.
     */
    public RemoteAppManager(BrostoreService service) {
        if (service == null) {
            throw new IllegalArgumentException("BrostoreService must not be null");
        }
        this.service = service;
    }

    /**
     * Loads the remote catalog synchronously on the current thread.
     * <p>
     * Example usage:
     * <pre>{@code
     * RemoteAppManager manager = new RemoteAppManager(service);
     * Result<List<RemoteAppInfo>> result = manager.fetchRemoteApps();
     * if (result.isSuccess()) {
     *     List<RemoteAppInfo> apps = result.getData();
     *     // Present apps to the user
     * }
     * }</pre>
     *
     * @return a {@link Result} containing the retrieved catalog data or an error message.
     * @throws IllegalStateException if the underlying network call fails unexpectedly.
     */
    public Result<List<RemoteAppInfo>> fetchRemoteApps() {
        try {
            Response<List<RemoteAppInfo>> response = service.getAppCatalog().execute();
            if (response == null) {
                throw new IllegalStateException("Null response from BrostoreService");
            }
            if (response.isSuccessful() && response.body() != null) {
                return Result.success(response.body());
            } else {
                return Result.error("Server error: " + (response != null ? response.code() : "unknown"));
            }
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            Log.e("RemoteAppManager", "fetch error " + e.getMessage());
            return Result.error(e.getMessage());
        }
    }
}
