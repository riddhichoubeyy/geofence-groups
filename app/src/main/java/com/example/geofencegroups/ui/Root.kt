package com.example.geofencegroups.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.geofencegroups.AppContainer
import com.example.geofencegroups.data.model.Group
import com.example.geofencegroups.data.model.userMessage
import com.example.geofencegroups.geofence.Work
import com.example.geofencegroups.ui.theme.OrbitTheme
import kotlinx.coroutines.flow.catch

@Composable
fun Root(c: AppContainer, deepLinkGroup: State<String?>) {
    OrbitTheme {
        Surface(Modifier.fillMaxSize()) {
            val flow = remember(c) { c.auth.authState() }
            val user by flow.collectAsState(initial = c.firebaseAuth.currentUser)
            var onboarded by remember { mutableStateOf(c.prefs.onboarded) }
            val uid = user?.uid
            when {
                uid != null -> SignedIn(c, uid, deepLinkGroup)
                !onboarded -> OnboardingScreen { c.prefs.onboarded = true; onboarded = true }
                else -> AuthScreen(c)
            }
        }
    }
}

@Composable
private fun SignedIn(c: AppContainer, uid: String, deepLinkGroup: State<String?>) {
    var groups by remember(uid) { mutableStateOf<List<Group>?>(null) }
    var loadError by remember(uid) { mutableStateOf<String?>(null) }
    var resumes by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) { resumes++; onPauseOrDispose { } }

    LaunchedEffect(uid) {
        runCatching { c.devices.registerCurrent() }
        Work.schedulePeriodicSync(c.context)
    }
    LaunchedEffect(uid) {
        c.groups.groups(uid).catch { loadError = it.userMessage() }.collect { groups = it; loadError = null }
    }
    // Re-register device geofences when groups change or the app returns to the foreground
    // (e.g. after the user grants background location in Settings).
    LaunchedEffect(groups, resumes) { groups?.let { runCatching { c.geofences.sync(it) } } }

    val nav = rememberNavController()
    val link = deepLinkGroup.value
    LaunchedEffect(link, groups) {
        if (link != null && groups?.any { it.id == link } == true) nav.navigate("group/$link")
    }
    val hasBoundary = groups.orEmpty().any { it.geofence != null }

    NavHost(nav, startDestination = "home") {
        composable("home") {
            HomeScreen(c, groups, loadError, onOpen = { nav.navigate("group/$it") }, onSettings = { nav.navigate("settings") })
        }
        composable("settings") { SettingsScreen(c, hasBoundary, onBack = { nav.popBackStack() }) }
        composable("group/{id}") { e ->
            val id = e.arguments!!.getString("id")!!
            GroupDetailScreen(c, uid, id, onBack = { nav.popBackStack() },
                onEditBoundary = { nav.navigate("group/$id/boundary") }, onSettings = { nav.navigate("settings") })
        }
        composable("group/{id}/boundary") { e ->
            BoundaryEditorScreen(c, e.arguments!!.getString("id")!!, onDone = { nav.popBackStack() })
        }
    }
}
