package com.example.mybooksapplication.ui.screen

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    val isBatchMode by viewModel.isBatchMode.collectAsState()
    val scanState by viewModel.scanState.collectAsState()
    val scannedBuffer by viewModel.scannedBuffer.collectAsState()
    var showSingleResultSheet by remember { mutableStateOf(false) }

    // 単体モードかつスキャン処理中にボトムシートを開く
    LaunchedEffect(scanState) {
        if (!isBatchMode && scanState !is ScanUiState.Idle) {
            showSingleResultSheet = true
        }
    }

    CameraPermissionWrapper {
        Box(Modifier.fillMaxSize()) {
            BarcodeScanner(onScan = { isbn -> viewModel.onIsbnScanned(isbn) })

            // モード切替タブ
            ModeSwitchTab(
                isBatchMode = isBatchMode,
                onModeChange = { batch -> viewModel.setBatchMode(batch) },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 40.dp)
            )

            if (isBatchMode) {
                // 一括モード時: 下部に完了バーを表示
                BatchScanBottomBar(
                    count = scannedBuffer.size,
                    onSave = {
                        viewModel.saveBufferedBooks()
                        onBack()
                    },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            } else {
                // 単体モード時: ボトムシート制御
                if (showSingleResultSheet || scanState is ScanUiState.Error) {
                    ModalBottomSheet(onDismissRequest = {
                        showSingleResultSheet = false
                        viewModel.resetScanState()
                    }) {
                        ScanResultContent(
                            state = scanState,
                            onSave = {
                                viewModel.saveCurrentBook()
                                showSingleResultSheet = false
                                onBack()
                            },
                            onRetry = {
                                showSingleResultSheet = false
                                viewModel.resetScanState()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ModeSwitchTab(
    isBatchMode: Boolean,
    onModeChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
            .padding(4.dp)
    ) {
        TabButton(
            text = "単体登録",
            isSelected = !isBatchMode,
            onClick = { onModeChange(false) }
        )
        TabButton(
            text = "一括登録",
            isSelected = isBatchMode,
            onClick = { onModeChange(true) }
        )
    }
}

@Composable
fun TabButton(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else Color.White
        ),
        shape = CircleShape,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        modifier = Modifier.height(40.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun BatchScanBottomBar(count: Int, onSave: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface) // 見やすく
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("一括登録モード", style = MaterialTheme.typography.labelMedium)
                Text("$count 冊スキャン済み", style = MaterialTheme.typography.titleLarge)
            }
            Button(onClick = onSave, enabled = count > 0) {
                Text("完了")
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
            modifier = Modifier.size(120.dp)
        )
        Text(state.book.title, style = MaterialTheme.typography.titleMedium)
        Text(state.book.author)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onSave) { Text("保存する") }
    }
}
