package eu.kanade.translation;

import com.google.mlkit.nl.translate.TranslateLanguage;

public enum LanguageTranslators {

    MLKIT("MlKit (Offline Version)"),
    GOOGLE("Google Translate"),
    GEMINI("Gemini AI (Key Needed)"),
    CHATGPT("ChatGPT (Key Needed)"),
    CLAUDE("Claude AI (Key Needed)");
    public String name;
    LanguageTranslators(String name) {
        this.name = name;
    }
}