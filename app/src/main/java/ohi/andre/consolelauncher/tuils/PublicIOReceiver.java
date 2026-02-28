package ohi.andre.consolelauncher.tuils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.core.app.RemoteInput;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import ohi.andre.consolelauncher.BuildConfig;
import ohi.andre.consolelauncher.MainManager;

/**
 * Created by francescoandreuzzi on 04/02/2018.
 */

public class PublicIOReceiver extends BroadcastReceiver {

    public static final String ACTION_OUTPUT = BuildConfig.APPLICATION_ID + ".action_public_output";
    public static final String ACTION_CMD = BuildConfig.APPLICATION_ID + ".action_public_cmd";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action;
        if(intent.getAction().equals(ACTION_CMD)) {
            Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);
            if (remoteInput != null) {
                String cmd = remoteInput.getString(PrivateIOReceiver.TEXT);
                boolean wasMusic = remoteInput.getBoolean(MainManager.MUSIC_SERVICE) || intent.getBooleanExtra(MainManager.MUSIC_SERVICE, false);

                intent.putExtra(MainManager.MUSIC_SERVICE, wasMusic);
                intent.putExtra(MainManager.CMD, cmd);
            } else {
                return;
            }

            action = MainManager.ACTION_EXEC;

            intent.putExtra(MainManager.CMD_COUNT, MainManager.commandCount);
        } else if(intent.getAction().equals(ACTION_OUTPUT)) {
            action = PrivateIOReceiver.ACTION_OUTPUT;
        } else return;

        LocalBroadcastManager.getInstance(context).sendBroadcast(intent.setAction(action));
    }
}
