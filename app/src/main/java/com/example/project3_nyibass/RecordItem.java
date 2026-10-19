package com.example.project3_nyibass;

public class RecordItem {
    public enum Source { SPOTIFY ,MAL, BACKLOGGD }

    public String id;
    public String title;
    public String subtitle;
    public int score;
    public String imageUrl;
    public Source source;

    public RecordItem() { }
    public RecordItem(String id, String title, String subtitle, int score, String imageUrl, Source source) {
        this.id = id;
        this.title = title;
        this.subtitle = subtitle;
        this.score = score;
        this.imageUrl = imageUrl;
        this.source = source;
    }
}