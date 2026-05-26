package fpl.ph60001.chathub

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import fpl.ph60001.chathub.domain.repository.AuthRepository
import fpl.ph60001.chathub.presentation.navigation.NavGraph
import fpl.ph60001.chathub.ui.theme.ChatHubTheme
import javax.inject.Inject


import android.content.Intent
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import fpl.ph60001.chathub.domain.repository.SettingsRepository
import fpl.ph60001.chathub.presentation.navigation.Screen
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // Tiêm AuthRepository để kiểm tra phiên đăng nhập tức thì trong NavGraph/Splash
    @Inject
    lateinit var authRepository: AuthRepository

    @Inject
    lateinit var settingsRepository: SettingsRepository

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Kích hoạt trải nghiệm hiển thị tràn viền (Edge to Edge)
        enableEdgeToEdge()

        // Yêu cầu quyền thông báo đẩy trên Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        
        setContent {
            val isDarkMode by settingsRepository.isDarkMode.collectAsState(initial = false)
            ChatHubTheme(darkTheme = isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Khởi tạo NavController cho Compose Navigation
                    val navController = rememberNavController()
                    
                    // Thiết lập Đồ thị điều hướng tổng cho toàn bộ ứng dụng
                    NavGraph(
                        navController = navController,
                        authRepository = authRepository
                    )

                    // Lắng nghe click từ thông báo đẩy để mở trực tiếp phòng chat
                    LaunchedEffect(intent) {
                        val conversationId = intent.getStringExtra("conversationId")
                        val partnerId = intent.getStringExtra("partnerId")
                        val partnerName = intent.getStringExtra("partnerName")
                        val isGroup = intent.getBooleanExtra("isGroup", false)

                        if (!conversationId.isNullOrEmpty() && !partnerId.isNullOrEmpty()) {
                            // Xóa extra tránh bị điều hướng lại khi xoay màn hình
                            intent.removeExtra("conversationId")
                            navController.navigate(Screen.Chat.createRoute(partnerId, partnerName ?: "Phòng chat", isGroup))
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onStart() {
        super.onStart()
        if (authRepository.getCurrentUser() != null) {
            lifecycleScope.launch {
                authRepository.updateOnlineStatus(true)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (authRepository.getCurrentUser() != null) {
            CoroutineScope(Dispatchers.IO).launch {
                authRepository.updateOnlineStatus(false)
            }
        }
    }
}