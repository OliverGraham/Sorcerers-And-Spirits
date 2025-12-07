package com.linklabs.sorcerersandspirits.utilities

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

class StateFlowContainer<T> {
    private val _flow = MutableStateFlow<T?>(value = null)
    val flow = _flow.filterNotNull()

    /** Called by IOSTavernRepositoryImplementation */
    fun emit(value: T) {
        CoroutineScope(context = Dispatchers.Default).launch {
            _flow.emit(value = value)
        }
    }
}
