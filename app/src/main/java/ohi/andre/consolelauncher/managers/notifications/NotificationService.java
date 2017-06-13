package ohi.andre.consolelauncher.managers.notifications;

/**
 * Created by francescoandreuzzi on 27/04/2017.
 */

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.support.annotation.IntDef;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ohi.andre.consolelauncher.BuildConfig;
import ohi.andre.consolelauncher.managers.XMLPrefsManager;

import static ohi.andre.consolelauncher.managers.notifications.NotificationManager.*;


@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class NotificationService extends NotificationListenerService {

    private final int UPDATE_TIME = 200;

    Map<String, Long> recentNotifications = new HashMap<>();
    Handler handler = new Handler();

    @Override
    public void onCreate() {
        super.onCreate();

        NotificationManager.create();

        if(NotificationManager.colorsLength() == 0) {
//            some nice apps
            NotificationManager.notificationsChangeFor(new ArrayList<>(Arrays.asList(
                    new NotificatedApp("com.whatsapp", Color.parseColor("#25D366"), true),
                    new NotificatedApp("com.google.android.apps.inbox", Color.parseColor("#03A9F4"), true),
                    new NotificatedApp("com.paypal.android.p2pmobile", Color.parseColor("#003087"), true),
                    new NotificatedApp("com.google.android.apps.plus", Color.parseColor("#dd4b39"), true),
                    new NotificatedApp("com.facebook.katana", Color.parseColor("#3b5998"), true),
                    new NotificatedApp("com.twitter.android", Color.parseColor("#1da1f2"), true),
                    new NotificatedApp("com.android.vending", Color.parseColor("#34a853"), true)
            )));
        }

        handler.post(new Runnable() {
            @Override
            public void run() {
                Map<String, Long> copy = new HashMap<>(recentNotifications);
                long time = System.currentTimeMillis();
                for(Map.Entry<String, Long> entry : copy.entrySet()) {
                    if(time - entry.getValue() > 300) {
                        recentNotifications.remove(entry.getKey());
                    }
                }

                handler.postDelayed(this, UPDATE_TIME);
            }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {

        if(recentNotifications.containsKey(sbn.getPackageName())) return;
        recentNotifications.put(sbn.getPackageName(), System.currentTimeMillis());

        Notification notification = sbn.getNotification();
        if (notification == null) {
            return;
        }

        String pack = sbn.getPackageName();
        NotificatedApp nApp = NotificationManager.getAppState(pack);
        if(nApp == null || !nApp.enabled) {
            return;
        }

        CharSequence textSequence = null, titleSequence = null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            textSequence = notification.extras.getCharSequence(Notification.EXTRA_TEXT);
            titleSequence = notification.extras.getCharSequence(Notification.EXTRA_TITLE);
        } else {
            textSequence = notification.tickerText;
        }

        String text = null, title = null;
        if(textSequence != null) {
            text = textSequence.toString();
        }

        if(titleSequence != null) {
            title = titleSequence.toString();
        }

        Intent msgrcv = new Intent("Msg");
        msgrcv.putExtra("package", pack);
        msgrcv.putExtra("title", title);
        msgrcv.putExtra("text", text);
        msgrcv.putExtra("color", nApp.color);

        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(msgrcv);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {}
}