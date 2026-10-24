package com.liskovsoft.smartyoutubetv2.mobile.widget

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.bumptech.glide.Glide
import com.liskovsoft.smartyoutubetv2.mobile.R
import com.liskovsoft.smartyoutubetv2.mobile.prefs.FavoriteVideo
import com.liskovsoft.smartyoutubetv2.mobile.prefs.FavoritesStore

/** Rows of the favorites widget (every folder, newest first, without duplicates) */
class FavoritesWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory = Factory(applicationContext)

    private class Factory(private val context: Context) : RemoteViewsFactory {
        private var videos: List<FavoriteVideo> = emptyList()

        override fun onCreate() {}

        override fun onDataSetChanged() {
            videos = FavoritesStore.instance(context).folders.flatMap { it.videos }.distinctBy { it.videoId }.take(MAX_ITEMS)
        }

        override fun onDestroy() {}

        override fun getCount(): Int = videos.size

        override fun getViewAt(position: Int): RemoteViews {
            val video = videos.getOrNull(position) ?: return emptyRow()
            return RemoteViews(context.packageName, R.layout.widget_favorites_item).apply {
                setTextViewText(R.id.widget_item_title, video.title ?: "")
                setTextViewText(R.id.widget_item_author, video.author ?: "")
                thumbnail(video.thumbnail)?.let { setImageViewBitmap(R.id.widget_item_thumb, it) }
                setOnClickFillInIntent(R.id.widget_item, Intent().setData(Uri.parse("https://www.youtube.com/watch?v=${video.videoId}")))
            }
        }

        /** Runs on a binder thread: blocking Glide load is allowed here */
        private fun thumbnail(url: String?): Bitmap? = try {
            if (url == null) null else Glide.with(context).asBitmap().load(url).centerCrop().submit(224, 126).get()
        } catch (e: Exception) {
            null
        }

        override fun getLoadingView(): RemoteViews = emptyRow()

        private fun emptyRow(): RemoteViews = RemoteViews(context.packageName, R.layout.widget_favorites_item)

        override fun getViewTypeCount(): Int = 1

        override fun getItemId(position: Int): Long = videos.getOrNull(position)?.videoId?.hashCode()?.toLong() ?: position.toLong()

        override fun hasStableIds(): Boolean = true
    }

    companion object {
        private const val MAX_ITEMS = 30
    }
}
