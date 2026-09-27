package com.mf650.manager.ui.screens.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ForwardToInbox
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mf650.manager.data.model.SmsItem
import com.mf650.manager.ui.components.AmbientBackground
import com.mf650.manager.ui.components.LiquidGlassCard
import com.mf650.manager.ui.theme.CyanPrimary
import com.mf650.manager.ui.theme.RiskR3
import com.mf650.manager.ui.viewmodel.SmsViewModel

@Composable
fun MessagesScreen(
    viewModel: SmsViewModel,
    onNavigateToForward: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var showSendDialog by remember { mutableStateOf(false) }
    var targetPhone by remember { mutableStateOf("") }
    var messageBody by remember { mutableStateOf("") }
    var deletingSmsId by remember { mutableStateOf<String?>(null) }

    val currentList = if (uiState.selectedTab == 0) uiState.inboxList else uiState.outboxList

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AmbientBackground()

        Column(modifier = Modifier.fillMaxSize()) {
            // Glass Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "短信中心",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "收发短信、验证码与自动化转发",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onNavigateToForward,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(CyanPrimary.copy(alpha = 0.12f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.ForwardToInbox,
                            contentDescription = "短信转发设置",
                            tint = CyanPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = { viewModel.loadSms() },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "刷新短信",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Liquid Glass Tabs
            TabRow(
                selectedTabIndex = uiState.selectedTab,
                containerColor = Color.Transparent,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[uiState.selectedTab]),
                        color = CyanPrimary,
                        height = 3.dp
                    )
                }
            ) {
                Tab(
                    selected = uiState.selectedTab == 0,
                    onClick = { viewModel.selectTab(0) },
                    text = {
                        Text(
                            "收件箱 (${uiState.inboxList.size})",
                            fontWeight = if (uiState.selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
                Tab(
                    selected = uiState.selectedTab == 1,
                    onClick = { viewModel.selectTab(1) },
                    text = {
                        Text(
                            "已发送 (${uiState.outboxList.size})",
                            fontWeight = if (uiState.selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }

            // Message List Content
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = CyanPrimary)
                }
            } else if (currentList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mail,
                            contentDescription = null,
                            tint = Color.Gray.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = if (uiState.selectedTab == 0) "暂无收到的短信" else "暂无已发送短信",
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(currentList, key = { it.id }) { sms ->
                        ModernSmsCard(
                            sms = sms,
                            onDelete = { deletingSmsId = sms.id },
                            onMarkRead = { if (!sms.isRead) viewModel.markRead(sms.id) }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(72.dp))
                    }
                }
            }
        }

        // Floating Action Button to Compose SMS
        FloatingActionButton(
            onClick = { showSendDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
            containerColor = CyanPrimary,
            contentColor = Color.Black
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null)
                Text("写短信", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }

    // Send SMS Dialog
    if (showSendDialog) {
        AlertDialog(
            onDismissRequest = { showSendDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Send, contentDescription = null, tint = CyanPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("发送蜂窝新短信", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = targetPhone,
                        onValueChange = { targetPhone = it },
                        label = { Text("收信人手机号 / 接入号") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = messageBody,
                        onValueChange = { messageBody = it },
                        label = { Text("短信内容") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 6
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "单条限制 70 字",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                        Text(
                            text = "${messageBody.length} / 70 字",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (messageBody.length > 70) RiskR3 else Color.Gray,
                            fontWeight = if (messageBody.length > 70) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSendDialog = false
                        viewModel.sendSms(targetPhone, messageBody)
                        targetPhone = ""
                        messageBody = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                    enabled = targetPhone.isNotBlank() && messageBody.isNotBlank()
                ) {
                    Text("立即发送", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showSendDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    // Delete Confirmation Dialog
    deletingSmsId?.let { smsId ->
        AlertDialog(
            onDismissRequest = { deletingSmsId = null },
            title = { Text("确认删除该短信？", fontWeight = FontWeight.Bold) },
            text = { Text("删除后不可恢复，确定从 SIM/设备 存储中彻底移除此短信记录吗？") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSms(smsId)
                        deletingSmsId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RiskR3)
                ) {
                    Text("确认删除")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { deletingSmsId = null }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun ModernSmsCard(
    sms: SmsItem,
    onDelete: () -> Unit,
    onMarkRead: () -> Unit
) {
    val avatarInitial = sms.address.firstOrNull()?.uppercaseChar()?.toString() ?: "#"

    LiquidGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onMarkRead() }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Modern Avatar Circle
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(CyanPrimary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = avatarInitial,
                    color = CyanPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }

            // Message Bubble & Info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!sms.isRead && sms.boxType == 0) {
                            Box(
                                modifier = Modifier
                                    .padding(end = 6.dp)
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(CyanPrimary)
                            )
                        }
                        Text(
                            text = sms.address,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = sms.timestamp,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }

                // Chat Bubble Style
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                        .padding(10.dp)
                ) {
                    Text(
                        text = sms.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Actions row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "删除短信",
                            tint = RiskR3.copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
