package eu.kanade.translation.translators;

import com.google.mlkit.nl.translate.TranslateLanguage;

public enum ScanLanguage {

    CHINESE(TranslateLanguage.CHINESE),
    JAPANESE(TranslateLanguage.JAPANESE),
    KOREAN(TranslateLanguage.KOREAN),
    LATIN(TranslateLanguage.ENGLISH);
    public String code;
    ScanLanguage(String lang) {
        this.code = lang;
    }
}