package eu.kanade.translation;

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
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.R
import eu.kanade.translation.translators.ChatGPTLanguageTranslator
import eu.kanade.translation.translators.GeminiLanguageTranslator
import eu.kanade.translation.translators.GoogleLanguageTranslator
import eu.kanade.translation.translators.LanguageTranslator
import eu.kanade.translation.translators.MLKitLanguageTranslator
import eu.kanade.translation.translators.TextRecognizer
import tachiyomi.core.common.storage.nameWithoutExtension
import tachiyomi.core.common.util.system.logcat


class ComicTranslator2(
    context: Context,
    var scanLanguage: ScanLanguage = ScanLanguage.CHINESE,
    var translationEngine: LanguageTranslators = LanguageTranslators.MLKIT,
    var apiKey:String = "",
) {
    private var debug = false;
    private var recognizer = TextRecognizer(scanLanguage)
    private var languageTranslator: LanguageTranslator = getTranslator(translationEngine,scanLanguage,apiKey)
    private val textPaint = TextPaint();

    init {
        textPaint.color = Color.BLACK
//            textPaint.textAlign=Paint.Align.CENTER
        textPaint.typeface = ResourcesCompat.getFont(context, R.font.manga_master_bb)
    }

    private fun getTranslator(engine: LanguageTranslators, language: ScanLanguage,key:String): LanguageTranslator {
        return when (engine) {
            LanguageTranslators.MLKIT -> MLKitLanguageTranslator(language)
            LanguageTranslators.GOOGLE -> GoogleLanguageTranslator(language)
            LanguageTranslators.CHATGPT->ChatGPTLanguageTranslator(language,key)
            LanguageTranslators.GEMINI->GeminiLanguageTranslator(language,key)
//          LanguageTranslators.CLAUDE->TODO(),
            else -> MLKitLanguageTranslator(language)
        }
    }

    fun update(language: ScanLanguage=scanLanguage,engine: LanguageTranslators=translationEngine,key:String=apiKey) {
        this.scanLanguage = language
        this.apiKey = key
        this.translationEngine = engine
        this.recognizer = TextRecognizer(scanLanguage)
        this.languageTranslator = getTranslator(translationEngine, scanLanguage,key)
    }


    suspend fun processImage(context: Context, image: InputImage, tmpDir: UniFile, filename: String) {
        try {
            val googleTranslate = true;
            val result = recognizer.recognize(image)

            if (debug) logcat { "Image OCR : ${result.text}" }

            //Convert Text Blocks to AITextblocks
            val blocks = result.textBlocks
            //Translate To English
            val translated =languageTranslator.translate(blocks)

            //Manupulate Image To Show New Translations
            val bmp = image.bitmapInternal

            val bitmap = Bitmap.createBitmap(image.width, image.height, Bitmap.Config.RGB_565)
            val canvas = Canvas(bitmap)
            if (bmp != null) {
                canvas.drawBitmap(bmp, 0F, 0F, null)
            };

            for (tBlock in translated) {
                if (debug) logcat { "Image OCR Translated : ${tBlock.translated}" }

                val block = tBlock.block
                val rect = block.boundingBox ?: continue;
                val text = tBlock.translated
                val angle = block.lines.first().angle

                //Calculate Text Size based on width of Rect
//                val testTextSize = 80f
//                textPaint.textSize = testTextSize
//                val bounds = Rect()
//                textPaint.getTextBounds(text, 0, text.length, bounds)
//                val desiredTextSize = testTextSize * rect.width() / bounds.width()
//                textPaint.textSize = desiredTextSize.coerceAtLeast(30f)
                textPaint.textSize =
                    (block.lines.first().elements.first().symbols.first().boundingBox?.height()?.toFloat() ?: 25f)

                //Paint Background
                val bgPaint = Paint()
                bgPaint.style = Paint.Style.FILL
                bgPaint.color = Color.WHITE
                canvas.drawRect(rect, bgPaint)

                canvas.save()
                canvas.translate(rect.left.toFloat(), rect.top.toFloat());
                //Rotate Text
                canvas.rotate(angle)
                //Static Layout for Text Wrapping
                val builder = StaticLayout.Builder.obtain(text, 0, text.length, textPaint, rect.width())
                    .setAlignment(Layout.Alignment.ALIGN_CENTER)
                    .setLineSpacing(1f, 1f)
                    .setIncludePad(false);
                val layout = builder.build()
                layout.draw(canvas);
                canvas.restore()
            }

            //Compress and Save
            val file = tmpDir.createFile("$filename.jpg")
            if (file != null) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, file.openOutputStream())
            }

        } catch (e: Exception) {
            logcat { "Process Image Error : ${e.message}" }
        }

    }

}
