package com.sameerasw.moview.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.sameerasw.moview.R
import com.sameerasw.moview.utils.ImageLoader

@Composable
fun MoviePoster(
    posterUrl: String?,
    modifier: Modifier = Modifier,
    contentAlpha: Float = 1f
) {
    val bitmap = ImageLoader.loadPosterImage(posterUrl)

    Card(
        modifier = modifier
            .width(120.dp)
            .height(180.dp)
            .clip(RoundedCornerShape(8.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Movie Poster",
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(contentAlpha),
                    contentScale = ContentScale.Crop
                )
            } else {
                // Placeholder image
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = "No Poster Available",
                    modifier = Modifier
                        .size(64.dp)
                        .align(Alignment.Center)
                        .alpha(0.5f)
                )
            }
        }
    }
}

// personal notes - https://sameerasw.notion.site/Images-fetching-in-Jetpack-w-o-3rd-party-libs-1e99c6099d40801ebf49f65dd50d9d02?pvs=4

// Out of scope references helped with fetchign and displaying images:
// BitmapFactory: https://developer.android.com/reference/android/graphics/BitmapFactory
// ImageView.setImageBitmap(): https://developer.android.com/reference/android/widget/ImageView#setImageBitmap(android.graphics.Bitmap)
// https://stackoverflow.com/questions/2471935/how-to-load-an-imageview-by-url-in-android
