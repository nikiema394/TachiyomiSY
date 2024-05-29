package eu.kanade.translation

import android.content.Context
import android.graphics.Color
import android.text.TextPaint
import androidx.core.content.res.ResourcesCompat

import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text.TextBlock
import eu.kanade.tachiyomi.R
import eu.kanade.translation.translators.ChatGPTTranslator
import eu.kanade.translation.translators.GeminiTranslator
import eu.kanade.translation.translators.GoogleTranslator
import eu.kanade.translation.translators.TextTranslator
import eu.kanade.translation.translators.LanguageTranslators
import eu.kanade.translation.translators.MLKitTranslator
import eu.kanade.translation.translators.ScanLanguage
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToStream
import logcat.logcat
import tachiyomi.core.common.storage.extension
import kotlin.math.abs
import kotlin.math.max

/**TODOS
 * Build a Translation Class for state of Job ✔
 * Build a Translation Job Manager ✔
 * Rework the Translation Process to output to translations.json  ✔
 * Read translations.json while loading chapter for reader
 * Build ScanDialogView to display text translations
 */
class ChapterTranslator(
    private val context: Context,
    private var scanLanguage: ScanLanguage = ScanLanguage.CHINESE,
    private var translationEngine: LanguageTranslators = LanguageTranslators.MLKIT,
    private var apiKey: String = "", var font: Int = 0,
) {
    private var debug = true
    private var recognizer = TextRecognizer(scanLanguage)
    private var textTranslator: TextTranslator = getTranslator(translationEngine, scanLanguage, apiKey)
    private val textPaint = TextPaint()
    private val fonts = arrayOf(R.font.animeace, R.font.manga_master_bb, R.font.comic_book)

    init {
        textPaint.color = Color.BLACK
        textPaint.typeface = ResourcesCompat.getFont(context, fonts[font])
    }

    suspend fun translateChapter(translation: Translation) {
        val files = translation.dir.listFiles()!!.filter { "image" in it.type.orEmpty() }
        val pages = HashMap<String, List<TextTranslation>>()
        for (file in files) {
            try {
                val image = InputImage.fromFilePath(context, file.uri)
                val result = recognizer.recognize(image)
                val blocks = result.textBlocks.filter { it.boundingBox != null && it.text.length > 1 }
                val resultant = toTextTranslation(blocks, image.width, image.height)
                file.name?.let { pages.put(it, resultant) }
            } catch (e: Exception) {
            }
        }

        //Translate All Pages
        textTranslator.translate(pages)
        pages.forEach { page -> tempTest(page.value) }
        Json.encodeToStream(pages, translation.dir.createFile("translations.json")!!.openOutputStream())
    }

    private fun tempTest(blocks: List<TextTranslation>){
        val resultant = ArrayList<TextTranslation>()
        val width = blocks.first().imgHeight
        val height = blocks.first().imgHeight
        for (block in blocks) {
            try {
                var passed = true;
                val bounds = block.textBlock!!.boundingBox!!
                logcat { "Text : ${block.translated}" }

                for (i in resultant.lastIndex downTo (resultant.lastIndex - 5).coerceAtLeast(0)) {

                    val b2 = resultant[i]
                    val b2b = b2.textBlock!!.boundingBox!!

                    if (b2b.bottom >= bounds.top && abs(b2b.left - bounds.left) < 20) {
                        logcat { "${b2.translated} ||| bottom : ${b2b.bottom} | top2 : ${bounds.left} | ${abs(b2b.left - bounds.left)}" }
                        logcat { "ADDED : ${block.translated}" }
                        passed = false
//                        b2.text += " " + block.text.replace("\n", " ")
//                        b2.height = (b2b.height() + bounds.height()) / height.toFloat()
//                        b2.width = max(bounds.width(), b2b.width()) / width.toFloat()
                        break;
                    }
                }
                if (passed) {
                    resultant.add(
                        block,
                    )
                }
            } catch (e: Exception) {
                logcat { "ERROR : ${e.stackTraceToString()}" }
            }
        }
    }

    private fun toTextTranslation(blocks: List<TextBlock>, width: Int, height: Int): ArrayList<TextTranslation> {
        val resultant = ArrayList<TextTranslation>()
        for (block in blocks) {
            try {
                var passed = true;
                val bounds = block.boundingBox!!
//                logcat { "Text : ${block.text}" }

//                for (i in resultant.lastIndex downTo (resultant.lastIndex - 5).coerceAtLeast(0)) {
//
//                    val b2 = resultant[i]
//                    val b2b = b2.textBlock!!.boundingBox!!
//                    logcat { "bottom : ${b2b.bottom} | top2 : ${bounds.left} | ${abs(b2b.left - bounds.left)}" }
//                    if (b2b.bottom >= bounds.top && abs(b2b.left - bounds.left) < 20) {
//
//                        logcat { "ADDED : ${ block.text.replace("\n", " ")}" }
//                        passed = false
//                        b2.text += " " + block.text.replace("\n", " ")
//                        b2.height=(b2b.height()+bounds.height())/height.toFloat()
//                        b2.width= max(bounds.width(),b2b.width())/width.toFloat()
//                        break;
//                    }
//                }
                if (passed) {
                    val symbolBound = block.lines.first().elements.first().symbols.first().boundingBox!!
                    resultant.add(
                        TextTranslation(
                            text = block.text.replace("\n", " "),
                            x = (symbolBound.left / width.toFloat()),
                            y = symbolBound.top / height.toFloat(),
                            width = bounds.width() / width.toFloat(),
                            height = bounds.height() / height.toFloat(),
                            angle = block.lines.first().angle,
                            textBlock = block, imgHeight = height, imgWidth = width
                        ),
                    )
                }
            } catch (e: Exception) {
                logcat { "ERROR : ${e.stackTraceToString()}" }
            }
        }

        return resultant
    }

    private fun getTranslator(engine: LanguageTranslators, language: ScanLanguage, key: String): TextTranslator {
        return when (engine) {
            LanguageTranslators.MLKIT -> MLKitTranslator(language)
            LanguageTranslators.GOOGLE -> GoogleTranslator(language)
            LanguageTranslators.CHATGPT -> ChatGPTTranslator(language, key)
            LanguageTranslators.GEMINI -> GeminiTranslator(language, key)
        }
    }

    fun updateLanguage(language: ScanLanguage) {
        this.scanLanguage = language
        this.recognizer = TextRecognizer(scanLanguage)
        this.textTranslator = getTranslator(translationEngine, scanLanguage, apiKey)
    }

    fun updateEngine(engine: LanguageTranslators) {
        this.translationEngine = engine
        this.textTranslator = getTranslator(translationEngine, scanLanguage, apiKey)
    }

    fun updateAPIKey(key: String) {
        this.apiKey = key
        this.textTranslator = getTranslator(translationEngine, scanLanguage, apiKey)
    }

    fun updateFont(context: Context, fontIndex: Int) {
        this.font = fontIndex
        this.textPaint.typeface = ResourcesCompat.getFont(context, fonts[font])
    }

}
