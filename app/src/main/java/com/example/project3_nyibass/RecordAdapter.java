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

public class RecordAdapter extends ArrayAdapter<RecordItem> {
    private Context context;
    private List<RecordItem> items;

    public RecordAdapter(Context context, List<RecordItem> items) {
        super(context, 0, items);
        this.context = context;
        this.items = items;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.list_item_record, parent, false);
        }

        RecordItem currItem = items.get(position);

        ImageView itemImage = convertView.findViewById(R.id.recordImage);
        TextView titleView = convertView.findViewById(R.id.recordTitle);
        TextView subtitleView = convertView.findViewById(R.id.recordSubtitle);

        titleView.setText(currItem.title);
        subtitleView.setText(currItem.subtitle);

        if (currItem.imageUrl != null && !currItem.imageUrl.isEmpty()) {
            Glide.with(context)
                    .load(currItem.imageUrl)
                    .centerCrop()
                    .placeholder(R.drawable.ic_launcher_background)
                    .error(R.drawable.ic_launcher_background)
                    .into(itemImage);
        } else {
            itemImage.setImageResource(R.drawable.ic_launcher_background);
        }

        return convertView;
    }
}