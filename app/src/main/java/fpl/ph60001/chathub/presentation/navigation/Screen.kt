package fpl.ph60001.chathub.presentation.navigation

/**
 * Lớp Sealed đại diện cho các Màn hình (Routes) điều hướng trong ứng dụng.
 */
sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Register : Screen("register")
    object Home : Screen("home")
    
    // Màn hình chat yêu cầu truyền tham số ID người nhận, Tên người nhận và cờ kiểm tra nhóm
    object Chat : Screen("chat/{partnerId}/{partnerName}?isGroup={isGroup}") {
        fun createRoute(partnerId: String, partnerName: String, isGroup: Boolean = false): String {
            return "chat/$partnerId/$partnerName?isGroup=$isGroup"
        }
    }
    
    object Profile : Screen("profile")
    object ForgotPassword : Screen("forgot_password")
    object Search : Screen("search")
    object CreateGroup : Screen("create_group")
    object GroupInfo : Screen("group_info/{groupId}") {
        fun createRoute(groupId: String): String {
            return "group_info/$groupId"
        }
    }
}
