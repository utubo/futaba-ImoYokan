package jp.dip.utb.imoyokan.notification

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.format.DateFormat
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.view.View
import android.widget.RemoteViews
import androidx.core.content.ContextCompat.getColor
import jp.dip.utb.imoyokan.*
import jp.dip.utb.imoyokan.futaba.model.ResInfo
import jp.dip.utb.imoyokan.futaba.model.ThreadInfo
import jp.dip.utb.imoyokan.futaba.presenter.ThreadInfoBuilder
import jp.dip.utb.imoyokan.futaba.util.*
import jp.dip.utb.imoyokan.model.Cache
import jp.dip.utb.imoyokan.model.Pref
import jp.dip.utb.imoyokan.util.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.math.min


class ThreadNotification(private val context: Context, private val intent: Intent) {

    companion object {
        private const val AUTO_SMALL_FONT_LINES = 10
        private const val AUTO_SMALL_FONT_LENGTH = 15 * AUTO_SMALL_FONT_LINES
    }

    private val quoteColor = getColor(context,
        R.color.textColorQuote
    )

    fun notify(title: String = "", text: String = "") {
        GlobalScope.launch {
            notifyAsync(getThreadInfo(), title,  text)
        }
    }

    fun notifyCache(title: String = "", text: String = "") {
        intent.putExtra(
            KEY_EXTRA_POSITION,
            POSITION_KEEP
        )
        notify(title, text)
    }

    private fun getThreadInfo(): ThreadInfo {
        var threadInfo: ThreadInfo? = null
        // まずはキャッシュから
        val cache = Cache(context)
        if (intent.getBooleanExtra(KEY_EXTRA_USE_CACHE, false)) {
            threadInfo = cache.loadThreadInfo()
        }
        // キャッシュを使わない or キャッシュが見つからないならインターネットから読み込む
        val pref = Pref.getInstance(context)
        if (threadInfo == null) {
            val builder = ThreadInfoBuilder()
                .apply {
                this.url = intent.str(KEY_EXTRA_URL).ifBlank { pref.lastThreadUrl }
            }
            threadInfo = builder.build()
            // 読み込みエラーだったら、できるだけキャッシュから復元する
            if (threadInfo.isFailed()) {
                val t = cache.loadThreadInfo()
                if (t != null && threadInfo.url == t.url) {
                    t.lastModified = threadInfo.lastModified
                    t.failedMessage = threadInfo.failedMessage
                    t.statusCode = threadInfo.statusCode
                    threadInfo = t
                }
            }
            // キャッシュに保存
            if (threadInfo.lastModified != pref.lastThreadModified || threadInfo.url != pref.lastThreadUrl) {
                cache.saveThreadInfo(threadInfo)
            }
        }
        // 読み込んだ情報を保存
        pref.lastThreadUrl = threadInfo.url
        pref.lastThreadModified = threadInfo.lastModified
        pref.apply()
        return threadInfo
    }

