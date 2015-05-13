package com.btrax.discoball;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.util.ArrayList;

public class MainActivity extends Activity {

    private final static int REQUEST_CODE = 100;
    private final static String TAG = MainActivity.class.getSimpleName();
    private final static String SPEED_UP_VOICE = "speed up";
    private final static String SPEED_DOWN_VOICE = "speed down";
    private final static String MESSAGE_PATH = "/watch";

    private TextView mSpeedText;
    private SeekBar mSeekBar;
    private GoogleApiClient mGoogleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // init views
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                mSpeedText = (TextView) stub.findViewById(R.id.speed_text);
                mSeekBar = (SeekBar) stub.findViewById(R.id.speed_control_bar);
                mSeekBar.setOnSeekBarChangeListener(mOnSeekBarChangeListener);

                mSpeedText.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        startVoiceRecognitionActivity();
                    }
                });
            }
        });

        // setup communicate with smartphone
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle bundle) {
                        Log.d(TAG, "onConnected");
                    }

                    @Override
                    public void onConnectionSuspended(int i) {
                        Log.d(TAG, "onConnectionSuspended");
                    }
                })
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult connectionResult) {
                        Log.d(TAG, "onConnectionFailed");
                    }
                })
                .addApi(Wearable.API)
                .build();

        mGoogleApiClient.connect();
    }

    /**
     * Seekbar Event listener
     */
    SeekBar.OnSeekBarChangeListener mOnSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            mSpeedText.setText(String.valueOf(progress));
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            int speed = seekBar.getProgress();
            sendSpeed(speed);
            mSpeedText.setText(String.valueOf(speed));
        }
    };

    /**
     * send speed to smartphone
     *
     * @param speed
     */
    private void sendSpeed(final int speed) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final String message = String.valueOf(speed);
                NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();
                for (Node node : nodes.getNodes()) {
                    MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(
                            mGoogleApiClient,
                            node.getId(),
                            MESSAGE_PATH,
                            message.getBytes())
                            .await();
                    if (result.getStatus().isSuccess()) {
                        Log.d(TAG, "success:send to mobile");
                    } else {
                        Log.e(TAG, "failure:send to mobile");
                    }
                }
            }
        }).start();
    }

    /**
     * launch voice input activity
     */
    private void startVoiceRecognitionActivity() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, R.string.voice_input_msg);
        startActivityForResult(intent, REQUEST_CODE);
    }

    /**
     * receieve data from voice input activity
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        // return from voice input activity
        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK) {
            ArrayList<String> matches = data.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS);
            String command = matches.get(0);


            // add 20 if voice is "speed up"
            if (command.equals(SPEED_UP_VOICE) || command.equals(SPEED_DOWN_VOICE)) {

                int speed = mSeekBar.getProgress();
                if (command.equals(SPEED_UP_VOICE)) {
                    speed += 20;
                    if (speed > 100) {
                        speed = 100;
                    }
                } else {
                    speed -= 20;
                    if (speed < 0) {
                        speed = 0;
                    }
                }

                mSeekBar.setProgress(speed);
                sendSpeed(speed);
                mSpeedText.setText(String.valueOf(speed));
            }
        }

        // for debug on Emulater
        /*
        int speed = 50;
        sendSpeed(speed);
        mSeekBar.setProgress(speed);
        mSpeedText.setText(String.valueOf(speed));
        */
        super.onActivityResult(requestCode, resultCode, data);
    }
}
