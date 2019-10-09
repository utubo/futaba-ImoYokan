package jp.dip.utb.imoyokan

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION_CODES
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.PRIORITY_LOW
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import jp.dip.utb.imoyokan.futaba.ThreadInfo
import jp.dip.utb.imoyokan.futaba.ThreadInfoBuilder
import jp.dip.utb.imoyokan.futaba.analyseUrl
import jp.dip.utb.imoyokan.futaba.toColoredText
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ThreadNotification {

    fun showThread(context: Context, intent: Intent, title: String? = null, text: String? = null): String {
        val builder = ThreadInfoBuilder().apply {
            url = intent.getStringExtra(KEY_EXTRA_URL) ?: ""
            mail = intent.getStringExtra(KEY_EXTRA_MAIL) ?: ""
            cacheImg = intent.getParcelableExtra(KEY_EXTRA_CACHE_IMG)
        }
        if (analyseUrl(builder.url) == null) return "スレッド取得失敗(URLが変！)\n${builder.url}"
        GlobalScope.launch {
            val threadInfo = builder.build()
            showNotification(
                context,
                threadInfo,
                title,
                text
            )
        }
        return "通知領域に表示しました"
    }

    private fun createIntent(context: Context, threadInfo: ThreadInfo): Intent {
        return Intent(context,  NotificationReceiver::class.java)
            .putExtra(KEY_EXTRA_URL, threadInfo.url)
            .putExtra(KEY_EXTRA_PTUA, threadInfo.form.ptua)
            .putExtra(KEY_EXTRA_MAIL, threadInfo.form.mail)
            .putExtra(KEY_EXTRA_CACHE_IMG, threadInfo.catalogImage)
    }

    @SuppressLint("SimpleDateFormat")
    private fun showNotification(context: Context, threadInfo: ThreadInfo, title: String? = null, text: String? = null) {
        // 入力されたテキストを受け取るPendingIntent
        val replyPendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_REPLY + Random().nextInt(1000), // 返信のrequestCodeはかぶらないようにする！
            createIntent(context, threadInfo),
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
        val reloadAction = NotificationCompat
            .Action.Builder(
                R.drawable.ic_action_reload,
                SimpleDateFormat("更新(HH:mm:ss)").format(Date()),
                PendingIntent.getBroadcast(context, REQUEST_CODE_RELOAD, createIntent(context, threadInfo).putExtra(KEY_EXTRA_REQUEST_CODE, REQUEST_CODE_RELOAD) , PendingIntent.FLAG_CANCEL_CURRENT)
            )
            .build()

        //共有ボタン
        val share = Intent(Intent.ACTION_VIEW, Uri.parse(threadInfo.url))
            .setFlags(FLAG_ACTIVITY_SINGLE_TOP) // としあき(仮)対策
        val shareAction = NotificationCompat
            .Action.Builder(
                android.R.drawable.ic_menu_share,
                "共有",
                PendingIntent.getActivity(context, REQUEST_CODE_SHARE, Intent.createChooser(share, threadInfo.url), PendingIntent.FLAG_UPDATE_CURRENT)
            )
            .build()

        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_imoyokan)
            .setContentTitle(title?: threadInfo.res)
            .setContentText(text?: threadInfo.text)
            .addAction(action)
            .addAction(reloadAction)
            .addAction(shareAction)

        // ここからカスタムView
        val view = RemoteViews(context.packageName, R.layout.notification_thread)

        // スレ画像
        if (threadInfo.catalogImage != null) {
            view.setImageViewBitmap(R.id.right_icon, threadInfo.catalogImage)
        }

        // テキスト
        val sb = SpannableStringBuilder()
        // レス一覧
        if (threadInfo.replies.isEmpty()) {
            sb.addResponse(threadInfo.res, threadInfo.text, "\n")
        } else {
            threadInfo.replies.takeLast(MAX_RES_COUNT).forEach {
                val user =
                    // レス番
                    (if (it.index == 0) threadInfo.res else it.index.toString()) +
                            // メールアドレス
                            (threadInfo.mails[it.number]?.around("[", "]") ?: "")

                sb.addResponse(user, it.compressText, if (it.index == 0) "\n" else " ")
            }
        }
        // メッセージ
        if (title != null || text != null) {
            sb.addResponse(title ?: "", text ?: "")
        }
        view.setTextViewText(R.id.text, sb)
        notificationBuilder.setCustomBigContentView(view)
        notificationBuilder.setCustomContentView(view)
        notificationBuilder.setStyle(NotificationCompat.DecoratedCustomViewStyle())

        // 音がならないようにする
        notificationBuilder.setPriority(PRIORITY_LOW).build()
        val notificationManager = NotificationManagerCompat.from(context)
        if (Build.VERSION.SDK_INT >= VERSION_CODES.O) {
            setupNotificationManager(notificationManager)
        }

        // 表示するよ！
        notificationManager.notify(0, notificationBuilder.build())
    }

    @TargetApi(VERSION_CODES.O)
    private fun setupNotificationManager(notificationManager: NotificationManagerCompat) {
        if (notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
            val mChannel = NotificationChannel(CHANNEL_ID, NOTIFY_NAME, NotificationManager.IMPORTANCE_LOW)
            mChannel.apply {
                description = NOTIFY_DESCRIPTION
            }
            notificationManager.createNotificationChannel(mChannel)
        }
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
