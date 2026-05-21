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


@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // Tiêm AuthRepository để kiểm tra phiên đăng nhập tức thì trong NavGraph/Splash
    @Inject
    lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Kích hoạt trải nghiệm hiển thị tràn viền (Edge to Edge)
        enableEdgeToEdge()
        
        setContent {
            ChatHubTheme {
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
                }
            }
        }
    }
}