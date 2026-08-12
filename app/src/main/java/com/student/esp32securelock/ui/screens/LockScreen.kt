package com.student.esp32securelock.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.unit.dp
import com.student.esp32securelock.R
import com.student.esp32securelock.ui.AppViewModel
import com.student.esp32securelock.ui.components.ErrorText
import com.student.esp32securelock.ui.components.PasscodeField
import com.student.esp32securelock.ui.components.PrimaryButton
import com.student.esp32securelock.ui.components.ScreenContainer
import com.student.esp32securelock.ui.components.errorMessageFor

@Composable
fun LockScreen(
    viewModel: AppViewModel,
    onUnlocked: () -> Unit
) {
    var passcode by remember { mutableStateOf("") }
    val ui by viewModel.ui.collectAsState()

    ScreenContainer {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics { testTag = "title_lock" }
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.enter_passcode),
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(24.dp))
        PasscodeField(
            value = passcode,
            onValueChange = { passcode = it; viewModel.clearError() },
            label = stringResource(R.string.enter_passcode)
        )
        ErrorText(errorMessageFor(ui.error))
        Spacer(Modifier.height(20.dp))
        PrimaryButton(
            text = stringResource(R.string.unlock),
            loading = ui.loading,
            onClick = { viewModel.unlock(passcode) { onUnlocked() } }
        )
    }
}
