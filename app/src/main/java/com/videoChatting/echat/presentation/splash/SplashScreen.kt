package com.videoChatting.echat.presentation.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.videoChatting.echat.R
import com.videoChatting.echat.presentation.theme.Background
import com.videoChatting.echat.presentation.theme.ObsidianBlack
import com.videoChatting.echat.presentation.theme.Primary
import com.videoChatting.echat.presentation.theme.TalksyLogoPurple
import com.videoChatting.echat.presentation.theme.purple_200


@Composable
fun SplashScreen(onSplashFinished: () -> Unit) {
    LaunchedEffect(key1 = true) {
        delay(2000) // 2 seconds delay
        onSplashFinished()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxHeight(.85f).fillMaxWidth()
        ) {
            Image(
                painter = painterResource(id = R.drawable.transparent_brand_logo),
                contentDescription = "Talksy Logo",
                modifier = Modifier.fillMaxWidth(.8f)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()){
            Text(
                text = "Be Real. Be You. Be Talksy",
                color = purple_200,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
