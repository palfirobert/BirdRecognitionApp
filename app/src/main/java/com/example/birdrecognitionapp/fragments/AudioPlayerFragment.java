package com.example.birdrecognitionapp.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.media.MediaPlayer;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.TextView;

import com.example.birdrecognitionapp.R;
import com.example.birdrecognitionapp.models.RecordingItem;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;


public class AudioPlayerFragment extends DialogFragment {

    private RecordingItem item;
    private Handler handler=new Handler();
    private MediaPlayer mediaPlayer;

    private boolean isPlaying=false;

    long minutes=0;
    long seconds=0;

    @BindView(R.id.file_name_text_view)
    TextView fileNameTextView;
    @BindView(R.id.file_length_text_view)
    TextView fileLengthTextView;
    @BindView(R.id.current_progress_text_view)
    TextView fileCurrentProgress;
    @BindView(R.id.seekbar)
    SeekBar seekBar;
    @BindView(R.id.fab_play)
    FloatingActionButton floatingActionButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        return inflater.inflate(R.layout.fragment_audio_player, container, false);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog=super.onCreateDialog(savedInstanceState);
        AlertDialog.Builder builder=new AlertDialog.Builder(getActivity());
        View view=getActivity().getLayoutInflater().inflate(R.layout.fragment_audio_player,null);
        ButterKnife.bind(this,view);

        setSeekBarsValues();
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    onPlay(isPlaying);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                isPlaying=!isPlaying;
            }
        });
        fileNameTextView.setText(item.getName());
        fileLengthTextView.setText(String.format("%02d:%02d",minutes,seconds));
        builder.setView(view);
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        return builder.create();

    }

    private void onPlay(boolean isPlaying) throws IOException {
        if(!isPlaying)
        {
            if(mediaPlayer==null)
            {
                startPlaying();
            }
        }
        else{
            pausePlaying();
        }
    }

    private void pausePlaying() {
        floatingActionButton.setImageResource(R.drawable.ic_media_play);
        handler.removeCallbacks(mRunnable);
        mediaPlayer.pause();
    }

    private void startPlaying() throws IOException {
        floatingActionButton.setImageResource(R.drawable.ic_media_pause);
        mediaPlayer=new MediaPlayer();
        mediaPlayer.setDataSource(item.getPath());
        mediaPlayer.prepare();
        seekBar.setMax(mediaPlayer.getDuration());
        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                mediaPlayer.start();
            }
        });
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                stopPlaying();
            }
        });
        updateSeekBar();
        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void setSeekBarsValues() {
        ColorFilter colorFilter=new LightingColorFilter(getResources().getColor(R.color.design_default_color_primary),
                getResources().getColor(R.color.design_default_color_primary));
        seekBar.getProgressDrawable().setColorFilter(colorFilter);
        seekBar.getThumb().setColorFilter(colorFilter);

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(mediaPlayer!=null && fromUser)
                {
                    mediaPlayer.seekTo(progress);
                    handler.removeCallbacks(mRunnable);
                    long minutes=TimeUnit.MILLISECONDS.toMinutes(mediaPlayer.getCurrentPosition());
                    long seconds=TimeUnit.MILLISECONDS.toSeconds(mediaPlayer.getCurrentPosition())-TimeUnit.MINUTES.toSeconds(minutes);
                    fileCurrentProgress.setText(String.format("%02d:%0d",minutes,seconds));
                    updateSeekBar();
                }
                else if(mediaPlayer==null && fromUser)
                {
                    try {
                        prepareMediaPlayerFromPoint(progress);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    updateSeekBar();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    private void prepareMediaPlayerFromPoint(int progress) throws IOException {
        mediaPlayer=new MediaPlayer();
        mediaPlayer.setDataSource(item.getPath());
        mediaPlayer.prepare();
        seekBar.setMax(mediaPlayer.getDuration());
        mediaPlayer.seekTo(progress);
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                stopPlaying();
            }
        });
    }

    private void stopPlaying() {
        floatingActionButton.setImageResource(R.drawable.ic_media_play);
        handler.removeCallbacks(mRunnable);
        mediaPlayer.stop();
        mediaPlayer.reset();
        mediaPlayer.release();
        mediaPlayer=null;

        seekBar.setProgress(seekBar.getMax());
        isPlaying=!isPlaying;

        fileCurrentProgress.setText(fileLengthTextView.getText());
        seekBar.setProgress(seekBar.getMax());

    }

    private Runnable mRunnable=new Runnable() {
        @Override
        public void run() {
            if(mediaPlayer!=null)
            {
                int currentPos=mediaPlayer.getCurrentPosition();
                seekBar.setProgress(currentPos);

                long minutes=TimeUnit.MILLISECONDS.toMinutes(currentPos);
                long seconds=TimeUnit.MILLISECONDS.toSeconds(currentPos)-TimeUnit.MINUTES.toSeconds(minutes);

                fileCurrentProgress.setText(String.format("%02d:%02d",minutes,seconds));
                updateSeekBar();
            }
        }
    };

    private void updateSeekBar() {
        handler.postDelayed(mRunnable,1000);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        item=(RecordingItem) getArguments().getSerializable("item");
        minutes= TimeUnit.MILLISECONDS.toMinutes(item.getLength());
        seconds=TimeUnit.NANOSECONDS.toSeconds(item.getLength())-TimeUnit.MINUTES.toSeconds(minutes);
    }
}