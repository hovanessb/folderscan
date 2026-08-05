package com.leonell.android.composefolderscanner.services

import android.util.Log
import com.google.gson.GsonBuilder
import com.leonell.android.composefolderscanner.BuildConfig
import com.leonell.android.composefolderscanner.data.settings.SettingsRepository
import com.leonell.android.composefolderscanner.models.BarcodeEntityModel
import com.leonell.android.composefolderscanner.models.BarcodeLookup
import com.leonell.android.composefolderscanner.models.Locations
import com.leonell.android.composefolderscanner.models.SuccessModel
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

private const val ACCEPT_JSON = "Accept: application/json"

interface PersonRestInterface {
   @Headers(ACCEPT_JSON)
   @GET("leonell/server/folder/profile")
   suspend fun getPersonByBarcode(@Query("folderBarcodeId") aBarcodeId: String): BarcodeEntityModel.Person

   @Headers(ACCEPT_JSON)
   @GET("leonell/server/folder/profile")
   suspend fun getPersonByAssociateId(@Query("associateId") aAssociateId: String): BarcodeEntityModel.Person

   @Headers(ACCEPT_JSON)
   @GET("leonell/server/proc/assignment/{aAssociateId}")
   suspend fun getAssignedStaff(@Path("aAssociateId") aAssociateId: Number): List<BarcodeEntityModel.StaffAssignment>

   @Headers(ACCEPT_JSON)
   @GET("leonell/server/search/v2/person/json")
   suspend fun searchPerson(@Query("query") aName: String): List<BarcodeEntityModel.Person>
}

interface FolderLocationsRestInterface {
   @Headers(ACCEPT_JSON)
   @GET("leonell/server/api/folder/locations")
   suspend fun getLocations(): Locations

   @Headers(ACCEPT_JSON)
   @GET("leonell/server/api/folder/log-location")
   fun logBarcode(
      @Query("barcodes") aBarcodeId: String,
      @Query("location") aLocationId: Number,
   ): Call<SuccessModel>

   @Headers(ACCEPT_JSON)
   @GET("leonell/server/folder/resolve")
   fun resolveBarcode(@Query("barcode") aBarcodeId: String): Call<BarcodeLookup>
}

/**
 * Attaches the operator's credentials to every outbound call.
 *
 * Authentication used to live in `@Headers("Authorization: Basic ")` annotations. Those are
 * compile-time constants, so they could never carry a real credential. Doing it here means
 * whatever is entered on the settings screen takes effect immediately.
 */
private class AuthInterceptor(
   private val settings: SettingsRepository,
) : Interceptor {
   override fun intercept(chain: Interceptor.Chain): Response {
      val header = settings.credentials.value.basicAuthHeader()
         ?: return chain.proceed(chain.request())
      val authorized = chain.request().newBuilder()
         .header("Authorization", header)
         .build()
      return chain.proceed(authorized)
   }
}

/** Logs non-2xx responses without swallowing them. */
private class ErrorLoggingInterceptor : Interceptor {
   override fun intercept(chain: Interceptor.Chain): Response {
      val response = chain.proceed(chain.request())
      if (!response.isSuccessful) {
         Log.e("WebApi", "${response.code} ${response.message} for ${chain.request().url}")
      }
      return response
   }
}

/**
 * Builds and caches the Retrofit services.
 *
 * Retrofit fixes its base URL at build time, so the service stack is rebuilt whenever the
 * configured server address changes. The expensive parts -- the OkHttp connection pool and
 * dispatcher -- are shared across rebuilds.
 */
@Singleton
class WebApi @Inject constructor(
   private val settings: SettingsRepository,
) {
   /**
    * Exposed so Coil shares this client: image endpoints sit behind the same Basic auth,
    * and reusing the client means one connection pool for the whole app.
    */
   val httpClient: OkHttpClient = OkHttpClient.Builder()
      .addInterceptor(AuthInterceptor(settings))
      .addInterceptor(ErrorLoggingInterceptor())
      .apply {
         if (BuildConfig.DEBUG) {
            addInterceptor(
               HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC),
            )
         }
      }
      .connectTimeout(15, TimeUnit.SECONDS)
      .readTimeout(30, TimeUnit.SECONDS)
      .build()

   private val gson = GsonBuilder().serializeNulls().setLenient().create()

   private val lock = Any()
   private var cachedBaseUrl: String? = null
   private var cachedRetrofit: Retrofit? = null

   private fun retrofit(): Retrofit = synchronized(lock) {
      val baseUrl = settings.credentials.value.normalizedBaseUrl()
      cachedRetrofit?.takeIf { cachedBaseUrl == baseUrl } ?: Retrofit.Builder()
         .baseUrl(baseUrl)
         .addConverterFactory(GsonConverterFactory.create(gson))
         .client(httpClient)
         .build()
         .also {
            cachedBaseUrl = baseUrl
            cachedRetrofit = it
         }
   }

   fun getPersonWebService(): PersonRestInterface =
      retrofit().create(PersonRestInterface::class.java)

   fun getFolderLocationWebService(): FolderLocationsRestInterface =
      retrofit().create(FolderLocationsRestInterface::class.java)

   /** Resolves [path] against the currently configured server. */
   fun absoluteUrl(path: String): String =
      settings.credentials.value.normalizedBaseUrl() + path.removePrefix("/")
}
