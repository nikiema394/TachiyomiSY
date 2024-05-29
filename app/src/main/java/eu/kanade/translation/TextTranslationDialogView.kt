package eu.kanade.translation

import android.content.Context
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.util.TypedValue
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.toFontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.TextUnitType.Companion.Sp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.view.isVisible
import com.inidamleader.ovtracker.util.compose.AutoSizeText
import eu.kanade.tachiyomi.R
import logcat.logcat


class TextTranslationsComposeView :
    AbstractComposeView {

    private val translations: List<TextTranslation>
    val font = Font(
        resId = R.font.animeace, // Resource ID of the font file
        weight = FontWeight.Bold, // Weight of the font
    ).toFontFamily()
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
    ) : super(context, attrs, defStyleAttr) {
        this.translations = emptyList()
    }

    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
        translations: List<TextTranslation> = emptyList(),
    ) : super(context, attrs, defStyleAttr) {
        this.translations = translations
    }

    @Composable
    override fun Content() {
        TranslationsContent(translations)
    }

    @Composable
    fun TranslationsContent(translations: List<TextTranslation>) {
        var size by remember { mutableStateOf(IntSize.Zero) }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged {
                    size = it
                },
        ) {
            if (size == IntSize.Zero) return
            val imgWidth = size.width
            val imgHeight = size.height
            translations.forEach { translation ->
                //TODO give paddding when translatingt the text
                val xPx = ((translation.x-0.02) * imgWidth).toFloat()
                val yPx =  ((translation.y-0.015) * imgHeight).toFloat()
                val width = ((translation.width+0.04) * imgWidth).toFloat()
                val height =((translation.height+0.03) * imgHeight).toFloat()
                TextBlock(
                    translation = translation,
                    modifier = Modifier
                        .absoluteOffset(pxToDp(xPx), pxToDp(yPx))
                        .rotate(translation.angle)
                        .size(pxToDp(width), pxToDp(height)),
                )
            }
        }
    }

    @Composable
    fun TextBlock(translation: TextTranslation, modifier: Modifier) {
        Box(modifier = modifier) {
            AutoSizeText(
                text = translation.translated,
                color = Color.Red,
                softWrap = true, fontFamily = font,
                overflow = TextOverflow.Clip,
                alignment = Alignment.Center,
                modifier = Modifier
                    .background(Color.Blue.copy(alpha = 1f))
                    .padding(1.dp)

            )
        }
    }

    fun show() {
        isVisible = true
    }

    fun hide() {
        isVisible = false
    }

    private fun pxToDp(px: Float): Dp {
        return Dp(px / (context.resources.displayMetrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT))
    }
}
