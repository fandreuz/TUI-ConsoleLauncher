package ohi.andre.consolelauncher.tuils;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.RemoteInput;

import ohi.andre.consolelauncher.LauncherActivity;
import ohi.andre.consolelauncher.R;

public class KeeperService extends Service {

    public static final int ONGOING_NOTIFICATION_ID = 1001;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(ONGOING_NOTIFICATION_ID, buildNotification(getApplicationContext()));
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return true;
    }

    public static Notification buildNotification(Context c) {
        Intent resultIntent = new Intent(c, LauncherActivity.class);
        PendingIntent resultPendingIntent = PendingIntent.getActivity(
                c,
                0,
                resultIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(c)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setTicker(c.getString(R.string.start_notification))
                .setWhen(System.currentTimeMillis())
                .setContentTitle(c.getString(R.string.app_name))
                .setContentText(c.getString(R.string.tui_running))
                .setContentIntent(resultPendingIntent);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            String label = "cmd";
            RemoteInput remoteInput = new RemoteInput.Builder(InputOutputReceiver.TEXT)
                    .setLabel(label)
                    .build();

            Intent i = new Intent(InputOutputReceiver.ACTION_CMD);
            i.putExtra(InputOutputReceiver.WAS_KEY, InputOutputReceiver.WAS_KEEPER_SERVICE);

            NotificationCompat.Action action = new NotificationCompat.Action.Builder(R.mipmap.ic_launcher, label,
                    PendingIntent.getBroadcast(c.getApplicationContext(), 40, i, PendingIntent.FLAG_UPDATE_CURRENT))
                    .addRemoteInput(remoteInput)
                    .build();

            builder.addAction(action);
        }

        return builder.build();
    }
}
