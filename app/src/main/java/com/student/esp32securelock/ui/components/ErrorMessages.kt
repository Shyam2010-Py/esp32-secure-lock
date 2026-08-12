package com.student.esp32securelock.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.student.esp32securelock.R
import com.student.esp32securelock.ui.AppViewModel

/**
 * Maps a ViewModel [AppViewModel.ErrorKind] to the exact user-facing message.
 * Each branch returns its own dedicated string — no fallbacks that would
 * mislabel a "non-numeric" error as "empty", etc.
 */
@Composable
fun errorMessageFor(kind: AppViewModel.ErrorKind?): String? {
    if (kind == null) return null
    return when (kind) {
        AppViewModel.ErrorKind.EMPTY -> stringResource(R.string.error_empty)
        AppViewModel.ErrorKind.TOO_SHORT -> stringResource(R.string.error_too_short)
        AppViewModel.ErrorKind.TOO_LONG -> stringResource(R.string.error_too_long)
        AppViewModel.ErrorKind.NOT_NUMERIC -> stringResource(R.string.error_not_numeric)
        AppViewModel.ErrorKind.MISMATCH -> stringResource(R.string.error_mismatch)
        AppViewModel.ErrorKind.INCORRECT -> stringResource(R.string.error_incorrect)
        AppViewModel.ErrorKind.STORAGE -> stringResource(R.string.error_storage)
        AppViewModel.ErrorKind.UNEXPECTED -> stringResource(R.string.error_unexpected)
    }
}
