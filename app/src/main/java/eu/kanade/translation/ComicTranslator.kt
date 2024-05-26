package eu.kanade.translation

import TextTranslation
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Point
import android.graphics.text.LineBreakConfig
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
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
import tachiyomi.core.common.util.system.logcat
import java.util.Deque


class ComicTranslator(
    context: Context,
    private var scanLanguage: ScanLanguage = ScanLanguage.CHINESE,
    private var translationEngine: LanguageTranslators = LanguageTranslators.MLKIT,
    private var apiKey:String = "", var font:Int=0
) {
    private val queue= HashMap<String,HashMap<String,List<TextBlock>>>()
    private val pageImages= HashMap<String,HashMap<String,InputImage>>()
    private var debug = true
    private var recognizer = TextRecognizer(scanLanguage)
    private var languageTranslator: LanguageTranslator = getTranslator(translationEngine,scanLanguage,apiKey)
    private val textPaint = TextPaint()
    private val fonts = arrayOf( R.font.animeace,R.font.manga_master_bb,R.font.comic_book)
    private val multipliers= arrayOf(0.9f,0.8f,0.8f)

    init {
        textPaint.color = Color.BLACK
        textPaint.typeface = ResourcesCompat.getFont(context, fonts[font])
    }

    suspend fun queuePage(key:String,image: InputImage,filename: String) {
        val result = recognizer.recognize(image)
        queue.getOrPut(key) { HashMap() }[filename] = result.textBlocks.filter { it.boundingBox != null&&it.text.length>1 }
        pageImages.getOrPut(key) { HashMap() }[filename]= image
    }
    suspend fun processChapter(key:String,tmpDir: UniFile) {
        try {
            val pages = queue[key]!!
            val translated = languageTranslator.translate(pages)
            for (page in translated) {
                try {
                    val image= pageImages[key]?.get(page.key)!!
                    renderImage(page.value,image,tmpDir,page.key.split(".")[0])
                }catch (_:Exception){
                }
            }
        }catch (_:Exception){

        }
        queue.remove(key)
        pageImages.remove(key)
    }
    fun updateLanguage(language: ScanLanguage){
        this.scanLanguage = language
        this.recognizer = TextRecognizer(scanLanguage)
        this.languageTranslator = getTranslator(translationEngine, scanLanguage,apiKey)
        queue.clear()
    }
    fun updateEngine(engine: LanguageTranslators){
        this.translationEngine = engine
        this.languageTranslator = getTranslator(translationEngine, scanLanguage,apiKey)
        queue.clear()
    }
    fun updateAPIKey(key:String){
        this.apiKey = key
        this.languageTranslator = getTranslator(translationEngine, scanLanguage,apiKey)
        queue.clear()
    }
    fun updateFont(context: Context,fontIndex:Int){
        this.font=fontIndex
        this.textPaint.typeface=ResourcesCompat.getFont(context,fonts[font] )
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
            val textMultiplier= 0.9f
            //Paint Background
            for (tBlock in blocks) {
                val block = tBlock.block
                val rect = block.boundingBox!!
                val angle = block.lines.first().angle
                canvas.save()
                if(angle<89) canvas.rotate(angle,rect.centerX().toFloat(),rect.centerY().toFloat())
                canvas.drawRect(rect, bgPaint)
                canvas.restore()
            }

            for (tBlock in blocks) {
                if (debug) logcat { "Image OCR Translated : ${tBlock.translated}" }
                val block = tBlock.block
                val rect = block.boundingBox ?: continue
                val text = tBlock.translated
                val angle = block.lines.first().angle
                val symbolRect = block.lines.first().elements.first().symbols.first().boundingBox!!
                val left=(symbolRect.left.toFloat()-rect.width()*0.15f).coerceAtLeast(0f)
                var top = (symbolRect.top.toFloat()-rect.height()*0.15f).coerceAtLeast(0f)
                textPaint.textSize = symbolRect.height().toFloat()*textMultiplier
                canvas.save()
                for (point in points) {
                    if(point.y-top>=0&&point.x-left>-30&& point.x-left<30){
                        top+=5+point.y-top
                        break;
                    }
                }
                canvas.translate(left,top)
                //Rotate Text
                if(angle<89) canvas.rotate(angle)
                //Static Layout for Text Wrapping
                val builder = StaticLayout.Builder.obtain(text, 0, text.length, textPaint, (rect.width()*1.3).toInt())
                    .setAlignment(Layout.Alignment.ALIGN_CENTER)
                    .setBreakStrategy(Layout.BREAK_STRATEGY_BALANCED)
                val layout = builder.build()
                points.add(Point(left.toInt(), (top+layout.height).toInt()))

                layout.draw(canvas)
                canvas.restore()
            }

            //Compress and Save
            val file = tmpDir.createFile("$filename.jpg")
            if (file != null) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, file.openOutputStream())
            }
        }catch (e:Exception){
            logcat { "Process Image Error : ${e.stackTrace}" }
        }

    }
    private fun getTranslator(engine: LanguageTranslators, language: ScanLanguage,key:String): LanguageTranslator {
        return when (engine) {
            LanguageTranslators.MLKIT -> MLKitLanguageTranslator(language)
            LanguageTranslators.GOOGLE -> GoogleLanguageTranslator(language)
            LanguageTranslators.CHATGPT->ChatGPTLanguageTranslator(language,key)
            LanguageTranslators.GEMINI->GeminiLanguageTranslator(language,key)
        }
    }

}
