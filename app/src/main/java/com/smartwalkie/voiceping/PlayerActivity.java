package com.smartwalkie.voiceping;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.smartwalkie.voicepingsdk.VoicePingPlayer;
import com.smartwalkie.voicepingsdk.models.AudioParam;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by kukuhsain on 10/12/17.
 */

public class PlayerActivity extends AppCompatActivity {

    private final String TAG = PlayerActivity.class.getSimpleName();
    private static final String FILE_PATH_DATA = "file_path_data";
    private final int RC_PICK_FILE = 100;

    private TextView filePathText;
    private Button pickFileButton;
    private SeekBar seekBar;
    private TextView timeProgress;
    private TextView timeDuration;
    private Button playButton;
    private Button pauseButton;
    private Button stopButton;

    private String mFilePath;
    private VoicePingPlayer mVoicePingPlayer;
    private Timer mTimer;

    public static Intent generateIntent(Context context, String filePath) {
        Intent intent = new Intent(context, PlayerActivity.class);
        intent.putExtra(FILE_PATH_DATA, filePath);
        return intent;
    }

    private final View.OnClickListener pickFileListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("file/*");
            startActivityForResult(intent, RC_PICK_FILE);
        }
    };

    private final View.OnClickListener playListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Log.v(TAG, "playListener");
//            if (mVoicePingPlayer == null || mVoicePingPlayer.isPlaying()) return;
            if (mVoicePingPlayer == null) return;
            File file = new File(mFilePath);
            if (!file.exists()) {
                Toast.makeText(PlayerActivity.this, "File not exist!", Toast.LENGTH_SHORT).show();
                return;
            }
            VoicePingClientApp.getVoicePing().muteAll();
            mVoicePingPlayer.start();
            mTimer = new Timer();
            mTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            seekBar.setProgress((int) mVoicePingPlayer.getCurrentPosition());
                            timeProgress.setText(getTimeFromMillis(mVoicePingPlayer.getCurrentPosition()));
                        }
                    });
                }
            }, 0, 500);
        }
    };

    private final View.OnClickListener pauseListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Log.v(TAG, "pauseListener");
            VoicePingClientApp.getVoicePing().unmuteAll();
            if (mVoicePingPlayer != null) mVoicePingPlayer.pause();
            if (mTimer != null) mTimer.cancel();
        }
    };

    private final View.OnClickListener stopListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Log.v(TAG, "stopListener");
            VoicePingClientApp.getVoicePing().unmuteAll();
            if (mVoicePingPlayer != null) mVoicePingPlayer.stop();
            if (mTimer != null) mTimer.cancel();
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        mFilePath = getIntent().getStringExtra(FILE_PATH_DATA);
        if (mFilePath == null || mFilePath.isEmpty()) {
            Toast.makeText(this, "You need to do PTT call first!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        filePathText = (TextView) findViewById(R.id.file_path);
        filePathText.setText(mFilePath);
        pickFileButton = (Button) findViewById(R.id.pick_file_button);
        pickFileButton.setOnClickListener(pickFileListener);
        seekBar = (SeekBar) findViewById(R.id.seek_bar);
        timeProgress = (TextView) findViewById(R.id.time_progress);
        timeDuration = (TextView) findViewById(R.id.time_duration);
        playButton = (Button) findViewById(R.id.play_button);
        playButton.setOnClickListener(playListener);
        pauseButton = (Button) findViewById(R.id.pause_button);
        pauseButton.setOnClickListener(pauseListener);
        stopButton = (Button) findViewById(R.id.stop_button);
        stopButton.setOnClickListener(stopListener);
    }

    @Override
    protected void onStart() {
        super.onStart();
        initPlayer();
    }

    private void initPlayer() {
        AudioParam audioParam = VoicePingClientApp.getVoicePing().getAudioParam();
        int bufferSize;
        if (audioParam.isUsingOpusCodec()) {
            bufferSize = 133;
        } else {
            bufferSize = audioParam.getRawBufferSize();
        }
        mVoicePingPlayer = new VoicePingPlayer(audioParam, bufferSize);
        try {
            mVoicePingPlayer.setDataSource(mFilePath);
            mVoicePingPlayer.prepare();
            seekBar.setMax((int) mVoicePingPlayer.getDuration());
            timeDuration.setText(getTimeFromMillis(mVoicePingPlayer.getDuration()));
            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    Log.d(TAG, "progress updated to: " + seekBar.getProgress());
                    mVoicePingPlayer.seekTo(seekBar.getProgress());
                }
            });
            mVoicePingPlayer.setOnPlaybackStartedListener(new VoicePingPlayer.OnPlaybackStartedListener() {
                @Override
                public void onStart(int audioSessionId) {
                    Log.d(TAG, "OnPlaybackStartedListener, session id: " + audioSessionId);
                }
            });
            mVoicePingPlayer.setOnCompletionListener(new VoicePingPlayer.OnCompletionListener() {
                @Override
                public void onComplete() {
                    VoicePingClientApp.getVoicePing().unmuteAll();
                    if (mTimer != null) mTimer.cancel();
                    seekBar.setProgress((int) mVoicePingPlayer.getDuration());
                    timeProgress.setText(getTimeFromMillis(mVoicePingPlayer.getDuration()));
                    Toast.makeText(PlayerActivity.this, "Playback Completed!", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private String getTimeFromMillis(long millis) {
        int secs = Math.round(millis / 1000);
        int timeMins = secs / 60;
        int timeSecs = secs % 60;
        return String.format(Locale.US, "%02d:%02d", timeMins, timeSecs);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_PICK_FILE && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            mFilePath = uri.getPath();
            filePathText.setText(mFilePath);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mVoicePingPlayer != null) mVoicePingPlayer.stop();
    }
}
