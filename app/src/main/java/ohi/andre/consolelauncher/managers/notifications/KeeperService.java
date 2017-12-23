package ohi.andre.consolelauncher.managers.notifications;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.RemoteInput;
import android.text.SpannableString;
import android.text.TextUtils;

import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.managers.TimeManager;
import ohi.andre.consolelauncher.managers.xml.XMLPrefsManager;
import ohi.andre.consolelauncher.managers.xml.options.Behavior;
import ohi.andre.consolelauncher.managers.xml.options.Theme;
import ohi.andre.consolelauncher.managers.xml.options.Ui;
import ohi.andre.consolelauncher.tuils.InputOutputReceiver;
import ohi.andre.consolelauncher.tuils.Tuils;

import static ohi.andre.consolelauncher.managers.TerminalManager.FORMAT_INPUT;
import static ohi.andre.consolelauncher.managers.TerminalManager.FORMAT_NEWLINE;
import static ohi.andre.consolelauncher.managers.TerminalManager.FORMAT_PREFIX;

public class KeeperService extends Service {

//    private final String PATH = "reply.xml";
//    public static final String BIND_NODE = "binding", ID_ATTRIBUTE = "id", APP_ATTRIBUTE = "pkg";

    public static final int ONGOING_NOTIFICATION_ID = 1001;
    public static final String CMD_KEY = "cmd", PATH_KEY = "path";

    private String title, subtitle, clickCmd, inputFormat, prefix, suPrefix;
    private boolean showHome, upDown;
    private int inputColor, timeColor, priority;

    private CharSequence[] lastCommands = null;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(startId == 1 || startId == 0) {
            title = XMLPrefsManager.get(Behavior.tui_notification_title);
            subtitle = XMLPrefsManager.get(Behavior.tui_notification_subtitle);
            clickCmd = XMLPrefsManager.get(Behavior.tui_notification_click_cmd);
            inputFormat = XMLPrefsManager.get(Behavior.input_format);
            showHome = XMLPrefsManager.getBoolean(Behavior.tui_notification_click_showhome);
            inputColor = XMLPrefsManager.getColor(Theme.input_color);
            timeColor = XMLPrefsManager.getColor(Theme.time_color);
            prefix = XMLPrefsManager.get(Ui.input_prefix);
            upDown = XMLPrefsManager.getBoolean(Behavior.tui_notification_lastcmds_updown);
            suPrefix = XMLPrefsManager.get(Ui.input_root_prefix);

            priority = XMLPrefsManager.getInt(Behavior.tui_notification_priority);
            if(priority > 2) priority = 2;
            if(priority < -2) priority = -2;

            String path = intent.getStringExtra(PATH_KEY);

            startForeground(ONGOING_NOTIFICATION_ID, buildNotification(getApplicationContext(), title, subtitle, Tuils.getHint(path),
                    clickCmd, showHome, lastCommands, upDown, priority));

            int lastCmdSize = XMLPrefsManager.getInt(Behavior.tui_notification_lastcmds_size);
            if(lastCmdSize > 0) {
                lastCommands = new CharSequence[lastCmdSize];
            }


        } else {
//            new cmd
//            update the list

            if(lastCommands != null) updateCmds(intent.getStringExtra(CMD_KEY));

            String path = intent.getStringExtra(PATH_KEY);

            NotificationManagerCompat.from(getApplicationContext()).notify(KeeperService.ONGOING_NOTIFICATION_ID,
                    KeeperService.buildNotification(getApplicationContext(), title, subtitle, Tuils.getHint(path),
                            clickCmd, showHome, lastCommands, upDown, priority));
        }

        return super.onStartCommand(intent, flags, startId);
    }

//    0 = most recent
//    4 = oldest

