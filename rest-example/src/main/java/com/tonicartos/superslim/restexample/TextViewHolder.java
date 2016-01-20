package com.tonicartos.superslim.restexample;

import android.support.v7.widget.RecyclerView;
import android.widget.TextView;
import android.view.View;

public class TextViewHolder extends RecyclerView.ViewHolder {
    final TextView textView;

    public TextViewHolder(View view) {
        super(view);
        textView = (TextView) view.findViewById(R.id.text);
    }

    public void bindText(String text) {
        textView.setText(text);
    }
}
