package com.linklabs.sorcerersandspirits.data.arena.message

// DataStore-backed implementation of the domain ArenaMessageStore. Mirrors DataStorePendingStatsStore:
// serialize the accumulated List<ArenaMessageEntity> to a JSON string under a Preferences key, keyed
// by arenaId; append dedups by message id; clear() removes the key at match/gauntlet boundaries.
// QUESTION: reuse ArenaMessageEntity (already @Serializable) for on-disk storage, or a slimmer
// dedicated persistence model? Leaning: reuse ArenaMessageEntity + its existing toDomain().
// yes, resue that