    private fun notifyAsync(threadInfo: ThreadInfo, title: String, text: String) {

        // 「mayのカタログを開く→imgのスレを共有で開く→カタログボタンをタップ」したときもimgのカタログを表示するように頑張る
        val pref = Pref.getInstance(context)
        val (_, _, sort) = analyseCatalogUrl(pref.lastCatalogUrl)
            ?: Triple("", "", "") // ソート順は引き継ぐ
        pref.lastCatalogUrl =
            getCatalogUrl(threadInfo.url, sort)

        // フォームデータ
        val formMail = pref.mail.get(threadInfo.url)

        // 通知作成開始
        val builder =
            ImoyokanNotificationBuilder(
                context,
                intent
            )

        // レス入力欄
        val mailLabel =
            aroundOrEmpty(
                "@",
                formMail,
                ""
            ) // "✉"もありかなぁ…
        val replyLabel = "返信$mailLabel"
        val replyPlaceHolder = if (formMail.isNotEmpty()) mailLabel else "@ﾒｰﾙｱﾄﾞﾚｽ(半角ｽﾍﾟｰｽ)本文"
        builder.addRemoteInput(
            R.drawable.ic_edit,
            replyLabel,
            replyPlaceHolder,
            KEY_EXTRA_REPLY_TEXT,
            builder.createImoyokanIntent()
                .putExtra(
                    KEY_EXTRA_ACTION,
                    INTENT_ACTION_REPLY
                )
                .putExtra(KEY_EXTRA_URL, threadInfo.url)
                .putExtra(KEY_EXTRA_MAIL, formMail)
                .putExtra(KEY_EXTRA_PTUA, threadInfo.form.ptua)
        )

        // 他のアクションボタン
        builder
            .addNextPageAction(
                R.drawable.ic_reload,
                DateFormat.format("更新(HH:mm:ss)", threadInfo.timestamp),
                threadInfo.url,
                KEY_EXTRA_POSITION to THREAD_BOTTOM
            )
            .addCatalogAction()

        // ここからカスタムView
        val view = RemoteViews(context.packageName,
            R.layout.notification_thread
        )

        // スクロール情報
        var position = 0
        var hasNext = false
        var gravityTop = true

        // 文字表示するところ
        var resList: List<ResInfo> = listOf()
        val sb = SpannableStringBuilder()
        if (threadInfo.replies.isEmpty()) {
            sb.addResponse(threadInfo.res, "", "")
        } else {
            // レス
            val extraPosition = builder.getExtraThreadPosition()
            position = min(extraPosition, threadInfo.replies.last().index)
            hasNext = position < threadInfo.replies.last().index
            gravityTop = when {
                position == 0 -> true
                extraPosition == THREAD_BOTTOM -> false
                else -> builder.getExtraGravityTop()
            }
            val showDeleted = pref.thread.showDeleted
            resList =
                if (gravityTop) threadInfo.replies.filter { position <= it.index && (showDeleted || !it.deleted) }.take(
                    MAX_RES_COUNT
                )
                else threadInfo.replies.filter { it.index <= position && (showDeleted || !it.deleted) }.takeLast(
                    MAX_RES_COUNT
                )
            resList.forEach {
                if (it.index == 0) {
                    sb.addResponse(it.number, it.mail, decorateResText(it.text), "\n")
                } else {
                    sb.addResponse(it.index.toString(), it.mail, decorateResText(it.text))
                }
            }
            sb.applyFontSize(pref.thread.fontSize)
        }
        // メッセージ
        when {
            threadInfo.statusCode == 404 -> sb.addResponse("スレッドが無いよ", "", "")
            threadInfo.isFailed() -> sb.addResponse("読込失敗", "", threadInfo.failedMessage)
        }
        if (title.isNotBlank() || text.isNotBlank()) {
            sb.addResponse(title, text, "\n")
        }
        // できたよ
        if (gravityTop) {
            view.setViewVisibility(R.id.text, View.GONE)
            view.setViewVisibility(R.id.text_gravity_top, View.VISIBLE)
            view.setTextViewText(R.id.text_gravity_top, sb)
        } else {
            view.setTextViewText(R.id.text, sb)
        }

        // スレ画像
        view.setOnClickOrGone(R.id.large_icon, threadInfo.imageUrls.isNotEmpty()) {
            val (bitmap, _) = loadImage(
                toCatalogImageUrl(
                    threadInfo.imageUrls[0]
                )
            )
            view.setImageViewAny(R.id.large_icon, bitmap ?: R.drawable.ic_broken_image)
            builder.createViewImageIntent()
        }
        view.setTextViewText(R.id.images_count, "x${threadInfo.imageUrls.size}")
        view.setOnClickOrGone(R.id.images, 1 < threadInfo.imageUrls.size) { builder.createViewImageIntent(threadInfo.imageUrls.maxIndex) }

        // いろんなボタン
        val prevId = if (pref.reverseScrolling) R.id.next else R.id.prev
        val nextId = if (pref.reverseScrolling) R.id.prev else R.id.next
        view.setOnClickOrInvisible(prevId, 0 < position         ) { builder.createThreadIntent(position.prev, KEY_EXTRA_GRAVITY_TOP to gravityTop) }
        view.setOnClickOrInvisible(nextId, hasNext || gravityTop) { builder.createThreadIntent(position.next, KEY_EXTRA_GRAVITY_TOP to (gravityTop && hasNext)) }
        view.setOnClickOrGone(R.id.top,    1 < resList.size && !gravityTop) { builder.createThreadIntent(0, KEY_EXTRA_GRAVITY_TOP to true) }
        view.setOnClickOrGone(R.id.bottom, 1 < resList.size && gravityTop ) { builder.createThreadIntent(threadInfo.replies.last().index, KEY_EXTRA_GRAVITY_TOP to false) }
        view.setOnClickPendingIntent(R.id.share, builder.createShareUrlIntent(threadInfo.url))
        view.setOnClickPendingIntent(
            R.id.mail,  builder.createPendingIntent(
                KEY_EXTRA_ACTION to INTENT_ACTION_GO_SET_MAIL, KEY_EXTRA_MAIL to formMail))
        if (formMail.isNotBlank()) {
            view.setInt(
                R.id.mail, "setColorFilter", getColor(context,
                    R.color.mailIcon
                ))
        }

        // 表示するよ！
        builder
            .setRemoteViews(view)
            .notifyThis()
    }

