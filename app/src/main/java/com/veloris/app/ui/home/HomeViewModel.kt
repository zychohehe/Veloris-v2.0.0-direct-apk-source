package com.veloris.app.ui.home

import androidx.lifecycle.ViewModel
import com.veloris.app.data.Track
import com.veloris.app.data.YouTubeApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class HomeState(
    val loading: Boolean = true,
    val trending: List<Track> = emptyList(),
    val mixes: List<Pair<String, List<Track>>> = emptyList(),
    val updatedAt: Long = 0L,
    val needsKey: Boolean = false,
    val error: Boolean = false,
)

class HomeViewModel : ViewModel() {
    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state
    private var tick = 0

    /** Dipanggil hanya saat layar aktif (repeatOnLifecycle). Trending tiap 60 dtk; mix tiap ~30 menit agar hemat kuota API. */
    suspend fun poll() {
        while (true) {
            refresh(withMixes = tick % 30 == 0 || _state.value.mixes.isEmpty())
            tick++
            delay(60_000)
        }
    }

    private suspend fun refresh(withMixes: Boolean) {
        if (!YouTubeApi.enabled) {
            _state.value = HomeState(loading = false, needsKey = true)
            return
        }
        try {
            coroutineScope {
                val tr = async { YouTubeApi.trending() }
                val mx = if (withMixes) MIXES.map { (n, q) -> async { n to YouTubeApi.search(q) } } else emptyList()
                val old = _state.value.mixes
                val newMixes = if (withMixes) mx.awaitAll().filter { it.second.isNotEmpty() } else old
                _state.value = HomeState(false, tr.await(), newMixes, System.currentTimeMillis())
            }
        } catch (e: Exception) {
            _state.value = _state.value.copy(loading = false, error = true)
        }
    }

    companion object {
        val MIXES = listOf(
            "Chill Vibes" to "chill music mix",
            "Pop Indonesia" to "lagu pop indonesia terbaru",
            "Hip-Hop" to "hip hop new releases",
            "Lofi Fokus" to "lofi hip hop focus",
        )
    }
}
