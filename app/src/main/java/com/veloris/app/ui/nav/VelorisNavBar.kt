package com.veloris.app.ui.nav

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.veloris.app.ui.theme.Accent
import com.veloris.app.ui.theme.TextLo

data class NavItem(val label: String, val icon: ImageVector)

/** Navbar pil melayang: gradien kaca, bayangan glow, indikator pegas, label muncul pada tab aktif. */
@Composable
fun VelorisNavBar(items: List<NavItem>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(32.dp)
    Row(
        modifier
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 14.dp)
            .shadow(24.dp, shape, ambientColor = Accent, spotColor = Accent)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Color(0xE61D1D30), Color(0xE6121221))))
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)), shape)
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEachIndexed { i, item -> NavPill(item, i == selected) { onSelect(i) } }
    }
}

@Composable
private fun NavPill(item: NavItem, selected: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(if (selected) Accent.copy(alpha = 0.2f) else Color.Transparent, spring(stiffness = Spring.StiffnessMedium), label = "bg")
    val tint by animateColorAsState(if (selected) Accent else TextLo, label = "tint")
    val scale by animateFloatAsState(if (selected) 1.1f else 1f, spring(Spring.DampingRatioMediumBouncy), label = "scale")
    Row(
        Modifier.clip(CircleShape).background(bg)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .animateContentSize(spring(stiffness = Spring.StiffnessMediumLow)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(item.icon, item.label, tint = tint, modifier = Modifier.size(24.dp).scale(scale))
        AnimatedVisibility(selected) {
            Text(item.label, Modifier.padding(start = 8.dp), color = tint, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
    }
}
