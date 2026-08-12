package com.student.esp32securelock.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.student.esp32securelock.ui.components.SecondaryButton
import com.student.esp32securelock.ui.components.errorMessageFor

@Composable
fun MainScreen(
    viewModel: AppViewModel,
    onLock: () -> Unit
) {
    val ui by viewModel.ui.collectAsState()

    var showChange by remember {
        mutableStateOf(false)
    }

    var showRemove by remember {
        mutableStateOf(false)
    }

    ScreenContainer {

        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        StatusCard(active = true)

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        Esp32Card(
            status = viewModel.esp32Status(),
            connecting = ui.esp32Connecting,
            error = ui.esp32Error,
            onConnect = {
                viewModel.tryConnectEsp32()
            }
        )

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        PrimaryButton(
            text = stringResource(R.string.change_passcode),
            onClick = {
                showChange = true
            }
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        SecondaryButton(
            text = stringResource(R.string.remove_passcode),
            onClick = {
                showRemove = true
            }
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        SecondaryButton(
            text = stringResource(R.string.lock),
            onClick = onLock
        )
    }

    if (showChange) {
        ChangePasscodeDialog(
            viewModel = viewModel,
            onDismiss = {
                showChange = false
                viewModel.clearError()
            }
        )
    }

    if (showRemove) {
        RemovePasscodeDialog(
            viewModel = viewModel,
            onDismiss = {
                showRemove = false
                viewModel.clearError()
            }
        )
    }
}

@Composable
private fun StatusCard(
    active: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor =
                MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.status),
                style = MaterialTheme.typography.labelLarge
            )

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            Text(
                text = stringResource(
                    if (active) {
                        R.string.status_active
                    } else {
                        R.string.status_inactive
                    }
                ),
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
private fun Esp32Card(
    status: String,
    connecting: Boolean,
    error: String?,
    onConnect: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.esp32_section),
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            Text(
                text = status,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text = stringResource(R.string.esp32_coming_soon),
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            Text(
                text =
                    stringResource(R.string.bluetooth_status) +
                        ": " +
                        (
                            error
                                ?: stringResource(
                                    R.string.bluetooth_unavailable
                                )
                            ),
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Button(
                onClick = onConnect,
                enabled = !connecting,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        testTag = "btn_test_esp32"
                    }
            ) {
                Text(
                    text = if (connecting) {
                        "..."
                    } else {
                        "Test connection"
                    }
                )
            }
        }
    }
}

@Composable
private fun ChangePasscodeDialog(
    viewModel: AppViewModel,
    onDismiss: () -> Unit
) {
    var current by remember {
        mutableStateOf("")
    }

    var newPass by remember {
        mutableStateOf("")
    }

    var confirm by remember {
        mutableStateOf("")
    }

    val ui by viewModel.ui.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,

        title = {
            Text(
                stringResource(R.string.change_passcode)
            )
        },

        text = {
            Column {

                PasscodeField(
                    value = current,
                    onValueChange = {
                        current = it
                        viewModel.clearError()
                    },
                    label = stringResource(
                        R.string.current_passcode
                    )
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                PasscodeField(
                    value = newPass,
                    onValueChange = {
                        newPass = it
                        viewModel.clearError()
                    },
                    label = stringResource(
                        R.string.new_passcode
                    )
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                PasscodeField(
                    value = confirm,
                    onValueChange = {
                        confirm = it
                        viewModel.clearError()
                    },
                    label = stringResource(
                        R.string.confirm_passcode
                    )
                )

                ErrorText(
                    errorMessageFor(ui.error)
                )
            }
        },

        confirmButton = {
            TextButton(
                onClick = {
                    viewModel.changePasscode(
                        current = current,
                        newPasscode = newPass,
                        confirm = confirm
                    ) {
                        onDismiss()
                    }
                },
                modifier = Modifier.semantics {
                    testTag = "btn_save_change"
                }
            ) {
                Text(
                    stringResource(R.string.save)
                )
            }
        },

        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(
                    stringResource(R.string.cancel)
                )
            }
        }
    )
}

@Composable
private fun RemovePasscodeDialog(
    viewModel: AppViewModel,
    onDismiss: () -> Unit
) {
    var current by remember {
        mutableStateOf("")
    }

    var confirmed by remember {
        mutableStateOf(false)
    }

    val ui by viewModel.ui.collectAsState()

    if (!confirmed) {

        AlertDialog(
            onDismissRequest = onDismiss,

            title = {
                Text(
                    stringResource(
                        R.string.confirm_remove_title
                    )
                )
            },

            text = {
                Text(
                    stringResource(
                        R.string.confirm_remove_message
                    )
                )
            },

            confirmButton = {
                TextButton(
                    onClick = {
                        confirmed = true
                        viewModel.clearError()
                    },
                    modifier = Modifier.semantics {
                        testTag = "btn_confirm_remove"
                    }
                ) {
                    Text(
                        stringResource(R.string.confirm)
                    )
                }
            },

            dismissButton = {
                TextButton(
                    onClick = onDismiss
                ) {
                    Text(
                        stringResource(R.string.cancel)
                    )
                }
            }
        )

    } else {

        AlertDialog(
            onDismissRequest = onDismiss,

            title = {
                Text(
                    stringResource(
                        R.string.current_passcode
                    )
                )
            },

            text = {
                Column {

                    PasscodeField(
                        value = current,
                        onValueChange = {
                            current = it
                            viewModel.clearError()
                        },
                        label = stringResource(
                            R.string.current_passcode
                        )
                    )

                    ErrorText(
                        errorMessageFor(ui.error)
                    )
                }
            },

            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.removePasscode(
                            current
                        ) {
                            onDismiss()
                        }
                    },
                    modifier = Modifier.semantics {
                        testTag = "btn_do_remove"
                    }
                ) {
                    Text(
                        stringResource(R.string.remove)
                    )
                }
            },

            dismissButton = {
                TextButton(
                    onClick = onDismiss
                ) {
                    Text(
                        stringResource(R.string.cancel)
                    )
                }
            }
        )
    }
}