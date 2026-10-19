package com.example.project3_nyibass;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import com.bumptech.glide.Glide;
import com.google.firebase.Firebase;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.FirebaseApp;

public class MainActivity extends AppCompatActivity {

    ListView listView;
    ArrayList<RecordItem> data;
    MainAdapter adapter;
    FirebaseDatabase database;
    DatabaseReference reference;
    static final String TAG = "MainActivity";
    enum SortOrder { NONE, ASCENDING, DESCENDING }
    TextView currentSortView = null;
    SortOrder currentSortOrder = SortOrder.NONE;
    TextView nameView, scoreView, typeView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        FirebaseApp.initializeApp(this.getApplicationContext());

        listView = findViewById(R.id.listView);
        data = new ArrayList<>();

        adapter = new MainAdapter(this, data);
        listView.setAdapter(adapter);

        database = FirebaseDatabase.getInstance("https://project3-nyibass-default-rtdb.firebaseio.com/");
        reference = database.getReference("records");

        attachFirebaseListener();

        Button addButton = findViewById(R.id.mainButton);

        nameView = findViewById(R.id.nameView);
        scoreView = findViewById(R.id.scoreView);
        typeView = findViewById(R.id.typeView);

        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent newRecordIntent = new Intent(MainActivity.this, AddRecordActivity.class);
                startActivityForResult(newRecordIntent, 0);
            }
        });

        View.OnClickListener sortClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TextView clicked = (TextView) view;

                SortOrder newOrder = SortOrder.DESCENDING;
                if(clicked == currentSortView) {
                    switch(currentSortOrder) {
                        case NONE: newOrder = SortOrder.DESCENDING; break;
                        case DESCENDING: newOrder = SortOrder.ASCENDING; break;
                        case ASCENDING: newOrder = SortOrder.NONE; break;
                    }
                }

                resetSortIndicators();

                currentSortView = clicked;
                currentSortOrder = newOrder;

                if(newOrder == SortOrder.ASCENDING) clicked.setText(clicked.getText() + " ↑");
                else if(newOrder == SortOrder.DESCENDING) clicked.setText(clicked.getText() + " ↓");

                applySorting();
            }
        };

        nameView.setOnClickListener(sortClickListener);
        scoreView.setOnClickListener(sortClickListener);
        typeView.setOnClickListener(sortClickListener);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                RecordItem item = data.get(position);

                LayoutInflater inflater = LayoutInflater.from(MainActivity.this);
                View dialogView = inflater.inflate(R.layout.dialog_add_record, null);

                RatingBar ratingBar = dialogView.findViewById(R.id.ratingBar);
                TextView ratingValue = dialogView.findViewById(R.id.ratingValue);
                ImageView albumImage = dialogView.findViewById(R.id.albumImage);

                if (item.imageUrl != null) {
                    albumImage.setVisibility(View.VISIBLE);
                    Glide.with(MainActivity.this)
                            .load(item.imageUrl)
                            .into(albumImage);
                }

                ratingBar.setOnRatingBarChangeListener((rb, rating, fromUser) -> ratingValue.setText("Score: " + (int) rating));
                ratingBar.setRating((float) item.score);

                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

                builder.setTitle("Edit Rating for \"" + item.title + "\"");
                builder.setView(dialogView);
                builder.setCancelable(true);

                builder.setPositiveButton("Add", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        item.score = Math.round(ratingBar.getRating());
                        adapter.notifyDataSetChanged();

                        reference.child(item.id).setValue(item)
                                .addOnSuccessListener(aVoid -> Log.d(TAG, "Record updated: " + item.title))
                                .addOnFailureListener(e -> Log.e(TAG, "Failed to update record", e));
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
        });

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long l) {
                RecordItem item = data.get(position);

                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Delete Record")
                        .setMessage("Are you sure you want to delete \"" + item.title + "\"?")
                        .setPositiveButton("Delete", (dialogInterface, i) -> {
                            data.remove(position);
                            adapter.notifyDataSetChanged();

                            reference.child(item.id).removeValue()
                                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Record deleted: " + item.title))
                                    .addOnFailureListener(e -> Log.e(TAG, "Failed to delete record", e));
                        })
                        .setNegativeButton("Cancel", null)
                        .show();

                return true;
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent dataIntent) {
        super.onActivityResult(requestCode, resultCode, dataIntent);
        if(requestCode == 0 && resultCode == RESULT_OK && dataIntent != null) {
            String id = dataIntent.getStringExtra("id");
            String title = dataIntent.getStringExtra("title");
            String subtitle = dataIntent.getStringExtra("subtitle");
            int score = dataIntent.getIntExtra("score", 0);
            String imageUrl = dataIntent.getStringExtra("image_url");
            String sourceName = dataIntent.getStringExtra("source");

            RecordItem.Source source = RecordItem.Source.valueOf(sourceName);

            RecordItem item = new RecordItem(id, title, subtitle, score, imageUrl, source);
            data.add(item);
            adapter.notifyDataSetChanged();

            saveToFirebase(item);
        }
    }

    private void saveToFirebase(RecordItem item) {
        if(item == null || item.id == null || item.id.isEmpty()) {
            String key = reference.push().getKey();
            if(key != null) { item.id = key; }
            else { Log.w(TAG, "Unable to generate key for item"); }
        }
        reference.child(item.id).setValue(item)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Record saved: " + item.title))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to save record", e));
    }

    private void attachFirebaseListener() {
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                data.clear();
                for(DataSnapshot child : dataSnapshot.getChildren()) {
                    RecordItem item = child.getValue(RecordItem.class);
                    if(item != null) {
                        if(item.id == null || item.id.isEmpty()) {
                            item.id = child.getKey();
                        }
                        data.add(item);
                    }
                }
                adapter.notifyDataSetChanged();
                Log.d(TAG, "Loaded " + data.size() + " records from Firebase");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Firebase read cancelled: " + databaseError.getMessage());
            }
        });
    }

    private void resetSortIndicators() {
        nameView.setText("Name");
        scoreView.setText("Score");
        typeView.setText("Type");
    }

    private void applySorting() {
        if(currentSortView == null || currentSortOrder == SortOrder.NONE) { return; }

        Comparator<RecordItem> comparator = null;

        if(currentSortView == nameView) {
            comparator = Comparator.comparing(r -> r.title.toLowerCase());
        } else if(currentSortView == scoreView) {
            comparator = Comparator.comparingInt(r -> r.score);
        } else if(currentSortView == typeView) {
            comparator = Comparator.comparingInt(r -> getSourceSortValue(r.source));
        }

        if(comparator != null) {
            if(currentSortOrder == SortOrder.ASCENDING && currentSortView != scoreView) {
                comparator = comparator.reversed();
            } else if (currentSortOrder == SortOrder.DESCENDING && currentSortView == scoreView) {
                comparator = comparator.reversed();
            }
            data.sort(comparator);
            adapter.notifyDataSetChanged();
        }
    }

    private int getSourceSortValue(RecordItem.Source source) {
        switch (source) {
            case BACKLOGGD: return 0;
            case MAL:       return 1;
            case SPOTIFY:   return 2;
            default:        return 99;
        }
    }
}