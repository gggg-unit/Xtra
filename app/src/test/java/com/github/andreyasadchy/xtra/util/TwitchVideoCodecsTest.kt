package com.github.andreyasadchy.xtra.util

import com.github.andreyasadchy.xtra.util.TwitchVideoCodec.AV1
import com.github.andreyasadchy.xtra.util.TwitchVideoCodec.AVC
import com.github.andreyasadchy.xtra.util.TwitchVideoCodec.HEVC
import org.junit.Assert.assertEquals
import org.junit.Test

class TwitchVideoCodecsTest {
    @Test
    fun automaticRanksHardwareCodecsByEfficiency() {
        val supported = listOf(VideoDecoderSupport(AVC, true), VideoDecoderSupport(AV1, true), VideoDecoderSupport(HEVC, true))
        assertEquals(listOf(AV1, HEVC, AVC), TwitchVideoCodecs.ordered("auto", supported))
    }

    @Test
    fun automaticSkipsSoftwareOnlyModernCodecs() {
        val supported = listOf(VideoDecoderSupport(AV1, false), VideoDecoderSupport(HEVC, true))
        assertEquals(listOf(HEVC, AVC), TwitchVideoCodecs.ordered(null, supported))
    }

    @Test
    fun manualAllowsSupportedSoftwareCodecsAndKeepsAvcFallback() {
        val supported = listOf(VideoDecoderSupport(AV1, false), VideoDecoderSupport(HEVC, true))
        assertEquals(listOf(AV1, AVC), TwitchVideoCodecs.ordered("av1", supported))
    }

    @Test
    fun unsupportedManualChoiceReturnsToAutomatic() {
        val supported = listOf(VideoDecoderSupport(HEVC, true), VideoDecoderSupport(AVC, true))
        assertEquals(listOf(HEVC, AVC), TwitchVideoCodecs.ordered("av1", supported))
        assertEquals(listOf(AVC), TwitchVideoCodecs.ordered("h264", supported))
        assertEquals(listOf(AVC), TwitchVideoCodecs.ordered(null, emptyList()))
    }
}
