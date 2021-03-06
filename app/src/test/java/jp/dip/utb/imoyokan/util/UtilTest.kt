package jp.dip.utb.imoyokan.util

import jp.dip.utb.imoyokan.futaba.util.FUTABA_CHARSET
import jp.dip.utb.imoyokan.futaba.util.USER_AGENT
import org.junit.Test

import org.junit.Assert.*

class UtilTest {
    @Test
    fun replaceForPost() {
        assertEquals("絵文字テスト&#128516;", "絵文字テスト😄".replaceForPost(FUTABA_CHARSET))
        assertEquals("波ダッシュ～", "波ダッシュ～".replaceForPost(FUTABA_CHARSET))
        assertEquals("全角チルダは波ダッシュにする", "にょろ～", "にょろ～".replaceForPost(FUTABA_CHARSET))
    }

    @Test
    fun removeHtmlTag() {
        assertEquals("タグを削除する", "&gt;<&", "&<b>gt;</b>&lt;&amp;".removeHtmlTag())
        assertEquals("絵文字のデコード", "😄あいうえお", "<span>&#128516;あいうえお</span>".removeHtmlTag())
    }

    @Test
    fun userAgent() {
        assertTrue("USER_AGENTは「アプリID/バージョン名」であること($USER_AGENT)", "jp.dip.utb.imoyokan/\\d+\\.\\d+".toRegex().matches(
            USER_AGENT
        ))
    }

    @Test
    fun aroundWhenIsNotEmpty() {
        assertEquals("[A]",
            aroundOrEmpty("[", "A", "]")
        )
        assertEquals("",
            aroundOrEmpty("[", null, "]")
        )
        assertEquals("", aroundOrEmpty("[", "", "]"))
    }

    @Test
    fun pick() {
        assertEquals("abc", "<b>abc</b>".pick(">([^<]+)<".toRegex()))
        assertEquals("", "".pick(">([^<]+)<".toRegex()))
    }
}
