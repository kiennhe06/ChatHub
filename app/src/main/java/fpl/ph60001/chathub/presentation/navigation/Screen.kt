package fpl.ph60001.chathub.presentation.navigation

/**
 * Lớp Sealed đại diện cho các Màn hình (Routes) điều hướng trong ứng dụng.
 */
sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Register : Screen("register")
    object Home : Screen("home")
    
    // Màn hình chat yêu cầu truyền tham số ID người nhận và Tên người nhận
    object Chat : Screen("chat/{partnerId}/{partnerName}") {
        fun createRoute(partnerId: String, partnerName: String): String {
            return "chat/$partnerId/$partnerName"
        }
    }
    
    object Profile : Screen("profile")
    object ForgotPassword : Screen("forgot_password")
    object Search : Screen("search")
}
