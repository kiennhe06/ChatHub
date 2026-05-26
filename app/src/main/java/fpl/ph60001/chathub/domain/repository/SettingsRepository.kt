package fpl.ph60001.chathub.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Interface cho việc quản lý cấu hình cài đặt của ứng dụng bằng DataStore Preferences.
 */
interface SettingsRepository {

    /**
     * Luồng dữ liệu kiểm tra xem chế độ tối (Dark mode) có được bật hay không.
     */
    val isDarkMode: Flow<Boolean>

    /**
     * Lưu cấu hình bật/tắt Dark mode.
     */
    suspend fun setDarkMode(enabled: Boolean)

    /**
     * Luồng dữ liệu kiểm tra xem thông báo toàn cục có bật hay không.
     */
    val isNotificationsEnabled: Flow<Boolean>

    /**
     * Lưu cấu hình bật/tắt thông báo toàn cục.
     */
    suspend fun setNotificationsEnabled(enabled: Boolean)

    /**
     * Luồng dữ liệu lấy danh sách cuộc trò chuyện bị tắt tiếng (Muted)
     * trả về Map chứa conversationId và thời gian hết hạn Mute (timestamp).
     */
    val mutedConversations: Flow<Map<String, Long>>

    /**
     * Tắt tiếng cuộc trò chuyện trong một khoảng thời gian nhất định (miliseconds).
     * durationMs = -1L là tắt tiếng vĩnh viễn (mãi mãi).
     */
    suspend fun muteConversation(conversationId: String, durationMs: Long)

    /**
     * Bật tiếng lại cuộc trò chuyện (Unmute).
     */
    suspend fun unmuteConversation(conversationId: String)
}
