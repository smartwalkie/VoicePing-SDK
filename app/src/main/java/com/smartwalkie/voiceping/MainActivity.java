package com.smartwalkie.voiceping;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.media.audiofx.AcousticEchoCanceler;
import android.media.audiofx.AutomaticGainControl;
import android.media.audiofx.BassBoost;
import android.media.audiofx.LoudnessEnhancer;
import android.media.audiofx.NoiseSuppressor;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.smartwalkie.voicepingsdk.callbacks.DisconnectCallback;
import com.smartwalkie.voicepingsdk.exceptions.VoicePingException;
import com.smartwalkie.voicepingsdk.listeners.AudioInterceptor;
import com.smartwalkie.voicepingsdk.listeners.AudioMetaData;
import com.smartwalkie.voicepingsdk.listeners.AudioReceiver;
import com.smartwalkie.voicepingsdk.listeners.AudioRecorder;
import com.smartwalkie.voicepingsdk.listeners.IncomingTalkListener;
import com.smartwalkie.voicepingsdk.listeners.OutgoingTalkCallback;
import com.smartwalkie.voicepingsdk.models.Channel;
import com.smartwalkie.voicepingsdk.models.ChannelType;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener,
        IncomingTalkListener, OutgoingTalkCallback {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String[] CHANNEL_TYPES = { "PRIVATE", "GROUP" };

    private EditText receiverIdText;
    private Button talkButton;
    private LinearLayout unsubscribeButtonsLayout;
    private Button subscribeButton;
    private Button unsubscribeButton;
    private Spinner channelTypeSpinner;
    private TextInputLayout channelInputLayout;
    private Button muteButton;
    private Button unmuteButton;
    private LinearLayout llOutgoingTalk;
    private TextView tvOutgoingTalk;
    private ProgressBar pbOutgoingTalk;
    private LinearLayout llIncomingTalk;
    private TextView tvIncomingTalk;
    private ProgressBar pbIncomingTalk;
    private String mDestinationPath;
    private Toast mToast;

    private int channelType = ChannelType.PRIVATE;

    private final View.OnTouchListener touchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    String receiverId = receiverIdText.getText().toString().trim();
                    if (receiverId == null || receiverId.isEmpty()) {
                        receiverIdText.setError(getString(R.string.error_invalid_user_id));
                        receiverIdText.requestFocus();
                        break;
                    }
                    talkButton.setText("RELEASE TO STOP");
                    talkButton.setBackgroundColor(Color.YELLOW);
                    mDestinationPath = getExternalFilesDir(null) + "/outgoing_ptt_audio.opus";
                    VoicePingClientApp.getVoicePing().startTalk(receiverId, channelType, MainActivity.this, mDestinationPath);
//                    VoicePingClientApp.getVoicePing().startTalk(receiverId, channelType, MainActivity.this);
                    Log.d(TAG, "Recording starts at: " + System.currentTimeMillis());
                    break;
                case MotionEvent.ACTION_UP:
                    talkButton.setText("START TALKING");
                    talkButton.setBackgroundColor(Color.GREEN);
                    VoicePingClientApp.getVoicePing().stopTalk();
                    Log.d(TAG, "Recording stops at: " + System.currentTimeMillis());
                    break;
                case MotionEvent.ACTION_CANCEL:
                    talkButton.setText("START TALKING");
                    talkButton.setBackgroundColor(Color.GREEN);
                    VoicePingClientApp.getVoicePing().stopTalk();
                    Log.d(TAG, "Recording stops at: " + System.currentTimeMillis());
                    break;
            }
            return false;
        }
    };

    private final View.OnClickListener subscribeListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Log.v(TAG, "subscribeListener");
            String groupId = receiverIdText.getText().toString().trim();
            VoicePingClientApp.getVoicePing().subscribe(groupId);
            showToast("Subscribe to " + groupId);
        }
    };

    private final View.OnClickListener unsubscribeListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Log.v(TAG, "unsubscribeListener");
            String groupId = receiverIdText.getText().toString().trim();
            VoicePingClientApp.getVoicePing().unsubscribe(groupId);
            showToast("Unsubscribe from " + groupId);
        }
    };

    private final View.OnClickListener muteListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Log.v(TAG, "muteListener");
            String receiverId = receiverIdText.getText().toString().trim();
            VoicePingClientApp.getVoicePing().mute(receiverId, channelType);
            showToast("Mute channel " + receiverId);
        }
    };

    private final View.OnClickListener unmuteListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Log.v(TAG, "unmuteListener");
            String receiverId = receiverIdText.getText().toString().trim();
            VoicePingClientApp.getVoicePing().unmute(receiverId, channelType);
            showToast("Unmute channel " + receiverId);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        channelInputLayout = (TextInputLayout) findViewById(R.id.channel_input_layout);
        channelTypeSpinner = (Spinner) findViewById(R.id.channel_type_spinner);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(MainActivity.this,
                android.R.layout.simple_spinner_item, CHANNEL_TYPES);

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        channelTypeSpinner.setAdapter(adapter);
        channelTypeSpinner.setOnItemSelectedListener(this);

        receiverIdText = (EditText) findViewById(R.id.receiver_id_text);

        talkButton = (Button) findViewById(R.id.talk_button);
        talkButton.setOnTouchListener(touchListener);

        subscribeButton = (Button) findViewById(R.id.subscribe_button);
        subscribeButton.setOnClickListener(subscribeListener);
        unsubscribeButton = (Button) findViewById(R.id.unsubscribe_button);
        unsubscribeButton.setOnClickListener(unsubscribeListener);
        unsubscribeButtonsLayout = (LinearLayout) findViewById(R.id.ll_unsubscribe_buttons);
        unsubscribeButtonsLayout.setVisibility(View.GONE);

        muteButton = (Button) findViewById(R.id.mute_button);
        muteButton.setOnClickListener(muteListener);
        unmuteButton = (Button) findViewById(R.id.unmute_button);
        unmuteButton.setOnClickListener(unmuteListener);

        llOutgoingTalk = (LinearLayout) findViewById(R.id.ll_outgoing_talk);
        tvOutgoingTalk = (TextView) findViewById(R.id.tv_outgoing_talk);
        pbOutgoingTalk = (ProgressBar) findViewById(R.id.pb_outgoing_talk);
        llIncomingTalk = (LinearLayout) findViewById(R.id.ll_incoming_talk);
        tvIncomingTalk = (TextView) findViewById(R.id.tv_incoming_talk);
        pbIncomingTalk = (ProgressBar) findViewById(R.id.pb_incoming_talk);

        llOutgoingTalk.setVisibility(View.GONE);
        llIncomingTalk.setVisibility(View.GONE);

        String userId = getIntent().getStringExtra("user_id");
        if (userId != null) setTitle("User ID: " + userId);
        talkButton.setText("START TALKING");
        talkButton.setBackgroundColor(Color.GREEN);

        VoicePingClientApp.getVoicePing().setIncomingTalkListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_open_player:
                startActivity(PlayerActivity.generateIntent(this, mDestinationPath));
                break;
            case R.id.action_disconnect:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.text_button_disconnect)
                        .setMessage("Are you sure you want to disconnect?")
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                VoicePingClientApp.getVoicePing()
                                        .disconnect(new DisconnectCallback() {
                                            @Override
                                            public void onDisconnected() {
                                                Log.v(TAG, "onDisconnected...");
                                                if (!isFinishing()) {
                                                    VoicePingClientApp.getVoicePing().unmuteAll();
                                                    startActivity(new Intent(MainActivity.this,
                                                            LoginActivity.class));
                                                    finish();
                                                    showToast("Disconnected!");
                                                }
                                            }
                                        });
                            }
                        })
                        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // do nothing
                            }
                        })
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
                break;
            default:
                break;
        }
        return true;
    }

    // OnItemSelectedListener
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (parent != channelTypeSpinner) return;
        switch (position) {
            case 0:
                receiverIdText.setHint("Receiver ID");
                channelType = ChannelType.PRIVATE;
                unsubscribeButtonsLayout.setVisibility(View.GONE);
                break;
            case 1:
                receiverIdText.setHint("Group ID");
                channelType = ChannelType.GROUP;
                unsubscribeButtonsLayout.setVisibility(View.VISIBLE);
                break;
            default:
                break;
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    // IncomingTalkListener
    @Override
    public void onIncomingTalkStarted(AudioReceiver audioReceiver, List<Channel> activeChannels) {
        Log.d(TAG, "onIncomingTalkStarted, channel: " + audioReceiver.getChannel().toString());
        // Audio processing
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            try {
                LoudnessEnhancer enhancer = new LoudnessEnhancer(audioReceiver.getAudioSessionId());
                enhancer.setTargetGain(300);
                enhancer.setEnabled(true);
            } catch (RuntimeException e) {
                // Do nothing
            }
        }
        BassBoost bassBoost = new BassBoost(10, audioReceiver.getAudioSessionId());
        bassBoost.setStrength((short) 100);
        bassBoost.setEnabled(true);

        showIncomingTalkLayoutOnUiThread(true);

        mDestinationPath = getExternalFilesDir(null) + "/incoming_ptt_audio.opus";
        audioReceiver.saveToLocal(mDestinationPath);
        audioReceiver.setInterceptorAfterDecoded(new AudioInterceptor() {
            @Override
            public byte[] proceed(byte[] data, final Channel channel) {
                ShortBuffer sb = ByteBuffer.wrap(data).asShortBuffer();
                short[] dataShortArray = new short[sb.limit()];
                sb.get(dataShortArray);
                final double amplitude = getRmsAmplitude(dataShortArray);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tvIncomingTalk.setText("Incoming from: " + channel.getSenderId() + ", to: " + channel.getReceiverId());
                        pbIncomingTalk.setProgress((int) amplitude - 7000);
                    }
                });
                return data;
            }
        });
    }

    @Override
    public void onIncomingTalkStopped(AudioMetaData audioMetaData, List<Channel> activeChannels) {
        Log.d(TAG, "onIncomingTalkStopped, channel: " +
                audioMetaData.getChannel().toString() + ", download url: " +
                audioMetaData.getDownloadUrl() + ", active channels count: " +
                activeChannels.size());
        if (activeChannels.size() == 0) {
            showIncomingTalkLayoutOnUiThread(false);
        }
    }

    @Override
    public void onIncomingTalkError(VoicePingException e) {
        e.printStackTrace();
        showIncomingTalkLayoutOnUiThread(false);
    }

    private void showIncomingTalkLayoutOnUiThread(final boolean show) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (show) {
                    llIncomingTalk.setVisibility(View.VISIBLE);
                } else {
                    llIncomingTalk.setVisibility(View.GONE);
                }
            }
        });
    }

    // OutgoingTalkCallback
    @Override
    public void onOutgoingTalkStarted(AudioRecorder audioRecorder) {
        Log.d(TAG, "outgoing: onOutgoingTalkStarted");
        // Audio processing
        if (NoiseSuppressor.isAvailable()) {
            Log.d(TAG, "NoiseSuppressor available");
            NoiseSuppressor noiseSuppressor = NoiseSuppressor.create(audioRecorder.getAudioSessionId());
            noiseSuppressor.setEnabled(true);
        }
        if (AcousticEchoCanceler.isAvailable()) {
            Log.d(TAG, "AcousticEchoCanceler available");
            AcousticEchoCanceler echoCanceler = AcousticEchoCanceler.create(audioRecorder.getAudioSessionId());
            echoCanceler.setEnabled(true);
        }
        if (AutomaticGainControl.isAvailable()) {
            Log.d(TAG, "AutomaticGainControl available");
            AutomaticGainControl automaticGainControl = AutomaticGainControl.create(audioRecorder.getAudioSessionId());
            automaticGainControl.setEnabled(true);
        }

        showOutgoingTalkLayoutOnUiThread(true);
        // Add interceptor before encoded
        audioRecorder.setInterceptorBeforeEncoded(new AudioInterceptor() {
            @Override
            public byte[] proceed(byte[] data, final Channel channel) {
                ShortBuffer sb = ByteBuffer.wrap(data).asShortBuffer();
                short[] dataShortArray = new short[sb.limit()];
                sb.get(dataShortArray);
                final double amplitude = getRmsAmplitude(dataShortArray);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tvOutgoingTalk.setText("Outgoing from: " + channel.getSenderId() + ", to: " + channel.getReceiverId());
                        pbOutgoingTalk.setProgress((int) amplitude - 7000);
                    }
                });
                return data;
            }
        });
    }

    @Override
    public void onOutgoingTalkStopped(boolean isTooShort, boolean isTooLong) {
        Log.d(TAG, "outgoing: onOutgoingTalkStopped, isTooShort: " + isTooShort + ", isTooLong: " + isTooLong);
        showOutgoingTalkLayoutOnUiThread(false);
        if (isTooShort) {
            showToast("Press and hold the button to send PTT. Release after you are done.");
        }
    }

    @Override
    public void onDownloadUrlReceived(String downloadUrl) {
        Log.d(TAG, "outgoing: onDownloadUrlReceived, download url: " + downloadUrl);
//        downloadFileAsync(downloadUrl);
    }

    @Override
    public void onOutgoingTalkError(VoicePingException e) {
        Log.e(TAG, "outgoing: onOutgoingTalkError: " + e.getMessage());
        e.printStackTrace();
        showOutgoingTalkLayoutOnUiThread(false);
        showToast(e.getMessage());
    }

    private void showOutgoingTalkLayoutOnUiThread(final boolean show) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (show) {
                    llOutgoingTalk.setVisibility(View.VISIBLE);
                } else {
                    llOutgoingTalk.setVisibility(View.GONE);
                }
            }
        });
    }

    private void downloadFileAsync(final String downloadUrl) {
        Log.d(TAG, "start to download file from: " + downloadUrl);
        OkHttpClient client = new OkHttpClient();
        final Request request = new Request.Builder().url(downloadUrl).build();
        client.newCall(request)
                .enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        if (!response.isSuccessful()) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    showToast("Failed to download file!");
                                }
                            });
                            return;
                        }
                        String[] split = downloadUrl.split("/");
                        String fileName = split[split.length-1];
                        mDestinationPath = getExternalFilesDir(null) + "/" + fileName;
                        if (response.body() == null) return;
                        FileOutputStream fileOutputStream = new FileOutputStream(mDestinationPath);
                        fileOutputStream.write(response.body().bytes());
                        fileOutputStream.close();
                        Log.d(TAG, "file downloaded to: " + mDestinationPath);
                    }
                });
    }

    private double getRmsAmplitude(short[] dataShortArray) {
        double sum = 0;
        for (short singleShort : dataShortArray) {
            sum += singleShort * singleShort;
        }
        double meanSquare = sum / dataShortArray.length;
        return Math.sqrt(meanSquare);
    }

    private double getMaxAmplitude(short[] dataShortArray) {
        double max = Math.abs(dataShortArray[0]);
        for (short singleShort : dataShortArray) {
            if (max < singleShort) max = Math.abs(singleShort);
        }
        return max;
    }

    private void showToast(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mToast != null) mToast.cancel();
                mToast = Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT);
                mToast.show();
            }
        });
    }
}
