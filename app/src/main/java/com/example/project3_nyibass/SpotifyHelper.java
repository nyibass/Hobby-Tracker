package com.example.project3_nyibass;

import android.util.Base64;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import okhttp3.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class SpotifyHelper {
    private static final String TOKEN_URL = "https://accounts.spotify.com/api/token";
    private static final String API_BASE = "https://api.spotify.com/v1";
    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson();
    private final String clientId;
    private final String clientSecret;
    private String accessToken;
    private long expiresAt = 0L;

    public SpotifyHelper(String clientId, String clientSecret) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public interface SpotifyCallback {
        void onSuccess(List<RecordItem> items);
        void onError(Exception e);
    }

    private synchronized void ensureToken(Runnable onTokenReady, SpotifyCallback cb) {
        if (accessToken != null && System.currentTimeMillis() < expiresAt - 5000) {
            onTokenReady.run();
            return;
        }

        String credentials = clientId + ":" + clientSecret;
        String basic = Base64.encodeToString(credentials.getBytes(StandardCharsets.UTF_8), android.util.Base64.NO_WRAP);

        RequestBody body = new FormBody.Builder().add("grant_type", "client_credentials").build();
        Request request = new Request.Builder()
                .url(TOKEN_URL)
                .header("Authorization", "Basic " + basic)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(@NonNull Call call, @NonNull IOException e) {
                cb.onError(e);
            }

            @Override public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    cb.onError(new IOException("Token fetch failed: " + response.code()));
                    return;
                }
                JsonObject job = gson.fromJson(response.body().charStream(), JsonObject.class);
                accessToken = job.has("access_token") ? job.get("access_token").getAsString() : null;
                int expires = job.has("expires_in") ? job.get("expires_in").getAsInt() : 3600;
                expiresAt = System.currentTimeMillis() + expires * 1000L;
                onTokenReady.run();
            }
        });
    }

    public void searchTracks(String query, SpotifyCallback callback) {
        Runnable doSearch = () -> {
            HttpUrl url = HttpUrl.parse(API_BASE + "/search").newBuilder()
                    .addQueryParameter("q", query)
                    .addQueryParameter("type", "track")
                    .addQueryParameter("limit", "12")
                    .build();

            Request request = new Request.Builder()
                    .url(url)
                    .header("Authorization", "Bearer " + accessToken)
                    .get()
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    callback.onError(e);
                }

                @Override public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        callback.onError(new IOException("Search failed: " + response.code()));
                        return;
                    }

                    JsonObject root = gson.fromJson(response.body().charStream(), JsonObject.class);
                    JsonObject tracks = root.has("tracks") ? root.getAsJsonObject("tracks") : null;
                    JsonArray items = (tracks != null && tracks.has("items")) ? tracks.getAsJsonArray("items") : new JsonArray();
                    List<RecordItem> output = new ArrayList<>();

                    for (int i = 0; i < items.size(); i++) {
                        JsonObject item = items.get(i).getAsJsonObject();
                        String id = item.has("id") ? item.get("id").getAsString() : "";
                        String name = item.has("name") ? item.get("name").getAsString() : "";

                        String artist = "";
                        if (item.has("artists")) {
                            JsonArray artists = item.getAsJsonArray("artists");
                            if (artists.size() > 0) {
                                JsonObject artistObj = artists.get(0).getAsJsonObject();
                                if (artistObj.has("name")) {
                                    artist = artistObj.get("name").getAsString();
                                }
                            }
                        }

                        String img = null;
                        if (item.has("album")) {
                            JsonObject album = item.getAsJsonObject("album");
                            if (album != null && album.has("images")) {
                                JsonArray images = album.getAsJsonArray("images");
                                if (images.size() > 0) {
                                    JsonObject imageObj = images.get(0).getAsJsonObject();
                                    if (imageObj.has("url")) {
                                        img = imageObj.get("url").getAsString();
                                    }
                                }
                            }
                        }

                        output.add(new RecordItem(id, name, artist, 0, img, RecordItem.Source.SPOTIFY));
                    }

                    callback.onSuccess(output);
                }
            });
        };

        ensureToken(doSearch, callback);
    }
}