package com.example.geofencegroups.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.geofencegroups.ui.components.*
import com.example.geofencegroups.ui.theme.Orbit
import com.example.geofencegroups.ui.theme.Space
import kotlinx.coroutines.launch

private data class Page(val title: String, val body: String)

private val pages = listOf(
    Page("Draw a boundary", "Pick a place that matters — a campsite, a hotel, a school gate — and choose how far it reaches."),
    Page("Bring your group in", "Share a six-character code. Everyone who joins sees the same boundary and the same history."),
    Page("A heads-up, nothing more", "Orbit tells the group when someone leaves or comes back. It doesn't show live locations or keep a trail."),
)

@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val pager = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()
    val last = pager.currentPage == pages.lastIndex
    Column(Modifier.fillMaxSize().background(Orbit.Paper).statusBarsPadding().padding(horizontal = Space.xl)) {
        Row(Modifier.fillMaxWidth().height(56.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            if (!last) QuietButton("Skip", onDone)
        }
        HorizontalPager(pager, Modifier.weight(1f)) { i ->
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
                OrbitMark(Modifier.size(160.dp))
                Spacer(Modifier.height(Space.xxl))
                Text(pages[i].title, style = MaterialTheme.typography.displayMedium)
                Spacer(Modifier.height(Space.md))
                Text(pages[i].body, style = MaterialTheme.typography.bodyLarge, color = Orbit.ForestSoft)
            }
        }
        Row(Modifier.fillMaxWidth().padding(vertical = Space.lg), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            pages.indices.forEach { i ->
                Box(Modifier.height(6.dp).width(if (i == pager.currentPage) 24.dp else 6.dp).clip(CircleShape)
                    .background(if (i == pager.currentPage) Orbit.Forest else Orbit.Line))
            }
        }
        PrimaryButton(if (last) "Get started" else "Next", modifier = Modifier.fillMaxWidth(), onClick = {
            if (last) onDone() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
        })
        Spacer(Modifier.height(Space.lg + navBarPadding()))
    }
}
