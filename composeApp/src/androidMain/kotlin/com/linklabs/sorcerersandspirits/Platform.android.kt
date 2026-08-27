package com.linklabs.sorcerersandspirits

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.nullable

actual fun <T : Any> nullOnTransactionDecodeCrashSerializer(
    serializer: KSerializer<T>,
): KSerializer<T?> = serializer.nullable
