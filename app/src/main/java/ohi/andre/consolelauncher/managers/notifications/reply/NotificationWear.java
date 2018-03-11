package ohi.andre.consolelauncher.managers.notifications.reply;

import android.app.PendingIntent;
import android.app.RemoteInput;
import android.os.Bundle;

/**
 * Created by francescoandreuzzi on 24/01/2018.
 */

public class NotificationWear {

    public BoundApp app;

    public PendingIntent pendingIntent;
    public RemoteInput[] remoteInputs;
    public Bundle bundle;
    public int id;

    public CharSequence text;

    @Override
    public boolean equals(Object obj) {
        try {
            NotificationWear h = (NotificationWear) obj;
            return h.app.packageName.equals(app.packageName);
        } catch (Exception e) {
            return false;
        }
    }
}