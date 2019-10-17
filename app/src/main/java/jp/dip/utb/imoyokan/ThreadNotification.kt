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
import jp.dip.utb.imoyokan.futaba.ResInfo
import jp.dip.utb.imoyokan.futaba.ThreadInfo
import jp.dip.utb.imoyokan.futaba.ThreadInfoBuilder
import jp.dip.utb.imoyokan.futaba.toColoredText
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.*
import java.util.*
import kotlin.math.min


class ThreadNotification(private val context: Context, private val intent: Intent) {

    fun notify(title: String = "", text: String = "") {
        GlobalScope.launch {
            notifyAsync(getThreadInfo(), title,  text)
        }
    }

    private fun getThreadInfo(): ThreadInfo {
        var threadInfo: ThreadInfo? = null
        // まずはキャッシュから
        val useCache = intent.getIntExtra(KEY_EXTRA_POSITION, RELOAD_THREAD) != RELOAD_THREAD
        if (useCache) {
            threadInfo = loadFromCache()
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
                saveToCache(threadInfo)
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
            builder.createImoyokanIntent(context, intent)
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
            .addNextPageAction(R.drawable.ic_action_reload, DateFormat.format("更新(HH:mm:ss)", threadInfo.timestamp), threadInfo.url)
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
        if (threadInfo.thumbUrl.isNotEmpty()) {
            val bitmap = Picasso.get().load(threadInfo.thumbUrl.replace("/thumb/", "/cat/").toHttps()).get()
            view.setImageViewBitmap(R.id.large_icon, bitmap)
            val imageIntent = builder.createNextPageIntent(
                threadInfo.thumbUrl,
                KEY_EXTRA_IMAGE_SRC_URL to threadInfo.imageUrl,
                KEY_EXTRA_POSITION to if (hasNext) position else RELOAD_THREAD
            )
            view.setOnClickPendingIntent(R.id.large_icon, imageIntent)
        } else {
            view.setViewVisibility(R.id.large_icon, View.GONE)
        }

        // いろんなボタン
        if (0 < position) {
            view.setViewVisibility(R.id.prev, View.VISIBLE)
            view.setOnClickPendingIntent(R.id.prev, builder.createThreadIntent(position - 1))
        }
        if (hasNext) {
            view.setViewVisibility(R.id.next, View.VISIBLE)
            view.setOnClickPendingIntent(R.id.next, builder.createThreadIntent(position + 1))
        }
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
        this.append(text.toColoredText())
        return this
    }

    companion object {
        const val CACHE_FILENAME = "thread_cache.dat"
    }

    private fun saveToCache(threadInfo: ThreadInfo) {
        try {
            val file = File(context.cacheDir, CACHE_FILENAME)
            ObjectOutputStream(FileOutputStream(file)).use{ it.writeObject(threadInfo) }
        } catch (e: Throwable) {
            Log.d(NOTIFY_NAME, "Failed to save thread cache.", e)
        }
    }

    private fun loadFromCache(): ThreadInfo? {
        try {
            val file = File(context.cacheDir, CACHE_FILENAME)
            ObjectInputStream(FileInputStream(file)).use {
                (it.readObject() as? ThreadInfo)?.also { result -> return result }
            }
        } catch (e: Throwable) {
            Log.d(NOTIFY_NAME, "Failed to load thread cache.", e)
        }
        return null
    }
}
