package com.veloris.app.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.veloris.app.data.Track
import com.veloris.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun openUrl(ctx: Context, url: String) {
    runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

/** Klik dengan efek pegas halus (scale saat ditekan). */
fun Modifier.bounceClick(onClick: () -> Unit): Modifier = composed {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val s by animateFloatAsState(
        if (pressed) 0.95f else 1f,
        spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium), label = "press",
    )
    this.graphicsLayer { scaleX = s; scaleY = s }.clickable(interactionSource = src, indication = null, onClick = onClick)
}

@Composable
fun Cover(url: String?, size: Dp, radius: Dp = 14.dp, modifier: Modifier = Modifier) {
    Box(
        modifier.size(size).clip(RoundedCornerShape(radius))
            .background(Brush.linearGradient(listOf(Accent.copy(.35f), Accent2.copy(.25f)))),
    ) {
        if (url != null) AsyncImage(model = url, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(text, Modifier.padding(horizontal = 20.dp, vertical = 8.dp), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextHi)
}

@Composable
fun TrackCard(t: Track, size: Dp = 150.dp) {
    val ctx = LocalContext.current
    Column(Modifier.width(size).bounceClick { openUrl(ctx, t.url) }) {
        Cover(t.thumb, size, 18.dp)
        Text(t.title, Modifier.padding(top = 8.dp), maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = TextHi)
        Text(t.artist, maxLines = 1, overflow = TextOverflow.Ellipsis, color = TextLo, fontSize = 12.sp)
    }
}

@Composable
fun TrackRow(t: Track) {
    val ctx = LocalContext.current
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp).bounceClick { openUrl(ctx, t.url) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Cover(t.thumb, 54.dp, 12.dp)
        Column(Modifier.padding(start = 14.dp).weight(1f)) {
            Text(t.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold, color = TextHi, fontSize = 15.sp)
            Text("${t.artist} · ${if (t.source == "spotify") "Spotify" else "YouTube"}", maxLines = 1, overflow = TextOverflow.Ellipsis, color = TextLo, fontSize = 12.sp)
        }
    }
}

@Composable
fun TrackShelf(title: String, tracks: List<Track>) {
    Column {
        SectionTitle(title)
        LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            items(tracks.distinctBy { it.id }, key = { it.source + it.id }) { TrackCard(it) }
        }
    }
}

@Composable
fun Skeleton(modifier: Modifier) {
    val a by rememberInfiniteTransition(label = "sk").animateFloat(
        initialValue = 0.25f, targetValue = 0.6f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "a",
    )
    Box(modifier.clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = a * 0.2f)))
}

@Composable
fun InfoCard(title: String, body: String) {
    Column(Modifier.padding(horizontal = 20.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Panel).padding(16.dp)) {
        Text(title, fontWeight = FontWeight.Bold, color = TextHi)
        Text(body, Modifier.padding(top = 4.dp), color = TextLo, fontSize = 13.sp)
    }
}

@Composable
fun LiveBadge(updatedAt: Long) {
    val a by rememberInfiniteTransition(label = "live").animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "dot",
    )
    Row(
        Modifier.clip(CircleShape).background(Color.White.copy(.07f)).padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(7.dp).alpha(a).clip(CircleShape).background(Accent2))
        Spacer(Modifier.width(6.dp))
        Text(
            if (updatedAt > 0) "LIVE · " + SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(updatedAt)) else "LIVE",
            fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextHi,
        )
    }
}
