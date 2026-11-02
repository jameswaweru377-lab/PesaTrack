package com.kedevjimmy.pesatrack.planning

import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.text.SpannableString
import android.text.style.UnderlineSpan
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.kedevjimmy.pesatrack.R

/** Native, system-owned phone banner with the approved PesaTrack content hierarchy. */
object PhoneNoticeDesign {
    enum class Illustration(val drawable: Int) {
        HOME(R.drawable.ic_notice_home),
        WALLET(R.drawable.ic_notice_wallet),
        TARGET(R.drawable.ic_notice_target),
        REVIEW(R.drawable.ic_notice_review),
        PEOPLE(R.drawable.ic_notice_people),
        SHIELD(R.drawable.ic_notice_shield),
        CLOUD(R.drawable.ic_notice_cloud),
        BELL(R.drawable.ic_notice_bell)
    }

    /** Android / Redmi decides the card corners, status header and expansion chevron. */
    fun apply(
        builder: NotificationCompat.Builder,
        context: Context,
        badge: String,
        title: String,
        description: String,
        action: String,
        art: Illustration,
        open: PendingIntent
    ): NotificationCompat.Builder {
        val compact = content(context, R.layout.notice_pesatrack_compact, badge, title, description, art)
        val expanded = content(context, R.layout.notice_pesatrack_expanded, badge, title, description, art)
        val linkedAction = SpannableString(action).apply {
            setSpan(UnderlineSpan(), 0, length, 0)
        }
        expanded.setTextViewText(R.id.notice_action, linkedAction)
        expanded.setOnClickPendingIntent(R.id.notice_action, open)
        return builder
            .setSmallIcon(R.drawable.ic_pesatrack_notification)
            .setColor(ContextCompat.getColor(context, R.color.notice_purple))
            .setLargeIcon(brandIcon(context))
            // Accessible fallbacks if the device chooses the standard collapsed template.
            .setContentTitle(title)
            .setContentText("$badge · $description")
            .setContentIntent(open)
            .setCustomContentView(compact)
            .setCustomBigContentView(expanded)
            .setCustomHeadsUpContentView(compact)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
    }

    private fun content(
        context: Context,
        layout: Int,
        badge: String,
        title: String,
        description: String,
        art: Illustration
    ): RemoteViews = RemoteViews(context.packageName, layout).apply {
        setTextViewText(R.id.notice_badge, badge)
        setTextViewText(R.id.notice_title, title)
        setTextViewText(R.id.notice_description, description)
        setImageViewResource(R.id.notice_category_icon, art.drawable)
    }

    private fun brandIcon(context: Context): Bitmap? {
        val drawable = ContextCompat.getDrawable(context, R.drawable.ic_pesatrack_launcher) ?: return null
        val size = (48 * context.resources.displayMetrics.density).toInt().coerceAtLeast(48)
        return Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).also { bitmap ->
            drawable.setBounds(0, 0, size, size)
            drawable.draw(Canvas(bitmap))
        }
    }
}
