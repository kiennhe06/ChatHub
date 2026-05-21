package fpl.ph60001.chathub.presentation.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import fpl.ph60001.chathub.domain.model.Conversation
import fpl.ph60001.chathub.ui.theme.BluePrimaryLight
import java.text.SimpleDateFormat
import java.util.*

// Hệ màu Premium Glassmorphism tối sang trọng
private val GlassBackground = Color(0xFF0F172A) // Slate đen vũ trụ
private val GlassCard = Color(0x331E293B)       // Thẻ gương mờ
private val GlassBorder = Color(0x3364D2FF)     // Viền gương phát sáng Neon
private val NeonBlue = Color(0xFF64D2FF)        // Xanh Neon chủ đạo
private val NeonCyan = Color(0xFF00F2FE)        // Xanh Cyan
private val GreenOnline = Color(0xFF4ADE80)     // Chấm xanh online tươi mới

/**
 * Giao diện trang chủ (HomeScreen) hiển thị danh sách hội thoại thời gian thực
 * với phong cách Premium Glassmorphism vô cùng bắt mắt.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToChat: (String, String) -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val conversations by viewModel.conversations.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isLoggedOut by viewModel.isLoggedOut.collectAsState()

    // Chuyển về màn hình đăng nhập khi đăng xuất thành công
    LaunchedEffect(isLoggedOut) {
        if (isLoggedOut) {
            onNavigateToLogin()
            viewModel.resetLogoutState()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A),
                        Color(0xFF1E1E38),
                        Color(0xFF0F172A)
                    )
                )
            )
    ) {
        // Vòng tròn phát sáng Neon trang trí nền ảo diệu
        Box(
            modifier = Modifier
                .size(300.dp)
                .align(Alignment.TopEnd)
                .offset(x = 100.dp, y = (-50).dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            NeonBlue.copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    )
                )
        )

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                LargeTopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "ChatHub",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 28.sp,
                                color = Color.White
                            )
                            Text(
                                text = "Xin chào, ${currentUser?.displayName ?: "Thành viên"}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Normal,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }
                    },
                    actions = {
                        // Nút xem hồ sơ cá nhân
                        IconButton(
                            onClick = onNavigateToProfile,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.08f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Xem hồ sơ",
                                tint = NeonBlue
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))

                        // Nút Đăng xuất
                        IconButton(
                            onClick = { viewModel.logout() },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.08f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.ExitToApp,
                                contentDescription = "Đăng xuất",
                                tint = Color(0xFFFF5252)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.largeTopAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = Color.White,
                        actionIconContentColor = Color.White
                    )
                )
            },
            floatingActionButton = {
                // Nút tìm kiếm Glassmorphic Floating Action Button nổi bật
                FloatingActionButton(
                    onClick = {
                        // Điều hướng sang màn hình tìm kiếm bạn bè
                        onNavigateToSearch()
                    },
                    containerColor = Color.Transparent,
                    elevation = FloatingActionButtonDefaults.elevation(0.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(NeonBlue, NeonCyan)
                            )
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Tìm kiếm người dùng",
                        tint = Color.Black,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = NeonBlue)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        // PHẦN 1: DANH SÁCH BẠN ĐANG TRỰC TUYẾN (ACTIVE NOW)
                        val onlineConversations = conversations.filter { it.partnerOnline }
                        if (onlineConversations.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Đang hoạt động",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 12.dp)
                                )
                                
                                LazyRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentPadding = PaddingValues(horizontal = 20.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    items(onlineConversations) { conv ->
                                        OnlineBuddyItem(
                                            conversation = conv,
                                            onClick = { onNavigateToChat(conv.partnerId, conv.partnerName) }
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(20.dp))
                            }
                        }

                        // PHẦN 2: DANH SÁCH CÁC CUỘC TRÒ CHUYỆN (CONVERSATIONS)
                        item {
                            Text(
                                text = "Cuộc trò chuyện",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.padding(start = 20.dp, top = 8.dp, bottom = 12.dp)
                            )
                        }

                        if (conversations.isEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp, vertical = 20.dp),
                                    shape = RoundedCornerShape(24.dp),
                                    colors = CardDefaults.cardColors(containerColor = GlassCard),
                                    border = BorderStroke(1.dp, GlassBorder)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(32.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "Chưa có cuộc hội thoại nào!",
                                            color = Color.White.copy(alpha = 0.7f),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Nhấn vào nút kính lúp ở dưới để tìm kiếm bạn bè và nhắn tin nhé.",
                                            color = Color.White.copy(alpha = 0.4f),
                                            fontSize = 13.sp,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        } else {
                            items(conversations) { conversation ->
                                GlassConversationItem(
                                    conversation = conversation,
                                    onClick = { onNavigateToChat(conversation.partnerId, conversation.partnerName) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Thành phần item của một người bạn trực tuyến ở hàng ngang (Active Now Row).
 */
@Composable
fun OnlineBuddyItem(
    conversation: Conversation,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .width(68.dp)
    ) {
        Box(
            modifier = Modifier.size(56.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            // Khung Avatar tròn Glassmorphic có viền sáng
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .border(2.dp, NeonBlue.copy(alpha = 0.5f), CircleShape)
            ) {
                AsyncImage(
                    model = conversation.partnerAvatar.ifEmpty { "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=150&q=80" },
                    contentDescription = conversation.partnerName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            
            // Chấm xanh trạng thái trực tuyến phát sáng tươi mát
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0F172A))
                    .padding(2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(GreenOnline)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = conversation.partnerName.substringBefore(" "),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Item cuộc trò chuyện dọc phong cách gương mờ Glassmorphism (GlassConversationItem).
 */
@Composable
fun GlassConversationItem(
    conversation: Conversation,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = GlassCard),
        border = BorderStroke(1.dp, GlassBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Ảnh đại diện của bạn chat
            Box(
                modifier = Modifier.size(54.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                ) {
                    AsyncImage(
                        model = conversation.partnerAvatar.ifEmpty { "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=150&q=80" },
                        contentDescription = conversation.partnerName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                
                // Chấm xanh online nếu bạn chat đang online
                if (conversation.partnerOnline) {
                    Box(
                        modifier = Modifier
                            .size(13.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0F172A))
                            .padding(2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(GreenOnline)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Tên và nội dung tin nhắn cuối cùng
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = conversation.partnerName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = conversation.lastMessage,
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = if (conversation.unreadCount > 0) 0.9f else 0.5f),
                    fontWeight = if (conversation.unreadCount > 0) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Thời gian gửi tin nhắn & Badge số lượng tin nhắn chưa đọc
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = formatTime(conversation.lastMessageTime),
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.4f),
                    fontWeight = FontWeight.Normal
                )
                
                if (conversation.unreadCount > 0) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(NeonBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = conversation.unreadCount.toString(),
                            color = Color.Black,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Định dạng lại timestamp thành dạng giờ Việt Nam đẹp mắt.
 */
private fun formatTime(timestamp: Long): String {
    if (timestamp == 0L) return ""
    val now = Calendar.getInstance()
    val time = Calendar.getInstance().apply { timeInMillis = timestamp }
    return if (now.get(Calendar.DATE) == time.get(Calendar.DATE)) {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        sdf.format(time.time)
    } else if (now.get(Calendar.YEAR) == time.get(Calendar.YEAR)) {
        val sdf = SimpleDateFormat("dd/MM", Locale.getDefault())
        sdf.format(time.time)
    } else {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        sdf.format(time.time)
    }
}
