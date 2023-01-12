package eu.cybershu.pocketstats.pocket.api

import org.json.JSONObject
import spock.lang.Specification

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class BaseTest extends Specification {
    String loadFile(String classPath) throws IOException {
        Path resourceDirectory = Paths.get("src", "test", "resources");
        String absolutePath = resourceDirectory.toFile().getAbsolutePath()

        Files.readString(Paths.get(absolutePath, classPath), StandardCharsets.UTF_8)
    }

    String loadJsonFile(String classPath) {
        parseJSONFile(classPath).toString()
    }

    JSONObject parseJSONFile(String classPath) throws IOException {
        parseJsonString(loadFile(classPath))
    }

    JSONObject parseJsonString(String string) {
        new JSONObject(string)
    }
}
