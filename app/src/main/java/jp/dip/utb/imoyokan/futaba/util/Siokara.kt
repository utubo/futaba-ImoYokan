package jp.dip.utb.imoyokan.futaba.util

import jp.dip.utb.imoyokan.model.Pref
import jp.dip.utb.imoyokan.util.pick

//val SIO_SERVER_REGEX = "http://www\\.nijibox\\d\\.com/".toRegex()
val SIO_FILE_REGEX = "(f|fu|sa|sp|sq|su|ss)\\d+$IMAGE_EXT".toRegex()
const val UP_F_ROOT = "https://dec.2chan.net/up/src/"
const val UP_FU_ROOT = "https://dec.2chan.net/up2/src/"
const val SIO_KARA_SA_ROOT = "http://www.nijibox6.com/futabafiles/001/src/"
const val SIO_KARA_SP_ROOT = "http://www.nijibox2.com/futabafiles/003/src/"
const val SIO_KARA_SQ_ROOT = "http://www.nijibox6.com/futabafiles/mid/src/"
const val SIO_KARA_SS_ROOT = "http://www.nijibox5.com/futabafiles/kobin/src/"
const val SIO_KARA_SU_ROOT = "http://www.nijibox5.com/futabafiles/tubu/src/"
const val SIO_CACHE_SERVER_ROOT = "https://x123.x0.to/~imoyokan_sio_cache_server/thumb/"

fun getSiokaraUrl(filename: String): String {
    return when {
        filename.startsWith("fu") -> (UP_FU_ROOT + filename)
        filename.startsWith("f") -> (UP_F_ROOT + filename)
        filename.startsWith("sa") -> (SIO_KARA_SA_ROOT + filename)
        filename.startsWith("sp") -> (SIO_KARA_SP_ROOT + filename)
        filename.startsWith("sq") -> (SIO_KARA_SQ_ROOT + filename)
        filename.startsWith("ss") -> (SIO_KARA_SS_ROOT + filename)
        filename.startsWith("su") -> (SIO_KARA_SU_ROOT + filename)
        else -> filename
    }
}

fun getSiokaraThumbnailUrl(url: String): String {
    return when {
            // 中間サーバ
            Pref.instance?.media?.useSioCacheServer ?: false -> "$SIO_CACHE_SERVER_ROOT${url.pick("([^/]+$)")}.thumb.jpg"
            // 動画はサムネ未対応
            url.contains(".mp4") -> url
            url.contains(".webm") -> url
            // 空瓶はサムネ無し
            url.startsWith(SIO_KARA_SA_ROOT) -> url
            // 塩のサムネ表示の画像
            else -> url.replace("src", "misc").replace(IMAGE_EXT_REGEX, ".thumb.jpg")
    }
}