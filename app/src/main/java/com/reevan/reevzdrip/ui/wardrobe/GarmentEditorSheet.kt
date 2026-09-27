package com.reevan.reevzdrip.ui.wardrobe

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.reevan.reevzdrip.R
import com.reevan.reevzdrip.data.Garment
import com.reevan.reevzdrip.data.GarmentType
import com.reevan.reevzdrip.data.PhotoStore
import com.reevan.reevzdrip.ui.common.GarmentImage

/**
 * Add or edit one garment. A bottom sheet rather than a screen: it is a short form, and a sheet
 * keeps the keyboard and the Save button in the same thumb's reach.
 *
 * Field order follows the user's own description of the flow — name, then photo, then type.
 *
 * Nothing is written to disk while the sheet is open. A picked image is held as a `content://` Uri
 * and only imported if the user saves, so abandoning the sheet cannot leave an orphaned file in
 * the photo directory.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GarmentEditorSheet(
    key: Long?,
    existing: Garment?,
    onDismiss: () -> Unit,
    onSave: (name: String, type: GarmentType, photo: PhotoEdit) -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val photoStore = remember { PhotoStore(context.applicationContext) }

    // Saveable so the phone flipping to dark at sunset cannot discard a half-filled form; keyed on
    // the garment so opening a different one starts from that garment rather than the last.
    var name by rememberSaveable(key) { mutableStateOf(existing?.name.orEmpty()) }
    var type by rememberSaveable(key) { mutableStateOf(existing?.type ?: GarmentType.TOP) }

    // The photo edit, as two saveable primitives rather than a sealed class, because Bundle can
    // carry a String and a Boolean without a custom Saver.
    var pickedUri by rememberSaveable(key) { mutableStateOf<String?>(null) }
    var photoCleared by rememberSaveable(key) { mutableStateOf(false) }

    // Where the camera will write. Held across the launch because the callback only reports
    // success, not the location.
    var cameraTarget by rememberSaveable(key) { mutableStateOf<String?>(null) }

    val pickPhoto = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            pickedUri = uri.toString()
            photoCleared = false
        }
    }

    val takePhoto = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { saved ->
        // False means the user backed out of the camera; the target file is then empty rubbish
        // that clearCameraCache() sweeps on the next successful save.
        if (saved) {
            pickedUri = cameraTarget
            photoCleared = false
        }
    }

    val currentPhoto: String? = when {
        photoCleared -> null
        pickedUri != null -> null // shown from the Uri instead
        else -> existing?.photoName
    }
    val hasPhoto = pickedUri != null || (!photoCleared && existing?.photoName != null)

    // The only rule: a garment needs a name. Type always has a value, and the photo is optional.
    val canSave = name.isNotBlank()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
        ) {
            Text(
                text = if (existing == null) "Add clothing" else "Edit clothing",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                singleLine = true,
                // A hint to the IME only — capitalisation is actually guaranteed by garmentOf().
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
            )

            Text(
                text = "Photo (optional)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 20.dp, bottom = 8.dp),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                PhotoPreview(
                    pickedUri = pickedUri,
                    storedPhotoName = currentPhoto,
                )
                Column(modifier = Modifier.padding(start = 16.dp)) {
                    OutlinedButton(
                        onClick = {
                            val target = photoStore.newCameraTarget()
                            cameraTarget = target.toString()
                            takePhoto.launch(target)
                        },
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_camera),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Text("Camera", modifier = Modifier.padding(start = 8.dp))
                    }
                    OutlinedButton(
                        onClick = {
                            pickPhoto.launch(
                                PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly,
                                ),
                            )
                        },
                        modifier = Modifier.padding(top = 8.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_gallery),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Text("Gallery", modifier = Modifier.padding(start = 8.dp))
                    }
                    if (hasPhoto) {
                        TextButton(
                            onClick = {
                                pickedUri = null
                                cameraTarget = null
                                photoCleared = true
                            },
                        ) {
                            Text("Remove photo")
                        }
                    }
                }
            }

            Text(
                text = "Type",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 20.dp, bottom = 8.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GarmentType.entries.forEach { option ->
                    FilterChip(
                        selected = type == option,
                        onClick = { type = option },
                        label = { Text(option.label) },
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 28.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (onDelete != null) {
                    TextButton(onClick = onDelete) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                }
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Button(
                        onClick = {
                            val edit = when {
                                pickedUri != null -> PhotoEdit.Replaced(Uri.parse(pickedUri))
                                photoCleared -> PhotoEdit.Removed
                                else -> PhotoEdit.Unchanged
                            }
                            onSave(name, type, edit)
                        },
                        enabled = canSave,
                        modifier = Modifier.padding(start = 8.dp),
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

/**
 * The square preview beside the photo buttons.
 *
 * A freshly picked image is shown straight from its `content://` Uri, because it has not been
 * imported yet and has no filename to look up — the import only happens on save.
 */
@Composable
private fun PhotoPreview(
    pickedUri: String?,
    storedPhotoName: String?,
) {
    val shape = RoundedCornerShape(12.dp)
    val boxModifier = Modifier
        .size(96.dp)
        .clip(shape)
        .background(MaterialTheme.colorScheme.surfaceVariant)

    if (pickedUri != null) {
        AsyncImage(
            model = pickedUri,
            contentDescription = "Selected photo",
            contentScale = ContentScale.Crop,
            modifier = boxModifier,
        )
    } else {
        Box(modifier = boxModifier, contentAlignment = Alignment.Center) {
            GarmentImage(
                photoName = storedPhotoName,
                contentDescription = if (storedPhotoName != null) "Current photo" else null,
                modifier = Modifier.size(96.dp),
                placeholderIcon = 24.dp,
            )
        }
    }
}
