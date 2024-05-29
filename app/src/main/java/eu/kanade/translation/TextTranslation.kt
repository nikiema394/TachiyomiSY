package eu.kanade.translation

import com.google.mlkit.vision.text.Text.TextBlock
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class TextTranslation(
    var text: String,
    var width: Float,
    var height: Float,
    val x: Float,
    val y: Float,
    val angle: Float,
    var translated: String = "",
    @Transient
    val textBlock: TextBlock? = null,
    @Transient
    val imgWidth: Int = 0,

    @Transient
    val imgHeight: Int = 0,
)
