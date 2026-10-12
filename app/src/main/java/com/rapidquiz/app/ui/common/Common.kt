package com.rapidquiz.app.ui.common

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rapidquiz.app.R
import com.rapidquiz.app.data.api.ServerState
import kotlinx.coroutines.delay

val LocalServerState = compositionLocalOf { ServerState().apply { awake = true } }

@StringRes
fun errorMessageRes(code: String): Int = when (code) {
    "network_error" -> R.string.error_network_error
    "rate_limited" -> R.string.error_rate_limited
    "category_unavailable" -> R.string.error_category_unavailable
    "category_not_found" -> R.string.error_category_not_found
    "session_not_found" -> R.string.error_session_not_found
    "session_expired" -> R.string.error_session_expired
    "session_completed" -> R.string.error_session_completed
    "session_not_completed" -> R.string.error_session_not_completed
    "name_already_set" -> R.string.error_name_already_set
    "name_window_closed" -> R.string.error_name_window_closed
    "name_invalid" -> R.string.error_name_invalid
    "validation_error" -> R.string.error_validation_error
    "server_error" -> R.string.error_server_error
    else -> R.string.error_unknown
}

/** Oturumun artık devam ettirilemeyeceği hata kodları. */
fun isSessionGone(code: String) = code == "session_expired" || code == "session_not_found"

@Composable
fun ErrorView(
    code: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(errorMessageRes(code)),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        if (onRetry != null) {
            Spacer(Modifier.height(16.dp))
            Button(onClick = onRetry) { Text(stringResource(R.string.retry)) }
        }
        if (secondaryLabel != null && onSecondary != null) {
            Spacer(Modifier.height(8.dp))
            androidx.compose.material3.TextButton(onClick = onSecondary) { Text(secondaryLabel) }
        }
    }
}

@Composable
fun LoadingView(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
        WakingBanner()
    }
}

/** İstek 3 sn'den uzun sürüyor ve sunucu henüz hiç yanıt vermediyse uyarı gösterir. */
@Composable
fun WakingBanner() {
    val server = LocalServerState.current
    var show by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(3_000)
        show = !server.awake
    }
    if (show && !server.awake) {
        Spacer(Modifier.height(24.dp))
        Text(
            stringResource(R.string.waking_title),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.waking_body),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
fun ProvideServerState(state: ServerState, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalServerState provides state, content = content)
}
