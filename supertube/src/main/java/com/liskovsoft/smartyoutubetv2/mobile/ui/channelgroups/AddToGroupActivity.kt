package com.liskovsoft.smartyoutubetv2.mobile.ui.channelgroups

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video
import com.liskovsoft.smartyoutubetv2.mobile.R
import com.liskovsoft.smartyoutubetv2.mobile.channelgroups.GroupChannel
import com.liskovsoft.smartyoutubetv2.mobile.ui.base.MobileActivity
import com.liskovsoft.smartyoutubetv2.mobile.ui.theme.SuperTubeTheme

/** Transparent screen with the "Add to group" sheet, opened from the long press menu of a video or a channel */
class AddToGroupActivity : MobileActivity() {
    override val themeResId: Int = R.style.Theme_SuperTube_Mobile_Dialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val channelId = intent.getStringExtra(EXTRA_CHANNEL_ID) ?: return finish()
        val channel = GroupChannel(channelId, intent.getStringExtra(EXTRA_TITLE), intent.getStringExtra(EXTRA_ICON))
        setContent {
            SuperTubeTheme {
                AddToGroupSheet(channel) { finish() }
            }
        }
    }

    override fun finish() {
        super.finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }

    companion object {
        private const val EXTRA_CHANNEL_ID = "channel_id"
        private const val EXTRA_TITLE = "title"
        private const val EXTRA_ICON = "icon"

        fun start(context: Context, item: Video) {
            val channelId = item.channelId ?: return
            // A video's card image is its thumbnail: only channels bring their avatar
            val icon = item.cardImageUrl.takeIf { item.isChannel }
            val title = item.getAuthor() ?: item.getTitle().takeIf { item.isChannel }
            val intent = Intent(context, AddToGroupActivity::class.java)
                .putExtra(EXTRA_CHANNEL_ID, channelId)
                .putExtra(EXTRA_TITLE, title)
                .putExtra(EXTRA_ICON, icon)
            if (context !is android.app.Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }
}
