package com.sameerasw.moview.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

object ImageLoader {

    // Download bitmap from URL
    suspend fun downloadBitmap(url: String): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.doInput = true
                connection.connect()
                val inputStream = connection.inputStream
                BitmapFactory.decodeStream(inputStream) // decode with BitmapFactory
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    @Composable
    fun loadPosterImage(posterUrl: String?): Bitmap? {
        var bitmap by remember { mutableStateOf<Bitmap?>(null) }

        LaunchedEffect(posterUrl) {
            if (!posterUrl.isNullOrEmpty() && posterUrl != "N/A") {
                bitmap = downloadBitmap(posterUrl)
            }
        }

        return bitmap
    }
}


// Out of scope references helped with fetchign and displaying images:
// BitmapFactory: https://developer.android.com/reference/android/graphics/BitmapFactory
// ImageView.setImageBitmap(): https://developer.android.com/reference/android/widget/ImageView#setImageBitmap(android.graphics.Bitmap)
// https://stackoverflow.com/questions/2471935/how-to-load-an-imageview-by-url-in-android
