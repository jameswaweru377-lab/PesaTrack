package com.kedevjimmy.pesatrack.planning

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.kedevjimmy.pesatrack.MainActivity
import com.kedevjimmy.pesatrack.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** Native notification layouts should remain legible on dark HyperOS shades. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class PhoneNoticeDesignTest {
    private val context: Context get() = RuntimeEnvironment.getApplication()

    @Test
    fun bothTemplatesHaveLightCanvasAndReadableDarkText() {
        for (id in listOf(R.layout.notice_pesatrack_compact, R.layout.notice_pesatrack_expanded)) {
            val root = LayoutInflater.from(context).inflate(id, null)
            val panel = root.background as GradientDrawable
            assertEquals(context.getColor(R.color.notice_card), panel.color?.defaultColor)
            val title = root.findViewById<TextView>(R.id.notice_title)
            val description = root.findViewById<TextView>(R.id.notice_description)
            assertEquals(context.getColor(R.color.notice_text), title.currentTextColor)
            assertEquals(context.getColor(R.color.notice_secondary), description.currentTextColor)
            assertNull(root.findViewById<View>(R.id.notice_category_icon))
        }
    }

    @Test
    fun notificationTemplatesHaveNoLargeIconOrAccentStripe() {
        for (id in listOf(R.layout.notice_pesatrack_compact, R.layout.notice_pesatrack_expanded)) {
            val root = LayoutInflater.from(context).inflate(id, null) as LinearLayout
            assertNull("The category icon is no longer shown on the right", root.findViewById<View>(R.id.notice_category_icon))
            assertEquals("No decorative edge stripe", 1, root.childCount)
        }
    }

    @Test
    fun expandedActionIsPlainReadableTextWithoutButtonOrUnderline() {
        val root = LayoutInflater.from(context).inflate(R.layout.notice_pesatrack_expanded, null)
        val action = root.findViewById<TextView>(R.id.notice_action)
        assertNull("Action should not have a filled button background", action.background)
        assertEquals(context.getColor(R.color.notice_coffee), action.currentTextColor)
        assertEquals(0, action.paintFlags and Paint.UNDERLINE_TEXT_FLAG)
        assertTrue("Action is tall enough to tap", action.layoutParams.height >=
            (48 * context.resources.displayMetrics.density).toInt())
    }

    @Test
    fun builderDoesNotAddDuplicateLargeBrandIcon() {
        val open = PendingIntent.getActivity(
            context,
            981,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = PhoneNoticeDesign.apply(
            NotificationCompat.Builder(context, "test-notification-layout"),
            context, "SALARY", "Has your salary arrived?",
            "No salary entry found after your expected payday.",
            "Add salary", PhoneNoticeDesign.Illustration.WALLET, open
        ).build()
        assertNull("The OS already shows the small PesaTrack icon", notification.getLargeIcon())
    }
}
