package com.liskovsoft.smartyoutubetv2.mobile.ui.about

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.liskovsoft.sharedutils.helpers.MessageHelpers
import com.liskovsoft.smartyoutubetv2.mobile.BuildConfig
import com.liskovsoft.smartyoutubetv2.mobile.R
import com.liskovsoft.smartyoutubetv2.mobile.update.UpdateChecker

private const val REPO = "https://github.com/jgestal/SuperTube"
private const val SMARTTUBE_REPO = "https://github.com/yuliskov/SmartTube"

private class Library(val name: String, val license: String, val url: String)

/** Third-party code bundled in the APK (checked against the release build) */
private val LIBRARIES = listOf(
    Library("SmartTube, MediaServiceCore, SharedModules (yuliskov)", "MIT", SMARTTUBE_REPO),
    Library("ExoPlayer 2.10 (Amazon port)", "Apache 2.0", "https://github.com/amzn/exoplayer-amazon-port"),
    Library("AndroidX, Jetpack Compose, Material 3", "Apache 2.0", "https://developer.android.com/jetpack/androidx"),
    Library("Kotlin, kotlinx.coroutines", "Apache 2.0", "https://github.com/JetBrains/kotlin"),
    Library("OkHttp, Retrofit", "Apache 2.0", "https://square.github.io/okhttp/"),
    Library("Glide", "BSD, MIT, Apache 2.0", "https://github.com/bumptech/glide"),
    Library("RxJava, RxAndroid", "Apache 2.0", "https://github.com/ReactiveX/RxJava"),
    Library("Gson, Guava", "Apache 2.0", "https://github.com/google/guava"),
    Library("Cronet (Chromium)", "BSD-3-Clause", "https://chromium.googlesource.com/chromium/src/+/main/components/cronet/"),
    Library("Conscrypt", "Apache 2.0", "https://github.com/google/conscrypt"),
    Library("J2V8", "EPL 1.0", "https://github.com/eclipsesource/J2V8"),
    Library("dnsjava", "BSD-2-Clause", "https://github.com/dnsjava/dnsjava"),
    Library("NetCipher", "Apache 2.0", "https://github.com/guardianproject/NetCipher"),
    Library("JsonPath, nanojson", "Apache 2.0", "https://github.com/json-path/JsonPath"),
    Library("Java Regex (florianingerl)", "GPL 2.0", "https://github.com/florianingerl/com.florianingerl.util.regex"),
    Library("ProcessPhoenix, DiskLruCache", "Apache 2.0", "https://github.com/JakeWharton/ProcessPhoenix"),
)

/** SuperTube "About": version, authors, links and open source licenses */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var showLicense by remember { mutableStateOf(false) }
    val transparent = ListItemDefaults.colors(containerColor = Color.Transparent)

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.verticalScroll(rememberScrollState()).navigationBarsPadding().padding(bottom = 16.dp)) {
            Row(Modifier.padding(horizontal = 24.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                val icon = remember { context.packageManager.getApplicationIcon(context.packageName).toBitmap(192, 192).asImageBitmap() }
                Image(icon, null, Modifier.size(56.dp))
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("SuperTube ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text(stringResource(R.string.mobile_about_tagline), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Header(stringResource(R.string.mobile_about_authors))
            ListItem(
                modifier = Modifier.clickable { openUrl(context, "https://github.com/jgestal") },
                colors = transparent,
                leadingContent = { Icon(Icons.Outlined.Person, null) },
                headlineContent = { Text("Juan Gestal") },
                supportingContent = { Text(stringResource(R.string.mobile_about_supertube_role)) },
            )
            ListItem(
                modifier = Modifier.clickable { openUrl(context, SMARTTUBE_REPO) },
                colors = transparent,
                leadingContent = { Icon(Icons.Outlined.Person, null) },
                headlineContent = { Text("yuliskov") },
                supportingContent = { Text(stringResource(R.string.mobile_about_smarttube_role)) },
            )

            Header(stringResource(R.string.mobile_about_links))
            ListItem(
                modifier = Modifier.clickable { (context as? Activity)?.let { UpdateChecker.check(it, manual = true) } },
                colors = transparent,
                leadingContent = { Icon(Icons.Outlined.SystemUpdate, null) },
                headlineContent = { Text(stringResource(R.string.mobile_check_updates)) },
            )
            LinkItem(Icons.Outlined.Code, stringResource(R.string.mobile_about_source), REPO)
            LinkItem(Icons.Outlined.NewReleases, stringResource(R.string.mobile_about_releases), "$REPO/releases")
            LinkItem(Icons.Outlined.BugReport, stringResource(R.string.mobile_about_issues), "$REPO/issues")

            Header(stringResource(R.string.mobile_about_licenses))
            ListItem(
                modifier = Modifier.clickable { showLicense = true },
                colors = transparent,
                leadingContent = { Icon(Icons.Outlined.Description, null) },
                headlineContent = { Text("SuperTube") },
                supportingContent = { Text("MIT · © yuliskov, Juan Gestal") },
            )
            LIBRARIES.forEach { library ->
                ListItem(
                    modifier = Modifier.clickable { openUrl(context, library.url) },
                    colors = transparent,
                    leadingContent = { Spacer(Modifier.width(24.dp)) },
                    headlineContent = { Text(library.name, style = MaterialTheme.typography.bodyMedium) },
                    supportingContent = { Text(library.license) },
                )
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            Text(
                stringResource(R.string.mobile_about_disclaimer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
            )
        }
    }

    if (showLicense) {
        val text = remember { context.resources.openRawResource(R.raw.supertube_license).bufferedReader().use { it.readText() } }
        AlertDialog(
            onDismissRequest = { showLicense = false },
            confirmButton = { TextButton(onClick = { showLicense = false }) { Text(stringResource(R.string.mobile_close)) } },
            title = { Text("MIT License") },
            text = {
                Text(
                    text.substringAfter("MIT License").trim(),
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp, lineHeight = 15.sp),
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                )
            },
        )
    }
}

@Composable
private fun Header(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 24.dp, top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun LinkItem(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, url: String) {
    val context = LocalContext.current
    ListItem(
        modifier = Modifier.clickable { openUrl(context, url) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        leadingContent = { Icon(icon, null) },
        headlineContent = { Text(text) },
        supportingContent = { Text(url.removePrefix("https://"), maxLines = 1) },
    )
}

private fun openUrl(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (e: ActivityNotFoundException) {
        MessageHelpers.showMessage(context, url)
    }
}
