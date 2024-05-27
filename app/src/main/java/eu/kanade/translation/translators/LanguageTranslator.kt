package eu.kanade.translation.translators

import TextTranslation
import com.google.mlkit.vision.text.Text.TextBlock


interface LanguageTranslator {

    suspend fun translate(pages:   HashMap<String, List<TextTranslation>>)
}
