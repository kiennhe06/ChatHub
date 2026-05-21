package fpl.ph60001.chathub.presentation.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fpl.ph60001.chathub.ui.theme.GradientEnd
import fpl.ph60001.chathub.ui.theme.GradientStart
import kotlinx.coroutines.delay

/**
 * Màn hình Splash chào mừng với hiệu ứng phóng to logo hoạt họa cực kỳ sang trọng.
 * Sử dụng SplashViewModel để kiểm tra phiên đăng nhập hiện tại một cách sạch sẽ.
 */
@Composable
fun SplashScreen(
    viewModel: SplashViewModel,
    onNavigateToHome: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val isUserLoggedIn by viewModel.isUserLoggedIn.collectAsState()

    // Giá trị scale của logo để hoạt họa
    val scale = remember { Animatable(0f) }

    LaunchedEffect(key1 = true) {
        // Chạy hoạt họa phóng to trong 800ms
        scale.animateTo(
            targetValue = 1.2f,
            animationSpec = tween(durationMillis = 800)
        )
        // Hiệu ứng nảy nhẹ quay về tỉ lệ 1.0
        scale.animateTo(
            targetValue = 1.0f,
            animationSpec = tween(durationMillis = 200)
        )
        
        // Kích hoạt việc kiểm tra phiên đăng nhập
        viewModel.checkSession()
    }

    // Điều hướng dựa vào kết quả kiểm tra trạng thái đăng nhập
    LaunchedEffect(isUserLoggedIn) {
        isUserLoggedIn?.let { loggedIn ->
            delay(1000) // Trễ thêm 1 giây để trải nghiệm thương hiệu mượt mà
            if (loggedIn) {
                onNavigateToHome()
            } else {
                onNavigateToLogin()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(GradientStart, GradientEnd)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icon Logo ChatHub cách điệu
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .scale(scale.value),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Forum,
                    contentDescription = "ChatHub Logo",
                    tint = Color.White,
                    modifier = Modifier.size(80.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Tên ứng dụng
            Text(
                text = "ChatHub",
                color = Color.White,
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.scale(scale.value)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Slogan tiếng Việt
            Text(
                text = "Kết nối tức thì - Sẻ chia yêu thương",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
