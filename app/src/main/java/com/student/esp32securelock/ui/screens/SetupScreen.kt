package com.student.esp32securelock.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.student.esp32securelock.ui.components.SecondaryButton
import com.student.esp32securelock.ui.components.errorMessageFor

@Composable
fun SetupScreen(
    viewModel: AppViewModel,
    onSetupComplete: () -> Unit
) {
    var passcode by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    val ui by viewModel.ui.collectAsState()

    // Once the state flips to Unlocked, navigate to main.
    LaunchedEffect(ui) {
        // No-op; navigation is driven by the auth state observed in MainActivity
    }

    ScreenContainer {
        Text(
            text = stringResource(R.string.welcome_title),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics { testTag = "title_setup" }
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.welcome_subtitle),
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(24.dp))
        PasscodeField(
            value = passcode,
            onValueChange = { passcode = it; viewModel.clearError() },
            label = stringResource(R.string.create_passcode)
        )
        Spacer(Modifier.height(12.dp))
        PasscodeField(
            value = confirm,
            onValueChange = { confirm = it; viewModel.clearError() },
            label = stringResource(R.string.confirm_passcode)
        )
        ErrorText(errorMessageFor(ui.error))
        Spacer(Modifier.height(20.dp))
        PrimaryButton(
            text = stringResource(R.string.set_passcode),
            loading = ui.loading,
            onClick = { viewModel.setPasscode(passcode, confirm) { onSetupComplete() } }
        )
    }
}
