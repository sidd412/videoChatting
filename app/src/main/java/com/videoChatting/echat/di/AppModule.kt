package com.videoChatting.echat.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.videoChatting.echat.data.repository_impl.MatchmakingRepositoryImpl
import com.videoChatting.echat.data.repository_impl.MessageRepositoryImpl
import com.videoChatting.echat.data.repository_impl.UserRepositoryImpl
import com.videoChatting.echat.domain.repository.MatchmakingRepository
import com.videoChatting.echat.domain.repository.MessageRepository
import com.videoChatting.echat.domain.repository.UserRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides
    @Singleton
    fun provideUserRepository(
        firestore: FirebaseFirestore,
        auth: FirebaseAuth
    ): UserRepository {
        return UserRepositoryImpl(firestore, auth)
    }

    @Provides
    @Singleton
    fun provideMatchmakingRepository(
        firestore: FirebaseFirestore
    ): MatchmakingRepository {
        return MatchmakingRepositoryImpl(firestore)
    }

    @Provides
    @Singleton
    fun provideMessageRepository(
        firestore: FirebaseFirestore
    ): MessageRepository {
        return MessageRepositoryImpl(firestore)
    }
}
