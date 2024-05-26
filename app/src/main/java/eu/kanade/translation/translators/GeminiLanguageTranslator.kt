package eu.kanade.translation.translators


import TextTranslation
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.BlockThreshold
import com.google.ai.client.generativeai.type.Content
import com.google.ai.client.generativeai.type.HarmCategory
import com.google.ai.client.generativeai.type.SafetySetting
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import com.google.mlkit.vision.text.Text.TextBlock
import eu.kanade.translation.ScanLanguage
import kotlinx.serialization.json.JsonObject
import org.json.JSONArray
import org.json.JSONObject
import tachiyomi.core.common.util.system.logcat

class GeminiLanguageTranslator(scanLanguage: ScanLanguage, var key: String) : LanguageTranslator {

    private var model: GenerativeModel = GenerativeModel(
        modelName = "gemini-1.5-pro",
        apiKey = apiKey,
        generationConfig = generationConfig {
            temperature = 1f
            topK = 64
            topP = 0.95f
            maxOutputTokens = 8192
            responseMimeType = "application/json"
        },
        safetySettings = listOf(
            SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.NONE),
            SafetySetting(HarmCategory.HATE_SPEECH, BlockThreshold.NONE),
            SafetySetting(HarmCategory.SEXUALLY_EXPLICIT, BlockThreshold.NONE),
            SafetySetting(HarmCategory.DANGEROUS_CONTENT, BlockThreshold.NONE),
        ),
        systemInstruction = content { text("You will be provided some text in some Language separated by \"|\" and your task is to translate it into English. output should be a json array containing all the translated text") },
    );
    private var chat = model.startChat()

    private suspend fun translateText(text: String): JSONArray{
        try {
            val response = chat.sendMessage(text)
            logcat { "gemini image res : ${response.text}" }
            return JSONArray("${response.text}")

        } catch (e: Exception) {
            logcat { "Image Translation Error : $e" }
        }
        return JSONArray()
    }

    override suspend fun translate(blocks: List<TextBlock>): ArrayList<TextTranslation> {
        val list = ArrayList<TextTranslation>()

        try {
            val text = blocks.joinToString(separator = " | ") { it.text.replace("\n", " ") };
            logcat { "gemini image res1 : ${text}" }
            val translations = translateText(text)
            blocks.forEachIndexed { index, textBlock ->
                list.add(
                    TextTranslation(
                        textBlock,
                        translations.getString(index),
                    ),
                )
            }
        } catch (e: Exception) {
            logcat { "Image Translation Error : ${e.message}" }
        }
        return list;
    }


}
