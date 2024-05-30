package tachiyomi.domain.download.service

import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore

class DownloadPreferences(
    private val preferenceStore: PreferenceStore,
) {
    //Translation Settings
    fun translateOnDownload() = preferenceStore.getBoolean("auto_translate_on_download", false)
    fun translateLanguage() = preferenceStore.getInt("auto_translate_language", 0)
    fun translationFont() = preferenceStore.getInt("auto_translate_font",0)
    fun translationEngine() = preferenceStore.getInt("auto_translation_engine", 0)
    fun translationApiKey() = preferenceStore.getString("auto_translation_api_key", "")


    fun downloadOnlyOverWifi() = preferenceStore.getBoolean(
        "pref_download_only_over_wifi_key",
        true,
    )

    fun saveChaptersAsCBZ() = preferenceStore.getBoolean("save_chapter_as_cbz", false)

    fun splitTallImages() = preferenceStore.getBoolean("split_tall_images", false)

    fun autoDownloadWhileReading() = preferenceStore.getInt("auto_download_while_reading", 0)

    fun removeAfterReadSlots() = preferenceStore.getInt("remove_after_read_slots", -1)

    fun removeAfterMarkedAsRead() = preferenceStore.getBoolean(
        "pref_remove_after_marked_as_read_key",
        false,
    )

    fun removeBookmarkedChapters() = preferenceStore.getBoolean("pref_remove_bookmarked", false)

    fun removeExcludeCategories() = preferenceStore.getStringSet(
        "remove_exclude_categories",
        emptySet(),
    )

    fun downloadNewChapters() = preferenceStore.getBoolean("download_new", false)

    fun downloadNewChapterCategories() = preferenceStore.getStringSet(
        "download_new_categories",
        emptySet(),
    )

    fun downloadNewChapterCategoriesExclude() = preferenceStore.getStringSet(
        "download_new_categories_exclude",
        emptySet(),
    )
}
