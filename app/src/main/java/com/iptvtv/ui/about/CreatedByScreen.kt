package com.iptvtv.ui.about

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.iptvtv.ui.theme.IptvColors

private const val GITHUB_URL = "https://github.com/mrandiw/iptv-android-tv"
private const val INSTAGRAM_URL = "https://www.instagram.com/codewithandiw/"

@Composable
fun CreatedByScreen() {
    val context = LocalContext.current

    Column(
        Modifier
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
            .padding(32.dp)
            .widthIn(max = 840.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        Text("Created By", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        // Creator Profile Card
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(IptvColors.Surface, RoundedCornerShape(16.dp))
                .padding(24.dp),
        ) {
            Box(
                Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(IptvColors.Red),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = Color.White,
                )
            }
            Spacer(Modifier.width(20.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "mrandiw",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
                Text(
                    text = "Creator & Lead Developer",
                    style = MaterialTheme.typography.bodyMedium,
                    color = IptvColors.Red,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = "IPTV for Android TV — Free live streaming around the world.",
                    style = MaterialTheme.typography.bodySmall,
                    color = IptvColors.TextSecondary,
                )
            }
        }

        Text(
            text = "Connect & Links",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )

        // GitHub Card
        SocialLinkCard(
            title = "GitHub Repository",
            subtitle = GITHUB_URL,
            description = "Explore the project source code, submit issues, or contribute.",
            actionLabel = "Open GitHub",
            onClick = { openUrl(context, GITHUB_URL) },
        )

        // Instagram Card
        SocialLinkCard(
            title = "Instagram (@codewithandiw)",
            subtitle = INSTAGRAM_URL,
            description = "Follow for development updates, tips, and future releases.",
            actionLabel = "Open Instagram",
            onClick = { openUrl(context, INSTAGRAM_URL) },
        )
    }
}

@Composable
private fun SocialLinkCard(
    title: String,
    subtitle: String,
    description: String,
    actionLabel: String,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.colors(
            containerColor = IptvColors.Surface,
            focusedContainerColor = IptvColors.SurfaceHigh,
        ),
        shape = CardDefaults.shape(RoundedCornerShape(14.dp)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = IptvColors.Red)
                }
                Button(onClick = onClick) {
                    Text(actionLabel)
                }
            }
            Text(description, style = MaterialTheme.typography.bodySmall, color = IptvColors.TextSecondary)
        }
    }
}

private fun openUrl(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(context, url, Toast.LENGTH_LONG).show()
    }
}
