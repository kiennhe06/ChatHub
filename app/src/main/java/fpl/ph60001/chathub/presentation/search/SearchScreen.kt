package fpl.ph60001.chathub.presentation.search

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Close
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
import fpl.ph60001.chathub.domain.model.User

// Hệ màu Premium Glassmorphism tối sang trọng
private val GlassCard = Color(0x331E293B)       // Thẻ gương mờ
private val GlassBorder = Color(0x3364D2FF)     // Viền gương phát sáng Neon
private val NeonBlue = Color(0xFF64D2FF)        // Xanh Neon chủ đạo

/**
 * Màn hình tìm kiếm người dùng (SearchScreen) phong cách Premium Glassmorphism tuyệt đẹp.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToChat: (String, String) -> Unit
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

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
                .align(Alignment.BottomStart)
                .offset(x = (-100).dp, y = 100.dp)
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
                        Text(
                            text = "Tìm kiếm bạn bè",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            color = Color.White
                        )
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
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = Color.White
                    ),
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Ô tìm kiếm Glassmorphic bo tròn sang trọng
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.onSearchQueryChanged(it) },
                    placeholder = { 
                        Text(
                            text = "Nhập tên hoặc email của bạn bè...",
                            color = Color.White.copy(alpha = 0.4f)
                        ) 
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search, 
                            contentDescription = "Tìm kiếm",
                            tint = NeonBlue
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                Icon(
                                    imageVector = Icons.Default.Close, 
                                    contentDescription = "Xóa",
                                    tint = Color.White.copy(alpha = 0.6f)
                                )
                            }
                        }
                    },
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = NeonBlue,
                        unfocusedBorderColor = GlassBorder,
                        focusedContainerColor = GlassCard,
                        unfocusedContainerColor = GlassCard
                    ),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                )

                // Trạng thái Loading hoặc Hiển thị danh sách kết quả
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = NeonBlue)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        if (error != null) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(40.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = error ?: "Đã xảy ra lỗi không xác định",
                                        color = Color(0xFFFF5252),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else if (searchQuery.trim().isEmpty()) {
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 80.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.15f),
                                        modifier = Modifier.size(80.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Nhập thông tin tìm kiếm",
                                        color = Color.White.copy(alpha = 0.5f),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Tìm kiếm bất kỳ người dùng nào trên ChatHub bằng tên hoặc địa chỉ email của họ.",
                                        color = Color.White.copy(alpha = 0.3f),
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 48.dp)
                                    )
                                }
                            }
                        } else if (searchResults.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 80.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Không tìm thấy thành viên nào khớp!",
                                        color = Color.White.copy(alpha = 0.4f),
                                        fontSize = 14.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            items(searchResults) { user ->
                                GlassSearchResultItem(
                                    user = user,
                                    onClick = {
                                        viewModel.startConversation(user.uid) { roomId ->
                                            onNavigateToChat(user.uid, user.displayName)
                                        }
                                    }
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
 * Item hiển thị kết quả tìm kiếm phong cách Glassmorphism (GlassSearchResultItem).
 */
@Composable
fun GlassSearchResultItem(
    user: User,
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
            // Ảnh đại diện dạng tròn
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
            ) {
                AsyncImage(
                    model = user.photoUrl.ifEmpty { "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=150&q=80" },
                    contentDescription = user.displayName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Thông tin hiển thị Tên & Email
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = user.displayName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = user.email,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.5f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
