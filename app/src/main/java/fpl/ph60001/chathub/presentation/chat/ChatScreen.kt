package fpl.ph60001.chathub.presentation.chat

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import fpl.ph60001.chathub.domain.model.Message
import fpl.ph60001.chathub.domain.model.User
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

import androidx.compose.ui.viewinterop.AndroidView
import com.zegocloud.uikit.prebuilt.call.invite.widget.ZegoSendCallInvitationButton
import com.zegocloud.uikit.service.defines.ZegoUIKitUser

import fpl.ph60001.chathub.ui.theme.*

// Alias cho ChatScreen
private val BubbleMeGrad = Brush.linearGradient(listOf(Color(0xFF7C3AED), Color(0xFF6D28D9)))
private val BubbleOtherBg = Color(0xFF1E1B4B)

/**
 * Giao diện phòng Chat chi tiết (ChatScreen) hoàn chỉnh hỗ trợ đầy đủ các tính năng
 * nhắn tin thời gian thực cao cấp và đính kèm hình ảnh/tệp tin tuyệt đẹp phong cách Glassmorphism.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToGroupInfo: (String) -> Unit
) {
    val messages by viewModel.messagesList.collectAsState()
    val messageText by viewModel.messageText.collectAsState()
    val isPartnerTyping by viewModel.isPartnerTyping.collectAsState()
    val replyingTo by viewModel.replyingTo.collectAsState()
    val editingMessage by viewModel.editingMessage.collectAsState()
    val isSecretMode by viewModel.isSecretMode.collectAsState()
    
    val uploadProgress by viewModel.uploadProgress.collectAsState()
    val isUploading by viewModel.isUploading.collectAsState()
    val uploadError by viewModel.uploadError.collectAsState()

    val groupMembers by viewModel.groupMembers.collectAsState()
    val groupAvatar by viewModel.groupAvatar.collectAsState()
    val groupNameFlow by viewModel.groupNameFlow.collectAsState()
 
    val context = LocalContext.current
    val listState = rememberLazyListState()

    // Trạng thái hiển thị Dialog Menu tin nhắn được Long-press
    var selectedMessageForMenu by remember { mutableStateOf<Message?>(null) }

    // Tạo file tạm cho camera capture
    val cameraFile = remember {
        val cachePhotosDir = File(context.cacheDir, "camera_photos").apply { mkdirs() }
        File(cachePhotosDir, "capture_${System.currentTimeMillis()}.jpg")
    }
    val cameraUri = remember {
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", cameraFile)
    }

    // Bộ chọn ảnh từ thư viện
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val bytes = context.contentResolver.openInputStream(it)?.readBytes()
            val fileName = "gallery_${System.currentTimeMillis()}.jpg"
            if (bytes != null) {
                viewModel.sendImageMessage(bytes, fileName)
            }
        }
    }

    // Trình chụp ảnh từ camera
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success) {
            val bytes = cameraFile.readBytes()
            val fileName = "camera_${System.currentTimeMillis()}.jpg"
            viewModel.sendImageMessage(bytes, fileName)
        }
    }

    // Yêu cầu cấp quyền CAMERA động trước khi kích hoạt chụp ảnh
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            cameraLauncher.launch(cameraUri)
        } else {
            android.widget.Toast.makeText(context, "Vui lòng cấp quyền CAMERA trong Cài đặt để sử dụng tính năng này!", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    // Trình chọn tệp đính kèm (PDF, Word, ZIP, etc)
    val fileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            var fileName = "file_${System.currentTimeMillis()}"
            var fileSize = 0L
            context.contentResolver.query(it, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                    if (nameIdx != -1) fileName = cursor.getString(nameIdx)
                    if (sizeIdx != -1) fileSize = cursor.getLong(sizeIdx)
                }
            }
            val bytes = context.contentResolver.openInputStream(it)?.readBytes()
            if (bytes != null) {
                viewModel.sendFileMessage(bytes, fileName, fileSize)
            }
        }
    }

    // Hiển thị thông báo nếu xảy ra lỗi upload
    LaunchedEffect(uploadError) {
        uploadError?.let {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_LONG).show()
            viewModel.clearUploadError()
        }
    }

    // Tự động cuộn xuống dưới cùng khi có tin nhắn mới
    LaunchedEffect(messages.size, isPartnerTyping) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
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
                .size(280.dp)
                .align(Alignment.TopCenter)
                .offset(y = (-80).dp)
                .blur(80.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(PrimaryViolet.copy(alpha = 0.3f), Color.Transparent)
                    ),
                    shape = CircleShape
                )
        )

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable(enabled = viewModel.isGroup) {
                                onNavigateToGroupInfo(viewModel.partnerId)
                            }
                        ) {
                            // Avatar tròn đối phương/nhóm
                            Box(modifier = Modifier.size(40.dp)) {
                                val avatarSource = if (viewModel.isGroup) {
                                    if (groupAvatar.isEmpty()) "https://images.unsplash.com/photo-1582213782179-e0d53f98f2ca?w=100" else groupAvatar
                                } else {
                                    "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=100&q=80"
                                }
                                AsyncImage(
                                    model = avatarSource,
                                    contentDescription = viewModel.partnerName,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                                if (!viewModel.isGroup) {
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
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = if (viewModel.isGroup) { if (groupNameFlow.isEmpty()) viewModel.partnerName else groupNameFlow } else viewModel.partnerName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = TextPrimary
                                )
                                Text(
                                    text = if (viewModel.isGroup) "${groupMembers.size} thành viên" else if (isPartnerTyping) "Đang nhập..." else "Đang trực tuyến",
                                    fontSize = 11.sp,
                                    color = if (isPartnerTyping && !viewModel.isGroup) PrimaryViolet else TextSecondary,
                                    fontWeight = if (isPartnerTyping && !viewModel.isGroup) FontWeight.Bold else FontWeight.Normal
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
                    actions = {
                        // Nút Gọi Thoại bằng ZegoCloud (Cái 1)
                        if (!viewModel.isGroup) {
                            AndroidView<ZegoSendCallInvitationButton>(
                                factory = { ctx ->
                                    ZegoSendCallInvitationButton(ctx).apply {
                                        setIsVideoCall(false)
                                        setInvitees(listOf(ZegoUIKitUser(viewModel.partnerId, viewModel.partnerName)))
                                        // Ghi log cuộc gọi thoại vào chat khi bấm
                                        setOnTouchListener { _, event ->
                                            if (event.action == android.view.MotionEvent.ACTION_UP) {
                                                viewModel.sendCallLogMessage(false)
                                            }
                                            false // Không chặn event, để ZegoCloud vẫn xử lý gọi bình thường
                                        }
                                    }
                                },
                                modifier = Modifier.size(40.dp).padding(4.dp)
                            )

                            // Nút Gọi Video bằng ZegoCloud
                            AndroidView<ZegoSendCallInvitationButton>(
                                factory = { ctx ->
                                    ZegoSendCallInvitationButton(ctx).apply {
                                        setIsVideoCall(true)
                                        setInvitees(listOf(ZegoUIKitUser(viewModel.partnerId, viewModel.partnerName)))
                                        // Ghi log cuộc gọi video vào chat khi bấm
                                        setOnTouchListener { _, event ->
                                            if (event.action == android.view.MotionEvent.ACTION_UP) {
                                                viewModel.sendCallLogMessage(true)
                                            }
                                            false
                                        }
                                    }
                                },
                                modifier = Modifier.size(40.dp).padding(4.dp)
                            )
                        }

                        // Nút Vanish Mode / Secret Chat (Cái 2)
                        IconButton(
                            onClick = {
                                viewModel.toggleSecretMode()
                                val msg = if (!isSecretMode) "🔥 Đã BẬT Tin nhắn tự hủy (10s)" else "Đã TẮT Tin nhắn tự hủy"
                                android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                            },
                            modifier = if (isSecretMode) Modifier
                                .clip(CircleShape)
                                .background(AccentPink.copy(alpha = 0.2f))
                            else Modifier
                        ) {
                            Icon(
                                imageVector = if (isSecretMode) Icons.Default.LocalFireDepartment else Icons.Default.Visibility,
                                contentDescription = "Tin nhắn tự hủy",
                                tint = if (isSecretMode) AccentPink else PrimaryViolet
                            )
                        }

                        if (viewModel.isGroup) {
                            IconButton(
                                onClick = { onNavigateToGroupInfo(viewModel.partnerId) }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Thông tin nhóm",
                                    tint = PrimaryViolet
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = DarkBg,
                        titleContentColor = TextPrimary
                    ),
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .imePadding()
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

                        // Tính năng Tin nhắn tự hủy (Cái 2): Tự động xóa sau 60 giây khi hiển thị
                        if (message.isSecret && !message.isDeleted) {
                            LaunchedEffect(message.messageId) {
                                kotlinx.coroutines.delay(60000) // Đếm ngược 60s
                                viewModel.deleteMessagePermanently(message.messageId)
                            }
                        }

                        MessageBubble(
                            message = message,
                            isMe = isMe,
                            partnerName = viewModel.partnerName,
                            partnerId = viewModel.partnerId,
                            isGroup = viewModel.isGroup,
                            groupMembers = groupMembers,
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

                // KHU VỰC 5: TIẾN TRÌNH UPLOAD FILE REALTIME
                AnimatedVisibility(visible = isUploading) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(GlassCard)
                            .border(BorderStroke(1.dp, GlassBorder.copy(alpha = 0.1f)))
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Đang tải tệp lên... $uploadProgress%",
                                color = NeonBlue,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            CircularProgressIndicator(
                                color = NeonBlue,
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { uploadProgress.toFloat() / 100f },
                            color = NeonBlue,
                            trackColor = Color.White.copy(alpha = 0.1f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(CircleShape)
                        )
                    }
                }

                // KHU VỰC 6: Ô NHẬP TIN NHẮN GLASSMORPHIC & HÀNH ĐỘNG ĐÍNH KÈM
                Surface(
                    color = DarkCard,
                    border = BorderStroke(1.dp, BorderColor),
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Nút Thư viện ảnh
                        IconButton(onClick = { galleryLauncher.launch("image/*") }) {
                            Icon(imageVector = Icons.Default.PhotoLibrary, contentDescription = "Thư viện ảnh", tint = NeonBlue)
                        }

                        // Nút Chụp ảnh
                        IconButton(onClick = {
                            val hasCameraPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                                context,
                                android.Manifest.permission.CAMERA
                            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                            
                            if (hasCameraPermission) {
                                cameraLauncher.launch(cameraUri)
                            } else {
                                cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                            }
                        }) {
                            Icon(imageVector = Icons.Default.CameraAlt, contentDescription = "Chụp ảnh", tint = NeonBlue)
                        }

                        // Nút File đính kèm
                        IconButton(onClick = { fileLauncher.launch("*/*") }) {
                            Icon(imageVector = Icons.Default.AttachFile, contentDescription = "Đính kèm file", tint = NeonBlue)
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        OutlinedTextField(
                            value = messageText,
                            onValueChange = { viewModel.onMessageTextChanged(it) },
                            placeholder = { 
                                Text(
                                    text = if (editingMessage != null) "Sửa tin nhắn..." else "Nhắp tin nhắn...",
                                    color = TextMuted, fontSize = 14.sp
                                ) 
                            },
                            maxLines = 4,
                            shape = RoundedCornerShape(20.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = PrimaryViolet,
                                unfocusedBorderColor = BorderColor,
                                focusedContainerColor = Color(0xFF0F0F1F),
                                unfocusedContainerColor = Color(0xFF0D0D1A),
                                cursorColor = PrimaryViolet
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .padding(vertical = 4.dp),
                            singleLine = false
                        )

                        Spacer(modifier = Modifier.width(8.dp))

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
                                    brush = if (messageText.isNotBlank())
                                        GradientButton
                                    else
                                        Brush.horizontalGradient(listOf(TextMuted, TextMuted))
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
                        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
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
                            if (msg.type == "text") {
                                TextButton(
                                    onClick = {
                                        viewModel.setEditingMessage(msg)
                                        selectedMessageForMenu = null
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(text = "✏️ Chỉnh sửa tin", color = NeonBlue, fontSize = 15.sp)
                                }
                            }

                            TextButton(
                                onClick = {
                                    viewModel.deleteMessage(msg.messageId)
                                    selectedMessageForMenu = null
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(text = "🗑️ Thu hồi tin nhắn", color = Color(0xFFFF9F0A), fontSize = 15.sp)
                            }
                        }

                        // Luôn cho phép xóa vĩnh viễn tin nhắn khỏi cuộc trò chuyện (xóa document Firestore)
                        TextButton(
                            onClick = {
                                viewModel.deleteMessagePermanently(msg.messageId)
                                selectedMessageForMenu = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = "❌ Xóa tin nhắn vĩnh viễn", color = Color(0xFFFF453A), fontSize = 15.sp)
                        }
                    }
                }
            }
        }

    }
}

