package com.linklabs.sorcerersandspirits.data.connectivity

import com.linklabs.sorcerersandspirits.domain.connectivity.ConnectivityObserver
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import platform.Network.nw_path_get_status
import platform.Network.nw_path_monitor_cancel
import platform.Network.nw_path_monitor_create
import platform.Network.nw_path_monitor_set_queue
import platform.Network.nw_path_monitor_set_update_handler
import platform.Network.nw_path_monitor_start
import platform.Network.nw_path_status_satisfied
import platform.darwin.dispatch_queue_create

class IosConnectivityObserver : ConnectivityObserver {

    override val isOnline: Flow<Boolean> =
        callbackFlow {
            val monitor = nw_path_monitor_create()
            val queue = dispatch_queue_create("connectivity.monitor", null)

            nw_path_monitor_set_update_handler(monitor) { path ->
                trySend(element = nw_path_get_status(path) == nw_path_status_satisfied)
            }
            nw_path_monitor_set_queue(monitor, queue)
            nw_path_monitor_start(monitor)

            awaitClose { nw_path_monitor_cancel(monitor) }
        }
            .distinctUntilChanged()
}
