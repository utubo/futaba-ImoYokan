package jp.dip.utb.imoyokan.futaba.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ResInfoTest {

    @Test
    fun compressTextTest() {
        // ThreadNotification移動したのでそのうち作り直す
//        val target = ResInfo(
//            0, "0", """
//            >>ABCDEFG
//            >>XYZ
//            >abcdefg
//            >xyz
//            1234567
//            890
//            >ZZZ
//            zzz
//        """.trimIndent()
//        )
//        assertEquals("適当に改行を削除すること", """
//            >>ABCDEFG XYZ >abcdefg xyz
//            1234567 890
//            >ZZZ
//            zzz
//        """.trimIndent(), target.getCompressText())
    }
}