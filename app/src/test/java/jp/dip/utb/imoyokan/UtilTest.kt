package jp.dip.utb.imoyokan

import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
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
}