    @SuppressLint("NewApi")
    private fun SpannableStringBuilder.addResponse(user: String, mail: String, text: CharSequence, delimiter: String = " "): SpannableStringBuilder {
        if (this.isNotEmpty()) {
            val responseDelimiter = SpannableStringBuilder("\n\n")
            responseDelimiter.setSpan(RelativeSizeSpan(0.5f), 0, 2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            this.append(responseDelimiter)
        }
        if (user.isNotBlank()) {
            val userSpan = SpannableStringBuilder(user)
            userSpan.setSpan(ForegroundColorSpan(context.resourceColor(R.color.textColorPrimary)), 0, user.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            this.append(userSpan)
        }
        if (mail.isNotBlank()) {
            val mailText = "[${mail}]"
            val mailSpan = SpannableStringBuilder(mailText)
            mailSpan.setSpan(ForegroundColorSpan(context.resourceColor(R.color.textColorMail)), 0, mailText.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            this.append(mailSpan)
        }
        if ((user.isNotBlank() || mail.isNotBlank()) && text.isNotBlank()) {
            this.append(delimiter)
        }
        this.append(text)
        return this
    }

    private fun decorateResText(text: String): Spannable {
        return text
            .removeLineBreaks()
            .shortKitaa()
            .toColoredText(quoteColor)
            .autoSmallFont()
    }

    private fun String.removeLineBreaks(): String {
        if (!Pref.getInstance(context).thread.removeLineBreaks) {
            return this
        }
        val levelRegex = "^(>*)".toRegex()
        val s = StringBuilder()
        var prevLevel = 0
        this.split("\n").forEach {
            val level = levelRegex.find(it)?.value?.length ?: 0
            if (prevLevel == level) {
                s.append(it.replace(levelRegex, " "))
            } else if (prevLevel == 0 || level == 0) {
                s.append("\n")
                s.append(it)
            } else {
                s.append(" ")
                s.append(it)
            }
            prevLevel = level
        }
        return s.toString().trim()
    }

    private fun String.shortKitaa(): String {
        return if (Pref.getInstance(context).thread.shortKitaa) this.replace(
            KITAA_REGEX,
            SHORT_KITAA
        ) else this
    }

    private fun Spannable.autoSmallFont(): Spannable {
        if (!Pref.getInstance(context).thread.autoSmallFont) {
            return this
        }
        if (this.length < AUTO_SMALL_FONT_LENGTH && this.split("\n").size < AUTO_SMALL_FONT_LINES) {
            return this
        }
        this.setSpan(RelativeSizeSpan(0.7f), 0, this.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        return this
    }

    private fun SpannableStringBuilder.applyFontSize(size: Float) {
        if (size != 1f) {
            this.setSpan(RelativeSizeSpan(size), 0, this.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

}
