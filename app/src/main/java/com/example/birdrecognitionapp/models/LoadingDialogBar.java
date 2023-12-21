package com.example.birdrecognitionapp.models;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.widget.TextView;

import com.example.birdrecognitionapp.R;

public class LoadingDialogBar {
    Context context;
    Dialog dialog;
    public LoadingDialogBar(Context context)
    {
        this.context=context;
    }
    public void showDialog(String title)
    {
        this.dialog=new Dialog(context);
        this.dialog.setContentView(R.layout.dialog);
        this.dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        TextView titleTextView=dialog.findViewById(R.id.textView);
        titleTextView.setText(title);
        dialog.create();
        dialog.show();
    }
    public void hideDialog(){
        this.dialog.dismiss();
    }
}
