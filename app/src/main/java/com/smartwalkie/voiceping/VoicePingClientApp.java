package com.smartwalkie.voiceping;

import android.app.Application;

import com.smartwalkie.voicepingsdk.VoicePing;

/**
 * Created by sirius on 7/3/17.
 */

public class VoicePingClientApp extends Application {

    private static VoicePing mVoicePing;

    @Override
    public void onCreate() {
        super.onCreate();
//        mVoicePing = VoicePing.init(this, "ws://vpjsex.southeastasia.cloudapp.azure.com");
        mVoicePing = VoicePing.newBuilder()
//                .setUsingOpusCodec(false)
                .setRecordingBoostInDb(20)
//                .setReceivingBoostInDb(20)
//                .setPlaybackBoostInDb(20)
                .buildAndInit(this, "wss://vpjsex-router.voiceoverping.net");
    }

    public static VoicePing getVoicePing() {
        return mVoicePing;
    }
}
