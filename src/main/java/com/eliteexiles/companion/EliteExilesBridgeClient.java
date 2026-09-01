package com.eliteexiles.companion;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.net.URI;
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

    private final OkHttpClient http;
    private final Gson gson;
    private final ConfigManager configManager;
    private final EliteExilesCompanionConfig config;

    @Inject
    public EliteExilesBridgeClient(OkHttpClient http, Gson gson, ConfigManager configManager, EliteExilesCompanionConfig config)
    {
        this.http = http;
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
        if (token == null || token.isBlank())
        {
            configManager.unsetConfiguration(EliteExilesCompanionConfig.GROUP, TOKEN_KEY);
        }
        else
        {
            configManager.setConfiguration(EliteExilesCompanionConfig.GROUP, TOKEN_KEY, token);
        }
    }

    public void clearToken()
    {
        saveToken(null);
    }

    private String baseUrl()
    {
        String value = config.bridgeUrl() == null ? "" : config.bridgeUrl().trim();
        if (value.isEmpty())
        {
            value = "http://127.0.0.1:47621";
        }
        while (value.endsWith("/"))
        {
            value = value.substring(0, value.length() - 1);
        }

        URI uri = URI.create(value);
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase();
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase();
        boolean loopback = "127.0.0.1".equals(host) || "localhost".equals(host) || "::1".equals(host);
        if (!loopback && !"https".equals(scheme))
        {
            throw new IllegalArgumentException("Remote Elite Exiles coach bridges must use HTTPS. HTTP is allowed only for localhost testing.");
        }
        if (!"http".equals(scheme) && !"https".equals(scheme))
        {
            throw new IllegalArgumentException("Coach bridge URL must start with http:// (localhost only) or https://.");
        }
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

    public void link(String code, String rsn, Consumer<JsonObject> success, Consumer<String> failure)
    {
        JsonObject body = new JsonObject();
        body.addProperty("code", normalizeLinkCode(code));
        body.addProperty("rsn", rsn == null ? "" : rsn);
        try
        {
            Request request = jsonRequest("POST", "/api/v1/link", body, false);
            execute(request, result ->
            {
                if (result.has("token"))
                {
                    saveToken(result.get("token").getAsString());
                }
                success.accept(result);
            }, failure);
        }
        catch (Exception e)
        {
            failure.accept(e.getMessage());
        }
    }

    public void getDashboard(Consumer<JsonObject> success, Consumer<String> failure)
    {
        guardedExecute("GET", "/api/v1/dashboard", null, true, success, failure);
    }

    public void refreshCoach(Consumer<JsonObject> success, Consumer<String> failure)
    {
        guardedExecute("POST", "/api/v1/refresh-coach", new JsonObject(), true, success, failure);
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
            execute(request, result ->
            {
                clearToken();
                success.accept(result);
            }, failure);
        }
        catch (Exception e)
        {
            failure.accept(e.getMessage());
        }
    }

    private void guardedExecute(String method, String path, JsonObject body, boolean authenticate, Consumer<JsonObject> success, Consumer<String> failure)
    {
        try
        {
            execute(jsonRequest(method, path, body, authenticate), success, failure);
        }
        catch (Exception e)
        {
            failure.accept(e.getMessage());
        }
    }

    private Request jsonRequest(String method, String path, JsonObject body, boolean authenticate)
    {
        if (!config.coachIntegration())
        {
            throw new IllegalStateException("Coach Integration is disabled. Enable it in the Elite Exiles Companion settings first.");
        }

        Request.Builder builder = new Request.Builder()
            .url(baseUrl() + path)
            .header("Accept", "application/json")
            .header("User-Agent", "Elite-Exiles-RuneLite-Companion/1.8");

        if (authenticate)
        {
            String token = token();
            if (token != null && !token.isBlank())
            {
                builder.header("Authorization", "Bearer " + token);
            }
        }

        if ("GET".equals(method))
        {
            builder.get();
        }
        else if ("DELETE".equals(method))
        {
            builder.delete();
        }
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
                failure.accept("Coach bridge unreachable: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response)
            {
                try (Response r = response)
                {
                    String text = r.body() == null ? "{}" : r.body().string();
                    JsonObject obj;
                    try
                    {
                        obj = gson.fromJson(text, JsonObject.class);
                    }
                    catch (Exception ex)
                    {
                        failure.accept("Coach bridge returned an unreadable response (HTTP " + r.code() + ").");
                        return;
                    }

                    if (!r.isSuccessful() || obj == null || (obj.has("ok") && !obj.get("ok").getAsBoolean()))
                    {
                        String message = obj != null && obj.has("error") ? obj.get("error").getAsString() : "HTTP " + r.code();
                        if (r.code() == 401)
                        {
                            clearToken();
                        }
                        failure.accept(message);
                        return;
                    }
                    success.accept(obj == null ? new JsonObject() : obj);
                }
                catch (IOException e)
                {
                    failure.accept("Could not read coach bridge response: " + e.getMessage());
                }
            }
        });
    }
}
