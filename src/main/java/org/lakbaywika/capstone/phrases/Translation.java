package org.lakbaywika.capstone.phrases;

public class Translation {
    private static String language;
    private static String translateText;

    public Translation(String language, String translatedText) {
        this.language = language;
        this.translateText = translatedText;
    }

    public static String getLanguage() {
        return language;
    }

    public static void setLanguage(String language) {
        Translation.language = language;
    }

    public static String getTranslateText() {
        return translateText;
    }

    public static void setTranslateText(String translateText) {
        Translation.translateText = translateText;
    }
}
