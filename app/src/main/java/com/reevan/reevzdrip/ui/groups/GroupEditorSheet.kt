package com.reevan.reevzdrip.ui.groups

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.reevan.reevzdrip.data.PeopleGroup
import com.reevan.reevzdrip.data.isDuplicateGroupName

/**
 * Add or rename one group. A single text field, so a bottom sheet rather than a screen.
 *
 * The duplicate check runs as you type rather than on save, because the failure it prevents is
 * silent: two groups called "Colleagues" look identical in every list the app has, and you would
 * only find out you had picked the wrong one after wearing the outfit.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupEditorSheet(
    key: Long?,
    existing: PeopleGroup?,
    allGroups: List<PeopleGroup>,
    onDismiss: () -> Unit,
    onSave: (name: String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by rememberSaveable(key) { mutableStateOf(existing?.name.orEmpty()) }

    val duplicate = isDuplicateGroupName(name, allGroups, excludingId = existing?.id)
    val canSave = name.isNotBlank() && !duplicate

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
        ) {
            Text(
                text = if (existing == null) "New group" else "Rename group",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "The people who'll see you in an outfit — colleagues, college friends.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                singleLine = true,
                isError = duplicate,
                supportingText = if (duplicate) {
                    { Text("You already have a group called that.") }
                } else {
                    null
                },
                // A hint to the IME only — capitalisation is guaranteed by peopleGroupOf().
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                Button(
                    onClick = { onSave(name) },
                    enabled = canSave,
                    modifier = Modifier.padding(start = 8.dp),
                ) {
                    Text("Save")
                }
            }
        }
    }
}
