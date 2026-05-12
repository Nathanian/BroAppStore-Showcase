package com.example.appstoredemo.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.example.appstoredemo.models.RemoteAppInfo;
import com.example.appstoredemo.repositories.CatalogRepository;
import com.example.appstoredemo.utils.AppExecutors;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * View model powering the discover list of remote applications.
 */
@HiltViewModel
public class DiscoverViewModel extends AndroidViewModel {

    private final MutableLiveData<List<RemoteAppInfo>> apps = new MutableLiveData<>(new ArrayList<>());
    private final CatalogRepository repository;
    private final Object cacheLock = new Object();
    private final List<RemoteAppInfo> catalogCache = new ArrayList<>();
    private final Observer<List<RemoteAppInfo>> catalogObserver = list -> {
        synchronized (cacheLock) {
            catalogCache.clear();
            if (list != null) {
                catalogCache.addAll(list);
            }
        }
        recompute();
    };

    @Inject
    /**
     * Constructs the view model and begins observing the catalog repository.
     *
     * @param application Android application context.
     * @param repository repository providing remote app metadata.
     * @throws IllegalArgumentException if {@code repository} is {@code null}.
     */
    public DiscoverViewModel(@NonNull Application application, @NonNull CatalogRepository repository) {
        super(application);
        if (repository == null) {
            throw new IllegalArgumentException("CatalogRepository must not be null");
        }
        this.repository = repository;
        this.repository.getCatalog().observeForever(catalogObserver);
        load();
    }

    /**
     * Exposes the filtered list of discoverable apps.
     *
     * @return live data stream of remote apps.
     * @throws IllegalStateException if the view model has been cleared.
     */
    public LiveData<List<RemoteAppInfo>> getApps() {
        if (apps == null) {
            throw new IllegalStateException("Apps LiveData not initialized");
        }
        return apps;
    }

    /**
     * Requests the latest catalog from the repository.
     *
     * @return void.
     * @throws IllegalStateException if the repository rejects the refresh.
     */
    public void load() {
        repository.refresh();
    }

    /**
     * Recomputes the filtered list to exclude already installed packages.
     *
     * @return void.
     * @throws IllegalStateException if background execution fails.
     */
    public void refreshInstalledState() {
        recompute();
    }

    @Override
    protected void onCleared() {
        repository.getCatalog().removeObserver(catalogObserver);
        super.onCleared();
    }

    private void recompute() {
        final List<RemoteAppInfo> snapshot;
        synchronized (cacheLock) {
            snapshot = new ArrayList<>(catalogCache);
        }
        AppExecutors.io().execute(() -> {
            if (snapshot.isEmpty()) {
                apps.postValue(new ArrayList<>());
                return;
            }
            var pm = getApplication().getPackageManager();
            List<RemoteAppInfo> filtered = new ArrayList<>();
            for (RemoteAppInfo info : snapshot) {
                if (info == null || info.packageName == null) {
                    filtered.add(info);
                    continue;
                }
                try {
                    pm.getPackageInfo(info.packageName, 0);
                } catch (Exception e) {
                    filtered.add(info); // not installed or not visible
                }
            }
            apps.postValue(filtered);
        });
    }
}