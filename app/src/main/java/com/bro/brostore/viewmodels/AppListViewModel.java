package com.example.appstoredemo.viewmodels;

import android.app.Application;
import android.content.pm.PackageInfo;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.example.appstoredemo.models.AppModel;

import com.example.appstoredemo.models.RemoteAppInfo;
import com.example.appstoredemo.models.UpdateInfo;
import com.example.appstoredemo.repositories.CatalogRepository;
import com.example.appstoredemo.utils.AppExecutors;
import com.example.appstoredemo.utils.VersionUtils;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * View model backing the list of installed applications and their update states.
 */
@HiltViewModel
public class AppListViewModel extends AndroidViewModel {

    private final MutableLiveData<List<AppModel>> apps = new MutableLiveData<>(new ArrayList<>());
    private final CatalogRepository repository;
    private final Object cacheLock = new Object();
    private final List<RemoteAppInfo> catalogCache = new ArrayList<>();
    private final Observer<List<RemoteAppInfo>> catalogObserver = remoteApps -> {
        synchronized (cacheLock) {
            catalogCache.clear();
            if (remoteApps != null) {
                catalogCache.addAll(remoteApps);
            }
        }
        recompute();
    };

    @Inject
    /**
     * Constructs the view model and subscribes to the catalog repository.
     *
     * @param application Android application context.
     * @param repository repository providing remote app metadata.
     * @throws IllegalArgumentException if {@code repository} is {@code null}.
     */
    public AppListViewModel(@NonNull Application application, @NonNull CatalogRepository repository) {
        super(application);
        if (repository == null) {
            throw new IllegalArgumentException("CatalogRepository must not be null");
        }
        this.repository = repository;
        this.repository.getCatalog().observeForever(catalogObserver);
        load();
    }

    /**
     * Exposes the currently computed list of installed applications.
     *
     * @return live data stream of installed applications.
     * @throws IllegalStateException if the view model has been cleared.
     */
    public LiveData<List<AppModel>> getApps() {
        if (apps == null) {
            throw new IllegalStateException("Apps LiveData not initialized");
        }
        return apps;
    }

    /**
     * Requests the latest catalog from the repository to refresh installed data.
     *
     * @return void.
     * @throws IllegalStateException if the repository rejects the refresh.
     */
    public void load() {
        repository.refresh();
    }

    /**
     * Recomputes the installed app list using the cached catalog snapshot.
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
            List<AppModel> result = new ArrayList<>();
            if (!snapshot.isEmpty()) {
                var pm = getApplication().getPackageManager();
                Set<String> added = new HashSet<>();
                for (RemoteAppInfo remote : snapshot) {
                    if (remote == null || remote.packageName == null) continue;
                    String pkgName = remote.packageName;
                    if (added.contains(pkgName)) continue;
                    try {
                        PackageInfo pkg = pm.getPackageInfo(pkgName, 0);
                        Drawable icon = pkg.applicationInfo.loadIcon(pm);
                        String label = pkg.applicationInfo.loadLabel(pm).toString();
                        String version = pkg.versionName;
                        AppModel app = new AppModel(icon, label, pkgName, version);
                        UpdateInfo latest = remote.getLatest();
                        if (latest != null &&
                                VersionUtils.compare(latest.versionName, version) > 0) {
                            app.updateInfo = latest;
                        }
                        result.add(app);
                        added.add(pkgName);
                    } catch (Exception ignore) {}
                }
            }
            apps.postValue(result);
        });
    }
}