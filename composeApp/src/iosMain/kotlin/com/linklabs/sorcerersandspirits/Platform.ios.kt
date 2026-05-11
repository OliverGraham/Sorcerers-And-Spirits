package com.linklabs.sorcerersandspirits

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

actual fun <T : Any> nullOnTransactionDecodeCrashSerializer(
    serializer: KSerializer<T>
): KSerializer<T?> =
    object : KSerializer<T?> {
        private val nullable = serializer.nullable

        override val descriptor: SerialDescriptor = nullable.descriptor

        override fun deserialize(decoder: Decoder): T? =
            try {
                nullable.deserialize(decoder)
            } catch (_: NullPointerException) {
                null
            } catch (_: ClassCastException) {
                null
            } catch (_: SerializationException) {
                null
            }

        override fun serialize(encoder: Encoder, value: T?) {
            nullable.serialize(encoder = encoder, value = value)
        }
    }
