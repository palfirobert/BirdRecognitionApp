package com.example.birdrecognitionapp.adapters;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.birdrecognitionapp.R;
import com.example.birdrecognitionapp.models.RecordingItem;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;

public class SavedRecordingsAdapter extends RecyclerView.Adapter<SavedRecordingsAdapter.FileViewerViewHolder> {

    Context context;
    ArrayList<RecordingItem>list;
    LinearLayoutManager linearLayoutManager;
    public SavedRecordingsAdapter(Context context, ArrayList<RecordingItem> list, LinearLayoutManager linearLayoutManager)
    {
        this.context=context;
        this.list=list;
        this.linearLayoutManager=linearLayoutManager;
    }
    @NonNull
    @Override
    public FileViewerViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        View itemView= LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_card_view,viewGroup,false);
        return new FileViewerViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull FileViewerViewHolder holder, int position) {
        RecordingItem recordingItem=list.get(position);
        long minutes= TimeUnit.MILLISECONDS.toMinutes(recordingItem.getLength());
        long seconds=TimeUnit.MILLISECONDS.toSeconds(recordingItem.getLength())-TimeUnit.MINUTES.toSeconds(minutes);

        holder.name.setText(recordingItem.getName());
        holder.length.setText(String.format("%02d:%02d",minutes,seconds));
        holder.timeAdded.setText(DateUtils.formatDateTime(context,recordingItem.getTime_added(),
                DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_NUMERIC_DATE | DateUtils.FORMAT_SHOW_TIME
                        | DateUtils.FORMAT_SHOW_YEAR));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public class FileViewerViewHolder extends RecyclerView.ViewHolder{

        @BindView(R.id.file_name_text)
        TextView name;
        @BindView(R.id.file_length_text)
        TextView length;
        @BindView(R.id.file_time_added)
        TextView timeAdded;
        @BindView(R.id.card_view)
        View cardView;
        public FileViewerViewHolder(@NonNull View itemView) {
            super(itemView);
            ButterKnife.bind(this,itemView);
        }
    }
}
