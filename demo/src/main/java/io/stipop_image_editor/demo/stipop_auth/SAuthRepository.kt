package io.stipop_image_editor.demo.stipop_auth

import androidx.annotation.Keep
import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

class SAuthRepository {

    companion object {

        const val API_KEY_VALUE = "YOUR_API_KEY"

        private var appId: String = "YOUR_APP_ID"
        private var clientId: String = "YOUR_APP_CLIENT_ID"
        private var clientSecret: String = "YOUR_APP_CLIENT_SECRET"
        private var refreshToken: String = "YOUR_APP_REFRESH_TOKEN"
        private var expiryTime: Int = 86400

        private var isSAuthWorking = false

        private var sAuthAccessToken = ""
        private var sAuthAccessTokenUserId = ""
        private var shouldRefreshAccessTokenTimeMillis = 0L

        internal suspend fun getAccessTokenIfOverExpiryTime(userId: String): String {
            setIsSAuthWorking(true)
            val currentTimeMillis = System.currentTimeMillis()
            if (sAuthAccessTokenUserId != userId) {
                return getAccessToken(userId)
            } else if (currentTimeMillis >= shouldRefreshAccessTokenTimeMillis) {
                return getAccessToken(userId)
            } else {
                setIsSAuthWorking(false)
                return sAuthAccessToken
            }
        }

        private suspend fun getAccessToken(userId: String): String {
            val result = StipopSampleApi.create().getAccessToken(
                getAccessTokenAPIBody = GetAccessTokenAPIBody(
                    appId = appId,
                    userId = userId,
                    clientId = clientId,
                    clientSecret = clientSecret,
                    refreshToken = refreshToken,
                    expiryTime = expiryTime
                )
            )
            setSAuthInformation(result, userId)
            setIsSAuthWorking(false)
            return sAuthAccessToken
        }

        private fun setIsSAuthWorking(isSAuthWorking: Boolean){
            this.isSAuthWorking = isSAuthWorking
        }

        fun getIsSAuthWorking(): Boolean{
            return isSAuthWorking
        }

        private fun setSAuthInformation(result: GetNewAccessTokenResponse, userId: String){
            val currentTimeMillis = System.currentTimeMillis()
            val expiryTimeMillis = ((if(expiryTime > 60) (expiryTime - 10) else (expiryTime - 1))* 1000).toLong()
            sAuthAccessToken = result.body?.accessToken ?: ""
            sAuthAccessTokenUserId = userId
            shouldRefreshAccessTokenTimeMillis = (currentTimeMillis + expiryTimeMillis)
        }
    }
}

@Keep
internal interface StipopSampleApi {

    @POST("access")
    suspend fun getAccessToken(
        @Body getAccessTokenAPIBody: GetAccessTokenAPIBody
    ): GetNewAccessTokenResponse

    companion object {

        fun create(): StipopSampleApi {
            val headers = Headers.Builder()
                .add("api_key", SAuthRepository.API_KEY_VALUE)
                .build()

            val loggingInterceptor = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
            val client = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.MINUTES)
                .readTimeout(10, TimeUnit.MINUTES)
                .writeTimeout(10, TimeUnit.MINUTES)
                .addInterceptor(loggingInterceptor)
                .addInterceptor(Interceptor {
                    it.proceed(it.request().newBuilder().headers(headers).build())
                })
                .addNetworkInterceptor {
                    it.proceed(it.request())
                }
                .build()

            val BASE_URL = "https://messenger.stipop.io/v1/"
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(StipopSampleApi::class.java)
        }
    }
}
