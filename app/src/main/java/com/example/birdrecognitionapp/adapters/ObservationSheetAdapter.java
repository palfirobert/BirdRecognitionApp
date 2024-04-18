package com.example.birdrecognitionapp.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import com.example.birdrecognitionapp.R;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.birdrecognitionapp.models.ObservationSheet;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Locale;

public class ObservationSheetAdapter extends RecyclerView.Adapter<ObservationSheetAdapter.ViewHolder> {

    private Context context;
    private ArrayList<ObservationSheet> observationList;

    public ObservationSheetAdapter(Context context, ArrayList<ObservationSheet> observationList) {
        this.context = context;
        this.observationList = observationList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.observation_sheet_card_view, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ObservationSheet observation = observationList.get(position);
        holder.fileNameText.setText(observation.getSpecies());
        holder.fileTimeAdded.setText(observation.getUploadDate());
        holder.itemView.setOnClickListener(v -> showObservationDetailDialog(observation));
    }

    @Override
    public int getItemCount() {
        return observationList.size();
    }

    private void showObservationDetailDialog(ObservationSheet observation) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_observation_sheet, null);
        builder.setView(dialogView);

        EditText editObservationDate = dialogView.findViewById(R.id.editObservationDate);
        EditText editSpecies = dialogView.findViewById(R.id.editSpecies);
        EditText editNumber = dialogView.findViewById(R.id.editNumber);
        EditText editObserver = dialogView.findViewById(R.id.editObserver);
        EditText editUploadDate = dialogView.findViewById(R.id.editUploadDate);
        EditText editLocation = dialogView.findViewById(R.id.editLocation);
//        if (ObservationSheet.getCalledFromSavedRecordingAdapter()) {
//            editObservationDate.setText(DateUtils.formatDateTime(context, Long.valueOf(ObservationSheet.getObservationDate()),
//                    DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_NUMERIC_DATE | DateUtils.FORMAT_SHOW_TIME
//                            | DateUtils.FORMAT_SHOW_YEAR));
//        } else
//            editObservationDate.setText(LocalDateTime.now().getYear() + "-" + LocalDateTime.now().getMonth().getValue() + "-" + LocalDateTime.now().getDayOfMonth());
        editObservationDate.setText(DateUtils.formatDateTime(context, Long.valueOf(observation.getObservationDate()),
                DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_NUMERIC_DATE | DateUtils.FORMAT_SHOW_TIME
                        | DateUtils.FORMAT_SHOW_YEAR));
        editSpecies.setText(observation.getSpecies());
        editNumber.setText(String.valueOf(observation.getNumber()));
        editObserver.setText(observation.getObserver());
        editUploadDate.setText(observation.getUploadDate());
        editLocation.setText(observation.getLocation());

        // Display the dialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView fileNameText;
        TextView audioFileName;
        TextView fileTimeAdded;

        public ViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageView);
            fileNameText = itemView.findViewById(R.id.file_name_text);
            fileTimeAdded=itemView.findViewById(R.id.file_time_added);
        }
    }
}
