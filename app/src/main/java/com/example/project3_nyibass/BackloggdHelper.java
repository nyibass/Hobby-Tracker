package com.example.project3_nyibass;

import android.net.Uri;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class BackloggdHelper {
    private static final String TAG = "BackloggdHelper";
    private static final String TOKEN_URL = "https://id.twitch.tv/oauth2/token";
    private static final String IGDB_BASE = "https://api.igdb.com/v4";
    private static final MediaType MEDIA_TEXT = MediaType.parse("text/plain");
    private final OkHttpClient client = new OkHttpClient();
    private final String clientId;
    private final String clientSecret;
    private String accessToken;
    private long expiresAt = 0L;

    public BackloggdHelper(String clientId, String clientSecret) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public interface BackloggdCallback {
        void onSuccess(List<RecordItem> items);
        void onError(Exception e);
    }

    public void searchGames(String query, BackloggdCallback callback) {
        ensureAccessToken(new TokenCallback() {
            public void onToken(String token, Exception error) {
                if(token == null || error != null) {
                    callback.onError(error != null ? error : new IOException("failed to obtain access token"));
                }
                callIgdbSearch(query, token, callback);
            }
        });
    }

    private interface TokenCallback { void onToken(String token, Exception error); }

    private void ensureAccessToken(TokenCallback cb) {
        long now = System.currentTimeMillis();
        if(accessToken != null && now < expiresAt - 5000) {
            cb.onToken(accessToken, null);
        }

        String url = TOKEN_URL + "?client_id=" + Uri.encode(clientId)
                + "&client_secret=" + Uri.encode(clientSecret)
                + "&grant_type=client_credentials";

        Request request = new Request.Builder()
                .url(url)
                .post(new FormBody.Builder().build())
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                cb.onToken(null, e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try {
                    if(!response.isSuccessful()) {
                        cb.onToken(null, new IOException("Token request failed: " + response.code() + " " + response.message()));
                    }

                    String json = response.body() != null ? response.body().string() : "";
                    JSONObject o = new JSONObject(json);
                    accessToken = o.optString("access_token", null);
                    int expiresIn = o.optInt("expires_in", 0);

                    if(accessToken != null && expiresIn > 0) {
                        expiresAt = System.currentTimeMillis() + (expiresIn * 1000L);
                    } else {
                        expiresAt = 0L;
                    }

                    if(accessToken == null) {
                        cb.onToken(null, new IOException("No access_token in tokenr response"));
                    } else {
                        cb.onToken(accessToken, null);
                    }
                } catch (Exception e) {
                    cb.onToken(null, e);
                } finally {
                    response.close();
                }
            }
        });
    }

    @SuppressWarnings("SpellCheckingInspection")
    private void callIgdbSearch(String query, String token, BackloggdCallback callback) {
        String igdbBody = "search \"" + escapeQuotes(query) + "\"; "
                + "fields name,first_release_date,cover.url; "
                + "limit 12;";

        RequestBody body = RequestBody.create(igdbBody, MEDIA_TEXT);

        Request request = new Request.Builder()
                .url(IGDB_BASE + "/games")
                .post(body)
                .addHeader("Client-ID", clientId)
                .addHeader("Authorization", "Bearer " + token)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                callback.onError(e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try {
                    if(!response.isSuccessful()) {
                        callback.onError(new IOException("IGDB search failed: " + response.code() + " " + response.message()));
                    }

                    String json = response.body().string();
                    List<RecordItem> output = new ArrayList<>();

                    try {
                        JSONArray array = new JSONArray(json);

                        for(int i  = 0; i < array.length(); i++) {
                            JSONObject item = array.getJSONObject(i);

                            String name = item.optString("name", "");
                            int igdbId = item.optInt("id", -1);

                            String subtitle = "";
                            if(item.has("first_release_date")) {
                                long l = item.optLong("first_release_date", 0L);

                                if(l > 0) {
                                    Calendar cal = Calendar.getInstance();
                                    cal.setTimeInMillis(l * 1000L);
                                    int year = cal.get(Calendar.YEAR);
                                    subtitle = String.valueOf(year);
                                }
                            }

                            String imageUrl = null;
                            if(item.has("cover")) {
                                JSONObject cover = item.optJSONObject("cover");
                                if(cover != null) {
                                    String raw = cover.optString("url", null);
                                    if(!raw.isEmpty()) {
                                        if (raw.startsWith("//")) { imageUrl = "https:" + raw; }
                                        else if (raw.startsWith("/")) { imageUrl = "https://images.igdb.com" + raw; }
                                        else { imageUrl = raw; }
                                    }
                                }
                            }

                            output.add(new RecordItem(String.valueOf(igdbId), name, subtitle, 0, imageUrl, RecordItem.Source.BACKLOGGD));
                        }

                        callback.onSuccess(output);
                    } catch (Exception e) {
                        callback.onError(e);
                    }
                } finally {
                    response.close();
                }
            }
        });
    }

    private String escapeQuotes(String s) {
        if(s == null) { return ""; }
        return s.replace("\"", "\\\"");
    }
}