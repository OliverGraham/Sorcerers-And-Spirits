package com.linklabs.sorcerersandspirits.data

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.getValue
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

inline fun <reified T : Any> DatabaseReference.getValueFlow(): Flow<T?> =
    callbackFlow {
            val listener =
                object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        trySend(snapshot.getValue<T>())
                    }

                    override fun onCancelled(error: DatabaseError) {
                        close(error.toException())
                    }
                }

            addValueEventListener(listener)
            awaitClose { removeEventListener(listener) }
        }
        .distinctUntilChanged()

inline fun <reified T : Any> DatabaseReference.getChildrenFlow(): Flow<List<T>> =
    callbackFlow {
            val listener =
                object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val items = snapshot.children.mapNotNull { it.getValue<T>() }
                        trySend(items)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        close(error.toException())
                    }
                }

            addValueEventListener(listener)
            awaitClose { removeEventListener(listener) }
        }
        .distinctUntilChanged()
