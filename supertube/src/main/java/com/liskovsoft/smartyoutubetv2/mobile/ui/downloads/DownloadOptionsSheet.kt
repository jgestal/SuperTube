package com.liskovsoft.smartyoutubetv2.mobile.ui.downloads

import android.text.format.Formatter
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.liskovsoft.smartyoutubetv2.mobile.R
import com.liskovsoft.smartyoutubetv2.mobile.download.DownloadKind
import com.liskovsoft.smartyoutubetv2.mobile.download.Downloads
import com.liskovsoft.smartyoutubetv2.mobile.prefs.MobilePrefs

/** Quality picker: one row per available video/audio quality with its size, plus the "Wi‑Fi only" switch */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadOptionsSheet(options: List<Downloads.Option>, onPick: (Downloads.Option) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val prefs = MobilePrefs.instance(context)
    val transparent = ListItemDefaults.colors(containerColor = Color.Transparent)

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.navigationBarsPadding().verticalScroll(rememberScrollState()).padding(bottom = 16.dp)) {
            Text(
                stringResource(R.string.mobile_download),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            for (kind in listOf(DownloadKind.VIDEO, DownloadKind.AUDIO)) {
                val ofKind = options.filter { it.kind == kind }
                if (ofKind.isEmpty()) continue
                Text(
                    stringResource(if (kind == DownloadKind.VIDEO) R.string.mobile_filter_video else R.string.mobile_filter_audio),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 24.dp, top = 12.dp, bottom = 4.dp),
                )
                ofKind.forEach { option ->
                    ListItem(
                        modifier = Modifier.clickable { onPick(option) },
                        colors = transparent,
                        leadingContent = { Icon(if (kind == DownloadKind.VIDEO) Icons.Outlined.Movie else Icons.Outlined.MusicNote, null) },
                        headlineContent = { Text(option.label) },
                        trailingContent = {
                            if (option.sizeBytes > 0) Text(Formatter.formatShortFileSize(context, option.sizeBytes), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        },
                    )
                }
            }
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            ListItem(
                modifier = Modifier.clickable { prefs.updateWifiOnlyDownloads(!prefs.wifiOnlyDownloads) },
                colors = transparent,
                leadingContent = { Icon(Icons.Outlined.Wifi, null) },
                headlineContent = { Text(stringResource(R.string.mobile_wifi_only)) },
                supportingContent = { Text(stringResource(R.string.mobile_wifi_only_desc)) },
                trailingContent = { Switch(checked = prefs.wifiOnlyDownloads, onCheckedChange = { prefs.updateWifiOnlyDownloads(it) }) },
            )
        }
    }
}
