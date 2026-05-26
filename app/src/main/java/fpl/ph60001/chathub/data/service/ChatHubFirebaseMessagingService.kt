package fpl.ph60001.chathub.data.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import fpl.ph60001.chathub.MainActivity
import fpl.ph60001.chathub.R
import fpl.ph60001.chathub.domain.repository.AuthRepository
import fpl.ph60001.chathub.domain.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

/**
 * Dịch vụ nhận thông báo đẩy Firebase Cloud Messaging (FCM).
 * Tích hợp Hilt DI để truy cập repositories kiểm tra cài đặt Mute & Bật/Tắt thông báo.
 */
@AndroidEntryPoint
class ChatHubFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var authRepository: AuthRepository

    @Inject
    lateinit var settingsRepository: SettingsRepository

    private val serviceScope = CoroutineScope(Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Cập nhật FCM token lên Firestore khi người dùng đang hoạt động
        serviceScope.launch {
            authRepository.updateFcmToken(token)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Đọc dữ liệu từ payload thông báo gửi đến
        val data = remoteMessage.data
        if (data.isEmpty()) return

        val conversationId = data["conversationId"] ?: ""
        val partnerId = data["partnerId"] ?: ""
        val partnerName = data["partnerName"] ?: ""
        val senderName = data["senderName"] ?: "Người dùng mới"
        val messageText = data["messageText"] ?: "Đã gửi một tin nhắn"
        val senderPhoto = data["senderPhoto"] ?: ""
        val isGroup = data["isGroup"]?.toBoolean() ?: false

        // Kiểm tra cài đặt thông báo của người dùng bằng DataStore (chạy trên background thread của FCM)
        runBlocking {
            val notificationsEnabled = settingsRepository.isNotificationsEnabled.first()
            if (!notificationsEnabled) return@runBlocking

            val mutedMap = settingsRepository.mutedConversations.first()
            val muteExpiry = mutedMap[conversationId] ?: 0L
            if (muteExpiry == -1L || muteExpiry > System.currentTimeMillis()) {
                // Đang bị tắt tiếng (vĩnh viễn hoặc chưa hết hạn)
                return@runBlocking
            }

            // Tiến hành hiển thị thông báo đẩy lên thiết bị
            showNotification(
                conversationId = conversationId,
                partnerId = partnerId,
                partnerName = partnerName,
                senderName = senderName,
                messageText = messageText,
                senderPhoto = senderPhoto,
                isGroup = isGroup
            )
        }
    }

    private fun showNotification(
        conversationId: String,
        partnerId: String,
        partnerName: String,
        senderName: String,
        messageText: String,
        senderPhoto: String,
        isGroup: Boolean
    ) {
        val channelId = "chathub_messages"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Tạo Notification Channel trên Android 8.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Tin nhắn ChatHub",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Kênh thông báo tin nhắn trò chuyện ChatHub"
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Đọc ảnh đại diện của người gửi thành Bitmap để làm LargeIcon
        val largeIconBitmap = getBitmapFromUrl(senderPhoto)

        // Chuẩn bị Intent để mở thẳng ChatScreen tương ứng
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("conversationId", conversationId)
            putExtra("partnerId", partnerId)
            putExtra("partnerName", partnerName)
            putExtra("isGroup", isGroup)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            conversationId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Cấu hình nội dung thông báo
        val title = if (isGroup) "$partnerName ($senderName)" else senderName
        
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(applicationInfo.icon) // Icon mặc định hệ thống
            .setContentTitle(title)
            .setContentText(messageText)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)

        if (largeIconBitmap != null) {
            notificationBuilder.setLargeIcon(largeIconBitmap)
        }

        notificationManager.notify(conversationId.hashCode(), notificationBuilder.build())
    }

    private fun getBitmapFromUrl(urlStr: String?): Bitmap? {
        if (urlStr.isNullOrEmpty()) return null
        return try {
            val url = URL(urlStr)
            val connection = url.openConnection() as HttpURLConnection
            connection.doInput = true
            connection.connect()
            val input = connection.inputStream
            BitmapFactory.decodeStream(input)
        } catch (e: Exception) {
            null
        }
    }
}
