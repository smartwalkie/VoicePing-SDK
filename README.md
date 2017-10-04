# VoicePing Android SDK

VoicePing Android SDK is an Android library, provided by 
[Smart Walkie Talkie](http://www.smartwalkie.com), 
for enabling Push-To-Talk (PTT) functionality to your Android project.

## Installation

How to install this SDK to your Android project

1. Go to app/libs
2. Copy voiceping-sdk.aar file to your project
3. Add the aar file as dependency to your build.gradle
4. Use it

## Steps to use VoicePing Android SDK

1. Initialization

Initialize and instantiate VoicePing in your Application code, inside onCreate method

```java
public class VoicePingClientApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        String serverUrl = "voiceping_sdk_server_url";
        VoicePing voicePing = VoicePing.init(this, serverUrl);
    }
}
```

You can then use the instance to connect to server, subscribe a channel, or do PTT.
In order to use the instance, you need to expose the instance to public.

```java
public class VoicePingClientApp extends Application {

    private static VoicePing mVoicePing;

    @Override
    public void onCreate() {
        super.onCreate();
        String serverUrl = "voiceping_sdk_server_url";
        mVoicePing = VoicePing.init(this, serverUrl);
    }

    public static VoicePing getVoicePing() {
        return mVoicePing;
    }
}
```

2. Connect

Before you can start talking using PTT, you need connect to server. You can do that by call connect
method from VoicePing instance.

```java
String userId = "your_user_id";
VoicePingClientApp.getVoicePing().connect(userId, new ConnectCallback() {
            @Override
            public void onConnected() {
                // Do something
            }

            @Override
            public void onFailed(PingException exception) {
                // Do something
            }
        });
```

3. Start Talking

After successfully connected, you can now start talking. You can start talking to individual 
receiver using,

```java
String receiverId = "receiver_id";
VoicePingClientApp.getVoicePing().startTalking(receiverId, ChannelType.PRIVATE, this);
```

or in a group using,

```java
String groupId = "group_id";
VoicePingClientApp.getVoicePing().startTalking(groupId, ChannelType.GROUP, this);
```

if you want to save the recorded audio as a WAV file at the end of the talk, you can use,

```java
String receiverId = "receiver_id";
int channelType = ChannelType.PRIVATE // or ChannelType.GROUP if you want to target group
String destinationPath = "destination_path";
VoicePingClientApp.getVoicePing().startTalking(receiverId, channelType, this, destinationPath);
```

instead.

4. Stop Talking

To stop talking, for both Private and Group PTT, you can use,

```java
VoicePingClientApp.getVoicePing().stopTalking();
```

5. Disconnect

You can disconnect to stop receiving PTT by using,

```java
VoicePingClientApp.getVoicePing().disconnect(new DisconnectCallback() {
            @Override
            public void onDisconnected() {
                // Do something
            }
    
            @Override
            public void onFailed(final PingException exception) {
                // Do something
            }
        });
```

6. Subscribe to a group

You cannot listen to a group channel before subscribing to it. To subscribe to a group channel, 
you can use,

```java
String groupId = "group_id";
VoicePingClientApp.getVoicePing().subscribe(groupId);
```

7. Unsubscribe from a group

To unsubscribe from a group channel, you can use,

```java
String groupId = "group_id";
VoicePingClientApp.getVoicePing().unsubscribe(groupId);
```

8. Mute from specific channel

To mute from specific channel, you can use,

```java
String senderId = "sender_id";
int channelType = ChannelType.PRIVATE // or ChannelType.GROUP if you want to target group
VoicePingClientApp.getVoicePing().mute(senderId, channelType);
```

9. Unmute to specific channel

To unmute to specific channel, you can use,

```java
String senderId = "sender_id";
int channelType = ChannelType.PRIVATE // or ChannelType.GROUP if you want to target group
VoicePingClientApp.getVoicePing().unmute(senderId, channelType);
```

## Advance

1. Custom audio parameters

You can use custom audio parameters in your app using builder pattern. Instead of directly using,

```java
String serverUrl = "voiceping_sdk_server_url";
mVoicePing = VoicePing.init(this, serverUrl);
```

to instantiate VoicePing, you can use,

```java
String serverUrl = "voiceping_sdk_server_url";
mVoicePing = VoicePing.newBuilder()
        .setUsingOpusCodec(true)
        .setSampleRate(16000)
        .setFrameSize(960)
        .setChannelSize(1)
        .setBufferSizeFactor(2)
        .buildAndInit(this, serverUrl);
```

2. OutgoingTalkCallback

To do some advance techniques for the recorded audio, such as showing amplitude of the 
audio, change pitch, and save the audio to local storage, you need to implement 
OutgoingTalkCallback and attach it to ```startTalking```.
OutgoingTalkCallback is needed to do ```startTalking```,

```java
String receiverId = "your_receiver_id";
VoicePingClientApp.getVoicePing().startTalking(receiverId, ChannelType.PRIVATE, this);
```

with ```this``` is the instance that has implemented OutgoingTalkCallback.

```java
public class MainActivity extends AppCompatActivity implements OutgoingTalkListener {
    
    /*
     * Other class code
     */
    
    @Override
    public void onOutgoingTalkStarted(AudioRecorder audioRecorder) {
        // Do something after invoking startTalking.
    }

    @Override
    public void onOutgoingTalkStopped() {
        // Do something after invoking stopTalking.
    }

    @Override
    public void onOutgoingTalkError(PingException e) {
        // Do something on outgoing talk error.
    }
}
```

You can do a lot of thing by putting your code inside the appropriate methods.

3. ChannelListener

To do some advance techniques for the received audio, you need to implement ChannelListener.
ChannelListener is needed to customize incoming talk.

```java
public class MainActivity extends AppCompatActivity implements ChannelListener {
    
    /*
     * Other class code
     */

    @Override
    public void onIncomingTalkStarted(AudioPlayer audioPlayer) {
        // Do something after incoming talk started.
    }

    @Override
    public void onIncomingTalkStopped() {
        // Do something after incoming talk stopped.
    }

    @Override
    public void onChannelError(PingException e) {
        // Do something on error.
    }
}
```

In order to make ChannelListener works, ChannelListener needs to be registered to VoicePing 
instance using,

```java
VoicePingClientApp.getVoicePing().setChannelListener(this);
```

### Warning

```AudioInterceptor``` in ```audioRecorder.addAudioInterceptor(AudioInterceptor audioInterceptor)``` 
and ```audioPlayer.addAudioInterceptor(AudioInterceptor audioInterceptor)``` are running on 
separated thread. If you want to touch UI from there, you need to run it on Main Thread.
