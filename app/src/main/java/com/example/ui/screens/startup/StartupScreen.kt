package com.example.ui.screens.startup

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.repository.SaveTrickRepository
import com.example.ui.theme.DeepBlack
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.PlusJakartaSansFamily
import kotlinx.coroutines.delay

@Composable
fun StartupScreen(
    repository: SaveTrickRepository,
    onNavigateToHome: () -> Unit,
    onNavigateToOnboarding: () -> Unit,
    modifier: Modifier = Modifier
) {
    var startAnimation by remember { mutableStateOf(false) }

    val alphaAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "startup_alpha"
    )

    val scaleAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.85f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "startup_scale"
    )

    val animEnabled = repository.preferences.isStartupAnimationEnabled()

    LaunchedEffect(Unit) {
        startAnimation = true
        val waitTime = if (animEnabled) 1500L else 300L
        delay(waitTime)

        if (repository.preferences.isFirstLaunch()) {
            onNavigateToOnboarding()
        } else {
            onNavigateToHome()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DeepBlack)
            .testTag("startup_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .alpha(alphaAnim)
                .scale(scaleAnim)
        ) {
            // SaveTrick Logo with subtle rounded corners
            Image(
                painter = painterResource(id = R.drawable.savetrick_logo),
                contentDescription = "SaveTrick Logo",
                modifier = Modifier
                    .size(110.dp)
                    .clip(RoundedCornerShape(24.dp))
            )

            Spacer(modifier = Modifier.height(20.dp))

            // SaveTrick App Title
            Text(
                text = buildAnnotatedString {
                    withStyle(
                        style = SpanStyle(
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold
                        )
                    ) {
                        append("Save")
                    }
                    withStyle(
                        style = SpanStyle(
                            color = ElectricCyan,
                            fontWeight = FontWeight.ExtraBold
                        )
                    ) {
                        append("Trick")
                    }
                },
                style = MaterialTheme.typography.displayMedium.copy(
                    fontFamily = PlusJakartaSansFamily,
                    fontSize = 32.sp,
                    letterSpacing = (-0.5).sp
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = stringResource(R.string.app_tagline),
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color(0xFF94A3B8),
                    fontSize = 13.sp
                )
            )

            Spacer(modifier = Modifier.height(36.dp))

            CircularProgressIndicator(
                color = ElectricCyan,
                strokeWidth = 2.5.dp,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
