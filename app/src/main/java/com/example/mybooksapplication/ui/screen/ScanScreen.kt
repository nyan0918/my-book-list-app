package com.example.mybooksapplication.ui.screen

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.mybooksapplication.ui.components.BarcodeScanner
import com.example.mybooksapplication.ui.components.CameraPermissionWrapper
import com.example.mybooksapplication.ui.viewmodel.BookViewModel
import com.example.mybooksapplication.ui.viewmodel.ScanUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    viewModel: BookViewModel,
    onBack: () -> Unit
) {
    val scanState by viewModel.scanState.collectAsState()
    var showSheet by remember { mutableStateOf(false) }

    // スキャン成功/失敗を検知してシートを開く
    LaunchedEffect(scanState) {
        if (scanState !is ScanUiState.Idle) {
            showSheet = true
        }
    }

    CameraPermissionWrapper {
        Box(Modifier.fillMaxSize()) {
            BarcodeScanner(onScan = { isbn -> viewModel.onIsbnScanned(isbn) })

            // 結果表示用モーダルシート
            if (showSheet) {
                ModalBottomSheet(onDismissRequest = {
                    showSheet = false
                    viewModel.resetScanState()
                }) {
                    ScanResultContent(
                        state = scanState,
                        onSave = {
                            viewModel.saveCurrentBook()
                            showSheet = false
                            onBack()
                        },
                        onRetry = {
                            showSheet = false
                            viewModel.resetScanState()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ScanResultContent(
    state: ScanUiState,
    onSave: () -> Unit,
    onRetry: () -> Unit
) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .heightIn(min = 200.dp),
        contentAlignment = Alignment.Center
    ) {
        when (state) {
            is ScanUiState.Loading -> CircularProgressIndicator()

            is ScanUiState.Error -> {
                ScanErrorContent(
                    state = state,
                    onRetry = onRetry
                )
            }

            is ScanUiState.Success -> {
                ScanSuccessContent(
                    state = state,
                    onSave = onSave
                )
            }

            else -> {}
        }
    }
}

@Composable
fun ScanErrorContent(
    state: ScanUiState.Error,
    onRetry: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(48.dp)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(state.message),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(16.dp))

        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            )
        ) {
            Text("再読み取り")
        }
    }
}

@Composable
fun ScanSuccessContent(
    state: ScanUiState.Success,
    onSave: () -> Unit
) {
    SideEffect {
        Log.d("ImageDebug", "Title: ${state.book.title}, URL: [${state.book.coverUrl}]")
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        AsyncImage(
            model = state.book.coverUrl,
            contentDescription = null,
            modifier = Modifier.size(100.dp)
        )
        Text(state.book.title, style = MaterialTheme.typography.titleMedium)
        Text(state.book.author)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onSave) { Text("保存する") }
    }
}
