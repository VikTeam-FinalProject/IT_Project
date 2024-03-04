package com.example.loginui.core

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.example.loginui.API.AuthService
import com.example.loginui.BuildConfig
import com.example.loginui.data.ListModelResponse
import com.example.loginui.data.Model
import com.example.loginui.data.ModelResource
import com.example.loginui.data.User
import com.example.loginui.data.Video

import com.example.loginui.data.authen.SignInRequest
import com.example.loginui.data.authen.SignInResponse
import com.example.loginui.data.authen.SignUpRequest
import com.example.loginui.data.authen.SignUpResponse
import com.example.loginui.navigation.repo
import com.example.loginui.navigation.user
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
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
import java.time.LocalDate
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class Repository {

    private val LOGIN_URL = BuildConfig.LOGIN_URL
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
    private lateinit var listModelResponse:ListModelResponse
    fun updateCurrentUser(user:String){
        this.currentUser = user
    }

    private val authService: AuthService by lazy {
        retrofit.create(AuthService::class.java)
    }
    fun createStorage(user_id:User, callback: (Boolean) -> Unit){
        apiService.createStorage(user_id).enqueue(object : Callback<ResponseBody>{
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if(response.isSuccessful){
                    callback(true)
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                println("Storage Creation Failed")
                callback(false)
                print(t.message)
            }
        })
    }
    private val LOCAL_URL = BuildConfig.LOCAL_URL
    private val uploadRetro: Retrofit = Retrofit.Builder()
        .baseUrl(LOCAL_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    val apiService = uploadRetro.create(AuthService::class.java)
    fun postModelInfo(modelName: String,classes:List<String>,dataSize:Int, bitmaps: List<Bitmap>, context: Context){
        val modelId = "CO"
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
                    if (bitmaps.isNotEmpty()) {
                        postUserImage(bitmaps, context, modelDetail.modelId)
                        println("Image Uploaded")
                    }
                    trainModel(modelDetail.modelId)
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                println("Model Info Post Failed")
                print(t.message)
            }
        })
        createNotificationChannel(modelId,context)
    }

    fun signIn(email:String,password:String,callback: (Boolean) -> Unit){
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
                    updateCurrentUser(response.body()?.localId.toString())
                } else {
                    callback(false)
                }
            }
            override fun onFailure(call: Call<SignInResponse>, t: Throwable) {
                callback(false)
            }
        })
    }

    fun trainModel(modelId:String){
        apiService.trainModel(Model(modelId)).enqueue(object : Callback<Void>{
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if(response.isSuccessful){
                    println("Model Training...")
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                println("Model Train Failed")
                print(t.message)
            }
        })
    }

    fun signup(email: String, password: String, callback: (Int, String) -> Unit) {
        val signUpRequest = SignUpRequest(email, password)
        authService.userSignUp(signUpRequest).enqueue(object : Callback<SignUpResponse> {
            override fun onResponse(call: Call<SignUpResponse>, response: Response<SignUpResponse>) {
                Log.d("111", "onResponse: ${response.code()}")
                callback(response.code(), response.body()?.localId.toString())
            }

            override fun onFailure(call: Call<SignUpResponse>, t: Throwable) {
                callback(500,"")
            }
        })
    }
    fun signOut(){
        user = ""
    }

    fun postURL( url:String, modelId: String){
        apiService.uploadURL(Video(url),modelId).enqueue(object : Callback<ResponseBody> {
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
        println("height"+bitmap.height)
        println("width"+bitmap.width)
        val file = File(context.cacheDir, "user_image${System.currentTimeMillis()}.jpg") // Tạo file tạm thời
        file.createNewFile()

        ByteArrayOutputStream().use { bos ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos)
            val bitmapData = bos.toByteArray()
            println(bitmapData.size)
            FileOutputStream(file).use { fos ->
                fos.write(bitmapData)
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

    suspend fun updateModelList():ListModelResponse? = suspendCancellableCoroutine{
        continuation ->
        apiService.getModelList(currentUser).enqueue(object : Callback<ListModelResponse> {
            override fun onResponse(call: Call<ListModelResponse>, response: Response<ListModelResponse>) {
                if (response.isSuccessful) {
                    continuation.resume(response.body()!!)
                }
                else {
                    continuation.resume(null)
                }
            }

            override fun onFailure(call: Call<ListModelResponse>, t: Throwable) {
                continuation.resumeWithException(t)
            }
        })
    }
    fun setModel(ls:ListModelResponse){
        listModelResponse = ls
    }
    fun getModelList():ListModelResponse{
        return listModelResponse
    }
    fun getModel(model_id: String):ModelResource{
        return listModelResponse.models.find {
            it.modelId == model_id
        }!!
    }

}