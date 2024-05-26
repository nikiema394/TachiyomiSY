package eu.kanade.translation.translators


import TextTranslation
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.vision.text.Text.TextBlock
import eu.kanade.translation.ScanLanguage
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import tachiyomi.core.common.util.system.logcat

class MLKitLanguageTranslator(scanLanguage: ScanLanguage) : LanguageTranslator {

    private var translator = Translation.getClient(
        TranslatorOptions.Builder().setSourceLanguage(scanLanguage.code).setTargetLanguage(TranslateLanguage.ENGLISH)
            .build(),
    )
    private var conditions = DownloadConditions.Builder().build()
    override suspend fun translate(pages: HashMap<String, List<TextBlock>>): Map<String, List<TextTranslation>> {
        try {
            translator.downloadModelIfNeeded(conditions).await()
            val result = pages.mapValues { (k,v)->v.map {  b->TextTranslation(b,translator.translate(b.text.replace("\n"," ")).await()) }}
            return result
        } catch (e: Exception) {
            logcat { "Image Translation Error : ${e.message}" }
        }
        return HashMap()
    }
}
