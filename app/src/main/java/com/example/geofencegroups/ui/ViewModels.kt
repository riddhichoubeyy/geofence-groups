package com.example.geofencegroups.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.geofencegroups.AppContainer
import com.example.geofencegroups.data.model.userMessage
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@Suppress("UNCHECKED_CAST")
inline fun <reified VM : ViewModel> vmFactory(crossinline create: () -> VM) =
    object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T = create() as T
    }

class AuthViewModel(private val c: AppContainer) : ViewModel() {
    val busy = MutableStateFlow(false)
    val error = MutableStateFlow<String?>(null)
    val notice = MutableStateFlow<String?>(null)

    fun resetPassword(email: String) {
        viewModelScope.launch {
            error.value = null; notice.value = null
            try { c.auth.resetPassword(email); notice.value = "If that email has an account, a reset link is on its way." }
            catch (e: Exception) { error.value = e.userMessage() }
        }
    }

    fun submit(signUp: Boolean, name: String, email: String, password: String) {
        viewModelScope.launch {
            busy.value = true; error.value = null
            try {
                if (signUp) c.auth.signUp(name.trim(), email.trim(), password)
                else c.auth.signIn(email.trim(), password)
            } catch (e: Exception) { error.value = e.userMessage() } finally { busy.value = false }
        }
    }
}

class GroupsViewModel(private val c: AppContainer) : ViewModel() {
    val busy = MutableStateFlow(false)
    val message = MutableStateFlow<String?>(null)

    private fun run(block: suspend () -> Unit) = viewModelScope.launch {
        busy.value = true; message.value = null
        try { block() } catch (e: Exception) { message.value = e.userMessage() } finally { busy.value = false }
    }

    fun create(name: String, onDone: (String) -> Unit) { run { onDone(c.groups.createGroup(name)) } }
    fun join(code: String, onDone: (String) -> Unit) { run { onDone(c.groups.joinGroup(code)) } }
}

class SettingsViewModel(private val c: AppContainer) : ViewModel() {
    fun signOut() { c.appScope.launch { c.auth.signOut() } }
}

class GroupDetailViewModel(private val c: AppContainer, val groupId: String) : ViewModel() {
    private fun <T> Flow<T>.keep(initial: T) =
        catch { }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initial)

    val group = c.groups.groupFlow(groupId).keep(null)
    val members = c.groups.membersFlow(groupId).keep(emptyList())
    val events = c.groups.eventsFlow(groupId).keep(emptyList())
    val busy = MutableStateFlow(false)
    val message = MutableStateFlow<String?>(null)
    val inviteCode = MutableStateFlow<String?>(null)
    val inviteCodeLoading = MutableStateFlow(false)

    fun loadInviteCode() {
        if (inviteCode.value != null || inviteCodeLoading.value) return
        viewModelScope.launch {
            inviteCodeLoading.value = true
            try { inviteCode.value = c.groups.getInviteCode(groupId) }
            catch (e: Exception) { message.value = e.userMessage() }
            finally { inviteCodeLoading.value = false }
        }
    }

    fun saveGeofence(lat: Double, lng: Double, radiusM: Double, onDone: () -> Unit) {
        viewModelScope.launch {
            busy.value = true; message.value = null
            try { c.groups.setGeofence(groupId, lat, lng, radiusM); onDone() }
            catch (e: Exception) { message.value = e.userMessage() } finally { busy.value = false }
        }
    }

    fun leave(onDone: () -> Unit) {
        viewModelScope.launch {
            busy.value = true
            try { c.groups.leaveGroup(groupId); onDone() }
            catch (e: Exception) { message.value = e.userMessage() } finally { busy.value = false }
        }
    }

    fun setRole(uid: String, role: String) {
        viewModelScope.launch {
            try { c.groups.setMemberRole(groupId, uid, role) } catch (e: Exception) { message.value = e.userMessage() }
        }
    }
}
