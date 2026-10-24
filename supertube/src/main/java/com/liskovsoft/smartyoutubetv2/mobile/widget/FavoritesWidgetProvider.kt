package com.liskovsoft.smartyoutubetv2.mobile.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.RemoteViews
import com.liskovsoft.smartyoutubetv2.mobile.R
import com.liskovsoft.smartyoutubetv2.mobile.ui.splash.SplashActivity

/** Home screen widget with the favorite videos. Tap a video to play it, tap the header to open the favorites. */
class FavoritesWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { id -> manager.updateAppWidget(id, buildViews(context, id)) }
    }

    companion object {
        const val EXTRA_OPEN_SECTION = "open_section"

        /** Called when the favorites change */
        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, FavoritesWidgetProvider::class.java))
            if (ids.isNotEmpty()) manager.notifyAppWidgetViewDataChanged(ids, R.id.widget_list)
        }

        private fun buildViews(context: Context, widgetId: Int): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_favorites)

            val service = Intent(context, FavoritesWidgetService::class.java)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                .apply { data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME)) }
            views.setRemoteAdapter(R.id.widget_list, service)
            views.setEmptyView(R.id.widget_list, R.id.widget_empty)

            // Items: the fill-in intent of each row adds the youtube.com/watch url (handled by the splash deep links)
            val mutable = if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0
            val play = PendingIntent.getActivity(
                context, 1,
                Intent(context, SplashActivity::class.java).setAction(Intent.ACTION_VIEW),
                PendingIntent.FLAG_UPDATE_CURRENT or mutable,
            )
            views.setPendingIntentTemplate(R.id.widget_list, play)

            val open = PendingIntent.getActivity(
                context, 2,
                Intent(context, SplashActivity::class.java).putExtra(EXTRA_OPEN_SECTION, com.liskovsoft.smartyoutubetv2.mobile.prefs.FavoritesStore.SECTION_ID),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.widget_header, open)
            views.setOnClickPendingIntent(R.id.widget_empty, open)
            return views
        }
    }
}
