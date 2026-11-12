package com.github.andreyasadchy.xtra.util

import android.content.SharedPreferences
import androidx.annotation.OptIn
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector

internal enum class TwitchVideoCodec(val token: String, val mimeType: String, val label: String) {
    AV1("av1", MimeTypes.VIDEO_AV1, "AV1"),
    HEVC("h265", MimeTypes.VIDEO_H265, "H.265 / HEVC"),
    AVC("h264", MimeTypes.VIDEO_H264, "H.264 / AVC"),
}

internal data class VideoDecoderSupport(val codec: TwitchVideoCodec, val hardware: Boolean)

@OptIn(UnstableApi::class)
internal object TwitchVideoCodecs {
    val available: List<VideoDecoderSupport> by lazy {
        TwitchVideoCodec.entries.mapNotNull { codec ->
            val decoders = runCatching {
                MediaCodecSelector.DEFAULT.getDecoderInfos(codec.mimeType, false, false)
            }.getOrDefault(emptyList())
            if (decoders.isEmpty()) null else VideoDecoderSupport(codec, decoders.any { it.hardwareAccelerated })
        }
    }

    fun ordered(selection: String?, supported: List<VideoDecoderSupport> = available): List<TwitchVideoCodec> {
        val manual = supported.firstOrNull { it.codec.token == selection }?.codec
        val hardware = TwitchVideoCodec.entries.filter { codec ->
            supported.any { it.codec == codec && it.hardware }
        }
        val preferred = if (manual != null) listOf(manual) else hardware
        // Twitch's AVC rendition is the compatibility fallback, including when
        // a vendor's decoder inventory is incomplete.
        return (preferred + TwitchVideoCodec.AVC).distinct()
    }

    fun requestedTokens(selection: String?): String =
        ordered(selection).joinToString(",") { it.token }

    fun preferredMimeTypes(preferences: SharedPreferences): Array<String> =
        ordered(preferences.getString(C.PLAYER_VIDEO_CODEC, null)).map { it.mimeType }.toTypedArray()

    fun rank(preferredMimeTypes: List<String>, codecs: String?, sampleMimeType: String? = null): Int {
        val mimeType = sampleMimeType ?: MimeTypes.getVideoMediaMimeType(codecs)
        return preferredMimeTypes.indexOf(mimeType).takeIf { it >= 0 } ?: Int.MAX_VALUE
    }
}
