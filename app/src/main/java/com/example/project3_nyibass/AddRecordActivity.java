package com.example.project3_nyibass;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RatingBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AddRecordActivity extends AppCompatActivity {

    Spinner spinner;
    EditText editText;
    Button button;
    ListView resultsList;
    ArrayList<RecordItem> searchResults = new ArrayList<RecordItem>();
    RecordAdapter resultsAdapter;
    SpotifyHelper spotifyHelper;
    MyAnimeListHelper myAnimeListHelper;
    MyMangaListHelper myMangaListHelper;
    BackloggdHelper backloggdHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_record);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        spinner = findViewById(R.id.addRecordSpinner);
        editText = findViewById(R.id.addRecordText);
        button = findViewById(R.id.addRecordButton);
        resultsList = findViewById(R.id.addRecordList);

        ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(this,
                R.array.sources_array, android.R.layout.simple_spinner_item);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(spinnerAdapter);

        resultsAdapter = new RecordAdapter(this, searchResults);
        resultsList.setAdapter(resultsAdapter);

        String spotifyClientId = getString(R.string.spotify_client_id);
        String spotifyClientSecret = getString(R.string.spotify_client_secret);
        spotifyHelper = new SpotifyHelper(spotifyClientId, spotifyClientSecret);

        String animeClientId = getString(R.string.myanimelist_client_id);
        myAnimeListHelper = new MyAnimeListHelper(animeClientId);
        myMangaListHelper = new MyMangaListHelper(animeClientId);

        String backloggdClientId = getString(R.string.twitch_client_id);
        String backloggdClientSecret = getString(R.string.twitch_client_secret);
        backloggdHelper = new BackloggdHelper(backloggdClientId, backloggdClientSecret);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String query = editText.getText().toString().trim();
                if(query.isEmpty()) {
                    Toast.makeText(getApplicationContext(), "Please enter a search query", Toast.LENGTH_SHORT).show();
                    return;
                }
                int spinnerPosition = spinner.getSelectedItemPosition();
                if(spinnerPosition == 0) {
                    spotifySearch(query);
                }
                else if(spinnerPosition == 1) {
                    animeSearch(query);
                }
                else if(spinnerPosition == 2) {
                    mangaSearch(query);
                }
                else if(spinnerPosition == 3) {
                    backloggdSearch(query);
                }
            }
        });

        resultsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                RecordItem picked = searchResults.get(position);
                showScoreDialog(picked);
            }
        });
    }

    private void spotifySearch(String query) {
        button.setEnabled(false);
        resultsAdapter.clear();
        searchResults.clear();

        spotifyHelper.searchTracks(query, new SpotifyHelper.SpotifyCallback() {
            @Override
            public void onSuccess(List<RecordItem> items) {
                runOnUiThread(() -> {
                    button.setEnabled(true);
                    searchResults.addAll(items);
                    /*ArrayList<String> display = new ArrayList<>();
                    for (RecordItem r : items) {
                        display.add(r.title + " - " + r.subtitle);
                    }*/
                    resultsAdapter.notifyDataSetChanged();
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> {
                    button.setEnabled(true);
                    Toast.makeText(AddRecordActivity.this, "Search failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    public void animeSearch(String query) {
        button.setEnabled(false);
        resultsAdapter.clear();
        searchResults.clear();

        myAnimeListHelper.searchAnime(query, new MyAnimeListHelper.MALCallback() {
            @Override
            public void onSuccess(List<RecordItem> items) {
                runOnUiThread(() -> {
                    button.setEnabled(true);
                    searchResults.addAll(items);
                    resultsAdapter.notifyDataSetChanged();
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> {
                    button.setEnabled(true);
                    Toast.makeText(AddRecordActivity.this, "Search failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    public void mangaSearch(String query) {
        button.setEnabled(false);
        resultsAdapter.clear();
        searchResults.clear();

        myMangaListHelper.searchManga(query, new MyMangaListHelper.MALCallback() {
            @Override
            public void onSuccess(List<RecordItem> items) {
                runOnUiThread(() -> {
                    button.setEnabled(true);
                    searchResults.addAll(items);
                    resultsAdapter.notifyDataSetChanged();
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> {
                    button.setEnabled(true);
                    Toast.makeText(AddRecordActivity.this, "Search failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    public void backloggdSearch(String query) {
        button.setEnabled(false);
        resultsAdapter.clear();
        searchResults.clear();

        backloggdHelper.searchGames(query, new BackloggdHelper.BackloggdCallback() {
            @Override
            public void onSuccess(List<RecordItem> items) {
                runOnUiThread(() -> {
                    button.setEnabled(true);
                    searchResults.addAll(items);
                    resultsAdapter.notifyDataSetChanged();
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> {
                    button.setEnabled(true);
                    Toast.makeText(AddRecordActivity.this, "Search failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void showScoreDialog(final RecordItem picked) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_add_record, null);

        RatingBar ratingBar = dialogView.findViewById(R.id.ratingBar);
        TextView ratingValue = dialogView.findViewById(R.id.ratingValue);
        ImageView albumImage = dialogView.findViewById(R.id.albumImage);

        if (picked.imageUrl != null) {
            albumImage.setVisibility(View.VISIBLE);
            Glide.with(this)
                    .load(picked.imageUrl)
                    .into(albumImage);
        }

        ratingBar.setOnRatingBarChangeListener((rb, rating, fromUser) -> ratingValue.setText("Score: " + (int) rating));

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle("Add \"" + picked.title + "\"");
        builder.setView(dialogView);
        builder.setCancelable(true);

        builder.setPositiveButton("Add", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                int score = Math.round(ratingBar.getRating());
                Intent finishIntent = new Intent();
                finishIntent.putExtra("id", picked.id);
                finishIntent.putExtra("title", picked.title);
                finishIntent.putExtra("subtitle", picked.subtitle);
                finishIntent.putExtra("score", score);
                finishIntent.putExtra("image_url", picked.imageUrl);
                finishIntent.putExtra("source", picked.source.name());
                setResult(RESULT_OK, finishIntent);
                finish();
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
            }
        });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }
}