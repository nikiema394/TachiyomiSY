package eu.kanade.translation

import com.google.mlkit.vision.text.Text.TextBlock
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class TextTranslation(
    var text: String,
    var width: Float,
    var height: Float,
    var x: Float,
    var y: Float,
    val angle: Float,
    var translated: String = "",
)
