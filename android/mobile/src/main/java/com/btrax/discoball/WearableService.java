package com.btrax.discoball;

import android.util.Log;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;
import com.pubnub.api.Callback;
import com.pubnub.api.Pubnub;
import com.pubnub.api.PubnubError;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * receive data from smart watch
 */
public class WearableService extends WearableListenerService {

    private final static String TAG = WearableService.class.getSimpleName();
    private final static String PUBNUB_PUBLISH_KEY = "<Your Publish Key>";
    private final static String PUBNUB_SUBSCRIBE_KEY = "<Your Subscribe Key>";
    private final static String CHANNEL_C_CONTROL = "c-control";
    private final static String PUBNUB_SPEED_KEY = "speed";

    private Pubnub mPubnub;

    @Override
    public void onCreate() {
        super.onCreate();
        mPubnub = new Pubnub(
                PUBNUB_PUBLISH_KEY,
                PUBNUB_SUBSCRIBE_KEY,
                "",      // SECRET_KEY
                "",      // CIPHER_KEY
                false    // SSL_ON?
        );
    }

    /**
     * called when receive message from Android Wear
     *
     * @param messageEvent
     */
    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Log.d(TAG, "onMessageReceived");

        String speedStr = new String(messageEvent.getData());
        int speed = Integer.valueOf(speedStr);

        updateDiscoBallSpeed(speed);
    }

    /**
     * send to pubnub, change disco ball speed
     *
     * @param speed rotation speed (0 - 100)
     */
    private void updateDiscoBallSpeed(int speed) {
        try {
            Log.d(TAG, "speed:" + speed);
            JSONObject object = new JSONObject();
            object.put(PUBNUB_SPEED_KEY, speed);
            mPubnub.publish(CHANNEL_C_CONTROL, object, new Callback() {
                @Override
                public void successCallback(String channel, Object message) {
                    super.successCallback(channel, message);
                    Log.i(TAG, message.toString());
                }

                @Override
                public void errorCallback(String channel, PubnubError error) {
                    super.errorCallback(channel, error);
                    Log.e(TAG, error.toString());
                }
            });

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
