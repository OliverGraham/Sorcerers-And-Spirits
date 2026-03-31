package com.linklabs.sorcerersandspirits.data.tavern

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.getValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal class ConnectedValueEventListener(
    val coroutineScope: CoroutineScope,
    val onConnectToDatabase: suspend () -> Unit,
) : ValueEventListener {

    /**
     * This gets called anytime this client connects to the database (including the first time on
     * app startup).
     */
    override fun onDataChange(snapshot: DataSnapshot) {
        val connected = snapshot.getValue<Boolean>() ?: false
        if (!connected) return

        coroutineScope.launch { onConnectToDatabase() }
    }

    override fun onCancelled(error: DatabaseError) = Unit
}
