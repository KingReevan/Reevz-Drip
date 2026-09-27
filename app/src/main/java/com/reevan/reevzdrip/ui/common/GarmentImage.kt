package com.reevan.reevzdrip.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.reevan.reevzdrip.R
import com.reevan.reevzdrip.data.garmentPhotoFile

/**
 * Renders a garment's photo, or a placeholder when there is not one.
 *
 * **This is the single place a garment image is drawn.** The card uses it, the editor preview uses
 * it, and the combination collage will use it in Phase 3 — so photo behaviour is changed in one
 * function rather than in each screen that happens to show a garment.
 *
 * The placeholder is drawn *underneath* the photo rather than in a loading/error slot, and the
 * structure is worth keeping:
 *
 * - [AsyncImage] with no `error` painter draws nothing when a file is missing or corrupt, so the
 *   placeholder simply shows through. No state to observe, no branch to get wrong.
 * - It avoids `SubcomposeAsyncImage`, whose slot content inherits the image's `contentScale` —
 *   which blew the 28dp placeholder icon up to fill the whole card — and which subcomposes on
 *   every item, which is exactly what you do not want in a scrolling grid.
 *
 * [photoName] being null is an ordinary, permanent state: the user asked for photos to be
 * optional. A *missing file* behind a non-null name should not happen, but a restored backup or an
 * interrupted delete could produce one, so it degrades to the same placeholder rather than an
 * error box or a crash.
 */
@Composable
fun GarmentImage(
    photoName: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    placeholderIcon: Dp = 28.dp,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Image(
            painter = painterResource(R.drawable.ic_photo),
            contentDescription = null,
            modifier = Modifier.size(placeholderIcon),
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.outline),
        )

        if (photoName != null) {
            val context = LocalContext.current
            val file = remember(photoName) { garmentPhotoFile(context, photoName) }
            AsyncImage(
                model = file,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
