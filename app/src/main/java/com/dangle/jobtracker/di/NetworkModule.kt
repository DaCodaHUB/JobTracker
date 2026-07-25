package com.dangle.jobtracker.di

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.network.websocket.WebSocketNetworkTransport
import com.dangle.jobtracker.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideApolloClient(): ApolloClient {
        val serverUrl = BuildConfig.BASE_URL
        val webSocketUrl = serverUrl.replace("http", "ws")
        
        return ApolloClient.Builder()
            .serverUrl(serverUrl)
            .subscriptionNetworkTransport(
                WebSocketNetworkTransport.Builder()
                    .serverUrl(webSocketUrl)
                    .build()
            )
            .build()
    }
}
