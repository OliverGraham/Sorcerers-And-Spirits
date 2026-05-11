package com.linklabs.sorcerersandspirits

import kotlinx.serialization.KSerializer

expect fun <T : Any> nullOnTransactionDecodeCrashSerializer(
    serializer: KSerializer<T>
): KSerializer<T?>
