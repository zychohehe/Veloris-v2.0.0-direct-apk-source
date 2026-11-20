package com.veloris.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.veloris.app.data.AuthManager
import com.veloris.app.data.LibraryRepository
import com.veloris.app.ui.home.HomeScreen
import com.veloris.app.ui.home.HomeViewModel
import com.veloris.app.ui.library.LibraryScreen
import com.veloris.app.ui.nav.NavItem
import com.veloris.app.ui.nav.VelorisNavBar
import com.veloris.app.ui.profile.ProfileScreen
import com.veloris.app.ui.search.SearchScreen
import com.veloris.app.ui.theme.Ink

private val ITEMS = listOf(
    NavItem("Beranda", Icons.Rounded.Home),
    NavItem("Cari", Icons.Rounded.Search),
    NavItem("Library", Icons.Rounded.LibraryMusic),
    NavItem("Profil", Icons.Rounded.Person),
)

@Composable
fun VelorisRoot() {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val homeVm: HomeViewModel = viewModel()
    val lib by LibraryRepository.data.collectAsStateWithLifecycle()
    val syncing by LibraryRepository.syncing.collectAsStateWithLifecycle()
    val linked by AuthManager.linked.collectAsStateWithLifecycle()

    // Sinkron otomatis saat app dibuka / akun baru terhubung
    LaunchedEffect(linked) { if (linked.isNotEmpty()) LibraryRepository.sync() }

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF1B1636), Ink), endY = 1400f))) {
        AnimatedContent(
            targetState = tab,
            transitionSpec = { (fadeIn(tween(240, delayMillis = 60)) + slideInVertically(tween(280)) { it / 24 }) togetherWith fadeOut(tween(120)) },
            label = "tabs",
        ) { t ->
            when (t) {
                0 -> HomeScreen(homeVm, lib)
                1 -> SearchScreen()
                2 -> LibraryScreen(lib, syncing)
                else -> ProfileScreen(lib, syncing)
            }
        }
        VelorisNavBar(ITEMS, tab, { tab = it }, Modifier.align(Alignment.BottomCenter))
    }
}
