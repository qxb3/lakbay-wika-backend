package org.lakbaywika.capstone;

import io.javalin.Javalin;
import org.apache.lucene.queryparser.classic.ParseException;
import org.lakbaywika.capstone.phrases.PhrasesLoader;
import org.vosk.Model;
import org.vosk.Recognizer;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.HashMap;

public class Main {
    public static void main(String[] args) throws ParserConfigurationException, IOException, SAXException, ParseException {
        Model model = new Model("/home/qxb3/Downloads/vosk-model-small-en-us-0.15");
        Javalin app = Javalin.create().start(4321);

        HashMap<String, HashMap<String, String>> phrases = PhrasesLoader.load();

        app.get("/translate", ctx -> {
            String targetLanguage = ctx.queryParam("targetLanguage");
            if (targetLanguage == null) {
                ctx.status(500).result("targetLanguage is required.");
                return;
            }

            String text = ctx.queryParam("text");
            if (targetLanguage == null) {
                ctx.status(500).result("text is required.");
                return;
            }

            HashMap<String, String> translationList = PhrasesLoader.translate(phrases, text);
            String translatedText = translationList.get(targetLanguage);
            if (translatedText == null) {
                ctx.status(500).result("Invalid targetLanguage.");
                return;
            }

            ctx.result(translatedText);
        });

        app.post("/speech-to-text",  ctx -> {
            Recognizer recognizer = new Recognizer(model, 16000);

            byte[] speechBytes = ctx.bodyAsBytes();
            recognizer.acceptWaveForm(speechBytes, speechBytes.length);

            String result = recognizer.getFinalResult();

            recognizer.close();

            ctx.result(result);
        });
    }
}