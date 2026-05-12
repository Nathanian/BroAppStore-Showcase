package com.example.appstoredemo.di;

import android.content.Context;

import com.example.appstoredemo.managers.RemoteAppManager;
import com.example.appstoredemo.network.BrostoreService;
import com.example.appstoredemo.repositories.CatalogRepository;
import com.example.appstoredemo.repositories.DownloadRepository;
import com.example.appstoredemo.utils.Constants;

import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Hilt module wiring network and repository dependencies.
 */
@Module
@InstallIn(SingletonComponent.class)
public class AppModule {

    @Provides
    @Singleton
    /**
     * Supplies a preconfigured {@link OkHttpClient} for API calls.
     *
     * @return configured {@link OkHttpClient} instance.
     * @throws IllegalStateException if the client cannot be built.
     */
    public static OkHttpClient provideOkHttpClient() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BASIC);
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(3, TimeUnit.SECONDS)
                .readTimeout(3, TimeUnit.SECONDS)
                .addInterceptor(logging)
                .build();
        if (client == null) {
            throw new IllegalStateException("Failed to build OkHttpClient");
        }
        return client;
    }

    @Provides
    @Singleton
    /**
     * Creates the shared {@link Retrofit} instance.
     *
     * @param client HTTP client configured for API requests.
     * @return configured {@link Retrofit} instance.
     * @throws IllegalArgumentException if {@code client} is {@code null}.
     */
    public static Retrofit provideRetrofit(OkHttpClient client) {
        if (client == null) {
            throw new IllegalArgumentException("OkHttpClient must not be null");
        }
        return new Retrofit.Builder()
                .baseUrl(Constants.BASE_API_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build();
    }

    @Provides
    @Singleton
    /**
     * Creates the Retrofit-backed {@link BrostoreService}.
     *
     * @param retrofit retrofit instance to bind the service to.
     * @return implementation of {@link BrostoreService}.
     * @throws IllegalArgumentException if {@code retrofit} is {@code null}.
     */
    public static BrostoreService provideBrostoreService(Retrofit retrofit) {
        if (retrofit == null) {
            throw new IllegalArgumentException("Retrofit must not be null");
        }
        return retrofit.create(BrostoreService.class);
    }

    @Provides
    @Singleton
    /**
     * Provides a singleton {@link RemoteAppManager} instance.
     *
     * @param service API service used to load the remote catalog.
     * @return configured {@link RemoteAppManager}.
     * @throws IllegalArgumentException if {@code service} is {@code null}.
     */
    public static RemoteAppManager provideRemoteAppManager(BrostoreService service) {
        return new RemoteAppManager(service);
    }

    @Provides
    @Singleton
    /**
     * Provides the catalog repository for view models.
     *
     * @param manager remote manager dependency.
     * @return {@link CatalogRepository} instance.
     * @throws IllegalArgumentException if {@code manager} is {@code null}.
     */
    public static CatalogRepository provideCatalogRepository(RemoteAppManager manager) {
        return new CatalogRepository(manager);
    }

    @Provides
    @Singleton
    /**
     * Provides the download repository used throughout the app.
     *
     * @param context application context required for system access.
     * @param service API service used to request signed URLs.
     * @return {@link DownloadRepository} instance.
     * @throws IllegalArgumentException if any dependency is {@code null}.
     */
    public static DownloadRepository provideDownloadRepository(@ApplicationContext Context context,
                                                              BrostoreService service) {
        if (context == null || service == null) {
            throw new IllegalArgumentException("Dependencies must not be null");
        }
        return new DownloadRepository(context, service);
    }
}

