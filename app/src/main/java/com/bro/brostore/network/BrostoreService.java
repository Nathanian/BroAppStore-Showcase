package com.example.appstoredemo.network;

import com.example.appstoredemo.models.RemoteAppInfo;
import com.example.appstoredemo.models.SignedUrlResponse;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Retrofit service describing the BroStore backend HTTP API.
 */
public interface BrostoreService {
    /**
     * Requests the full catalog of remotely available applications.
     *
     * @return a {@link Call} that emits the catalog response body when executed.
     * @throws IllegalStateException if Retrofit fails to create or execute the call.
     */
    @GET("getAppCatalog")
    Call<List<RemoteAppInfo>> getAppCatalog();

    /**
     * Requests a signed download URL for the provided file identifier.
     *
     * @param fileName server-side identifier of the APK to download.
     * @return a {@link Call} that resolves to the signed URL payload when executed.
     * @throws IllegalStateException if Retrofit fails to create or execute the call.
     */
    @GET("getSignedUrl")
    Call<SignedUrlResponse> getSignedUrl(@Query("fileName") String fileName);
}
