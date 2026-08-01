package com.linklabs.sorcerersandspirits.data.arena.message

// Factory + filename const for the messages Preferences DataStore, mirroring createPendingStatsDataStore.
// A SECOND DataStore<Preferences> is needed (separate file from pending stats). QUESTION: the current
// Koin single<DataStore<Preferences>> is already bound to pending stats — so this store must be
// disambiguated. Preferred: a named Koin qualifier for the messages DataStore (vs. wrapping it in a
// dedicated type). Confirm the qualifier approach; platform modules will supply the file path.
// Yes, that sounds good on the qualifier approach
