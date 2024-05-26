package eu.kanade.translation;

public enum LanguageTranslators {

    MLKIT("MlKit (Offline Version)"),
    GOOGLE("Google Translate"),
    GEMINI("Gemini AI (Key Needed)"),
    CHATGPT("ChatGPT (Key Needed) (Not Stable)");
    public String name;
    LanguageTranslators(String name) {
        this.name = name;
    }

}