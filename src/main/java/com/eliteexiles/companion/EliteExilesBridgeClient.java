package com.eliteexiles.companion;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import javax.inject.Inject;
import net.runelite.client.config.ConfigManager;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class EliteExilesBridgeClient
{
    private static final String TOKEN_KEY = "bridgeToken";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final int MAX_RESPONSE_BYTES = 256 * 1024;
    private static final int CONNECT_TIMEOUT_SECONDS = 5;
    private static final int READ_TIMEOUT_SECONDS = 20;
    private static final int WRITE_TIMEOUT_SECONDS = 10;
    private static final int CALL_TIMEOUT_SECONDS = 25;

    private final OkHttpClient http;
    private final Gson gson;
    private final ConfigManager configManager;
    private final EliteExilesCompanionConfig config;

    @Inject
    public EliteExilesBridgeClient(OkHttpClient http, Gson gson, ConfigManager configManager, EliteExilesCompanionConfig config)
    {
        this.http = http.newBuilder()
            .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .callTimeout(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build();
        this.gson = gson;
        this.configManager = configManager;
        this.config = config;
    }

    public boolean isLinked()
    {
        String token = token();
        return token != null && !token.isBlank();
    }

    public String token()
    {
        return configManager.getConfiguration(EliteExilesCompanionConfig.GROUP, TOKEN_KEY);
    }

    private void saveToken(String token)
    {
        if (token == null || token.isBlank()) configManager.unsetConfiguration(EliteExilesCompanionConfig.GROUP, TOKEN_KEY);
        else configManager.setConfiguration(EliteExilesCompanionConfig.GROUP, TOKEN_KEY, token);
    }

    public void clearToken() { saveToken(null); }

    private String baseUrl()
    {
        String value = config.bridgeUrl() == null ? "" : config.bridgeUrl().trim();
        if (value.isEmpty()) value = EliteExilesCompanionConfig.PRODUCTION_BRIDGE_URL;
        while (value.endsWith("/")) value = value.substring(0, value.length() - 1);

        URI uri = URI.create(value);
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
        if (host.isBlank()) throw new IllegalArgumentException("Elite Exiles bridge URL must contain a valid hostname.");
        if (uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null)
            throw new IllegalArgumentException("Elite Exiles bridge URL cannot contain credentials, a query string, or a fragment.");
        String path = uri.getPath();
        if (path != null && !path.isBlank() && !"/".equals(path))
            throw new IllegalArgumentException("Elite Exiles bridge URL must be the server origin only, without an extra path.");
        if (!"https".equals(scheme)) throw new IllegalArgumentException("Elite Exiles bridge connections must use HTTPS.");
        if (uri.getPort() != -1 && uri.getPort() != 443)
            throw new IllegalArgumentException("Elite Exiles bridge URLs must use the standard HTTPS port 443.");
        return value;
    }

    private String normalizeLinkCode(String code)
    {
        return code == null ? "" : code.toUpperCase().replaceAll("[^A-HJ-NP-Z2-9]", "");
    }

    public void health(Consumer<JsonObject> success, Consumer<String> failure)
    {
        guardedExecute("GET", "/api/v1/health", null, false, success, failure);
    }

    public void diagnostics(Consumer<JsonObject> success, Consumer<String> failure)
    {
        guardedExecute("GET", "/api/v1/diagnostics", null, true, success, failure);
    }

    public void diagnosticsEcho(String nonce, Consumer<JsonObject> success, Consumer<String> failure)
    {
        JsonObject body = new JsonObject();
        String cleanNonce = nonce == null ? "" : nonce.replaceAll("[^A-Za-z0-9._-]", "");
        if (cleanNonce.length() > 64) cleanNonce = cleanNonce.substring(0, 64);
        body.addProperty("nonce", cleanNonce);
        guardedExecute("POST", "/api/v1/diagnostics/echo", body, true, success, failure);
    }

    public void link(String code, String rsn, Consumer<JsonObject> success, Consumer<String> failure)
    {
        JsonObject body = new JsonObject();
        body.addProperty("code", normalizeLinkCode(code));
        body.addProperty("rsn", rsn == null ? "" : rsn);
        try
        {
            Request request = jsonRequest("POST", "/api/v1/link", body, false);
            execute(request, result -> {
                if (result.has("token")) saveToken(result.get("token").getAsString());
                success.accept(result);
            }, failure);
        }
        catch (Exception e) { failure.accept(e.getMessage()); }
    }

    public void getDashboard(Consumer<JsonObject> success, Consumer<String> failure)
    {
        guardedExecute("GET", "/api/v1/dashboard", null, true, success, failure);
    }

    public void refreshProgression(Consumer<JsonObject> success, Consumer<String> failure)
    {
        guardedExecute("POST", "/api/v1/progression/refresh", new JsonObject(), true, success, failure);
    }

    public void hostGroup(String activity, int world, Consumer<JsonObject> success, Consumer<String> failure)
    {
        JsonObject body = new JsonObject();
        body.addProperty("activity", activity == null ? "" : activity.trim());
        if (world >= 301 && world <= 599) body.addProperty("world", world);
        guardedExecute("POST", "/api/v1/group/host", body, true, success, failure);
    }

    public void joinGroup(String groupId, Consumer<JsonObject> success, Consumer<String> failure)
    {
        JsonObject body = new JsonObject();
        body.addProperty("groupId", groupId == null ? "" : groupId.trim());
        guardedExecute("POST", "/api/v1/group/join", body, true, success, failure);
    }

    public void startGroup(String groupId, Consumer<JsonObject> success, Consumer<String> failure)
    {
        JsonObject body = new JsonObject();
        body.addProperty("groupId", groupId == null ? "" : groupId.trim());
        guardedExecute("POST", "/api/v1/group/start", body, true, success, failure);
    }

    public void leaveGroup(String groupId, Consumer<JsonObject> success, Consumer<String> failure)
    {
        JsonObject body = new JsonObject();
        body.addProperty("groupId", groupId == null ? "" : groupId.trim());
        guardedExecute("POST", "/api/v1/group/leave", body, true, success, failure);
    }

    public void completeGroup(String groupId, Consumer<JsonObject> success, Consumer<String> failure)
    {
        JsonObject body = new JsonObject();
        body.addProperty("groupId", groupId == null ? "" : groupId.trim());
        guardedExecute("POST", "/api/v1/group/complete", body, true, success, failure);
    }

    public void sendClanBroadcast(String message, Consumer<JsonObject> success, Consumer<String> failure)
    {
        JsonObject body = new JsonObject();
        String clean = message == null ? "" : message.replaceAll("[\\p{Cntrl}]", " ").replaceAll("\\s+", " ").trim();
        if (clean.length() > 280) clean = clean.substring(0, 280);
        body.addProperty("message", clean);
        guardedExecute("POST", "/api/v1/activity/broadcast", body, true, success, failure);
    }


    private static String cleanClanName(String value)
    {
        String clean = value == null ? "" : value.replaceAll("[\\p{Cntrl}]", " ").replaceAll("\\s+", " ").trim();
        return clean.length() > 40 ? clean.substring(0, 40) : clean;
    }

    public void checkIn(Consumer<JsonObject> success, Consumer<String> failure)
    {
        guardedExecute("POST", "/api/v1/checkin", new JsonObject(), true, success, failure);
    }

    public void sendLive(JsonObject live, Consumer<JsonObject> success, Consumer<String> failure)
    {
        guardedExecute("POST", "/api/v1/live", live, true, success, failure);
    }

    public void unlink(Consumer<JsonObject> success, Consumer<String> failure)
    {
        try
        {
            Request request = jsonRequest("DELETE", "/api/v1/link", null, true);
            execute(request, result -> { clearToken(); success.accept(result); }, failure);
        }
        catch (Exception e) { failure.accept(e.getMessage()); }
    }

    private void guardedExecute(String method, String path, JsonObject body, boolean authenticate, Consumer<JsonObject> success, Consumer<String> failure)
    {
        try { execute(jsonRequest(method, path, body, authenticate), success, failure); }
        catch (Exception e) { failure.accept(e.getMessage()); }
    }

    private Request jsonRequest(String method, String path, JsonObject body, boolean authenticate)
    {
        if (!config.coachIntegration())
            throw new IllegalStateException("Elite Exiles Sync is disabled. Enable it in the Elite Exiles Companion settings first.");

        Request.Builder builder = new Request.Builder()
            .url(baseUrl() + path)
            .header("Accept", "application/json")
            .header("User-Agent", "Elite-Exiles-RuneLite-Companion/2.1.0");

        if (authenticate)
        {
            String token = token();
            if (token != null && !token.isBlank()) builder.header("Authorization", "Bearer " + token);
        }

        if ("GET".equals(method)) builder.get();
        else if ("DELETE".equals(method)) builder.delete();
        else
        {
            String json = body == null ? "{}" : gson.toJson(body);
            builder.method(method, RequestBody.create(JSON, json));
        }
        return builder.build();
    }

    private void execute(Request request, Consumer<JsonObject> success, Consumer<String> failure)
    {
        http.newCall(request).enqueue(new Callback()
        {
            @Override
            public void onFailure(Call call, IOException e)
            {
                failure.accept("Elite Exiles bridge unreachable: " + safeMessage(e));
            }

            @Override
            public void onResponse(Call call, Response response)
            {
                try (Response r = response)
                {
                    String text = readBodyLimited(r);
                    JsonObject obj;
                    try { obj = gson.fromJson(text, JsonObject.class); }
                    catch (Exception ex)
                    {
                        failure.accept("Elite Exiles bridge returned an unreadable response (HTTP " + r.code() + ").");
                        return;
                    }
                    if (!r.isSuccessful() || obj == null || (obj.has("ok") && !booleanValue(obj, "ok", false)))
                    {
                        String message = safeError(obj, r.code());
                        if (r.code() == 401) clearToken();
                        failure.accept(message);
                        return;
                    }
                    success.accept(obj == null ? new JsonObject() : obj);
                }
                catch (Exception e)
                {
                    failure.accept("Could not process Elite Exiles bridge response: " + safeMessage(e));
                }
            }
        });
    }

    private static boolean booleanValue(JsonObject object, String key, boolean fallback)
    {
        try
        {
            return object != null && object.has(key) && !object.get(key).isJsonNull()
                ? object.get(key).getAsBoolean() : fallback;
        }
        catch (Exception ignored) { return fallback; }
    }

    private static String safeError(JsonObject object, int statusCode)
    {
        try
        {
            if (object != null && object.has("error") && !object.get("error").isJsonNull())
            {
                String error = object.get("error").getAsString();
                if (error != null && !error.isBlank()) return error.length() <= 240 ? error : error.substring(0, 240) + "…";
            }
        }
        catch (Exception ignored) { }
        return "HTTP " + statusCode;
    }

    private static String safeMessage(Exception exception)
    {
        String message = exception == null ? null : exception.getMessage();
        if (message == null || message.isBlank()) return "unexpected response";
        return message.length() <= 180 ? message : message.substring(0, 180) + "…";
    }

    private String readBodyLimited(Response response) throws IOException
    {
        if (response.body() == null) return "{}";
        long declared = response.body().contentLength();
        if (declared > MAX_RESPONSE_BYTES) throw new IOException("response exceeded the 256 KiB safety limit");
        try (InputStream in = response.body().byteStream(); ByteArrayOutputStream out = new ByteArrayOutputStream())
        {
            byte[] buffer = new byte[8192];
            int total = 0;
            int read;
            while ((read = in.read(buffer)) != -1)
            {
                total += read;
                if (total > MAX_RESPONSE_BYTES) throw new IOException("response exceeded the 256 KiB safety limit");
                out.write(buffer, 0, read);
            }
            return out.toString(StandardCharsets.UTF_8.name());
        }
    }
}
