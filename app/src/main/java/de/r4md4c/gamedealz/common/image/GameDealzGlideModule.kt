package de.r4md4c.gamedealz.common.image

import android.content.Context
import android.util.Log
import com.bumptech.glide.Glide
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.Excludes
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.integration.okhttp3.OkHttpLibraryGlideModule
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.module.AppGlideModule
import de.r4md4c.gamedealz.BuildConfig
import okhttp3.OkHttpClient
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import java.io.InputStream

@GlideModule
@Excludes(value = [OkHttpLibraryGlideModule::class])
class GameDealzGlideModule : AppGlideModule(), KoinComponent {

    private val okHttpClient: OkHttpClient by inject()

    override fun isManifestParsingEnabled(): Boolean = false

    override fun applyOptions(context: Context, builder: GlideBuilder) {
        builder.setLogLevel(if (BuildConfig.DEBUG) Log.INFO else Log.ERROR)
    }

    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        registry.replace(GlideUrl::class.java, InputStream::class.java, OkHttpUrlLoader.Factory(okHttpClient))
    }
}