package Util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.json.JSONObject;

public class saveJson {
    public static JSONObject saveJsonObject(JSONObject json) {
        File file = new File("json_save");
        if (!file.exists()) {
            file.mkdir();
        }
        String filename = "json_save/log.json";
        try (FileWriter writer = new FileWriter(filename)) {
            writer.write(json.toString(4));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return json;
    }

    public static JSONObject retrieveJsonObject() {
        try {
            String content = new String(Files.readAllBytes(Paths.get("json_save/log.json")));
            return new JSONObject(content);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
