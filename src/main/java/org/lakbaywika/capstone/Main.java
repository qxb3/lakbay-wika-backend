package org.lakbaywika.capstone;

import io.javalin.Javalin;
import org.apache.lucene.queryparser.classic.ParseException;
import org.lakbaywika.capstone.phrases.PhrasesLoader;
import org.vosk.Model;
import org.vosk.Recognizer;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.*;

public class Main {
    static final String DB_URL = "jdbc:mysql://localhost:3306/lakbaywika";
    static final String DB_USER = "lakbay";
    static final String DB_PASS = "pass123";

    public static void main(String[] args) throws ParserConfigurationException, IOException, SAXException, ParseException, SQLException {
        Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);

        String modelPath = Main.class.getClassLoader().getResource("vosk-model-small-en-us-0.15").getPath();
        File modelFolder = new File(modelPath);
        Model model = new Model(modelFolder.getPath());
        Javalin app = Javalin.create(config -> {
            config.bundledPlugins.enableCors(cors -> {
                cors.addRule(it -> {
                    it.anyHost();
                });
            });
        }).start(4321);

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
            if (translationList == null) {
                ctx.status(500).result("Cannot translate phrase");
                return;
            }

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

        app.post("/feedback", ctx -> {
            String userId = ctx.queryParam("userid");
            String name = ctx.queryParam("name");
            String feedback = ctx.queryParam("feedback");

            if (userId == null || name == null || feedback == null) {
                ctx.status(500).result("userid, name and feedback is required");
                return;
            }

            PreparedStatement statement = connection.prepareStatement("INSERT INTO feedbacks (user_id, name, feedback) VALUES (?, ?, ?)");
            statement.setString(1, userId);
            statement.setString(2, name);
            statement.setString(3, feedback);

            int affected = statement.executeUpdate();
            if (affected > 0) {
                ctx.status(200).result("success");
            } else {
                ctx.status(500).result("Failed to send feedback, Please try again");
            }
        });

        app.get("/list-feedbacks", ctx -> {
            PreparedStatement statement = connection.prepareStatement("SELECT * FROM feedbacks");
            ResultSet result = statement.executeQuery();

            List<Map<String, Object>> jsonResult = new ArrayList<>();

            while (result.next()) {
                String userId = result.getString("user_id");
                String name = result.getString("name");
                String feedback = result.getString("feedback");

                Map<String, Object> item = new HashMap<>();
                item.put("user_id", userId);
                item.put("name", name);
                item.put("feedback", feedback);

                jsonResult.add(item);
            }

            ctx.status(200).json(jsonResult);
        });

        app.get("/request-uuid", ctx -> {
            String uuid = UUID.randomUUID().toString();

            PreparedStatement statement = connection.prepareStatement("INSERT INTO users (id) VALUES (?)");
            statement.setString(1, uuid);

            int affected = statement.executeUpdate();

            if (affected > 0) {
                ctx.status(200).result(uuid);
            } else {
                ctx.status(500).result("Failed to give uuid, Please Try again");
            }
        });

        app.post("/admin-login", ctx -> {
            String username = ctx.queryParam("username");
            String password = ctx.queryParam("password");

            if (username == null || password == null) {
                ctx.status(500).result("username & password is required");
                return;
            }

            PreparedStatement statement = connection.prepareStatement("SELECT * FROM admins WHERE username = ? AND password = ?");
            statement.setString(1, username);
            statement.setString(2, password);
            statement.executeQuery();

            ResultSet result = statement.getResultSet();
            if (result.next()) {
                ctx.status(200).result(username);
            } else {
                ctx.status(404).result("Invalid username or password");
            }
        });
    }
}