package jp.dip.utb.imoyokan

import jp.dip.utb.imoyokan.futaba.ResInfo
import org.junit.Assert.assertEquals
import org.junit.Test

class ResInfoTest {

    @Test
    fun compressTextTest() {
        val target = ResInfo(0, "0", """
            >>ABCDEFG
            >>XYZ
            >abcdefg
            >xyz
            1234567
            890
            >ZZZ
            zzz
        """.trimIndent())
        assertEquals("適当に改行を削除すること", """
            >>ABCDEFG XYZ >abcdefg xyz
            1234567 890
            >ZZZ
            zzz
        """.trimIndent(), target.getCompressText())
    }
}