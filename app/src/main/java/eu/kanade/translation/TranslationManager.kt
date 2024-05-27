package eu.kanade.translation

import TextTranslation
import android.content.Context
import eu.kanade.presentation.manga.components.TranslationState
import eu.kanade.tachiyomi.data.download.DownloadProvider
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.source.Source
import tachiyomi.core.common.storage.extension
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class TranslationManager(context: Context, private val downloadProvider: DownloadProvider = Injekt.get(),private val comicTranslator: ComicTranslator = Injekt.get()) {
    suspend fun getChapterTranslation(): TextTranslation {
        TODO()
    }

    suspend fun deleteTranslation(): TextTranslation {
        TODO()
    }
    fun isChapterTranslating(chapterName:String,scanlator:String,title:String,source:Source): Boolean {
        val dir = downloadProvider.findChapterDir(
            chapterName,
            scanlator,
            title,
            source,
        )
        //TODO()
        return false
//       return  dir?.name?.let { it1 -> comicTranslator.isInQueue(it1) } ?: false
    }
    fun getChapterTranslationStatus(chapterName:String,scanlator:String,title:String,source:Source): Boolean {

        val dir = downloadProvider.findChapterDir(
            chapterName,
            scanlator,
            title,
            source,
        )
            val translated =  dir?.findFile("translations.json")?.exists() ?: false
        val   activeTranslating = dir?.name?.let { it1 -> comicTranslator.isInQueue(it1) } ?: false
        TODO()
        val translationState = when {
            activeTranslating -> TranslationState.TRANSLATING
            translated -> TranslationState.TRANSLATED
            else -> TranslationState.NOT_TRANSLATED
        }
       return translationState;
    }
    fun isChapterTranslated(chapterName:String,scanlator:String,title:String,source:Source): Boolean {
        val dir = downloadProvider.findChapterDir(
            chapterName,
            scanlator,
            title,
            source,
        )
        return dir?.findFile("translations.json")?.exists() ?: false
    }

}
