package com.linklabs.sorcerersandspirits.domain.arena

// Domain seam for the on-device, temporary message log that becomes the source of truth for the
// messages the UI shows. CoordinateArenaMatchUseCase appends each incoming RTDB batch here and reads
// back the full accumulated log. QUESTION: intended surface is roughly
//   suspend fun appendAndLoad(arenaId, batch: List<IncomingArenaMessage>): List<IncomingArenaMessage>
//   suspend fun clear(arenaId)
// (dedup by message id inside appendAndLoad). Confirm the shape — or do you want a Flow<List<...>>
// the use case combine()s instead of the append-driven approach?
// Answer: whatever you think is best