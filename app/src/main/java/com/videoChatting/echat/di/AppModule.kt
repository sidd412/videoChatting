package com.videoChatting.echat.di

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.videoChatting.echat.data.local.SessionManager
import com.videoChatting.echat.data.remote.ApiService
import com.videoChatting.echat.data.remote.AuthInterceptor
import com.videoChatting.echat.data.repository_impl.MatchmakingRepositoryImpl
import com.videoChatting.echat.data.repository_impl.MessageRepositoryImpl
import com.videoChatting.echat.data.repository_impl.UserRepositoryImpl
import com.videoChatting.echat.domain.repository.MatchmakingRepository
import com.videoChatting.echat.domain.repository.MessageRepository
import com.videoChatting.echat.domain.repository.UserRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
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
    fun provideSessionManager(
        @ApplicationContext context: Context
    ): SessionManager = SessionManager(context)

    @Provides
    @Singleton
    fun provideHttpLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(com.videoChatting.echat.utils.Constants.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService {
        return retrofit.create(ApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideUserRepository(
        firestore: FirebaseFirestore,
        auth: FirebaseAuth,
        apiService: ApiService,
        sessionManager: SessionManager
    ): UserRepository {
        // We will update UserRepositoryImpl to support custom backend API requests
        return UserRepositoryImpl(firestore, auth, apiService, sessionManager)
    }

    @Provides
    @Singleton
    fun provideMatchmakingRepository(
        socketManager: com.videoChatting.echat.data.remote.SocketManager
    ): MatchmakingRepository {
        return MatchmakingRepositoryImpl(socketManager)
    }

    @Provides
    @Singleton
    fun provideMessageRepository(
        apiService: ApiService,
        socketManager: com.videoChatting.echat.data.remote.SocketManager,
        dbHelper: com.videoChatting.echat.data.local.ChatDatabaseHelper
    ): MessageRepository {
        return MessageRepositoryImpl(apiService, socketManager, dbHelper)
    }
}
