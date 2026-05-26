package fpl.ph60001.chathub.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import fpl.ph60001.chathub.domain.repository.AuthRepository
import fpl.ph60001.chathub.presentation.chat.ChatScreen
import fpl.ph60001.chathub.presentation.chat.ChatViewModel
import fpl.ph60001.chathub.presentation.forgotpassword.ForgotPasswordScreen
import fpl.ph60001.chathub.presentation.forgotpassword.ForgotPasswordViewModel
import fpl.ph60001.chathub.presentation.home.HomeScreen
import fpl.ph60001.chathub.presentation.home.HomeViewModel
import fpl.ph60001.chathub.presentation.login.LoginScreen
import fpl.ph60001.chathub.presentation.login.LoginViewModel
import fpl.ph60001.chathub.presentation.profile.ProfileScreen
import fpl.ph60001.chathub.presentation.profile.ProfileViewModel
import fpl.ph60001.chathub.presentation.register.RegisterScreen
import fpl.ph60001.chathub.presentation.register.RegisterViewModel
import fpl.ph60001.chathub.presentation.search.SearchScreen
import fpl.ph60001.chathub.presentation.search.SearchViewModel
import fpl.ph60001.chathub.presentation.splash.SplashScreen
import fpl.ph60001.chathub.presentation.splash.SplashViewModel
import fpl.ph60001.chathub.presentation.group.CreateGroupScreen
import fpl.ph60001.chathub.presentation.group.CreateGroupViewModel
import fpl.ph60001.chathub.presentation.group.GroupInfoScreen
import fpl.ph60001.chathub.presentation.group.GroupInfoViewModel

/**
 * Định nghĩa Đồ thị Điều hướng (NavGraph) chính quản lý tất cả các trang màn hình trong ChatHub.
 * Các ViewModel được khởi tạo tự động bằng Hilt DI thông qua hàm `hiltViewModel()`.
 */
@Composable
fun NavGraph(
    navController: NavHostController,
    authRepository: AuthRepository
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        
        // Màn hình Splash
        composable(route = Screen.Splash.route) {
            val splashViewModel: SplashViewModel = hiltViewModel()
            SplashScreen(
                viewModel = splashViewModel,
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        // Màn hình Đăng nhập (Login)
        composable(route = Screen.Login.route) {
            val loginViewModel: LoginViewModel = hiltViewModel()
            LoginScreen(
                viewModel = loginViewModel,
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                },
                onNavigateToForgotPassword = {
                    navController.navigate(Screen.ForgotPassword.route)
                }
            )
        }

        // Màn hình Đăng ký (Register)
        composable(route = Screen.Register.route) {
            val registerViewModel: RegisterViewModel = hiltViewModel()
            RegisterScreen(
                viewModel = registerViewModel,
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                }
            )
        }

        // Màn hình Quên mật khẩu (ForgotPassword)
        composable(route = Screen.ForgotPassword.route) {
            val forgotViewModel: ForgotPasswordViewModel = hiltViewModel()
            ForgotPasswordScreen(
                viewModel = forgotViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Màn hình Trang chủ (Home)
        composable(route = Screen.Home.route) {
            val homeViewModel: HomeViewModel = hiltViewModel()
            HomeScreen(
                viewModel = homeViewModel,
                onNavigateToChat = { partnerId, partnerName, isGroup ->
                    navController.navigate(Screen.Chat.createRoute(partnerId, partnerName, isGroup))
                },
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route)
                },
                onNavigateToSearch = {
                    navController.navigate(Screen.Search.route)
                },
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                onNavigateToCreateGroup = {
                    navController.navigate(Screen.CreateGroup.route)
                }
            )
        }

        // Màn hình Phòng chat chi tiết (Chat Screen)
        composable(
            route = Screen.Chat.route,
            arguments = listOf(
                navArgument("partnerId") { type = NavType.StringType },
                navArgument("partnerName") { type = NavType.StringType },
                navArgument("isGroup") {
                    type = NavType.BoolType
                    defaultValue = false
                }
            )
        ) {
            val chatViewModel: ChatViewModel = hiltViewModel()
            ChatScreen(
                viewModel = chatViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToGroupInfo = { groupId ->
                    navController.navigate(Screen.GroupInfo.createRoute(groupId))
                }
            )
        }

        // Màn hình Tìm kiếm người dùng (Search Screen)
        composable(route = Screen.Search.route) {
            val searchViewModel: SearchViewModel = hiltViewModel()
            SearchScreen(
                viewModel = searchViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToChat = { partnerId, partnerName ->
                    navController.navigate(Screen.Chat.createRoute(partnerId, partnerName, false)) {
                        popUpTo(Screen.Search.route) { inclusive = true }
                    }
                }
            )
        }

        // Màn hình Hồ sơ cá nhân (Profile Screen)
        composable(route = Screen.Profile.route) {
            val profileViewModel: ProfileViewModel = hiltViewModel()
            ProfileScreen(
                viewModel = profileViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Màn hình Tạo nhóm chat (CreateGroup Screen)
        composable(route = Screen.CreateGroup.route) {
            val createGroupViewModel: CreateGroupViewModel = hiltViewModel()
            CreateGroupScreen(
                viewModel = createGroupViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToChat = { groupId, groupName ->
                    navController.navigate(Screen.Chat.createRoute(groupId, groupName, isGroup = true)) {
                        popUpTo(Screen.CreateGroup.route) { inclusive = true }
                    }
                }
            )
        }

        // Màn hình Thông tin nhóm chat (GroupInfo Screen)
        composable(
            route = Screen.GroupInfo.route,
            arguments = listOf(
                navArgument("groupId") { type = NavType.StringType }
            )
        ) {
            val groupInfoViewModel: GroupInfoViewModel = hiltViewModel()
            GroupInfoScreen(
                viewModel = groupInfoViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }
    }
}
