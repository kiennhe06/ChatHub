package fpl.ph60001.chathub.presentation.chat

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import fpl.ph60001.chathub.domain.model.Message
import java.text.SimpleDateFormat
import java.util.*

// Hệ màu Premium Glassmorphism
private val GlassCard = Color(0x331E293B)
private val GlassBorder = Color(0x3364D2FF)
private val NeonBlue = Color(0xFF64D2FF)
private val NeonCyan = Color(0xFF00F2FE)
private val GreenOnline = Color(0xFF4ADE80)
private val BubbleMeColor = Color(0xFF0284C7)      // Xanh đại dương bóng bẩy
private val BubbleOtherColor = Color(0x661E293B)   // Xám gương mờ

/**
 * Giao diện phòng Chat chi tiết (ChatScreen) hoàn chỉnh hỗ trợ đầy đủ các tính năng
 * nhắn tin thời gian thực cao cấp theo phong cách Premium Glassmorphism.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onNavigateBack: () -> Unit
) {
    val messages by viewModel.messagesList.collectAsState()
    val messageText by viewModel.messageText.collectAsState()
    val isPartnerTyping by viewModel.isPartnerTyping.collectAsState()
    val replyingTo by viewModel.replyingTo.collectAsState()
    val editingMessage by viewModel.editingMessage.collectAsState()

    val listState = rememberLazyListState()

    // Trạng thái hiển thị Dialog Menu tin nhắn được Long-press
    var selectedMessageForMenu by remember { mutableStateOf<Message?>(null) }

    // Tự động cuộn xuống dưới cùng khi có tin nhắn mới
    LaunchedEffect(messages.size, isPartnerTyping) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
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
                .align(Alignment.TopStart)
                .offset(x = (-120).dp, y = (-80).dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            NeonBlue.copy(alpha = 0.12f),
                            Color.Transparent
                        )
                    )
                )
        )

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Avatar tròn đối phương
                            Box(modifier = Modifier.size(40.dp)) {
                                AsyncImage(
                                    model = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=100&q=80",
                                    contentDescription = viewModel.partnerName,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF0F172A))
                                        .padding(1.5.dp)
                                        .align(Alignment.BottomEnd)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape)
                                            .background(GreenOnline)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = viewModel.partnerName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = if (isPartnerTyping) "Đang nhập..." else "Đang trực tuyến",
                                    fontSize = 11.sp,
                                    color = if (isPartnerTyping) NeonBlue else Color.White.copy(alpha = 0.5f),
                                    fontWeight = if (isPartnerTyping) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.08f))
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Quay lại",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // KHU VỰC 1: DANH SÁCH TIN NHẮN REALTIME
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messages, key = { it.messageId }) { message ->
                        val isMe = message.senderId == viewModel.currentUserUid

                        // Đánh dấu đã xem khi tin nhắn hiển thị lên màn hình (nếu chưa xem)
                        if (!isMe && !message.seenBy.contains(viewModel.currentUserUid)) {
                            LaunchedEffect(message.messageId) {
                                viewModel.markMessageAsSeen(message.messageId)
                            }
                        }

                        MessageBubble(
                            message = message,
                            isMe = isMe,
                            partnerName = viewModel.partnerName,
                            partnerId = viewModel.partnerId,
                            onLongClick = {
                                selectedMessageForMenu = message
                            }
                        )
                    }
                }

                // KHU VỰC 2: CHỈ BÁO ĐANG GÕ TIN NHẮN (TYPING INDICATOR)
                AnimatedVisibility(
                    visible = isPartnerTyping,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = GlassCard),
                            border = BorderStroke(1.dp, GlassBorder)
                        ) {
                            Text(
                                text = "${viewModel.partnerName} đang nhập tin nhắn...",
                                fontSize = 12.sp,
                                fontStyle = FontStyle.Italic,
                                color = NeonBlue,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // KHU VỰC 3: BANNER ĐANG PHẢN HỒI (REPLYING BANNER)
                AnimatedVisibility(visible = replyingTo != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(GlassCard)
                            .border(BorderStroke(1.dp, GlassBorder.copy(alpha = 0.1f)))
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Đang phản hồi tin nhắn của ${if (replyingTo?.senderId == viewModel.currentUserUid) "bạn" else viewModel.partnerName}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonBlue
                            )
                            Text(
                                text = replyingTo?.content ?: "",
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.7f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(onClick = { viewModel.setReplyingTo(null) }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Hủy", tint = Color.White)
                        }
                    }
                }

                // KHU VỰC 4: BANNER ĐANG CHỈNH SỬA (EDITING BANNER)
                AnimatedVisibility(visible = editingMessage != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF3B2A0F))
                            .border(BorderStroke(1.dp, Color(0xFFFE9F00).copy(alpha = 0.3f)))
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = null, tint = Color(0xFFFE9F00), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Đang chỉnh sửa tin nhắn",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFE9F00)
                            )
                            Text(
                                text = editingMessage?.content ?: "",
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.7f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(onClick = { viewModel.setEditingMessage(null) }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Hủy", tint = Color.White)
                        }
                    }
                }

                // KHU VỰC 5: Ô NHẬP TIN NHẮN GLASSMORPHIC
                Surface(
                    color = GlassCard,
                    border = BorderStroke(1.dp, GlassBorder.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = messageText,
                            onValueChange = { viewModel.onMessageTextChanged(it) },
                            placeholder = { 
                                Text(
                                    text = if (editingMessage != null) "Sửa tin nhắn..." else "Nhập tin nhắn...",
                                    color = Color.White.copy(alpha = 0.4f)
                                ) 
                            },
                            maxLines = 4,
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = NeonBlue,
                                unfocusedBorderColor = GlassBorder,
                                focusedContainerColor = Color(0x330F172A),
                                unfocusedContainerColor = Color(0x330F172A)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .padding(vertical = 4.dp),
                            singleLine = false
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        // Nút Gửi / Lưu Sửa tròn phát sáng
                        IconButton(
                            onClick = {
                                if (editingMessage != null) {
                                    viewModel.saveEditMessage()
                                } else {
                                    viewModel.sendMessage()
                                }
                            },
                            enabled = messageText.isNotBlank(),
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = if (messageText.isNotBlank()) listOf(NeonBlue, NeonCyan) else listOf(Color.Gray, Color.DarkGray)
                                    )
                                )
                        ) {
                            Icon(
                                imageVector = if (editingMessage != null) Icons.Default.Refresh else Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Gửi",
                                tint = Color.Black,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        // HỘP THOẠI BOTTOMSHEET/DIALOG LỰA CHỌN KHI LONG-PRESS TIN NHẮN
        if (selectedMessageForMenu != null) {
            val msg = selectedMessageForMenu!!
            val isMsgMe = msg.senderId == viewModel.currentUserUid

            Dialog(onDismissRequest = { selectedMessageForMenu = null }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    border = BorderStroke(1.5.dp, GlassBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Bày tỏ cảm xúc",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))

                        // Dòng chọn Emoji reactions cực mịn
                        Row(
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf("❤️", "😂", "👍", "😮", "😢").forEach { emoji ->
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.08f))
                                        .clickable {
                                            viewModel.reactToMessage(msg.messageId, emoji)
                                            selectedMessageForMenu = null
                                        }
                                ) {
                                    Text(text = emoji, fontSize = 24.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                        Divider(color = Color.White.copy(alpha = 0.1f))
                        Spacer(modifier = Modifier.height(10.dp))

                        // Các nút Hành động trong danh sách menu dọc
                        TextButton(
                            onClick = {
                                viewModel.setReplyingTo(msg)
                                selectedMessageForMenu = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = "💬 Trả lời (Reply)", color = Color.White, fontSize = 15.sp)
                        }

                        if (isMsgMe && !msg.isDeleted) {
                            TextButton(
                                onClick = {
                                    viewModel.setEditingMessage(msg)
                                    selectedMessageForMenu = null
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(text = "✏️ Chỉnh sửa tin", color = NeonBlue, fontSize = 15.sp)
                            }

                            TextButton(
                                onClick = {
                                    viewModel.deleteMessage(msg.messageId)
                                    selectedMessageForMenu = null
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(text = "🗑️ Thu hồi tin nhắn", color = Color(0xFFFF5252), fontSize = 15.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Thành phần bong bóng tin nhắn (Message Bubble) hai bên gửi/nhận cực kỳ sang trọng
 * với đầy đủ icon reaction, trạng thái tích xanh và phản hồi reply.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: Message,
    isMe: Boolean,
    partnerName: String,
    partnerId: String,
    onLongClick: () -> Unit
) {
    val bubbleColor = if (isMe) BubbleMeColor else BubbleOtherColor
    val alignment = if (isMe) Alignment.End else Alignment.Start
    
    val bubbleShape = if (isMe) {
        RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 4.dp)
    } else {
        RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 4.dp, bottomEnd = 20.dp)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalAlignment = alignment
    ) {
        // Hộp chính của bong bóng tin nhắn, hỗ trợ cả Long Click
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(bubbleShape)
                .background(bubbleColor)
                .border(
                    BorderStroke(
                        0.5.dp, 
                        if (isMe) NeonCyan.copy(alpha = 0.4f) else GlassBorder.copy(alpha = 0.2f)
                    ),
                    bubbleShape
                )
                .combinedClickable(
                    onClick = {},
                    onLongClick = onLongClick
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Column {
                // PHẦN 1: BẢN IN TIN NHẮN ĐƯỢC PHẢN HỒI (REPLIED MESSAGE SNIPPET)
                if (message.replyTo != null && !message.isDeleted) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                            .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                            .border(BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f)), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(24.dp)
                                .background(NeonBlue, RoundedCornerShape(2.dp))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (message.replyTo.id.startsWith("bot_") || message.replyTo.id == "m1" || message.replyTo.id == "m3") partnerName else "Bạn",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonBlue
                            )
                            Text(
                                text = message.replyTo.content,
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.6f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // PHẦN 2: NỘI DUNG VĂN BẢN TIN NHẮN CHÍNH
                if (message.isDeleted) {
                    Text(
                        text = "Tin nhắn đã bị thu hồi",
                        color = Color.White.copy(alpha = 0.35f),
                        fontSize = 14.sp,
                        fontStyle = FontStyle.Italic
                    )
                } else {
                    Text(
                        text = message.content,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }

                // PHẦN 3: HIỂN THỊ CHỮ (ĐÃ SỬA) NẾU CÓ
                if (message.isEdited && !message.isDeleted) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "(đã chỉnh sửa)",
                        fontSize = 9.sp,
                        fontStyle = FontStyle.Italic,
                        color = Color.White.copy(alpha = 0.4f)
                    )
                }
            }
        }

        // HÀNG NGANG DƯỚI BONG BÓNG: THỜI GIAN GỬI & TRẠNG THÁI TÍCH XANH & EMOJIS
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            // Hiển thị Emoji Reactions đè đẹp mắt dưới bong bóng
            if (message.reactions.isNotEmpty() && !message.isDeleted) {
                val reactionSummary = message.reactions.values.distinct().joinToString(" ")
                Card(
                    shape = CircleShape,
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    border = BorderStroke(0.5.dp, GlassBorder.copy(alpha = 0.3f)),
                    modifier = Modifier.padding(end = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = reactionSummary, fontSize = 11.sp)
                        if (message.reactions.size > 1) {
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(text = message.reactions.size.toString(), color = Color.White, fontSize = 9.sp)
                        }
                    }
                }
            }

            // Định dạng thời gian gửi tin nhắn
            val timeString = remember(message.timestamp) {
                val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                sdf.format(Date(message.timestamp))
            }
            Text(
                text = timeString,
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.4f)
            )

            // Hiển thị Tích Xanh Trạng thái tin nhắn đối với tin nhắn mình gửi
            if (isMe && !message.isDeleted) {
                Spacer(modifier = Modifier.width(4.dp))
                val isSeen = message.seenBy.contains(partnerId)
                Text(
                    text = if (isSeen) "✓✓" else "✓",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSeen) NeonBlue else Color.White.copy(alpha = 0.4f)
                )
            }
        }
    }
}
