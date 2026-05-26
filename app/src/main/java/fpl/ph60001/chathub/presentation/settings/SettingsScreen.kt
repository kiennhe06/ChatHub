package fpl.ph60001.chathub.presentation.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import fpl.ph60001.chathub.domain.model.Conversation

// Palette màu Premium
private val GlassCard = Color(0x331E293B)
private val GlassBorder = Color(0x3364D2FF)
private val NeonBlue = Color(0xFF64D2FF)
private val NeonCyan = Color(0xFF00F2FE)
private val NeonRed = Color(0xFFFF5252)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val isNotificationsEnabled by viewModel.isNotificationsEnabled.collectAsState()
    val mutedConversations by viewModel.mutedConversations.collectAsState()
    val conversations by viewModel.conversations.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val isLoggedOut by viewModel.isLoggedOut.collectAsState()

    var showMuteDialogForConv by remember { mutableStateOf<Conversation?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }

    // Tự động điều hướng về login khi logout/xóa tài khoản thành công
    LaunchedEffect(isLoggedOut) {
        if (isLoggedOut) {
            onNavigateToLogin()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cài đặt hệ thống", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        },
        containerColor = Color(0xFF0F172A)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF0F172A), Color(0xFF1E293B))
                    )
                )
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 20.dp)
            ) {
                // Hiển thị thông báo lỗi
                error?.let {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = NeonRed.copy(alpha = 0.2f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, NeonRed, RoundedCornerShape(12.dp))
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Error, contentDescription = "Lỗi", tint = NeonRed)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = it,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { viewModel.clearError() }) {
                                    Icon(Icons.Default.Close, contentDescription = "Đóng", tint = Color.White)
                                }
                            }
                        }
                    }
                }

                // 1. Nhóm cấu hình hiển thị & chủ đề
                item {
                    SettingsSectionTitle("GIAO DIỆN & HIỂN THỊ")
                }

                item {
                    SettingsToggleRow(
                        title = "Chế độ tối (Dark Mode)",
                        subtitle = "Chuyển giao diện sáng hoặc tối cho ứng dụng",
                        icon = Icons.Default.DarkMode,
                        checked = isDarkMode,
                        onCheckedChange = { viewModel.toggleDarkMode(it) }
                    )
                }

                // 2. Nhóm cấu hình thông báo
                item {
                    SettingsSectionTitle("THÔNG BÁO ĐẨY")
                }

                item {
                    SettingsToggleRow(
                        title = "Nhận thông báo cuộc trò chuyện",
                        subtitle = "Bật/Tắt tất cả thông báo tin nhắn mới",
                        icon = Icons.Default.NotificationsActive,
                        checked = isNotificationsEnabled,
                        onCheckedChange = { viewModel.toggleNotifications(it) }
                    )
                }

                // 3. Danh sách cuộc trò chuyện cấu hình tắt tiếng (Mute)
                item {
                    SettingsSectionTitle("TẮT TIẾNG TỪNG CUỘC CHAT")
                }

                if (conversations.isEmpty()) {
                    item {
                        Text(
                            text = "Chưa có cuộc trò chuyện nào để cấu hình.",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 14.sp,
                            modifier = Modifier.padding(vertical = 10.dp)
                        )
                    }
                } else {
                    items(conversations) { conversation ->
                        val isMuted = mutedConversations.containsKey(conversation.id)
                        val expiry = mutedConversations[conversation.id] ?: 0L
                        val statusText = if (isMuted) {
                            if (expiry == -1L) "Đã tắt tiếng vĩnh viễn"
                            else {
                                val remainingMin = (expiry - System.currentTimeMillis()) / 60000
                                if (remainingMin <= 0) "Mới hết hạn"
                                else if (remainingMin > 60) "Đã tắt tiếng (còn ${remainingMin / 60} giờ)"
                                else "Đã tắt tiếng (còn $remainingMin phút)"
                            }
                        } else "Đang bật thông báo"

                        MuteConversationRow(
                            conversation = conversation,
                            statusText = statusText,
                            isMuted = isMuted,
                            onClick = { showMuteDialogForConv = conversation }
                        )
                    }
                }

                // 4. Nhóm tài khoản & bảo mật
                item {
                    SettingsSectionTitle("TÀI KHOẢN & BẢO MẬT")
                }

                item {
                    SettingsActionRow(
                        title = "Đăng xuất tài khoản",
                        subtitle = "Đăng xuất khỏi thiết bị hiện tại của bạn",
                        icon = Icons.AutoMirrored.Filled.Logout,
                        iconTint = NeonBlue,
                        onClick = { showLogoutConfirmDialog = true }
                    )
                }

                item {
                    SettingsActionRow(
                        title = "Xóa tài khoản vĩnh viễn",
                        subtitle = "Hành động này không thể hoàn tác, xóa mọi dữ liệu",
                        icon = Icons.Default.DeleteForever,
                        iconTint = NeonRed,
                        onClick = { showDeleteConfirmDialog = true }
                    )
                }
            }

            if (isLoading) {
                CircularProgressIndicator(
                    color = NeonBlue,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }

    // Dialog lựa chọn thời gian tắt tiếng
    if (showMuteDialogForConv != null) {
        val conversation = showMuteDialogForConv!!
        val isMuted = mutedConversations.containsKey(conversation.id)

        AlertDialog(
            onDismissRequest = { showMuteDialogForConv = null },
            title = { Text("Tắt tiếng thông báo") },
            text = {
                Text("Chọn khoảng thời gian muốn tắt tiếng cuộc trò chuyện với \"${conversation.partnerName}\":")
            },
            confirmButton = {
                TextButton(onClick = { showMuteDialogForConv = null }) {
                    Text("HỦY BỎ", color = Color.Gray)
                }
            },
            dismissButton = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (isMuted) {
                        TextButton(
                            onClick = {
                                viewModel.unmuteConversation(conversation.id)
                                showMuteDialogForConv = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Icon(Icons.Default.Notifications, contentDescription = null)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Bật tiếng lại thông báo")
                            }
                        }
                    }
                    TextButton(
                        onClick = {
                            viewModel.muteConversation(conversation.id, 3600000L) // 1 giờ
                            showMuteDialogForConv = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                            Icon(Icons.AutoMirrored.Filled.VolumeMute, contentDescription = null)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Tắt tiếng trong 1 giờ")
                        }
                    }
                    TextButton(
                        onClick = {
                            viewModel.muteConversation(conversation.id, 28800000L) // 8 giờ
                            showMuteDialogForConv = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                            Icon(Icons.AutoMirrored.Filled.VolumeMute, contentDescription = null)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Tắt tiếng trong 8 giờ")
                        }
                    }
                    TextButton(
                        onClick = {
                            viewModel.muteConversation(conversation.id, -1L) // Mãi mãi
                            showMuteDialogForConv = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                            Icon(Icons.AutoMirrored.Filled.VolumeOff, contentDescription = null)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Tắt tiếng mãi mãi")
                        }
                    }
                }
            }
        )
    }

    // Dialog xác nhận xóa tài khoản
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = NeonRed, modifier = Modifier.size(40.dp)) },
            title = { Text("Xác nhận xóa tài khoản") },
            text = {
                Text(
                    "Bạn có chắc chắn muốn xóa tài khoản ChatHub vĩnh viễn?\n" +
                            "Hành động này sẽ xóa sạch dữ liệu tin nhắn, bạn bè của bạn khỏi hệ thống và KHÔNG THỂ HOÀN TÁC."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAccount()
                        showDeleteConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonRed)
                ) {
                    Text("XÓA TÀI KHOẢN", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("HỦY BỎ", color = Color.Gray)
                }
            }
        )
    }

    // Dialog xác nhận đăng xuất tài khoản
    if (showLogoutConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirmDialog = false },
            icon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = NeonBlue, modifier = Modifier.size(40.dp)) },
            title = { Text("Đăng xuất tài khoản") },
            text = {
                Text("Bạn có chắc chắn muốn đăng xuất khỏi tài khoản ChatHub hiện tại không?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.logout()
                        showLogoutConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonBlue)
                ) {
                    Text("ĐĂNG XUẤT", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirmDialog = false }) {
                    Text("HỦY BỎ", color = Color.Gray)
                }
            }
        )
    }
}

@Composable
fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = NeonBlue,
        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
    )
}

@Composable
fun SettingsToggleRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = GlassCard),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, GlassBorder.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(GlassBorder.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(22.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(title, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                    Text(subtitle, color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                }
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = NeonCyan,
                    checkedTrackColor = NeonBlue.copy(alpha = 0.5f)
                )
            )
        }
    }
}

@Composable
fun SettingsActionRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = GlassCard),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, GlassBorder.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                Text(subtitle, color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.White.copy(alpha = 0.4f))
        }
    }
}

@Composable
fun MuteConversationRow(
    conversation: Conversation,
    statusText: String,
    isMuted: Boolean,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = GlassCard),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, GlassBorder.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = conversation.partnerAvatar.ifEmpty {
                    if (conversation.isGroup) "https://images.unsplash.com/photo-1582213782179-e0d53f98f2ca?w=100"
                    else "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=100"
                },
                contentDescription = null,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(conversation.partnerName, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                Text(statusText, color = if (isMuted) NeonRed else Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
            }
            Icon(
                imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = null,
                tint = if (isMuted) NeonRed else Color.White.copy(alpha = 0.4f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
