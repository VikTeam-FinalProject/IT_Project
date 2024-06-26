package com.example.loginui.API

import com.example.loginui.BuildConfig
import com.example.loginui.data.ListModelResponse
import com.example.loginui.data.Model
import com.example.loginui.data.ModelResource
import com.example.loginui.data.User
import com.example.loginui.data.authen.SignInRequest
import com.example.loginui.data.authen.SignInResponse
import com.example.loginui.data.authen.SignUpRequest
import com.example.loginui.data.authen.SignUpResponse
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.Call
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query


interface AuthService {
        @POST("v1/accounts:signInWithPassword?key=${BuildConfig.API_KEY}")
        fun userLogin(@Body request: SignInRequest): Call<SignInResponse>

        @POST("v1/accounts:signUp?key=${BuildConfig.API_KEY}")
        fun userSignUp(@Body request: SignUpRequest): Call<SignUpResponse>
        @Multipart
        @POST("api/models/{model_id}/images")
        fun uploadImages(@Part images: List<MultipartBody.Part>,@Path("model_id") model_id:String): Call<ResponseBody>

        @POST("api/models")
        fun postModelInfo(@Body modelDetail: ModelResource): Call<Void>

        @Multipart
        @POST("api/models/{model_id}/videos")
        fun uploadVideo(@Part video: MultipartBody.Part?=null,@Part("url") url:RequestBody?=null,@Part("token") token:RequestBody,@Path("model_id") model_id: String): Call<Void>
        @GET("api/models")
        fun getModelList(@Query("user_id") field:String): Call<ListModelResponse>

        @POST("api/yolo")
        fun trainModel(@Body model_id: Model): Call<Void>

        @POST("api/users")
        fun createStorage(@Body user_id:User): Call<ResponseBody>
}
