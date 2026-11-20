package com.veloris.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.veloris.app.data.LibraryData
import com.veloris.app.data.Track
import com.veloris.app.ui.components.*
import com.veloris.app.ui.theme.*
import java.util.Calendar

@Composable
fun HomeScreen(vm: HomeViewModel, library: LibraryData) {
    val s by vm.state.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(Unit) { lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { vm.poll() } }

    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(top = 12.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        item { Header(s.updatedAt) }
        when {
            s.needsKey -> item { InfoCard("YT_API_KEY belum diatur", "Isi YT_API_KEY (lihat README) agar beranda tersinkron real-time dengan YouTube.") }
            s.loading -> item {
                Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Skeleton(Modifier.fillMaxWidth().height(60.dp))
                    Skeleton(Modifier.fillMaxWidth().height(60.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) { repeat(3) { Skeleton(Modifier.size(150.dp)) } }
                }
            }
            else -> {
                if (s.trending.isNotEmpty()) {
                    item { QuickPicks(s.trending.distinctBy { it.id }.take(6)) }
                    item { TrackShelf("Trending sekarang", s.trending) }
                }
                s.mixes.forEach { (n, l) -> item(key = n) { TrackShelf(n, l) } }
                if (s.error) item { InfoCard("Koneksi bermasalah", "Menampilkan data terakhir, mencoba lagi otomatis.") }
            }
        }
        if (library.liked.isNotEmpty()) item { TrackShelf("Dari library kamu", library.liked.take(25)) }
    }
}

@Composable
private fun Header(updatedAt: Long) {
    val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greet = when { h < 11 -> "Selamat pagi"; h < 15 -> "Selamat siang"; h < 18 -> "Selamat sore"; else -> "Selamat malam" }
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(greet, color = TextLo, fontSize = 14.sp)
            Text(
                "Veloris",
                style = TextStyle(brush = Brush.linearGradient(listOf(Accent, Accent2)), fontSize = 32.sp, fontWeight = FontWeight.ExtraBold),
            )
        }
        LiveBadge(updatedAt)
    }
}

@Composable
private fun QuickPicks(list: List<Track>) {
    val ctx = LocalContext.current
    Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        list.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                pair.forEach { t ->
                    Row(
                        Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(Panel).bounceClick { openUrl(ctx, t.url) },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Cover(t.thumb, 54.dp, 0.dp)
                        Text(t.title, Modifier.padding(horizontal = 10.dp), maxLines = 2, overflow = TextOverflow.Ellipsis, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextHi)
                    }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}
