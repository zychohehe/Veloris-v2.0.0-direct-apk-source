package com.veloris.app.ui.library

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.veloris.app.data.AuthManager
import com.veloris.app.data.LibraryData
import com.veloris.app.data.LibraryRepository
import com.veloris.app.data.PlaylistItem
import com.veloris.app.ui.components.*
import com.veloris.app.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun LibraryScreen(lib: LibraryData, syncing: Boolean) {
    val linked by AuthManager.linked.collectAsStateWithLifecycle()
    var filter by rememberSaveable { mutableStateOf("all") }
    val scope = rememberCoroutineScope()
    val rot by rememberInfiniteTransition(label = "r").animateFloat(0f, 360f, infiniteRepeatable(tween(900, easing = LinearEasing)), label = "rot")
    val pls = lib.playlists.filter { filter == "all" || it.source == filter }
    val liked = lib.liked.filter { filter == "all" || it.source == filter }.distinctBy { it.source + it.id }

    LazyColumn(Modifier.fillMaxSize().statusBarsPadding(), contentPadding = PaddingValues(top = 12.dp, bottom = 120.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Library", Modifier.weight(1f), fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = TextHi)
                IconButton({ scope.launch { LibraryRepository.sync() } }) {
                    Icon(Icons.Rounded.Sync, "Sinkron", tint = Accent, modifier = Modifier.rotate(if (syncing) rot else 0f))
                }
            }
        }
        item {
            Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("all" to "Semua", "youtube" to "YouTube", "spotify" to "Spotify").forEach { (k, l) ->
                    FilterChip(filter == k, { filter = k }, label = { Text(l) })
                }
            }
        }
        if (linked.isEmpty()) item { InfoCard("Belum ada akun terhubung", "Hubungkan YouTube atau Spotify di tab Profil untuk sinkron library otomatis.") }
        if (pls.isNotEmpty()) {
            item { SectionTitle("Playlist") }
            items(pls, key = { it.source + it.id }) { PlaylistRow(it) }
        }
        if (liked.isNotEmpty()) {
            item { SectionTitle("Lagu disukai") }
            items(liked, key = { it.source + it.id }) { TrackRow(it) }
        }
    }
}

@Composable
private fun PlaylistRow(p: PlaylistItem) {
    val ctx = LocalContext.current
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp).bounceClick { openUrl(ctx, p.url) }, verticalAlignment = Alignment.CenterVertically) {
        Cover(p.thumb, 56.dp, 14.dp)
        Column(Modifier.padding(start = 14.dp)) {
            Text(p.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold, color = TextHi)
            Text("${p.count} lagu · ${if (p.source == "spotify") "Spotify" else "YouTube"}", color = TextLo, fontSize = 12.sp)
        }
    }
}
