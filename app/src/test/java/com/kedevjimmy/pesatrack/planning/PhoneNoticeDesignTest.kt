package com.kedevjimmy.pesatrack.planning

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
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
            assertNotNull(root.findViewById<ImageView>(R.id.notice_category_icon))
        }
    }

    @Test
    fun categoryIllustrationStaysWithinRightSideOfBothTemplates() {
        for (id in listOf(R.layout.notice_pesatrack_compact, R.layout.notice_pesatrack_expanded)) {
            val root = LayoutInflater.from(context).inflate(id, null)
            root.findViewById<TextView>(R.id.notice_badge).text = "SALARY"
            root.findViewById<TextView>(R.id.notice_title).text = "Has your salary arrived?"
            root.findViewById<TextView>(R.id.notice_description).text =
                "No salary entry found after your expected payday."
            val width = (320 * context.resources.displayMetrics.density).toInt()
            root.measure(
                View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.AT_MOST)
            )
            root.layout(0, 0, root.measuredWidth, root.measuredHeight)
            val icon = root.findViewById<ImageView>(R.id.notice_category_icon)
            var position = icon.left
            var parent = icon.parent
            while (parent is View && parent !== root) {
                position += parent.left
                parent = parent.parent
            }
            assertTrue("Illustration should be on the right side", position > root.width / 2)
            assertTrue("Illustration should not be clipped", position + icon.width <= root.width)
        }
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