/**
 * Thành phần bong bóng tin nhắn (Message Bubble) hai bên gửi/nhận cực kỳ sang trọng
 * với đầy đủ đính kèm hình ảnh, tệp tin, reaction, tích xanh và phản hồi reply.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: Message,
    isMe: Boolean,
    partnerName: String,
    partnerId: String,
    isGroup: Boolean,
    groupMembers: Map<String, User>,
    onLongClick: () -> Unit
) {
    val context = LocalContext.current
    val alignment = if (isMe) Alignment.End else Alignment.Start
    
    val bubbleShape = if (isMe) {
        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp)
    } else {
        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 4.dp, bottomEnd = 18.dp)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalAlignment = alignment
    ) {
        // Tên người gửi trong nhóm chat
        if (isGroup && !isMe && !message.isDeleted) {
            val senderProfile = groupMembers[message.senderId]
            val senderDisplayName = senderProfile?.displayName ?: message.senderName
            Text(
                text = senderDisplayName,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryViolet,
                modifier = Modifier.padding(start = 6.dp, bottom = 2.dp)
            )
        }

        // Bong bóng tin nhắn
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(bubbleShape)
                .then(
                    if (isMe) Modifier.background(brush = BubbleMeGrad)
                    else Modifier.background(BubbleOtherBg)
                )
                .border(
                    BorderStroke(
                        if (message.isSecret) 1.5.dp else 0.5.dp,
                        if (message.isSecret) AccentPink.copy(alpha = 0.8f)
                        else if (isMe) PrimaryViolet.copy(alpha = 0.5f)
                        else BorderColor
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
                                .background(PrimaryViolet, RoundedCornerShape(2.dp))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (message.replyTo.id.startsWith("bot_") || message.replyTo.id == "m1" || message.replyTo.id == "m3") partnerName else "Bạn",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryViolet
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

                // PHẦN 2: NỘI DUNG CHÍNH (VĂN BẢN / ẢNH / FILE ĐÍNH KÈM)
                if (message.isDeleted) {
                    Text(
                        text = "Tin nhắn đã bị thu hồi",
                        color = Color.White.copy(alpha = 0.35f),
                        fontSize = 14.sp,
                        fontStyle = FontStyle.Italic
                    )
                } else {
                    when (message.type) {
                        "image" -> {
                            var isFullScreenOpen by remember { mutableStateOf(false) }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { isFullScreenOpen = true }
                            ) {
                                AsyncImage(
                                    model = message.mediaUrl,
                                    contentDescription = "Ảnh đính kèm",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }

                            if (isFullScreenOpen) {
                                Dialog(onDismissRequest = { isFullScreenOpen = false }) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.95f))
                                            .clickable { isFullScreenOpen = false },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        AsyncImage(
                                            model = message.mediaUrl,
                                            contentDescription = "Ảnh toàn màn hình",
                                            modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                                            contentScale = ContentScale.Fit
                                        )
                                        IconButton(
                                            onClick = { isFullScreenOpen = false },
                                            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                        ) {
                                            Icon(imageVector = Icons.Default.Close, contentDescription = "Đóng", tint = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                        "file" -> {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
                                border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (message.mediaUrl.isNotEmpty()) {
                                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse(message.mediaUrl))
                                            context.startActivity(intent)
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.InsertDriveFile,
                                        contentDescription = "File đính kèm",
                                        tint = NeonBlue,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = message.fileName.ifEmpty { "Tệp đính kèm" },
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = formatFileSize(message.fileSize),
                                            fontSize = 10.sp,
                                            color = Color.White.copy(alpha = 0.5f)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.FileDownload,
                                        contentDescription = "Tải về",
                                        tint = Color.White.copy(alpha = 0.7f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                        else -> {
                            // Mặc định type = "text"
                            Text(
                                text = message.content,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        }
                    }
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

            // Hiển thị Tích Xanh Trạng thái tin nhắn đối với tin nhắn mình gửi (chỉ dành cho chat 1-1)
            if (!isGroup && isMe && !message.isDeleted) {
                Spacer(modifier = Modifier.width(4.dp))
                val isSeen = message.seenBy.contains(partnerId)
                Text(
                    text = if (isSeen) "✓✓" else "✓",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSeen) NeonBlue else Color.White.copy(alpha = 0.4f)
                )
            }

            // Hiển thị avatar những người đã xem tin nhắn này (dành cho nhóm)
            val seenUsers = message.seenBy.filter { it != message.senderId }
            if (isGroup && seenUsers.isNotEmpty() && !message.isDeleted) {
                Spacer(modifier = Modifier.width(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy((-6).dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    seenUsers.take(4).forEach { seenUserId ->
                        val userProfile = groupMembers[seenUserId]
                        val avatarUrl = userProfile?.photoUrl ?: ""
                        AsyncImage(
                            model = if (avatarUrl.isEmpty()) "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=50&q=80" else avatarUrl,
                            contentDescription = "Seen by",
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .border(1.dp, Color(0xFF0F172A), CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }
                    if (seenUsers.size > 4) {
                        Text(
                            text = "+${seenUsers.size - 4}",
                            fontSize = 8.sp,
                            color = Color.White.copy(alpha = 0.5f),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Định dạng dung lượng tệp tin (bytes -> KB/MB/GB).
 */
private fun formatFileSize(size: Long): String {
    if (size <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format(Locale.US, "%.1f %s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}
