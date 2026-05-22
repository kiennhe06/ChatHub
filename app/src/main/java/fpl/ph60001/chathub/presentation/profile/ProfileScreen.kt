package fpl.ph60001.chathub.presentation.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import fpl.ph60001.chathub.ui.theme.GradientEnd
import fpl.ph60001.chathub.ui.theme.GradientStart
import kotlinx.coroutines.delay

/**
 * Giao diện quản lý thông tin tài khoản người dùng (ProfileScreen).
 * Cho phép người dùng chỉnh sửa tên hiển thị và nhấn vào Avatar để chọn ảnh từ máy/thư viện và tải lên cực chuyên nghiệp.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onNavigateBack: () -> Unit
) {
    val user by viewModel.user.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()

    val context = LocalContext.current
    var nameInput by remember { mutableStateOf("") }

    // Đồng bộ hóa trạng thái Họ và Tên khi thông tin người dùng được tải về
    LaunchedEffect(user) {
        user?.let {
            nameInput = it.displayName
        }
    }

    // Tự động xóa thông báo thành công/lỗi sau 3 giây
    LaunchedEffect(successMessage, errorMessage) {
        if (successMessage != null || errorMessage != null) {
            delay(3000)
            viewModel.clearMessages()
        }
    }

    // Đăng ký Launcher để chọn ảnh từ thiết bị
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            try {
                val inputStream = context.contentResolver.openInputStream(selectedUri)
                val bytes = inputStream?.readBytes()
                inputStream?.close()
                if (bytes != null) {
                    viewModel.uploadAvatar(bytes)
                }
            } catch (e: Exception) {
                // Xử lý lỗi nếu có khi đọc tệp tin
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hồ sơ cá nhân", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Khung Avatar lớn, có thể nhấn để chọn ảnh + Biểu tượng camera đè lên mờ ảo
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .clip(CircleShape)
                    .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), CircleShape)
                    .clickable(enabled = !isLoading) {
                        imagePickerLauncher.launch("image/*")
                    },
                contentAlignment = Alignment.BottomCenter
            ) {
                // Ảnh Avatar chính
                AsyncImage(
                    model = user?.photoUrl?.ifEmpty { "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=150&q=80" }
                        ?: "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=150&q=80",
                    contentDescription = "Avatar của tôi",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Lớp mờ hình camera báo hiệu có thể thay đổi
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Chọn ảnh",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Email tĩnh của tài khoản
            Text(
                text = user?.email ?: "",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Ô nhập Họ và Tên mới
            OutlinedTextField(
                value = nameInput,
                onValueChange = { nameInput = it },
                label = { Text("Họ và Tên") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Person, contentDescription = "Name Icon")
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Hiển thị thông báo lỗi tiếng Việt
            AnimatedVisibility(visible = errorMessage != null) {
                errorMessage?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
            }

            // Hiển thị thông báo thành công tiếng Việt (màu xanh lá)
            AnimatedVisibility(visible = successMessage != null) {
                successMessage?.let {
                    Text(
                        text = it,
                        color = Color(0xFF34C759),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Nút Lưu thay đổi Gradient bo góc cực sang trọng
            Button(
                onClick = { viewModel.updateProfile(nameInput, user?.photoUrl ?: "") },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(12.dp)),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = if (isLoading) {
                                    listOf(Color.Gray, Color.Gray)
                                } else {
                                    listOf(GradientStart, GradientEnd)
                                }
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text(
                            text = "LƯU THAY ĐỔI",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}
