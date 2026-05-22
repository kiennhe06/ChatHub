package fpl.ph60001.chathub.presentation.register

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fpl.ph60001.chathub.ui.theme.GradientEnd
import fpl.ph60001.chathub.ui.theme.GradientStart

/**
 * Giao diện màn hình Đăng ký (RegisterScreen) mang phong cách Premium Glassmorphism mờ ảo.
 * Kết nối đồng bộ với thiết kế màn hình Đăng nhập để tạo nên bộ nhận diện hoàn chỉnh.
 */
@Composable
fun RegisterScreen(
    viewModel: RegisterViewModel,
    onNavigateToHome: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val displayName by viewModel.displayName.collectAsState()
    val email by viewModel.email.collectAsState()
    val password by viewModel.password.collectAsState()
    val confirmPassword by viewModel.confirmPassword.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val isRegisterSuccess by viewModel.isRegisterSuccess.collectAsState()

    var isPasswordVisible by remember { mutableStateOf(false) }
    var isConfirmPasswordVisible by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(isRegisterSuccess) {
        if (isRegisterSuccess) {
            android.widget.Toast.makeText(
                context,
                "Đăng ký tài khoản thành công! Vui lòng đăng nhập lại.",
                android.widget.Toast.LENGTH_LONG
            ).show()
            onNavigateToLogin()
            viewModel.resetSuccessState()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A), // Slate 900
                        Color(0xFF1E293B)  // Slate 800
                    )
                )
            )
    ) {
        // Glowing Orbs trang trí nền
        Box(
            modifier = Modifier
                .size(250.dp)
                .offset(x = (-80).dp, y = (-50).dp)
                .clip(CircleShape)
                .background(Color(0xFF0A84FF).copy(alpha = 0.15f))
        )
        Box(
            modifier = Modifier
                .size(300.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 100.dp, y = 100.dp)
                .clip(CircleShape)
                .background(Color(0xFF64D2FF).copy(alpha = 0.1f))
        )

        // Column cuộn mượt chống tràn
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // Logo & Tiêu đề chính (Dùng Forum làm biểu tượng chat bubble mới thay vì Send giống Telegram)
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFF00C6FF), Color(0xFF0072FF))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Forum,
                    contentDescription = "Logo",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Tạo tài khoản mới!",
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
            )

            Text(
                text = "Tham gia ChatHub chỉ trong vài giây",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
            )

            // Thẻ đăng ký Glassmorphic Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1E293B).copy(alpha = 0.85f)
                ),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "ĐĂNG KÝ",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64D2FF),
                        letterSpacing = 1.5.sp,
                        modifier = Modifier.padding(bottom = 20.dp)
                    )

                    // Trường nhập Họ Tên
                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { viewModel.onDisplayNameChanged(it) },
                        placeholder = { Text("Họ và Tên", color = Color.White.copy(alpha = 0.4f)) },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = Color(0xFF64D2FF))
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color(0xFF0F172A).copy(alpha = 0.5f),
                            unfocusedContainerColor = Color(0xFF0F172A).copy(alpha = 0.3f),
                            focusedBorderColor = Color(0xFF00C6FF),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                            focusedPlaceholderColor = Color.White.copy(alpha = 0.4f),
                            unfocusedPlaceholderColor = Color.White.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Trường nhập Email
                    OutlinedTextField(
                        value = email,
                        onValueChange = { viewModel.onEmailChanged(it) },
                        placeholder = { Text("Địa chỉ Email", color = Color.White.copy(alpha = 0.4f)) },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Email, contentDescription = null, tint = Color(0xFF64D2FF))
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color(0xFF0F172A).copy(alpha = 0.5f),
                            unfocusedContainerColor = Color(0xFF0F172A).copy(alpha = 0.3f),
                            focusedBorderColor = Color(0xFF00C6FF),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                            focusedPlaceholderColor = Color.White.copy(alpha = 0.4f),
                            unfocusedPlaceholderColor = Color.White.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Trường nhập Mật khẩu (Sử dụng biểu tượng mắt Hiện/Ẩn thay vì chữ viết thông thường)
                    OutlinedTextField(
                        value = password,
                        onValueChange = { viewModel.onPasswordChanged(it) },
                        placeholder = { Text("Mật khẩu", color = Color.White.copy(alpha = 0.4f)) },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = Color(0xFF64D2FF))
                        },
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        trailingIcon = {
                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                Icon(
                                    imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (isPasswordVisible) "Ẩn mật khẩu" else "Hiện mật khẩu",
                                    tint = Color(0xFF64D2FF)
                                )
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color(0xFF0F172A).copy(alpha = 0.5f),
                            unfocusedContainerColor = Color(0xFF0F172A).copy(alpha = 0.3f),
                            focusedBorderColor = Color(0xFF00C6FF),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                            focusedPlaceholderColor = Color.White.copy(alpha = 0.4f),
                            unfocusedPlaceholderColor = Color.White.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Trường nhập Xác nhận Mật khẩu (Sử dụng biểu tượng mắt Hiện/Ẩn thay vì chữ viết thông thường)
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { viewModel.onConfirmPasswordChanged(it) },
                        placeholder = { Text("Xác nhận Mật khẩu", color = Color.White.copy(alpha = 0.4f)) },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = Color(0xFF64D2FF))
                        },
                        visualTransformation = if (isConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        trailingIcon = {
                            IconButton(onClick = { isConfirmPasswordVisible = !isConfirmPasswordVisible }) {
                                Icon(
                                    imageVector = if (isConfirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (isConfirmPasswordVisible) "Ẩn mật khẩu" else "Hiện mật khẩu",
                                    tint = Color(0xFF64D2FF)
                                )
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color(0xFF0F172A).copy(alpha = 0.5f),
                            unfocusedContainerColor = Color(0xFF0F172A).copy(alpha = 0.3f),
                            focusedBorderColor = Color(0xFF00C6FF),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                            focusedPlaceholderColor = Color.White.copy(alpha = 0.4f),
                            unfocusedPlaceholderColor = Color.White.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Thông báo lỗi
                    AnimatedVisibility(
                        visible = errorMessage != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        errorMessage?.let {
                            Text(
                                text = it,
                                color = Color(0xFFFF453A),
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Nút Đăng ký Gradient
                    Button(
                        onClick = { viewModel.register() },
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(RoundedCornerShape(16.dp)),
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
                                            listOf(Color(0xFF00C6FF), Color(0xFF0072FF))
                                        }
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            } else {
                                Text(
                                    text = "ĐĂNG KÝ NGAY",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Đã có tài khoản
                    TextButton(onClick = onNavigateToLogin) {
                        Text(
                            text = "Đã có tài khoản? Đăng nhập",
                            color = Color(0xFF64D2FF),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
