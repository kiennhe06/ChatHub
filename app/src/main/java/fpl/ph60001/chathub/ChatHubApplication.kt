package fpl.ph60001.chathub

import fpl.ph60001.chathub.BuildConfig
import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import dagger.hilt.android.HiltAndroidApp

/**
 * Lớp Application chính của ứng dụng ChatHub.
 * Được chú thích bằng @HiltAndroidApp để kích hoạt Hilt Dependency Injection trên toàn bộ ứng dụng.
 */
@HiltAndroidApp
class ChatHubApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Khởi tạo FirebaseApp
        FirebaseApp.initializeApp(this)
        
        // Cấu hình và khởi tạo Firebase App Check
        val firebaseAppCheck = FirebaseAppCheck.getInstance()
        if (BuildConfig.DEBUG) {
            // Sử dụng Debug Provider khi chạy trên Emulator/Debug để bỏ qua cảnh báo
            firebaseAppCheck.installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            )
        } else {
            // Sử dụng Play Integrity khi phát hành bản chính thức
            firebaseAppCheck.installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance()
            )
        }
    }
}
