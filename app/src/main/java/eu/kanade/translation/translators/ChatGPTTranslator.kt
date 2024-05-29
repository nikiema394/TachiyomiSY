package eu.kanade.translation.translators


import eu.kanade.translation.TextTranslation
import eu.kanade.tachiyomi.network.await
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import tachiyomi.core.common.util.system.logcat

class ChatGPTTranslator(scanLanguage: ScanLanguage, private var apiKey: String) : TextTranslator {
    //TODO IMRPOVE THIS ONE ONCE I GOT A API KEY
    override suspend fun translate(pages: HashMap<String, List<TextTranslation>>){
        try {
            pages.forEach { (k,v)->v.forEach {  b->b.translated=translateText(b.text) }}
        } catch (e: Exception) {
            logcat { "Image Translation Error : ${e.message}" }
        }

    }
    private suspend fun translateText(text: String): String {
        try {
            val okHttpClient = OkHttpClient()
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val jsonObject = buildJsonObject {
                put("model", "gpt-3.5-turbo")
                putJsonArray("messages") {
                    addJsonObject {
                        put("role", "system")
                        put(
                            "content",
                            "You will be provided with a sentence in some Language, and your task is to translate it into English.",
                        )
                    }
                    addJsonObject {
                        put("role", "user")
                        put(
                            "content", text,
                        )
                    }
                }

            }.toString()
            val body = jsonObject.toRequestBody(mediaType)
            logcat { "Image : $jsonObject" }
            val access = "https://api.openai.com/v1/chat/completions"
            val build: Request =
                Request.Builder().url(access).header("Authorization", "Bearer $apiKey").post(body).build()
            val response = okHttpClient.newCall(build).await()
            val rBody = response.body
            val json = JSONObject(rBody.string())
            logcat { "Image Process result : $json" }
            return json.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");

        } catch (e: Exception) {
            logcat { "Image Translation Error : $e" }
        }
        return ""
    }
}
