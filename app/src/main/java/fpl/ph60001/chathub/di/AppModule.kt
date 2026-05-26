package fpl.ph60001.chathub.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.messaging.FirebaseMessaging
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import fpl.ph60001.chathub.data.repository.AuthRepositoryImpl
import fpl.ph60001.chathub.data.repository.ChatRepositoryImpl
import fpl.ph60001.chathub.data.repository.MessageRepositoryImpl
import fpl.ph60001.chathub.data.repository.SettingsRepositoryImpl
import fpl.ph60001.chathub.domain.repository.AuthRepository
import fpl.ph60001.chathub.domain.repository.ChatRepository
import fpl.ph60001.chathub.domain.repository.MessageRepository
import fpl.ph60001.chathub.domain.repository.SettingsRepository
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "chathub_settings")

/**
 * Lớp Module cung cấp các dependencies toàn cục cho ứng dụng bằng Dagger Hilt.
 * Được cài đặt trong SingletonComponent để sống suốt vòng đời ứng dụng.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // ==========================================
// CUNG CẤP CÁC PHIÊN BẢN (INSTANCES) FIREBASE
// ==========================================

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        return FirebaseFirestore.getInstance()
    }

    @Provides
    @Singleton
    fun provideFirebaseStorage(): FirebaseStorage {
        return FirebaseStorage.getInstance()
    }

    @Provides
    @Singleton
    fun provideFirebaseMessaging(): FirebaseMessaging {
        return FirebaseMessaging.getInstance()
    }

    // ==========================================
// CUNG CẤP REPOSITORIES CHO TOÀN BỘ APP
// ==========================================

    @Provides
    @Singleton
    fun provideAuthRepository(
        firebaseAuth: FirebaseAuth,
        firestore: FirebaseFirestore
    ): AuthRepository {
        return AuthRepositoryImpl(firebaseAuth, firestore)
    }

    @Provides
    @Singleton
    fun provideChatRepository(
        firestore: FirebaseFirestore,
        storage: FirebaseStorage,
        auth: FirebaseAuth
    ): ChatRepository {
        return ChatRepositoryImpl(firestore, storage, auth)
    }

    @Provides
    @Singleton
    fun provideMessageRepository(
        firestore: FirebaseFirestore,
        storage: FirebaseStorage
    ): MessageRepository {
        return MessageRepositoryImpl(firestore, storage)
    }

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return context.dataStore
    }

    @Provides
    @Singleton
    fun provideSettingsRepository(dataStore: DataStore<Preferences>): SettingsRepository {
        return SettingsRepositoryImpl(dataStore)
    }
}
