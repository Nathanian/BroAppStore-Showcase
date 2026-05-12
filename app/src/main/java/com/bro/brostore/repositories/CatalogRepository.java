package com.example.appstoredemo.repositories;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.appstoredemo.managers.RemoteAppManager;
import com.example.appstoredemo.models.RemoteAppInfo;
import com.example.appstoredemo.utils.AppExecutors;
import com.example.appstoredemo.utils.Result;

import java.util.List;

import javax.inject.Singleton;

/**
 * Thin repository layer around {@link RemoteAppManager} providing asynchronous loading
 * and simple in-memory caching. Network operations are off the main thread.
 */
@Singleton
public class CatalogRepository {
    private final MutableLiveData<List<RemoteAppInfo>> catalog = new MutableLiveData<>();
    private final RemoteAppManager manager;

    /**
     * Creates a repository backed by a {@link RemoteAppManager}.
     *
     * @param manager manager responsible for performing network requests.
     * @throws IllegalArgumentException if {@code manager} is {@code null}.
     */
    public CatalogRepository(@NonNull RemoteAppManager manager) {
        if (manager == null) {
            throw new IllegalArgumentException("RemoteAppManager must not be null");
        }
        this.manager = manager;
    }

    /**
     * Returns a live stream of the cached remote catalog.
     *
     * @return {@link LiveData} emitting the latest catalog snapshot.
     * @throws IllegalStateException if the repository has been cleared.
     */
    public LiveData<List<RemoteAppInfo>> getCatalog() {
        if (catalog == null) {
            throw new IllegalStateException("Catalog LiveData not initialized");
        }
        return catalog;
    }

    /**
     * Reloads the remote catalog asynchronously and updates observers.
     *
     * @return void.
     * @throws IllegalStateException if the executor rejects the background task.
     */
    public void refresh() {
        AppExecutors.io().execute(() -> {
            Result<List<RemoteAppInfo>> res = manager.fetchRemoteApps();
            if (res.isSuccess()) {
                catalog.postValue(res.getData());
            } else {
                Log.e("CatalogRepository", "refresh error " + res.getError());
            }
        });
    }
}
