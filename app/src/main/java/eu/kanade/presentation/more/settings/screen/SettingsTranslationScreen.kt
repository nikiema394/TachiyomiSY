package eu.kanade.presentation.more.settings.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.util.fastMap
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.category.visualName
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.widget.TriStateListDialog
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.util.system.toast
import eu.kanade.translation.LanguageTranslators
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.runBlocking
import okhttp3.Headers
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.download.service.DownloadPreferences
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

object SettingsTranslationScreen : SearchableSettings {

    @ReadOnlyComposable
    @Composable
    override fun getTitleRes() = StringResource(R.string.pref_category_translation)

    @Composable
    override fun getPreferences(): List<Preference> {
        val downloadPreferences = remember { Injekt.get<DownloadPreferences>() }
        return listOf(
            Preference.PreferenceItem.SwitchPreference(
                pref = downloadPreferences.translateOnDownload(),
                title = "Auto Translate",
            ),
            getTranslateLanguage(downloadPreferences = downloadPreferences),
            getTranslateFont(downloadPreferences = downloadPreferences),
            getTranslateEngineGroup(downloadPreferences = downloadPreferences)
        )
    }

    @Composable
    private fun getTranslateLanguage(
        downloadPreferences: DownloadPreferences,
    ): Preference {
        val opts = listOf("Chinese", "Japanese", "Korean", "Latin")
        return Preference.PreferenceItem.ListPreference(
            pref = downloadPreferences.translateLanguage(),
            title = "Translate From",
            entries = listOf(0, 1, 2, 3)
                .associateWith {
                   opts[it]
                }
                .toImmutableMap(),
        )
    }
    @Composable
    private fun getTranslateFont(
        downloadPreferences: DownloadPreferences,
    ): Preference {
        val opts = listOf("Anime Ace", "Manga Master BB", "Comic Font")
        return Preference.PreferenceItem.ListPreference(
            pref = downloadPreferences.translationFont(),
            title = "Translation Font",
            entries = listOf(0, 1, 2)
                .associateWith {
                    opts[it]
                }
                .toImmutableMap(),
        )
    }

    @Composable
    private fun getTranslateEngineGroup(
        downloadPreferences: DownloadPreferences,
    ): Preference.PreferenceGroup {
        val opts = LanguageTranslators.entries.map { v -> v.name }
        return Preference.PreferenceGroup(
            title = "Translation Engine",
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.ListPreference(
                    pref = downloadPreferences.translationEngine(),
                    title = "Translator",
                    entries = listOf(0, 1, 2, 3)
                        .associateWith {
                            opts[it]
                        }
                        .toImmutableMap(),
                ),
                Preference.PreferenceItem.EditTextPreference(
                    pref = downloadPreferences.translationApiKey(),
                    title = "Translator API Key",
                    ),
            ),
        )
    }
}
