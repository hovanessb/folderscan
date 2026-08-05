package com.leonell.android.composefolderscanner

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.SvgDecoder
import com.leonell.android.composefolderscanner.services.WebApi
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import javax.inject.Provider

@HiltAndroidApp
class MainApplication : Application(), ImageLoaderFactory, Configuration.Provider {

   @Inject
   lateinit var workerFactory: HiltWorkerFactory

   // Provider, not a direct injection: Coil asks for the loader during startup, and WebApi
   // pulls in the credential store, which should not be built before it is needed.
   @Inject
   lateinit var webApi: Provider<WebApi>

   /**
    * Coil needs an SVG-capable loader for the profile avatars, and it shares the app's
    * authenticated OkHttp client so photo endpoints get the same Basic auth as the REST
    * calls.
    */
   override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
      .okHttpClient { webApi.get().httpClient }
      .components { add(SvgDecoder.Factory()) }
      .build()

   override val workManagerConfiguration: Configuration
      get() = Configuration.Builder()
         .setWorkerFactory(workerFactory)
         .setMinimumLoggingLevel(Log.DEBUG)
         .build()
}
