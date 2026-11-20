package com.veloris.app.ui.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.veloris.app.data.Track
import com.veloris.app.data.YouTubeApi
import com.veloris.app.ui.components.InfoCard
import com.veloris.app.ui.components.TrackRow
import com.veloris.app.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun SearchScreen() {
    var q by rememberSaveable { mutableStateOf("") }
    var res by remember { mutableStateOf(emptyList<Track>()) }
    LaunchedEffect(q) {
        if (q.length < 2) { res = emptyList(); return@LaunchedEffect }
        delay(500) // debounce, hemat kuota API
        res = runCatching { YouTubeApi.search(q) }.getOrDefault(emptyList())
    }
    LazyColumn(Modifier.fillMaxSize().statusBarsPadding(), contentPadding = PaddingValues(top = 12.dp, bottom = 120.dp)) {
        item {
            Text("Cari", Modifier.padding(horizontal = 20.dp), fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = TextHi)
            OutlinedTextField(
                q, { q = it }, Modifier.fillMaxWidth().padding(20.dp), singleLine = true,
                placeholder = { Text("Lagu, artis, album…") }, shape = RoundedCornerShape(18.dp),
            )
        }
        if (!YouTubeApi.enabled) item { InfoCard("YT_API_KEY belum diatur", "Pencarian membutuhkan YouTube Data API key (lihat README).") }
        items(res.distinctBy { it.id }, key = { it.id }) { TrackRow(it) }
    }
}
