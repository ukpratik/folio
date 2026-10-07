// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.feature.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import io.github.ukpratik.folio.core.model.AppInfo

/**
 * "Send feedback" (FR-34): the user's email app with the app version and phone model filled in.
 * Nothing about their documents is included, and Folio itself sends nothing.
 */
internal fun Context.sendFeedback(info: AppInfo) {
    val body = getString(R.string.feedback_body, info.versionName, Build.VERSION.RELEASE, Build.MANUFACTURER, Build.MODEL)
    val uri = Uri.parse("mailto:" + Uri.encode(info.feedbackEmail))
        .buildUpon()
        .appendQueryParameter("subject", getString(R.string.feedback_subject))
        .appendQueryParameter("body", body)
        .build()
    startOrToast(Intent(Intent.ACTION_SENDTO, uri), R.string.no_email_app)
}

/** "Rate Folio": Play Store app (market://) with a web fallback, or the F-Droid page. */
internal fun Context.openStoreListing(info: AppInfo) {
    val store = Intent(Intent.ACTION_VIEW, Uri.parse(info.rateUrl))
    try {
        startActivity(store)
    } catch (e: ActivityNotFoundException) {
        val web = info.rateUrl.replace("market://details", "https://play.google.com/store/apps/details")
        startOrToast(Intent(Intent.ACTION_VIEW, Uri.parse(web)), R.string.no_store_app)
    }
}

private fun Context.startOrToast(intent: Intent, message: Int) {
    try {
        startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
