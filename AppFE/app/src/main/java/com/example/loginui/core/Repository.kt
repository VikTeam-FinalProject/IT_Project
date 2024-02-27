package com.example.loginui.core

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.widget.Toast
import com.example.loginui.API.AuthService
import com.example.loginui.data.ModelResource

import com.example.loginui.data.authen.SignInRequest
import com.example.loginui.data.authen.SignInResponse
import com.example.loginui.data.authen.SignUpRequest
import com.example.loginui.data.authen.SignUpResponse
import com.example.loginui.navigation.repo
import com.example.loginui.navigation.user
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.util.Base64

class Repository {

    private val LOGIN_URL = "https://identitytoolkit.googleapis.com/"
    private var currentUser: String = ""
    private val retrofit: Retrofit by lazy {
        val httpLoggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val okHttpClient = OkHttpClient.Builder().addInterceptor(httpLoggingInterceptor).build()

        Retrofit.Builder()
            .baseUrl(LOGIN_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .build()
    }
    fun updateCurrentUser(user:String){
        this.currentUser = user
    }

    val authService: AuthService by lazy {
        retrofit.create(AuthService::class.java)
    }

    private val LOCAL_URL = "http://172.16.168.95:5000/"
    private val uploadRetro: Retrofit = Retrofit.Builder()
        .baseUrl(LOCAL_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    val apiService = uploadRetro.create(AuthService::class.java)
    fun postModelInfo(modelName: String,classes:List<String>,dataSize:Int, bitmaps: List<Bitmap>, context: Context){
        val modelId = encodeObjectId(modelName)
        val modelDetail = ModelResource(
            modelId,
            currentUser,
            modelName,
            classes,
            dataSize
        )
        apiService.postModelInfo(modelDetail).enqueue(object : Callback<ResponseBody>{
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if(response.isSuccessful){
                    println("Model Info Posted")
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                println("Model Info Post Failed")
                print(t.message)
            }
        })
        createNotificationChannel(modelId,context)
        if (bitmaps.isNotEmpty())
            postUserImage(bitmaps,context,modelDetail.modelId)
    }
    fun signIn(email:String,password:String,context: Context,callback: (Boolean) -> Unit){
        val signInRequest = SignInRequest(email, password)

        val signInResponseCall: Call<SignInResponse> =
            repo.authService.userLogin(signInRequest)
        signInResponseCall.enqueue(object : Callback<SignInResponse> {
            override fun onResponse(
                call: Call<SignInResponse>,
                response: Response<SignInResponse>
            ) {
                if (response.isSuccessful) {
                    callback(true)
                    user = email
                    repo.updateCurrentUser(user)
                } else {
                    callback(false)
                }
            }
            override fun onFailure(call: Call<SignInResponse>, t: Throwable) {
                callback(false)
            }
        })
    }
    fun signup(email: String, password: String, callback: (Int) -> Unit) {
        val signUpRequest = SignUpRequest(email, password)
        authService.userSignUp(signUpRequest).enqueue(object : Callback<SignUpResponse> {
            override fun onResponse(call: Call<SignUpResponse>, response: Response<SignUpResponse>) {
                callback(response.code())
            }

            override fun onFailure(call: Call<SignUpResponse>, t: Throwable) {
                callback(500)
            }
        })
    }
    fun signOut(){
        user = ""
    }

    fun postURL( url:String, modelId: String){
        apiService.uploadURL(url,modelId).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    println("URL uploaded successfully")
                } else {
                    println("Upload error: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                println("Error: ${t.message}")
            }
        })
    }

    private fun generateId(modelName: String): String {
        val timestamp = Instant.now().toEpochMilli()
        return "$timestamp-$modelName"
    }

    private fun encodeObjectId(modelName: String): String {
        return Base64.getEncoder().encodeToString(generateId(modelName).toByteArray())
    }
    private fun createNotificationChannel(modelName: String, context: Context) {
        val channelId = "train"
        val channelName = "train_processing"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(channelId, channelName, importance).apply {
            description = "model $modelName processing"
        }
        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)

    }

    private fun postUserImage(bitmaps: List<Bitmap>, context: Context, modelId:String){
        val imagesParts = prepareImagesParts(bitmaps, context)
        apiService.uploadImages(imagesParts,modelId).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    println("Image uploaded successfully")
                } else {
                    println("Upload error: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                println("Error: ${t.message}")
            }
        })
    }

    private fun bitmapToFile(bitmap: Bitmap, context: Context): File {
        val file = File(context.cacheDir, "user_image${System.currentTimeMillis()}.jpg") // Tạo file tạm thời
        file.createNewFile()

        ByteArrayOutputStream().use { bos ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos) // Nén bitmap và ghi vào ByteArrayOutputStream
            val bitmapData = bos.toByteArray()

            FileOutputStream(file).use { fos ->
                fos.write(bitmapData) // Ghi dữ liệu vào file
                fos.flush()
            }
        }

        return file
    }
    private fun prepareImagesParts(bitmaps: List<Bitmap>, context: Context): List<MultipartBody.Part> {
        return bitmaps.map { bitmap ->
            val file = bitmapToFile(bitmap, context)
            val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
            MultipartBody.Part.createFormData("images", file.name, requestFile)
        }
    }

}