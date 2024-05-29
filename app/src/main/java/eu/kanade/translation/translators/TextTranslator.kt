package eu.kanade.translation.translators

import eu.kanade.translation.TextTranslation


interface TextTranslator {

    suspend fun translate(pages:   HashMap<String, List<TextTranslation>>)
}
