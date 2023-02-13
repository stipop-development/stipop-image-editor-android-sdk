package io.stipop_image_editor.demo.stipop_auth

import com.google.gson.annotations.SerializedName

internal data class GetAccessTokenAPIBody(
    @SerializedName("appId") val appId: String,
    @SerializedName("userId") val userId: String,
    @SerializedName("clientId") val clientId: String,
    @SerializedName("clientSecret") val clientSecret: String,
    @SerializedName("refreshToken") val refreshToken: String,
    @SerializedName("expiryTime") val expiryTime: Int
)

internal data class GetNewAccessTokenResponse(
    @SerializedName("header") val header: ResponseHeader,
    @SerializedName("body") val body: ResponseBody?
){
    data class ResponseBody(@SerializedName("accessToken") val accessToken: String?)
}

internal data class ResponseHeader(
    @SerializedName("code") val code: String,
    @SerializedName("status") val status: String,
    @SerializedName("message") val message: String
)