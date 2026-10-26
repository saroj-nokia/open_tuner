@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.opentuner.app.ui

import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

private const val SOURCE_URL = "https://github.com/saroj-nokia/open_tuner"
private const val GPL_URL = "https://www.gnu.org/licenses/gpl-3.0.html"
private const val APACHE_URL = "https://www.apache.org/licenses/LICENSE-2.0"

private class Credit(val name: String, val detail: String, val url: String)

// Ideas and designs the app is built on.
private val methodCredits = listOf(
    Credit("YIN pitch detection", "A. de Cheveigné and H. Kawahara, 2002", "https://doi.org/10.1121/1.1458024"),
    Credit(
        "Karplus-Strong plucked-string synthesis",
        "K. Karplus and A. Strong, 1983",
        "https://en.wikipedia.org/wiki/Karplus%E2%80%93Strong_string_synthesis",
    ),
    Credit("Material 3 Expressive", "Google's design system, used for the look and motion", "https://m3.material.io"),
)

// Keep in step with gradle/libs.versions.toml: everything the APK ships from outside this repo.
private val libraryCredits = listOf(
    Credit("Jetpack Compose and Material 3", "Google, Apache License 2.0", "https://developer.android.com/jetpack/compose"),
    Credit("AndroidX Core, Activity and Lifecycle", "Google, Apache License 2.0", "https://developer.android.com/jetpack/androidx"),
    Credit("AndroidX Graphics Shapes", "Google, Apache License 2.0", "https://developer.android.com/jetpack/androidx/releases/graphics"),
)

@Composable
fun AboutSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val version = remember {
        runCatching {
            val pm = context.packageManager
            val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(context.packageName, 0)
            }
            info.versionName
        }.getOrNull() ?: "unknown"
    }
    // A phone with no browser would otherwise crash on tap; say so instead of doing nothing.
    fun open(url: String) {
        runCatching { uriHandler.openUri(url) }.onFailure {
            Toast.makeText(context, "Couldn't open the link", Toast.LENGTH_SHORT).show()
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Open Tuner", style = MaterialTheme.typography.headlineMediumEmphasized)
                Text(
                    "Version $version",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                "A free, ad-free tuner, metronome and chord library. No accounts, no tracking, no internet access: " +
                    "the microphone is used only to hear your instrument, and nothing is recorded or sent anywhere.",
                style = MaterialTheme.typography.bodyMedium,
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Text("License", style = MaterialTheme.typography.titleMedium)
            Text(
                "Open Tuner is free software, released under the GNU General Public License, version 3. " +
                    "You may use, study, share and change it. If you distribute a modified version, it must stay " +
                    "under the same license with its source available. It comes with no warranty.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = { open(SOURCE_URL) }) { Text("Source code") }
                OutlinedButton(onClick = { open(GPL_URL) }) { Text("Full license text") }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Text("Credits", style = MaterialTheme.typography.titleMedium)
            methodCredits.forEach { CreditRow(it, onOpen = ::open) }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Text("Open source libraries", style = MaterialTheme.typography.titleMedium)
            libraryCredits.forEach { CreditRow(it, onOpen = ::open) }
            OutlinedButton(onClick = { open(APACHE_URL) }) { Text("Apache License 2.0") }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Text(
                "© 2026 Saroj-Tajpuriya",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** The whole row is the tap target, so there is no tiny link text to hit. */
@Composable
private fun CreditRow(credit: Credit, onOpen: (String) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(role = Role.Button, onClickLabel = "Open ${credit.name}") { onOpen(credit.url) }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(credit.name, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
            Text(
                credit.detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text("↗", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
    }
}