//    * = null
//    3 cases
//    1: |*|*|*|*|*| -> lastNull = 0
//    2: |a|b|c|*|*| -> lastNull = n < length
//    3: |a|b|c|d|e| -> lastNull = -1
    private void updateCmds(String cmd) {
        try {
            int lastNull = lastNull();
            int toCopy = lastNull == -1 ? lastCommands.length - 1 : lastNull;
            System.arraycopy(lastCommands, 0, lastCommands, 1, toCopy);
            lastCommands[0] = formatInput(cmd, inputFormat, prefix, suPrefix, inputColor, timeColor);
        } catch (Exception e) {
            Tuils.log(e);
        }
    }

    private static CharSequence formatInput(String cmd, String inputFormat, String prefix, String suPrefix, int inputColor, int timeColor) {
        if(cmd == null) return null;
        boolean su = cmd.startsWith("su ");

        SpannableString si = Tuils.span(inputFormat, inputColor);

        CharSequence s = TimeManager.replace(si, timeColor);
        s = TextUtils.replace(s,
                new String[] {FORMAT_INPUT, FORMAT_PREFIX, FORMAT_NEWLINE, FORMAT_INPUT.toUpperCase(), FORMAT_PREFIX.toUpperCase(), FORMAT_NEWLINE.toUpperCase()},
                new CharSequence[] {cmd, su ? suPrefix : prefix, Tuils.NEWLINE, cmd, su ? suPrefix : prefix, Tuils.NEWLINE});

        return s;
    }

    private int lastNull() {
        for(int c = 0; c < lastCommands.length; c++) if(lastCommands[c] == null) return c;
        return -1;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return true;
    }

    private static Notification buildNotification(Context c, String title, String subtitle, String cmdLabel, String clickCmd, boolean showHome, CharSequence[] lastCommands, boolean upDown, int priority) {

        PendingIntent pendingIntent;
        if(showHome) {
            Intent startMain = new Intent(Intent.ACTION_MAIN);
            startMain.addCategory(Intent.CATEGORY_HOME);
            startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            if(clickCmd != null && clickCmd.length() > 0) {
                startMain.putExtra(InputOutputReceiver.TEXT, clickCmd);
            }

            pendingIntent = PendingIntent.getActivity(
                    c,
                    0,
                    startMain,
                    PendingIntent.FLAG_CANCEL_CURRENT
            );
        } else if(clickCmd != null && clickCmd.length() > 0) {
            Intent cmdIntent = new Intent(InputOutputReceiver.ACTION_CMD);
            cmdIntent.putExtra(InputOutputReceiver.TEXT, clickCmd);
            cmdIntent.putExtra(InputOutputReceiver.SHOW_CONTENT, true);

            pendingIntent = PendingIntent.getBroadcast(
                    c,
                    0,
                    cmdIntent,
                    0
            );
        } else {
            pendingIntent = null;
        }

        NotificationCompat.Style style = null;
        if(lastCommands != null && lastCommands[0] != null) {
            NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();

            if(upDown) {
                for (CharSequence lastCommand : lastCommands) {
                    if (lastCommand == null) break;
                    inboxStyle.addLine(lastCommand);
                }
            } else {
                for(int j = lastCommands.length - 1; j >= 0; j--) {
                    if(lastCommands[j] == null) continue;
                    inboxStyle.addLine(lastCommands[j]);
                }
            }

            style = inboxStyle;
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(c)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setTicker(c.getString(R.string.start_notification))
                .setWhen(System.currentTimeMillis())
                .setPriority(priority)
                .setContentTitle(title)
                .setContentIntent(pendingIntent);

        if(style != null) builder.setStyle(style);
        else {
            builder.setContentTitle(title);
            builder.setContentText(subtitle);
        }

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            RemoteInput remoteInput = new RemoteInput.Builder(InputOutputReceiver.TEXT)
                    .setLabel(cmdLabel)
                    .build();

            Intent i = new Intent(InputOutputReceiver.ACTION_CMD);
            i.putExtra(InputOutputReceiver.WAS_KEY, InputOutputReceiver.WAS_KEEPER_SERVICE);

            NotificationCompat.Action action = new NotificationCompat.Action.Builder(R.mipmap.ic_launcher, cmdLabel,
                    PendingIntent.getBroadcast(c.getApplicationContext(), 40, i, PendingIntent.FLAG_UPDATE_CURRENT))
                    .addRemoteInput(remoteInput)
                    .build();

            builder.addAction(action);
        }

        return builder.build();
    }
}
