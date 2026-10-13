package com.example.appkaiju_grupo5.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appkaiju_grupo5.navigation.NavigationEvent
import com.example.appkaiju_grupo5.navigation.Screen
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class NavigationViewModel : ViewModel() {
    // Cada evento se consume una vez, incluso si la Activity se recrea.
    private val events = Channel<NavigationEvent>(Channel.BUFFERED)
    val navigationEvents = events.receiveAsFlow()

    fun navigateTo(screen: Screen) {
        viewModelScope.launch { events.send(NavigationEvent.NavigateTo(screen)) }
    }

    fun goBack() {
        viewModelScope.launch { events.send(NavigationEvent.Back) }
    }
}
