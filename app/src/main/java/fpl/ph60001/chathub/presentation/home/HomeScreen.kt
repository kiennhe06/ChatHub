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
import androidx.compose.material.icons.filled.Close
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
import fpl.ph60001.chathub.domain.model.FriendRequest
import fpl.ph60001.chathub.domain.model.User
import java.text.SimpleDateFormat
import java.util.*

// Hệ màu Premium Glassmorphism tối sang trọng
private val GlassCard = Color(0x331E293B)       // Thẻ gương mờ
private val GlassBorder = Color(0x3364D2FF)     // Viền gương phát sáng Neon
private val NeonBlue = Color(0xFF64D2FF)        // Xanh Neon chủ đạo
private val NeonCyan = Color(0xFF00F2FE)        // Xanh Cyan
private val GreenOnline = Color(0xFF4ADE80)     // Chấm xanh online tươi mới

/**
 * Giao diện trang chủ (HomeScreen) hiển thị danh sách hội thoại & bạn bè thời gian thực
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
    val friendsList by viewModel.friendsList.collectAsState()
    val incomingRequests by viewModel.incomingRequests.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Thanh Tab Switcher Glassmorphic cao cấp
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val tabs = listOf("Tin nhắn", "Bạn bè")
                    tabs.forEachIndexed { index, title ->
                        val isSelected = selectedTab == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .then(
                                    if (isSelected) {
                                        Modifier.background(
                                            brush = Brush.horizontalGradient(
                                                colors = listOf(NeonBlue.copy(alpha = 0.2f), NeonCyan.copy(alpha = 0.2f))
                                            )
                                        )
                                    } else {
                                        Modifier.background(Color.Transparent)
                                    }
                                )
                                .border(
                                    width = if (isSelected) 1.dp else 0.dp,
                                    color = if (isSelected) NeonBlue.copy(alpha = 0.4f) else Color.Transparent,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { viewModel.setSelectedTab(index) },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = title,
                                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.5f),
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 14.sp
                                )
                                val badgeCount = if (index == 1) incomingRequests.size else 0
                                if (badgeCount > 0) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clip(CircleShape)
                                            .background(NeonBlue),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = badgeCount.toString(),
                                            color = Color.Black,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = NeonBlue)
                    }
                } else {
                    if (selectedTab == 0) {
                        // TAB 1: TIN NHẮN & CUỘC HỘI THOẠI
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 80.dp)
                        ) {
                            // DANH SÁCH BẠN ĐANG TRỰC TUYẾN (ACTIVE NOW)
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
                    } else {
                        // TAB 2: DANH SÁCH BẠN BÈ ĐẦY ĐỦ VÀ LỜI MỜI KẾT BẠN
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 80.dp)
                        ) {
                            item {
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            
                            // 1. Phân mục Lời mời kết bạn (chỉ hiển thị khi có lời mời đang chờ)
                            if (incomingRequests.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "Lời mời kết bạn (${incomingRequests.size})",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NeonBlue,
                                        modifier = Modifier.padding(start = 20.dp, top = 8.dp, bottom = 12.dp)
                                    )
                                }
                                
                                items(incomingRequests) { request ->
                                    GlassFriendRequestItem(
                                        request = request,
                                        onAccept = { viewModel.acceptFriendRequest(request.id, request.senderId) },
                                        onDecline = { viewModel.declineFriendRequest(request.id) }
                                    )
                                }
                                
                                item {
                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                            }
                            
                            // 2. Phân mục Tất cả bạn bè
                            item {
                                Text(
                                    text = "Tất cả bạn bè (${friendsList.size})",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.padding(start = 20.dp, top = 8.dp, bottom = 12.dp)
                                )
                            }

                            if (friendsList.isEmpty()) {
                                item {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 20.dp, vertical = 12.dp),
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
                                                text = "Danh sách bạn bè trống!",
                                                color = Color.White.copy(alpha = 0.7f),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp,
                                                textAlign = TextAlign.Center
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = "Nhấn vào nút kính lúp ở dưới để tìm kiếm kết bạn với mọi người.",
                                                color = Color.White.copy(alpha = 0.4f),
                                                fontSize = 13.sp,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            } else {
                                items(friendsList) { friend ->
                                    GlassFriendListItem(
                                        friend = friend,
                                        onChatClick = { onNavigateToChat(friend.uid, friend.displayName) },
                                        onUnfriendClick = { viewModel.removeFriend(friend.uid) }
                                    )
                                }
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
 * Item danh sách bạn bè phong cách Glassmorphism (GlassFriendListItem).
 */
@Composable
fun GlassFriendListItem(
    friend: User,
    onChatClick: () -> Unit,
    onUnfriendClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
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
            // Ảnh đại diện dạng tròn có chấm online phát sáng
            Box(
                modifier = Modifier.size(52.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                ) {
                    AsyncImage(
                        model = friend.photoUrl.ifEmpty { "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=150&q=80" },
                        contentDescription = friend.displayName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                if (friend.isOnline) {
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

            // Tên & Trạng thái hoạt động
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = friend.displayName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (friend.isOnline) "Đang trực tuyến" else "Ngoại tuyến",
                    fontSize = 12.sp,
                    color = if (friend.isOnline) GreenOnline else Color.White.copy(alpha = 0.4f),
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Nút "Nhắn tin"
            Button(
                onClick = onChatClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.08f),
                    contentColor = NeonBlue
                ),
                border = BorderStroke(1.dp, NeonBlue.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Text(
                    text = "Nhắn tin",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Nút hủy kết bạn
            IconButton(
                onClick = onUnfriendClick,
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0x1AFF5252))
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Hủy kết bạn",
                    tint = Color(0xFFFF5252),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * Component hiển thị Lời mời kết bạn phong cách Glassmorphism (GlassFriendRequestItem).
 */
@Composable
fun GlassFriendRequestItem(
    request: FriendRequest,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
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
            // Ảnh đại diện
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
            ) {
                AsyncImage(
                    model = request.senderAvatar.ifEmpty { "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=150&q=80" },
                    contentDescription = request.senderName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Tên và thông báo mời
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = request.senderName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Muốn kết bạn với bạn",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Nút Đồng ý
            Button(
                onClick = onAccept,
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonBlue,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Text(
                    text = "Đồng ý",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Nút Từ chối
            Button(
                onClick = onDecline,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.08f),
                    contentColor = Color(0xFFFF5252)
                ),
                border = BorderStroke(1.dp, Color(0xFFFF5252).copy(alpha = 0.3f)),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Text(
                    text = "Từ chối",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
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
