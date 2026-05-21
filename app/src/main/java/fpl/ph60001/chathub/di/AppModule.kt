package fpl.ph60001.chathub.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.messaging.FirebaseMessaging
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import fpl.ph60001.chathub.data.repository.AuthRepositoryImpl
import fpl.ph60001.chathub.data.repository.ChatRepositoryImpl
import fpl.ph60001.chathub.domain.repository.AuthRepository
import fpl.ph60001.chathub.domain.repository.ChatRepository
import javax.inject.Singleton

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
        storage: FirebaseStorage
    ): ChatRepository {
        return ChatRepositoryImpl(firestore, storage)
    }
}
