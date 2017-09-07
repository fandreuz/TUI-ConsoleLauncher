package ohi.andre.consolelauncher.tuils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.RemoteInput;

import ohi.andre.consolelauncher.managers.TerminalManager;
import ohi.andre.consolelauncher.tuils.interfaces.CommandExecuter;
import ohi.andre.consolelauncher.tuils.interfaces.Outputable;

/**
 * Created by francescoandreuzzi on 18/08/2017.
 */

public class InputOutputReceiver extends BroadcastReceiver {

    public static final int WAS_MUSIC_SERVICE = 10;
    public static final int WAS_KEEPER_SERVICE = 11;

    public static final String WAS_KEY = "was";

    public static final String ACTION_CMD = "ohi.andre.consolelauncher.action_cmd";
    public static final String ACTION_OUTPUT = "ohi.andre.consolelauncher.action_output";
    public static final String TEXT = "ohi.andre.consolelauncher.text";
    public static final String TYPE = "ohi.andre.consolelauncher.type";
    public static final String COLOR = "ohi.andre.consolelauncher.color";

    CommandExecuter executer;
    Outputable outputable;

    public InputOutputReceiver(CommandExecuter executer, Outputable outputable) {
        this.executer = executer;
        this.outputable = outputable;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);
        if(remoteInput == null || remoteInput.size() == 0) {
            CharSequence text = intent.getCharSequenceExtra(TEXT);
            if(text == null) text = intent.getStringExtra(TEXT);
            if(text == null) return;

            if(intent.getAction().equals(ACTION_CMD)) {
                executer.exec(text.toString());
            } else {
                int color = intent.getIntExtra(COLOR, Integer.MAX_VALUE);

                if(color != Integer.MAX_VALUE) {
                    outputable.onOutput(color, text);
                }
                else {
                    int type = intent.getIntExtra(TYPE, -1);
                    if(type != -1) outputable.onOutput(text, type);
                    else outputable.onOutput(text, TerminalManager.CATEGORY_OUTPUT);
                }
            }
        } else {
            String cmd = remoteInput.getString(TEXT);
            executer.exec(cmd, true);

            int was = intent.getIntExtra(WAS_KEY, 0);
            if(was == WAS_KEEPER_SERVICE) {
                NotificationManagerCompat.from(context).notify(KeeperService.ONGOING_NOTIFICATION_ID, KeeperService.buildNotification(context));
            } else if(was == WAS_MUSIC_SERVICE) {
//                do nothing
            }
        }
    }
}
