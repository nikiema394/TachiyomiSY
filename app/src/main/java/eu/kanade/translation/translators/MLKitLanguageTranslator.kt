package eu.kanade.translation.translators


import TextTranslation
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.vision.text.Text.TextBlock
import eu.kanade.translation.ScanLanguage
import kotlinx.coroutines.tasks.await
import tachiyomi.core.common.util.system.logcat

class MLKitLanguageTranslator(scanLanguage: ScanLanguage) : LanguageTranslator {

    private var translator = Translation.getClient(
        TranslatorOptions.Builder().setSourceLanguage(scanLanguage.code).setTargetLanguage(TranslateLanguage.ENGLISH)
            .build(),
    )
    private var conditions = DownloadConditions.Builder().build()
    override suspend fun translate(blocks: List<TextBlock>) :ArrayList<TextTranslation>  {
        val list =ArrayList<TextTranslation>()
        try {
            translator.downloadModelIfNeeded(conditions).await()
            blocks.forEach { block ->   list.add(TextTranslation(block,translator.translate(block.text.replace("\n"," ")).await()))}
        } catch (e: Exception) {
            logcat { "Image Translation Error : ${e.message}" }
        }
        return list;
    }
}
