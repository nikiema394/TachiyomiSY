package eu.kanade.translation

import TextTranslation
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.res.ResourcesCompat

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


class ComicTranslator(
    context: Context,
    private var scanLanguage: ScanLanguage = ScanLanguage.CHINESE,
    private var translationEngine: LanguageTranslators = LanguageTranslators.MLKIT,
    private var apiKey:String = "", var font:Int=0
) {
    private val queue= HashMap<String,HashMap<String,List<TextBlock>>>()
    private val pageImages= HashMap<String,HashMap<String,InputImage>>()
    private var debug = false
    private var recognizer = TextRecognizer(scanLanguage)
    private var languageTranslator: LanguageTranslator = getTranslator(translationEngine,scanLanguage,apiKey)
    private val textPaint = TextPaint()
    private val fonts = intArrayOf(R.font.animeace,R.font.manga_master_bb,R.font.comic_book)

    init {
        textPaint.color = Color.BLACK
        textPaint.typeface = ResourcesCompat.getFont(context, fonts[font])
    }

    suspend fun queuePage(key:String,image: InputImage,filename: String) {
        val result = recognizer.recognize(image)
        queue.getOrPut(key) { HashMap() }[filename] = result.textBlocks
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
            val canvas = Canvas(bitmap)
            if (bmp != null) {
                canvas.drawBitmap(bmp, 0F, 0F, null)
            }
            val bgPaint = Paint()
            bgPaint.style = Paint.Style.STROKE
            bgPaint.color = Color.BLACK

            //Paint Background
            for (tBlock in blocks) {
                if (debug) logcat { "Image OCR Translated : ${tBlock.translated}" }
                val block = tBlock.block
                val angle = block.lines.first().angle
                val symbolRect = block.lines.first().elements.first().symbols.first().boundingBox!!
                canvas.save()
                canvas.translate(symbolRect.left.toFloat(), symbolRect.top.toFloat())
                canvas.rotate(angle)
                for (line in block.lines) {
                    val r = line.boundingBox!!
                    canvas.drawRect(0f,0f,  r.width().toFloat(), r.height().toFloat(), bgPaint)
                }
                canvas.restore()
            }
            for (tBlock in blocks) {
                val block = tBlock.block
                val rect = block.boundingBox ?: continue
                val text = tBlock.translated
                val angle = block.lines.first().angle
                val symbolRect = block.lines.first().elements.first().symbols.first().boundingBox!!
                textPaint.textSize = symbolRect.height().toFloat()
                canvas.save()
                canvas.translate(symbolRect.left.toFloat(), symbolRect.top.toFloat())
                //Rotate Text
                canvas.rotate(angle)

                //Static Layout for Text Wrapping
                val builder = StaticLayout.Builder.obtain(text, 0, text.length, textPaint, rect.width())
                    .setAlignment(Layout.Alignment.ALIGN_CENTER)
                    .setLineSpacing(1f, 1f)
                    .setIncludePad(false)
                val layout = builder.build()
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
