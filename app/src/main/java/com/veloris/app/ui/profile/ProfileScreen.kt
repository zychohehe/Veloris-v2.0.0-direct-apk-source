package com.veloris.app.ui.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.veloris.app.BuildConfig
import com.veloris.app.R
import com.veloris.app.data.AuthManager
import com.veloris.app.data.LibraryData
import com.veloris.app.data.LibraryRepository
import com.veloris.app.data.Provider
import com.veloris.app.ui.components.Cover
import com.veloris.app.ui.components.bounceClick
import com.veloris.app.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProfileScreen(lib: LibraryData, syncing: Boolean) {
    val linked by AuthManager.linked.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val main = lib.profiles.firstOrNull()
    val synced = if (lib.syncedAt > 0) SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date(lib.syncedAt)) else "belum pernah"

    LazyColumn(Modifier.fillMaxSize().statusBarsPadding(), contentPadding = PaddingValues(top = 12.dp, bottom = 120.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                if (main?.avatar != null) Cover(main.avatar, 96.dp, 48.dp)
                else Box(Modifier.size(96.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Accent, Accent2))), contentAlignment = Alignment.Center) {
                    Text((main?.name ?: "V").take(1).uppercase(), fontSize = 38.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
                }
                Text(main?.name ?: "Tamu Veloris", Modifier.padding(top = 12.dp), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextHi)
                Text("${lib.liked.size} lagu disukai · ${lib.playlists.size} playlist", color = TextLo, fontSize = 13.sp)
            }
        }
        item { Text("Akun terhubung", Modifier.padding(horizontal = 20.dp), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextHi) }
        items(Provider.values().toList(), key = { it.id }) { p ->
            ProviderCard(p, p in linked, lib.profiles.firstOrNull { it.source == p.id }?.name)
        }
        item {
            Row(Modifier.padding(horizontal = 20.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Sinkron terakhir: $synced", color = TextHi, fontSize = 13.sp)
                    Text("Auto-sync tiap 15 menit & saat akun terhubung", color = TextLo, fontSize = 12.sp)
                }
                OutlinedButton({ scope.launch { LibraryRepository.sync() } }, enabled = !syncing && linked.isNotEmpty()) {
                    Text(if (syncing) "Menyinkron…" else "Sinkron")
                }
            }
        }
        item { About() }
    }
}

@Composable
private fun ProviderCard(p: Provider, connected: Boolean, name: String?) {
    val ctx = LocalContext.current
    val c = if (p == Provider.SPOTIFY) Color(0xFF1DB954) else Color(0xFFFF4444)
    Row(Modifier.padding(horizontal = 20.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Panel).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(44.dp).clip(CircleShape).background(c.copy(.2f)), contentAlignment = Alignment.Center) {
            Text(p.label.take(1), color = c, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
        }
        Column(Modifier.padding(horizontal = 12.dp).weight(1f)) {
            Text(p.label, fontWeight = FontWeight.SemiBold, color = TextHi)
            Text(if (connected) (name ?: "Terhubung") else "Belum terhubung", color = TextLo, fontSize = 12.sp)
        }
        if (connected) OutlinedButton({ AuthManager.logout(p) }) { Text("Putuskan") }
        else Button({ AuthManager.start(ctx, p) }) { Text("Hubungkan") }
    }
}

@Composable
private fun About() {
    Row(Modifier.padding(horizontal = 20.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Panel).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Image(painterResource(R.drawable.ic_claude), "Logo Claude", Modifier.size(44.dp))
        Column(Modifier.padding(start = 14.dp)) {
            Text("Kontributor", color = TextLo, fontSize = 12.sp)
            Text("Claude · Anthropic", fontWeight = FontWeight.Bold, color = TextHi)
            Text("Veloris v${BuildConfig.VERSION_NAME}", color = TextLo, fontSize = 12.sp)
        }
    }
}
