package com.example.project3_nyibass;

import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.JsonObject;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MyMangaListHelper {
    private static final String TAG = "MALHalper";
    private static final String API_BASE = "https://api.myanimelist.net/v2";
    private final OkHttpClient client = new OkHttpClient();
    private final String clientId;

    public MyMangaListHelper(String clientId) {
        this.clientId = clientId;
    }
    public interface MALCallback {
        void onSuccess(List<RecordItem> items);
        void onError(Exception e);
    }

    public void searchManga(String query, MALCallback callback) {
        try {
            String encoded = Uri.encode(query);
            int limit = 10;
            String fields = "id,title,main_picture,start_date,media_type,mean";
            String url = API_BASE + "/manga?q=" + encoded + "&limit=" + limit + "&fields=" + Uri.encode(fields);

            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .addHeader("X-MAL-CLIENT-ID", clientId)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    callback.onError(e);
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if(!response.isSuccessful()) {
                        callback.onError(new IOException("Search failed: " + response.code() + " " + response.message()));
                    }

                    String json = response.body().string();
                    List<RecordItem> output = new ArrayList<>();

                    try {
                        JSONObject root = new JSONObject(json);
                        JSONArray data = root.optJSONArray("data");

                        if(data == null) { callback.onSuccess(output); }

                        for(int i = 0; i < data.length(); i++) {
                            JSONObject entry = data.getJSONObject(i);
                            JSONObject node = entry.optJSONObject("node");

                            int id = node.optInt("id", -1);
                            String link = id != -1 ? "https://myanimelist.net/manga/" + id : "";

                            String title = node.optString("title", "");

                            String subtitle = "";
                            String mediaType = node.optString("media_type", "");
                            String startDate = node.optString("start_date", "");

                            if (!startDate.isEmpty()) {
                                String year = startDate.length() >= 4 ? startDate.substring(0, 4) : startDate;
                                if(!mediaType.isEmpty()) { subtitle = mediaType + " " + year; }
                                else { subtitle = year; }
                            }
                            else if(!mediaType.isEmpty()) {
                                subtitle = mediaType;
                            }

                            String imageUrl = null;
                            JSONObject mainPicture = node.optJSONObject("main_picture");
                            if(mainPicture != null) {
                                imageUrl = mainPicture.optString("large", null);
                                if (imageUrl == null) { imageUrl = mainPicture.optString("medium", null); }
                            }

                            Log.d(TAG, "Parsed -> id:" + id + " title:[" + title + "] subtitle:[" + subtitle + "] img:[" + imageUrl + "]");


                            output.add(new RecordItem(String.valueOf(id), title, subtitle, 0, imageUrl, RecordItem.Source.MAL));
                        }
                        callback.onSuccess(output);
                    } catch(Exception e) {
                        callback.onError(e);
                    }
                }
            });
        } catch(Exception e) {
            callback.onError(e);
        }
    }
}