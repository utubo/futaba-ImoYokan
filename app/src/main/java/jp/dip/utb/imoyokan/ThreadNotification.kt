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
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import jp.dip.utb.imoyokan.futaba.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*

class ThreadNotification(private val context: Context, private val intent: Intent) {

    fun notify(title: String? = null, text: String? = null) {
        GlobalScope.launch {
            val builder = ThreadInfoBuilder().apply {
                this.url = intent.str(Intent.EXTRA_TEXT)
                this.mail = intent.str(KEY_EXTRA_MAIL)
            }
            val threadInfo = builder.build()
            notifyAsync(threadInfo, title,  text)
        }
    }

    private fun createIntent(threadInfo: ThreadInfo): Intent {
        return Intent(context, NotificationReceiver::class.java)
            .putExtra(Intent.EXTRA_TEXT, threadInfo.url)
            .putExtra(KEY_EXTRA_PTUA, threadInfo.form.ptua)
            .putExtra(KEY_EXTRA_MAIL, threadInfo.form.mail)
            .putExtra(KEY_EXTRA_SORT, intent.str(KEY_EXTRA_SORT))
    }

    private fun notifyAsync(threadInfo: ThreadInfo, title: String? = null, text: String? = null) {
        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_imoyokan)
            .setContentTitle(title?: threadInfo.res)

        // 入力されたテキストを受け取るPendingIntent
        val replyRequestCode = REQUEST_CODE_REPLY + Random().nextInt(1000) // 返信のrequestCodeはかぶらないようにする！
        val replyPendingIntent = PendingIntent.getBroadcast(
            context,
            replyRequestCode,
            createIntent(threadInfo).putExtra(KEY_EXTRA_REQUEST_CODE, replyRequestCode) ,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        val replyTitle = if (threadInfo.form.mail.isNotEmpty()) "返信 ${STR_MAILADDRESS}${threadInfo.form.mail}" else "返信"
        val replyLabel = if (threadInfo.form.mail.isNotEmpty()) "${STR_MAILADDRESS}${threadInfo.form.mail}" else "@ﾒｰﾙｱﾄﾞﾚｽ(半角ｽﾍﾟｰｽ)本文"
        // 入力を受け取るやつ
        val remoteInput = RemoteInput.Builder(KEY_TEXT_REPLY)
            .setLabel(replyLabel)
            .build()
        val action = NotificationCompat
            .Action.Builder(android.R.drawable.ic_menu_send, replyTitle, replyPendingIntent)
            .addRemoteInput(remoteInput)
            .build()

        // リロードボタン
        var requestCode = REQUEST_CODE_RELOAD
        val reloadAction = NotificationCompat
            .Action.Builder(
                R.drawable.ic_action_reload,
                DateFormat.format("更新(HH:mm:ss)", Date()),
                PendingIntent.getBroadcast(context, ++requestCode, createIntent(threadInfo).putExtra(KEY_EXTRA_REQUEST_CODE, REQUEST_CODE_RELOAD) , PendingIntent.FLAG_CANCEL_CURRENT)
            )
            .build()

        // カタログボタン
        val catalogIntent = Intent(context, NotificationReceiver::class.java)
            .putExtra(Intent.EXTRA_TEXT, threadInfo.getCatalogUrl(intent.str(KEY_EXTRA_SORT)))
        val catalogAction = NotificationCompat
            .Action.Builder(
            android.R.drawable.ic_menu_gallery,
            DateFormat.format("カタログ", Date()),
            PendingIntent.getBroadcast(context, ++requestCode, catalogIntent, PendingIntent.FLAG_CANCEL_CURRENT)
        ).build()

        notificationBuilder
            .addAction(action)
            .addAction(reloadAction)
            .addAction(catalogAction)

        // 読み込みに失敗していた場合
        if (threadInfo.isFailed) {
            threadInfo.replies.add(ResInfo(0, threadInfo.res, "スレッド取得失敗${aroundWhenIsNotEmpty("\n", threadInfo.message, "")}"))
        }

        // ここからカスタムView
        val view = RemoteViews(context.packageName, R.layout.notification_thread)

        // スレ画像
        if (threadInfo.catalogImage != null) {
            view.setImageViewBitmap(R.id.large_icon, threadInfo.catalogImage)
        } else {
            view.setViewVisibility(R.id.large_icon, View.GONE)
        }

        // レス
        val sb = SpannableStringBuilder()
        threadInfo.replies.takeLast(MAX_RES_COUNT).forEach {
            val mail = aroundWhenIsNotEmpty("[", it.mail, "]") // メールは[]で囲う
            if (it.index == 0) {
                sb.addResponse("${it.number}${mail}", it.getCompressText(), "\n")
            } else {
                sb.addResponse("${it.index}${mail}", it.getCompressText())
            }
        }
        // メッセージ
        if (title != null || text != null) {
            sb.addResponse(title ?: "", text ?: "")
        }

        view.setTextViewText(R.id.text, sb)

        //共有ボタン
        view.setOnClickPendingIntent(R.id.share, createShareUrlIntent(context, threadInfo.url))

        // 表示するよ！
        notificationBuilder.setCustomBigContentView(view)
        notificationBuilder.setCustomContentView(view)
        notificationBuilder.setStyle(NotificationCompat.DecoratedCustomViewStyle())
        notificationBuilder.notifySilent(context, CHANNEL_ID)
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
}
