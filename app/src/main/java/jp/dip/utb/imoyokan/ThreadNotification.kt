package jp.dip.utb.imoyokan

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.format.DateFormat
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import com.squareup.picasso.Picasso
import jp.dip.utb.imoyokan.futaba.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*
import kotlin.math.min


class ThreadNotification(private val context: Context, private val intent: Intent) {

    companion object {
        private const val AUTO_SMALL_FONT_LINES = 10
        private const val AUTO_SMALL_FONT_LENGTH = 15 * AUTO_SMALL_FONT_LINES
    }

    fun notify(title: String = "", text: String = "") {
        GlobalScope.launch {
            notifyAsync(getThreadInfo(), title,  text)
        }
    }

    private fun getThreadInfo(): ThreadInfo {
        var threadInfo: ThreadInfo? = null
        // まずはキャッシュから
        val cache = Cache(context)
        val useCache = intent.getIntExtra(KEY_EXTRA_POSITION, RELOAD_THREAD) != RELOAD_THREAD
        if (useCache) {
            threadInfo = cache.loadFromCache()
        }
        // キャッシュを使わない or キャッシュが見つからないならインターネットから読み込む
        val pref = Pref.getInstance(context)
        if (threadInfo == null) {
            val builder = ThreadInfoBuilder().apply {
                this.url = intent.str(KEY_EXTRA_URL)
                this.mail = intent.str(KEY_EXTRA_MAIL)
            }
            threadInfo = builder.build()
            if (threadInfo.lastModified != pref.lastThreadModified || threadInfo.url != pref.lastThreadUrl) {
                cache.saveToCache(threadInfo)
            }
        }
        // 読み込んだ情報を保存
        pref.lastThreadUrl = threadInfo.url
        pref.lastThreadModified = threadInfo.lastModified
        pref.apply()
        return threadInfo
    }

    private fun notifyAsync(threadInfo: ThreadInfo, title: String, text: String) {

        // フォームデータを上書き
        intent.putExtra(KEY_EXTRA_MAIL, threadInfo.form.mail)

        // 通知作成開始
        val builder = ImoyokanNotificationBuilder(context, intent)

        // レス入力欄
        val replyPendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_REPLY_MIN + Random().nextInt(10000), // 返信のrequestCodeはかぶらないようにする！,
            builder.createImoyokanIntent()
                .putExtra(KEY_EXTRA_ACTION, INTENT_ACTION_REPLY)
                .putExtra(KEY_EXTRA_URL, threadInfo.url)
                .putExtra(KEY_EXTRA_PTUA, threadInfo.form.ptua),
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        val replyTitle = if (threadInfo.form.mail.isNotEmpty()) "返信 ${STR_MAIL_LABEL}${threadInfo.form.mail}" else "返信"
        val replyLabel = if (threadInfo.form.mail.isNotEmpty()) "${STR_MAIL_LABEL}${threadInfo.form.mail}" else "@ﾒｰﾙｱﾄﾞﾚｽ(半角ｽﾍﾟｰｽ)本文"
        val remoteInput = RemoteInput.Builder(KEY_EXTRA_REPLY_TEXT)
            .setLabel(replyLabel)
            .build()
        val replyAction = NotificationCompat.Action
            .Builder(android.R.drawable.ic_menu_send, replyTitle, replyPendingIntent)
            .addRemoteInput(remoteInput)
            .build()
        // アクションボタンを登録
        builder
            .addAction(replyAction)
            .addNextPageAction(R.drawable.ic_action_reload, DateFormat.format("更新(HH:mm:ss)", threadInfo.timestamp), threadInfo.url, KEY_EXTRA_POSITION to RELOAD_THREAD)
            .addCatalogAction()

        // 読み込みに失敗していた場合
        if (threadInfo.isFailed()) {
            threadInfo.replies.add(ResInfo(0, threadInfo.res, "スレッド取得失敗${aroundWhenIsNotEmpty("\n", threadInfo.failedMessage, "")}"))
        }

        // ここからカスタムView
        val view = RemoteViews(context.packageName, R.layout.notification_thread)

        // レス
        val sb = SpannableStringBuilder()
        val position  = min(intent.getIntExtra(KEY_EXTRA_POSITION, RELOAD_THREAD), threadInfo.replies.last().index)
        val hasNext = position < threadInfo.replies.last().index
        threadInfo.replies.filter{ it.index <= position }.takeLast(MAX_RES_COUNT).forEach {
            val mail = aroundWhenIsNotEmpty("[", it.mail, "]") // メールは[]で囲う
            if (it.index == 0) {
                sb.addResponse("${it.number}${mail}", it.getCompressText(), "\n")
            } else {
                sb.addResponse("${it.index}${mail}", it.getCompressText())
            }
        }
        // メッセージ
        if (title.isNotBlank() || text.isNotBlank()) {
            sb.addResponse(title, text)
        }
        // 文字表示するところできたよ
        view.setTextViewText(R.id.text, sb)

        // スレ画像
        view.setOnClickOrGone(R.id.large_icon, threadInfo.imageUrls.isNotEmpty()) {
            try {
                val bitmap = Picasso.get().load(toCatalogImageUrl(threadInfo.imageUrls[0])).get()
                view.setImageViewBitmap(R.id.large_icon, bitmap)
            } catch (e: Throwable) {
                Log.d(NOTIFY_NAME, "スレ画読み込み失敗", e)
                view.setImageViewResource(R.id.large_icon, android.R.drawable.ic_delete)
            }
            builder.createViewImageIntent()
        }
        view.setTextViewText(R.id.images_count, "x${threadInfo.imageUrls.size}")
        view.setOnClickOrGone(R.id.images, 1 < threadInfo.imageUrls.size) { builder.createViewImageIntent(threadInfo.imageUrls.maxIndex) }

        // いろんなボタン
        view.setOnClickOrGone(R.id.prev, 0 < position, View.INVISIBLE) { builder.createThreadIntent(position.prev) }
        view.setOnClickOrGone(R.id.next, hasNext, View.INVISIBLE) { builder.createThreadIntent(position.next) }
        view.setOnClickPendingIntent(R.id.share, builder.createShareUrlIntent(threadInfo.url))

        // 表示するよ！
        builder
            .setRemoteViews(view)
            .notifyThis()
    }

    @SuppressLint("NewApi")
    private fun SpannableStringBuilder.addResponse(user: String, text: String, delimiter: String = " "): SpannableStringBuilder {
        if (this.isNotEmpty()) {
            val responseDelimiter = SpannableStringBuilder("\n\n")
            responseDelimiter.setSpan(RelativeSizeSpan(0.5f), 0, 2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            this.append(responseDelimiter)
        }
        val userSpan = SpannableStringBuilder(user)
        userSpan.setSpan(ForegroundColorSpan(Color.BLACK), 0, user.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        this.append(userSpan)
        this.append(delimiter)
        this.append(text.shortKitaa().toColoredText().autoSmallFont())
        return this
    }

    private fun String.shortKitaa(): String {
        return if (Pref.getInstance(context).thread.shortKitaa) this.replace(KITAA_REGEX, SHORT_KITAA) else this
    }

    private fun SpannableStringBuilder.autoSmallFont(): SpannableStringBuilder {
        if (!Pref.getInstance(context).thread.autoSmallFont) {
            return this
        }
        if (this.length < AUTO_SMALL_FONT_LENGTH && this.split("\n").size < AUTO_SMALL_FONT_LINES) {
            return this
        }
        this.setSpan(RelativeSizeSpan(0.7f), 0, this.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        return this
    }

}
