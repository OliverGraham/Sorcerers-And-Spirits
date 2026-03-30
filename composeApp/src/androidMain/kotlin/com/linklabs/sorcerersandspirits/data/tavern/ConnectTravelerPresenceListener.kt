package com.linklabs.sorcerersandspirits.data.tavern

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.getValue
import com.linklabs.sorcerersandspirits.data.model.TravelerEntity
import com.linklabs.sorcerersandspirits.domain.tavern.TavernStatus

internal class ConnectTravelerPresenceListener(
    private val userId: String,
    private val displayName: String,
    private val travelerReference: DatabaseReference,
    private val mailBoxReference: DatabaseReference,
) : ValueEventListener {

    override fun onDataChange(snapshot: DataSnapshot) {
        val connected = snapshot.getValue<Boolean>() ?: false
        if (!connected) return

        travelerReference.onDisconnect().removeValue()

        mailBoxReference.get().addOnSuccessListener { mailboxSnapshot ->
            val hasActiveChallenge = !mailboxSnapshot.getValue<String>().isNullOrBlank()

            updateTravelerPresence(hasActiveChallenge = hasActiveChallenge)
        }
    }

    private fun updateTravelerPresence(hasActiveChallenge: Boolean) {
        travelerReference.runTransaction(
            object : Transaction.Handler {
                override fun doTransaction(currentData: MutableData): Transaction.Result {
                    currentData.value =
                        TravelerEntity(
                            userId = userId,
                            displayName = displayName,
                            tavernStatus =
                                currentData.getValue<TravelerEntity>()?.tavernStatus
                                    ?: if (hasActiveChallenge) {
                                        TavernStatus.Busy
                                    } else {
                                        TavernStatus.Available
                                    },
                        )

                    return Transaction.success(currentData)
                }

                override fun onComplete(
                    error: DatabaseError?,
                    committed: Boolean,
                    currentData: DataSnapshot?,
                ) = Unit
            }
        )
    }

    override fun onCancelled(error: DatabaseError) = Unit
}
