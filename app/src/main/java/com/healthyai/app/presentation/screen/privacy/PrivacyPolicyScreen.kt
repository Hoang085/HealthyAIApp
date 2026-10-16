package com.healthyai.app.presentation.screen.privacy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.healthyai.app.R
import com.healthyai.app.presentation.ui.theme.HealthyAITheme

/** Màn "Chính sách bảo mật": mở từ tab Phân tích AI và từ yêu cầu hiển thị rationale của Health Connect (nên không nằm sau cổng đăng nhập). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.privacy_title),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                stringResource(R.string.privacy_intro),
                style = MaterialTheme.typography.bodyLarge.copy(color = Color.Gray)
            )

            PolicySection(
                title = stringResource(R.string.privacy_local_title),
                body = stringResource(R.string.privacy_local_body)
            )

            PolicySection(
                title = "2. Health Connect",
                body = stringResource(R.string.health_permissions_rationale) +
                    stringResource(R.string.privacy_health_connect_body)
            )

            PolicySection(
                title = stringResource(R.string.privacy_camera_title),
                body = stringResource(R.string.privacy_camera_body)
            )

            PolicySection(
                title = stringResource(R.string.privacy_cloud_title),
                body = stringResource(R.string.privacy_cloud_body)
            )

            PolicySection(
                title = stringResource(R.string.privacy_no_sale_title),
                body = stringResource(R.string.privacy_no_sale_body)
            )

            PolicySection(
                title = stringResource(R.string.privacy_rights_title),
                body = stringResource(R.string.privacy_rights_body)
            )

            Text(
                stringResource(R.string.privacy_footer),
                style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray, fontSize = 12.sp)
            )
        }
    }
}

@Composable
private fun PolicySection(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PrivacyPolicyPreview() {
    HealthyAITheme {
        PrivacyPolicyScreen(onBack = {})
    }
}
