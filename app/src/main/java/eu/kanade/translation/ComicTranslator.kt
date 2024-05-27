package eu.kanade.translation

import TextTranslation
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Point
import android.graphics.text.LineBreakConfig
import android.graphics.text.LineBreaker
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.annotation.RequiresApi
import androidx.compose.ui.util.fastForEachReversed
import androidx.core.content.res.ResourcesCompat
import com.google.common.collect.EvictingQueue

import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text.TextBlock
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.R
import eu.kanade.translation.translators.ChatGPTLanguageTranslator
import eu.kanade.translation.translators.GeminiLanguageTranslator
import eu.kanade.translation.translators.GoogleLanguageTranslator
import eu.kanade.translation.translators.LanguageTranslator
import eu.kanade.translation.translators.MLKitLanguageTranslator
import eu.kanade.translation.translators.TextRecognizer
import logcat.logcat
import tachiyomi.core.common.util.system.logcat
import java.util.Deque
import kotlin.math.abs


class ComicTranslator(
    context: Context,
    private var scanLanguage: ScanLanguage = ScanLanguage.CHINESE,
    private var translationEngine: LanguageTranslators = LanguageTranslators.MLKIT,
    private var apiKey: String = "", var font: Int = 0,
) {
    private val queue = HashMap<String, HashMap<String, List<TextTranslation>>>()
    private val pageImages = HashMap<String, HashMap<String, InputImage>>()
    private var debug = true
    private var recognizer = TextRecognizer(scanLanguage)
    private var languageTranslator: LanguageTranslator = getTranslator(translationEngine, scanLanguage, apiKey)
    private val textPaint = TextPaint()
    private val fonts = arrayOf(R.font.animeace, R.font.manga_master_bb, R.font.comic_book)
    private val multipliers = arrayOf(0.9f, 0.8f, 0.8f)

    init {
        textPaint.color = Color.BLACK
        textPaint.typeface = ResourcesCompat.getFont(context, fonts[font])
    }

    fun isInQueue(key: String): Boolean {
        return queue.containsKey(key)
    }

    fun removeQueue(key: String) {
        queue.remove(key)
    }

    suspend fun queuePage(key: String, image: InputImage, filename: String) {
        val result = recognizer.recognize(image)
        val blocks=result.textBlocks.filter { it.boundingBox != null && it.text.length > 1 }
        val resultant=ArrayList<TextTranslation>()
        for (block in blocks){
            try {
                var passed=true;
                val bounds=block.boundingBox!!
//                for (i in resultant.lastIndex downTo (resultant.lastIndex-5).coerceAtLeast(0)) {
//                    val b2=resultant[i]
//                    val b2b=b2.bounds
//                    if(b2b.bottom>=bounds.top&& abs(b2b.left-bounds.left) < 20){
//                        passed=false
//                        b2.text+=" "+block.text.replace("\n"," ")
//                        break;
//                    }
//                }
                if(passed){
                    resultant.add(TextTranslation(text=block.text.replace("\n"," "),bounds=bounds, symbolBound = block.lines.first().elements.first().symbols.first().boundingBox!!,angle =block.lines.first().angle))
                }
            }
            catch (e:Exception){
                logcat { "ERROR : ${e.stackTraceToString()}" }
            }
        }
        if(resultant.isNotEmpty()){
            queue.getOrPut(key) { HashMap() }[filename] =resultant
            pageImages.getOrPut(key) { HashMap() }[filename] = image
        }
    }

    suspend fun processChapter(key: String, tmpDir: UniFile) {
        try {
            val pages = queue[key]!!
            languageTranslator.translate(pages)
            for (page in pages) {
                try {
                    val image = pageImages[key]?.get(page.key)!!
                    renderImage(page.value, image, tmpDir, page.key)
                } catch (_: Exception) {
                }
            }
        } catch (_: Exception) {

        }
        queue.remove(key)
        pageImages.remove(key)
    }

    fun updateLanguage(language: ScanLanguage) {
        this.scanLanguage = language
        this.recognizer = TextRecognizer(scanLanguage)
        this.languageTranslator = getTranslator(translationEngine, scanLanguage, apiKey)
        queue.clear()
    }

    fun updateEngine(engine: LanguageTranslators) {
        this.translationEngine = engine
        this.languageTranslator = getTranslator(translationEngine, scanLanguage, apiKey)
        queue.clear()
    }

    fun updateAPIKey(key: String) {
        this.apiKey = key
        this.languageTranslator = getTranslator(translationEngine, scanLanguage, apiKey)
        queue.clear()
    }

    fun updateFont(context: Context, fontIndex: Int) {
        this.font = fontIndex
        this.textPaint.typeface = ResourcesCompat.getFont(context, fonts[font])
    }

    private fun renderImage(blocks: List<TextTranslation>, image: InputImage, tmpDir: UniFile, filename: String) {
        try {
            val bmp = image.bitmapInternal
            val bitmap = Bitmap.createBitmap(image.width, image.height, Bitmap.Config.RGB_565)
            val points = EvictingQueue.create<Point>(4)
            val canvas = Canvas(bitmap)
            if (bmp != null) {
                canvas.drawBitmap(bmp, 0F, 0F, null)
            }
            val bgPaint = Paint()
            bgPaint.style = Paint.Style.FILL
            bgPaint.color = Color.WHITE
            val textMultiplier = 0.9f
            //Paint Background
            for (block in blocks) {
                val rect = block.bounds
                val angle = block.angle
                canvas.save()
                if (angle < 89) canvas.rotate(angle, rect.centerX().toFloat(), rect.centerY().toFloat())
                canvas.drawRect(rect, bgPaint)
                canvas.restore()
            }

            for (block in blocks) {
                if (debug) logcat { "Image OCR Translated : ${block.translated}" }
                val rect = block.bounds
                val text = block.translated
                val angle = block.angle
                val symbolRect = block.symbolBound
                val left = (symbolRect.left.toFloat() - rect.width() * 0.15f).coerceAtLeast(0f)
                var top = (symbolRect.top.toFloat() - rect.height() * 0.15f).coerceAtLeast(0f)
                textPaint.textSize = symbolRect.height().toFloat() * textMultiplier
                canvas.save()
                for (point in points) {
                    logcat { "POINT : ${point.y-top} : ${point.x-left}" }
                    if (point.y > top && abs(point.x - left) <30 ) {
                        top += 5 + point.y - top
                        break;
                    }
                }
                canvas.translate(left, top)
                //Rotate Text
                if (angle < 89) canvas.rotate(angle)
                //Static Layout for Text Wrapping
                val builder =
                    StaticLayout.Builder.obtain(text, 0, text.length, textPaint, (rect.width() * 1.3).toInt())
                        .setAlignment(Layout.Alignment.ALIGN_CENTER)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    builder.setBreakStrategy(LineBreaker.BREAK_STRATEGY_HIGH_QUALITY)
                }
                val layout = builder.build()
                points.add(Point(left.toInt(), (top + layout.height).toInt()))
                bgPaint.color = Color.RED
                bgPaint.style= Paint.Style.STROKE
                canvas.drawRect(0f,0f,layout.width.toFloat(),layout.height.toFloat(),bgPaint)
                layout.draw(canvas)
                canvas.restore()
            }

            //Compress and Save
            tmpDir.findFile(filename)?.renameTo("$filename.bkp")
            val file = tmpDir.createFile("${filename.split(".")[0]}.jpg")
            if (file != null) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, file.openOutputStream())
            }
        } catch (e: Exception) {
            logcat { "Process Image Error : ${e.stackTrace}" }
        }

    }

    private fun getTranslator(engine: LanguageTranslators, language: ScanLanguage, key: String): LanguageTranslator {
        return when (engine) {
            LanguageTranslators.MLKIT -> MLKitLanguageTranslator(language)
            LanguageTranslators.GOOGLE -> GoogleLanguageTranslator(language)
            LanguageTranslators.CHATGPT -> ChatGPTLanguageTranslator(language, key)
            LanguageTranslators.GEMINI -> GeminiLanguageTranslator(language, key)
        }
    }

}
