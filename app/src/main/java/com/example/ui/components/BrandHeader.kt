package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.PlusJakartaSansFamily

@Composable
fun BrandHeader(
    modifier: Modifier = Modifier,
    showVersion: Boolean = true,
    onBrandClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .testTag("brand_header")
            .then(
                if (onBrandClick != null) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onBrandClick() }
                } else Modifier
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.savetrick_logo),
            contentDescription = "SaveTrick Logo",
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(
                text = buildAnnotatedString {
                    withStyle(
                        style = SpanStyle(
                            color = MaterialTheme.colorScheme.onBackground,
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
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = PlusJakartaSansFamily,
                    fontSize = 22.sp,
                    letterSpacing = (-0.5).sp
                )
            )

            if (showVersion) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "v2.5.7",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        ),
                        modifier = Modifier.testTag("app_version_tag")
                    )
                }
            }
        }
    }
}
