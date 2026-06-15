package com.linklabs.sorcerersandspirits.data

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

// TODO(SS-038): Figure out if this is needed or useful
object IOSNullableStringSerializer : KSerializer<String?> {
    override val descriptor =
        PrimitiveSerialDescriptor("NullableStringSanitizing", PrimitiveKind.STRING)

    @OptIn(ExperimentalSerializationApi::class)
    override fun serialize(encoder: Encoder, value: String?) {
        val cleaned = value.nullIfIosFakeNull()
        if (cleaned == null) encoder.encodeNull() else encoder.encodeString(cleaned)
    }

    override fun deserialize(decoder: Decoder): String? =
        runCatching { decoder.decodeString() }.getOrNull().nullIfIosFakeNull()

    private fun String?.nullIfIosFakeNull(): String? =
        when (this?.trim()) {
            null,
            "",
            "<null>",
            "null" -> null

            else -> this
        }
}
