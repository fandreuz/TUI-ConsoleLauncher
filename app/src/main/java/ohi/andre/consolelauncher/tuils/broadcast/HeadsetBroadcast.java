package ohi.andre.consolelauncher.tuils.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by francescoandreuzzi on 24/01/16.
 */
public class HeadsetBroadcast extends BroadcastReceiver {

    Runnable headsetUnplugged;

    public HeadsetBroadcast(Runnable r) {
        super();

        this.headsetUnplugged = r;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getIntExtra("state", 0) != 1)
            headsetUnplugged.run();
    }
}
