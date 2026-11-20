package com.veloris.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.veloris.app.data.AuthManager
import com.veloris.app.ui.VelorisRoot
import com.veloris.app.ui.theme.VelorisTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleRedirect(intent)
        setContent { VelorisTheme { VelorisRoot() } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleRedirect(intent)
    }

    private fun handleRedirect(i: Intent?) {
        val uri = i?.data ?: return
        lifecycleScope.launch { runCatching { AuthManager.handleRedirect(uri) } }
    }
}
