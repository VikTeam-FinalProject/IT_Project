package com.example.loginui.Screen

import android.net.Uri
import android.util.Log
import android.view.ViewGroup
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Mail
import androidx.compose.material.icons.rounded.PartyMode
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavHostController
import com.example.loginui.navigation.repo
import com.example.loginui.navigation.user
import com.example.loginui.ui.theme.GoldSand
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView

@Composable
fun UrlInputTextBox(navController: NavHostController,modelId:String) {
    val model = repo.getModel(modelId)
    Column {
        val isClick = remember { mutableStateOf(false) }
        var videoUri by remember { mutableStateOf<Uri?>(null) }
        var videoReady by remember { mutableStateOf(false) }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.padding(start = 8.dp)

        ) {
            Icon(
                imageVector = Icons.Rounded.Mail,
                contentDescription = "Model Info",
                modifier = Modifier
                    .size(25.dp)
                    .padding(end = 3.dp, top = 2.dp),
                tint = GoldSand
            )
            Text(
                text = "Email: $user",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp
            )
        }
        Row(
            modifier = Modifier.padding(start = 8.dp)

        ) {
            Icon(
                imageVector = Icons.Rounded.PartyMode,
                contentDescription = "Model Info",
                modifier = Modifier
                    .size(25.dp)
                    .padding(end = 3.dp, top = 2.dp),
                tint = GoldSand
            )
            Text(
                text = "Model: ${model.modelName}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp
            )
        }
        Row(
            modifier = Modifier.padding(start = 8.dp)

        ) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = "Model Info",
                modifier = Modifier
                    .size(25.dp)
                    .padding(end = 3.dp, top = 2.dp),
                tint = GoldSand
            )
            Text(
                text = "Accuracy: ...",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp
            )
        }

        Column(modifier = Modifier.padding(16.dp)) {
            var url by remember { mutableStateOf("") }
            val isValidUrl = remember(url) { url.isValidUrl() }

            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("Enter URL") },
                isError = !isValidUrl,
                singleLine = true,
                modifier = Modifier.padding(bottom = 8.dp),
                trailingIcon = {
                    if (url.isNotEmpty()) {
                        IconButton(onClick = {
                            isClick.value = true
                        }) {
                            Icon(
                                imageVector = Icons.Filled.Clear,
                                contentDescription = "Clear text"
                            )
                        }
                    }
                },
            )
            if (!isValidUrl) {
                Text(
                    text = "Please enter a valid URL",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Button(
                onClick = {
                    repo.postURL(url,modelId)
                },
                enabled = videoReady
            ) {
                Text("Start Calculate")
            }

            val pickVideoLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetContent(),
                onResult = { uri: Uri? ->
                    videoUri = uri
                }
            )

            Button(onClick = { pickVideoLauncher.launch("video/*") }) {
                Text("Pick Video")
            }
            if(isClick.value){
                VideoPlayer(Uri.parse(url)){
                    videoReady = it
                }
                url = ""
            }
            if (videoUri != null) {
                VideoPlayer(videoUri!!){
                    videoReady = it
                }
            }
        }
    }



}


@Composable
fun VideoPlayer(uri: Uri, videoReady:(Boolean)->Unit) {
    val validYoutube = listOf("youtube.com", "youtu.be")
    var isYoutubeLink = false
    val localLifeCycle = LocalLifecycleOwner.current
    Log.d(TAG, "VideoPlayer: $uri")
    AndroidView(
        modifier = Modifier.fillMaxWidth(),
        factory = { context ->
            validYoutube.forEach{
                if(uri.toString().contains(it)){
                    isYoutubeLink = true
                }
            }
            when{
                isYoutubeLink -> {
                    val youtubeVideoId = extractYouTubeVideoIdFromShortUrl(uri.toString())
                    print(youtubeVideoId)
                    YouTubePlayerView(context = context).apply {
                    localLifeCycle.lifecycle.addObserver(this)
                    addYouTubePlayerListener(object : AbstractYouTubePlayerListener() {
                        override fun onReady(youTubePlayer: YouTubePlayer) {
                            youTubePlayer.loadVideo(youtubeVideoId, 0f)
                        }
                    })
                    }
                }
                else ->
                {
                    VideoView(context).apply {
                        setMediaController(MediaController(context))
                        setVideoURI(uri)
                        requestFocus()
                        start()
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                    }
                }
            }
        },
        update = {
            videoReady(true)
        }
    )
}
fun extractYouTubeVideoIdFromShortUrl(url: String): String {
    val path = url.substringAfter("youtu.be/").substringAfter("watch?v=")
    return path.substringBefore('?').substringBefore('&')
}
fun String.isValidUrl(): Boolean = this.isNotEmpty() && android.util.Patterns.WEB_URL.matcher(this).matches()