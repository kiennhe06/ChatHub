package fpl.ph60001.chathub.presentation.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
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
import fpl.ph60001.chathub.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

// Sử dụng Design System từ ChatHubTheme.kt

/**
 * Giao diện trang chủ (HomeScreen) hiển thị danh sách hội thoại & bạn bè thời gian thực
 * với phong cách Premium Glassmorphism vô cùng bắt mắt.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToChat: (String, String, Boolean) -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateToCreateGroup: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val conversations by viewModel.conversations.collectAsState()
    val friendsList by viewModel.friendsList.collectAsState()
    val incomingRequests by viewModel.incomingRequests.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isLoggedOut by viewModel.isLoggedOut.collectAsState()

    var conversationToDelete by remember { mutableStateOf<Conversation?>(null) }

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
            .background(DarkBg)
    ) {
        // Aurora glow trên nền
        Box(
            modifier = Modifier
                .size(300.dp)
                .align(Alignment.TopEnd)
                .offset(x = 80.dp, y = (-60).dp)
                .blur(80.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(PrimaryViolet.copy(alpha = 0.35f), Color.Transparent)
                    ),
                    shape = CircleShape
                )
        )
        Box(
            modifier = Modifier
                .size(200.dp)
                .align(Alignment.BottomStart)
                .offset(x = (-40).dp, y = 40.dp)
                .blur(60.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(AccentPink.copy(alpha = 0.2f), Color.Transparent)
                    ),
                    shape = CircleShape
                )
        )

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        brush = Brush.linearGradient(
                                            colors = listOf(PrimaryViolet, AccentPink)
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Forum,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "ChatHub",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 20.sp,
                                    color = TextPrimary,
                                    letterSpacing = (-0.3).sp
                                )
                                Text(
                                    text = currentUser?.displayName ?: "Thành viên",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = onNavigateToCreateGroup,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(PrimaryViolet.copy(alpha = 0.15f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.GroupAdd,
                                contentDescription = "Tạo nhóm",
                                tint = PrimaryViolet,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(
                            onClick = onNavigateToProfile,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Hồ sơ",
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(
                            onClick = onNavigateToSettings,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Cài đặt",
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = TextPrimary
                    )
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = onNavigateToSearch,
                    containerColor = Color.Transparent,
                    elevation = FloatingActionButtonDefaults.elevation(0.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(brush = GradientButton)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Tìm kiếm",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Tab switcher mới
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF1A1A2E))
                        .border(1.dp, BorderColor, RoundedCornerShape(14.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val tabs = listOf("Tin nhắn", "Bạn bè")
                    tabs.forEachIndexed { index, title ->
                        val isSelected = selectedTab == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSelected)
                                        Brush.horizontalGradient(listOf(PrimaryViolet, AccentIndigo))
                                    else
                                        Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
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
                                    color = if (isSelected) Color.White else TextMuted,
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
                                            .background(AccentPink),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = badgeCount.toString(),
                                            color = Color.White,
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
                                                onClick = { onNavigateToChat(conv.partnerId, conv.partnerName, conv.isGroup) }
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
                                        onClick = { onNavigateToChat(conversation.partnerId, conversation.partnerName, conversation.isGroup) },
                                        onLongClick = { conversationToDelete = conversation }
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
                                        onChatClick = { onNavigateToChat(friend.uid, friend.displayName, false) },
                                        onUnfriendClick = { viewModel.removeFriend(friend.uid) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (conversationToDelete != null) {
            val target = conversationToDelete!!
            AlertDialog(
                onDismissRequest = { conversationToDelete = null },
                title = { Text("Xóa cuộc trò chuyện", color = Color.White, fontWeight = FontWeight.Bold) },
                text = { Text("Bạn có chắc chắn muốn xóa cuộc trò chuyện với \"${target.partnerName}\"? Hành động này sẽ xóa toàn bộ tin nhắn liên quan ở cả hai phía và không thể khôi phục lại.", color = Color.White.copy(alpha = 0.8f)) },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteConversation(target.id)
                            conversationToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF453A))
                    ) {
                        Text("XÓA SẠCH", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { conversationToDelete = null }) {
                        Text("HỦY BỎ", color = Color.White.copy(alpha = 0.6f))
                    }
                },
                containerColor = Color(0xFF1E293B),
                shape = RoundedCornerShape(24.dp)
            )
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
 * Item cuộc trò chuyện mới (Premium Purple Aurora)
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GlassConversationItem(
    conversation: Conversation,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(DarkCard)
            .border(
                width = 1.dp,
                brush = if (conversation.unreadCount > 0)
                    Brush.horizontalGradient(listOf(PrimaryViolet.copy(alpha = 0.6f), AccentPink.copy(alpha = 0.3f)))
                else
                    Brush.horizontalGradient(listOf(BorderColor, BorderColor)),
                shape = RoundedCornerShape(20.dp)
            )
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Avatar với gradient ring
            Box(modifier = Modifier.size(52.dp), contentAlignment = Alignment.BottomEnd) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .border(
                            width = 2.dp,
                            brush = if (conversation.partnerOnline)
                                Brush.linearGradient(listOf(PrimaryViolet, AccentPink))
                            else
                                Brush.linearGradient(listOf(BorderColor, BorderColor)),
                            shape = CircleShape
                        )
                ) {
                    AsyncImage(
                        model = conversation.partnerAvatar.ifEmpty { "https://ui-avatars.com/api/?name=${conversation.partnerName}&background=7C3AED&color=fff&size=100" },
                        contentDescription = conversation.partnerName,
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
                if (conversation.partnerOnline) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(DarkBg)
                            .padding(2.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize().clip(CircleShape).background(OnlineGreen))
                    }
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = conversation.partnerName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = formatTime(conversation.lastMessageTime),
                        fontSize = 11.sp,
                        color = if (conversation.unreadCount > 0) PrimaryViolet else TextMuted
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = conversation.lastMessage,
                        fontSize = 13.sp,
                        color = if (conversation.unreadCount > 0) TextPrimary else TextSecondary,
                        fontWeight = if (conversation.unreadCount > 0) FontWeight.Medium else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (conversation.unreadCount > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(
                                    brush = Brush.linearGradient(listOf(PrimaryViolet, AccentPink))
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = conversation.unreadCount.toString(),
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
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
 * Lời mời kết bạn Premium Aurora
 */
@Composable
fun GlassFriendRequestItem(
    request: FriendRequest,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(DarkCard)
            .border(
                1.dp,
                Brush.horizontalGradient(listOf(PrimaryViolet.copy(0.5f), AccentPink.copy(0.3f))),
                RoundedCornerShape(18.dp)
            )
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = request.senderAvatar.ifEmpty { "https://ui-avatars.com/api/?name=${request.senderName}&background=7C3AED&color=fff&size=100" },
                contentDescription = request.senderName,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .border(1.dp, Brush.linearGradient(listOf(PrimaryViolet, AccentPink)), CircleShape),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = request.senderName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = TextPrimary
                )
                Text(
                    text = "Muốn kết bạn với bạn 👋",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(brush = GradientButton)
                    .clickable(onClick = onAccept)
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text("Đồng ý", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.width(6.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(ErrorRed.copy(alpha = 0.12f))
                    .border(1.dp, ErrorRed.copy(0.3f), RoundedCornerShape(10.dp))
                    .clickable(onClick = onDecline)
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text("Từ chối", color = ErrorRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
