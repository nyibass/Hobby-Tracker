package com.example.project3_nyibass;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;

import java.util.List;

public class MainAdapter extends ArrayAdapter<RecordItem> {
    private Context context;
    private List<RecordItem> items;
    public MainAdapter(Context context, List<RecordItem> items) {
        super(context, 0, items);
        this.context = context;
        this.items = items;
    }

    @NonNull
    public View getView(int position, View convertView, ViewGroup parent) {
        if(convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.list_item_main, parent, false);
        }

        RecordItem currItem = items.get(position);

        ImageView itemImage = convertView.findViewById(R.id.itemImage);
        TextView titleView = convertView.findViewById(R.id.titleItem);
        TextView subtitleView = convertView.findViewById(R.id.subtitleItem);
        TextView sourceView = convertView.findViewById(R.id.sourceItem);
        TextView scoreView = convertView.findViewById(R.id.scoreItem);

        titleView.setText(currItem.title);
        subtitleView.setText(currItem.subtitle);
        sourceView.setText(currItem.source.name());
        scoreView.setText("Score: " + String.valueOf(currItem.score));

        Glide.with(context)
                .load(currItem.imageUrl)
                .centerCrop()
                .placeholder(R.drawable.ic_launcher_background)
                .error(R.drawable.ic_launcher_background)
                .into(itemImage);

        return convertView;
    }
}
