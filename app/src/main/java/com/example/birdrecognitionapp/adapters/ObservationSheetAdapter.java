package com.example.birdrecognitionapp.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.example.birdrecognitionapp.R;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.birdrecognitionapp.database.DbHelper;
import com.example.birdrecognitionapp.dto.DeleteSoundDto;
import com.example.birdrecognitionapp.dto.ObservationSheetDto;
import com.example.birdrecognitionapp.interfaces.OnDatabaseChangedListener;
import com.example.birdrecognitionapp.models.ObservationSheet;
import com.example.birdrecognitionapp.models.RecordingItem;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ObservationSheetAdapter extends RecyclerView.Adapter<ObservationSheetAdapter.ViewHolder> implements OnDatabaseChangedListener {

    private Context context;
    private ArrayList<ObservationSheetDto> observationList;
    private DbHelper dbHelper;

    public ObservationSheetAdapter(Context context, ArrayList<ObservationSheetDto> observationList) {
        this.context = context;
        this.observationList = observationList;
        dbHelper = new DbHelper(context);
        DbHelper.setOnDatabaseChangedListener(this);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.observation_sheet_card_view, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ObservationSheetDto observation = observationList.get(position);
//        System.out.println(observation.toString());
        holder.fileNameText.setText(observation.getSpecies());
        holder.fileTimeAdded.setText(observation.getUploadDate());
    }

    @Override
    public int getItemCount() {
        return observationList.size();
    }


    @Override
    public void onNewDatabaseEntryAdded(RecordingItem recordingItem) {

    }

    @Override
    public void onDatabaseEntryDeleted() {

    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView fileNameText;
        TextView fileTimeAdded;
        @BindView(R.id.card_view)
        View cardView;

        public ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            imageView = itemView.findViewById(R.id.imageView);
            fileNameText = itemView.findViewById(R.id.file_name_text);
            fileTimeAdded = itemView.findViewById(R.id.file_time_added);

            cardView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    showOptionsPopupMenu(view);
                    return true;
                }
            });
            cardView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ObservationSheetDto observation = observationList.get(getAdapterPosition());
                    showObservationDetailDialog(observation);
                }
            });
        }

        private void showOptionsPopupMenu(View view) {
            PopupMenu popup = new PopupMenu(context, view);
            // Inflate the menu from xml
            popup.inflate(R.menu.options_menu_observation_sheet);
            // Add click listener for menu items
            popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem menuItem) {
                    switch (menuItem.getItemId()) {
                        case R.id.menu_delete:
                            ObservationSheetDto itemToDelete = observationList.get(getAdapterPosition());
                            if(itemToDelete.getSoundId()!=null)
                                dbHelper.deleteObservation(itemToDelete.getSoundId());
                            else
                                dbHelper.deleteObservationByUserLocationDate(itemToDelete.getUserId(),itemToDelete.getLocation(),itemToDelete.getUploadDate());
                            dbHelper.deleteObservationSheetFromDb(itemToDelete);
                            observationList.remove(getAdapterPosition());
                            notifyItemRemoved(getAdapterPosition());
                            return true;
                        default:
                            return false;
                    }
                }
            });
            popup.show();
        }

        private void showObservationDetailDialog(ObservationSheetDto observation) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_observation_sheet, null);
            builder.setView(dialogView);
            //System.out.println(observation.toString());
            EditText editObservationDate = dialogView.findViewById(R.id.editObservationDate);
            EditText editSpecies = dialogView.findViewById(R.id.editSpecies);
            EditText editNumber = dialogView.findViewById(R.id.editNumber);
            EditText editObserver = dialogView.findViewById(R.id.editObserver);
            EditText editUploadDate = dialogView.findViewById(R.id.editUploadDate);
            EditText editLocation = dialogView.findViewById(R.id.editLocation);
//            DateTimeFormatter formatterObservationDate = DateTimeFormatter.ofPattern("yyyy-MM-dd, HH:mm a");
//            try {
//                // Convert timestamp to Instant
//                Instant instant = Instant.ofEpochMilli(Long.valueOf(ObservationSheet.getObservationDate()));
//
//                // Convert Instant to LocalDateTime using system default time zone
//                LocalDateTime dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
//
//                // Format LocalDateTime
//                String formattedDate = dateTime.format(formatterObservationDate);
//
//                editObservationDate.setText(formattedDate);
//
//
//            } catch (Exception e) {
//                editObservationDate.setText(observation.getObservationDate());
//            }
            editObservationDate.setText(observation.getObservationDate());
            editSpecies.setText(observation.getSpecies());
            editNumber.setText(String.valueOf(observation.getNumber()));
            editNumber.setEnabled(false);
            editObserver.setText(observation.getObserver());
            editUploadDate.setText(observation.getUploadDate());
            editLocation.setText(observation.getLocation());

            // Display the dialog
            AlertDialog dialog = builder.create();
            dialog.show();
        }
    }
}
