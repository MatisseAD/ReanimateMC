package fr.jachou.reanimatemc.utils.updater;


import com.google.gson.*;
import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.regex.Pattern;

public class UpdateChecker {
    private static final String BASE = "https://api.modrinth.com/v2";
    private static final String PROJECT_SLUG = "reanimatemc";
    private static final Pattern DIGITS = Pattern.compile("[^0-9.]");

    private final HttpClient http;

    public UpdateChecker(String pluginVersion) {
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.pluginVersion = pluginVersion;
    }

    private final String pluginVersion;

    public Optional<UpdateInfo> check(String bukkitGameVersion) {
        try {
            List<String> loaders = List.of("paper", "bukkit", "spigot", "purpur");
            String loadersJson = new Gson().toJson(loaders);
            String gameVersionsJson = new Gson().toJson(List.of(normalizeGameVersion(bukkitGameVersion)));

            String url = BASE + "/project/" + PROJECT_SLUG + "/version"
                    + "?loaders=" + URLEncoder.encode(loadersJson, StandardCharsets.UTF_8)
                    + "&game_versions=" + URLEncoder.encode(gameVersionsJson, StandardCharsets.UTF_8);

            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .header("User-Agent", "ReanimateMC-UpdateChecker/1.0 (+server)")
                    .timeout(Duration.ofSeconds(8))
                    .GET().build();

            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) return Optional.empty();

            JsonArray arr = JsonParser.parseString(resp.body()).getAsJsonArray();
            if (arr.size() == 0) return Optional.empty();

            JsonObject latest = arr.get(0).getAsJsonObject();
            String latestVersion = latest.get("version_number").getAsString();
            String versionId = latest.get("id").getAsString();
            String webUrl = "https://modrinth.com/plugin/" + PROJECT_SLUG + "/version/" + urlify(latestVersion);

            if (isOutdated(pluginVersion, latestVersion)) {
                return Optional.of(new UpdateInfo(latestVersion, webUrl));
            }
            return Optional.empty();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        } catch (IOException | RuntimeException ex) {
            return Optional.empty();
        }
    }

    private static String urlify(String versionNumber) {
        return versionNumber;
    }

    private static String normalizeGameVersion(String bukkitVersion) {
        int dash = bukkitVersion.indexOf('-');
        return dash == -1 ? bukkitVersion : bukkitVersion.substring(0, dash);
    }

    private static boolean isOutdated(String current, String remote) {
        String c = DIGITS.matcher(current).replaceAll("");
        String r = DIGITS.matcher(remote).replaceAll("");
        int[] cv = parse(c);
        int[] rv = parse(r);
        for (int i = 0; i < 3; i++) {
            if (cv[i] < rv[i]) return true;
            if (cv[i] > rv[i]) return false;
        }
        return false;
    }

    private static int[] parse(String v) {
        String[] p = v.split("\\.");
        int[] out = new int[] {0,0,0};
        for (int i=0; i<Math.min(3, p.length); i++) {
            try { out[i] = Integer.parseInt(p[i]); } catch (NumberFormatException ignored) {}
        }
        return out;
    }
}
