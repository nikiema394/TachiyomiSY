package eu.kanade.translation.translators


import TextTranslation
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.BlockThreshold
import com.google.ai.client.generativeai.type.HarmCategory
import com.google.ai.client.generativeai.type.SafetySetting
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import com.google.mlkit.vision.text.Text.TextBlock
import eu.kanade.translation.ScanLanguage
import org.json.JSONObject
import tachiyomi.core.common.util.system.logcat

class GeminiLanguageTranslator(scanLanguage: ScanLanguage, var key: String) : LanguageTranslator {
    private var apiKey = ""
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
        systemInstruction = content { text("You will be provided some text in some Language and your task is to translate it into English, output should be a objects with name as key and array of translations as value") },
    );
    private var chat = model.startChat()

    override suspend fun translate(pages: HashMap<String, List<TextTranslation>>){
        try {
            val data= pages.mapValues { (k,v)->v.map { b -> b.text}}
            val json = JSONObject(data)
            val response = chat.sendMessage(json.toString())
            val resJson = JSONObject("${response.text}")
            pages.forEach { (k,v)->v.forEachIndexed{ i,b->b.translated=resJson.getJSONArray(k).getString(i) }}

        } catch (e: Exception) {
            logcat { "Image Translation Error : ${e.message}" }
        }

    }


}
