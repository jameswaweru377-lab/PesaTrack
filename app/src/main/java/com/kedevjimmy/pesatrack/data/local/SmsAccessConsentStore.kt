package com.kedevjimmy.pesatrack.data.local

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.kedevjimmy.pesatrack.domain.onboarding.ConsentVersionStorage
import com.kedevjimmy.pesatrack.domain.onboarding.SmsAccessConsent

/** Android permission is not a substitute for the user's saved disclosure decision. */
object SmsAccessConsentStore {
    private fun consent(context: Context): SmsAccessConsent {
        val preferences = context.applicationContext.getSharedPreferences("sms_access_consent", Context.MODE_PRIVATE)
        return SmsAccessConsent(object : ConsentVersionStorage {
            override fun readVersion(): Int = preferences.getInt("accepted_disclosure_version", 0)
            override fun writeVersion(version: Int): Boolean {
                val saved = preferences.edit().putInt("accepted_disclosure_version", version).commit()
                // SharedPreferences updates memory even when its disk write fails.
                if (!saved) preferences.edit().remove("accepted_disclosure_version").apply()
                return saved
            }
        })
    }

    fun hasConsent(context: Context): Boolean = consent(context).hasConsent()

    fun setAccepted(context: Context, accepted: Boolean): Boolean =
        if (accepted) consent(context).accept() else consent(context).decline()

    fun canRead(context: Context): Boolean = consent(context).canRead(
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED)

    fun requireRead(context: Context) {
        if (!canRead(context)) throw SecurityException(
            "SMS access is off. Accept the disclosure in Import history and allow Android SMS access before reading messages.")
    }

    fun requireConsent(context: Context) {
        if (!hasConsent(context)) throw SecurityException(
            "SMS consent is off. Accept the disclosure in Import history to enable reading financial messages.")
    }
}
