package ohi.andre.consolelauncher.managers.notifications;

/**
 * Created by francescoandreuzzi on 27/04/2017.
 */

import android.annotation.TargetApi;
import android.app.Notification;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.support.v4.content.LocalBroadcastManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static ohi.andre.consolelauncher.managers.notifications.NotificationManager.NotificatedApp;
import static ohi.andre.consolelauncher.managers.notifications.NotificationManager.default_color;


@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class NotificationService extends NotificationListenerService {

    private final int UPDATE_TIME = 200;

    Map<String, Long> recentNotifications = new HashMap<>();
    Handler handler = new Handler();

    @Override
    public void onCreate() {
        super.onCreate();

        NotificationManager.create();

        if(NotificationManager.apps() == 0) {
//            some nice apps
            NotificationManager.notificationsChangeFor(new ArrayList<>(Arrays.asList(
                    new NotificatedApp("com.whatsapp", "#25D366", true),
                    new NotificatedApp("com.google.android.apps.inbox", "#03A9F4", true),
                    new NotificatedApp("com.paypal.android.p2pmobile", "#003087", true),
                    new NotificatedApp("com.google.android.apps.plus", "#dd4b39", true),
                    new NotificatedApp("com.facebook.katana", "#3b5998", true),
                    new NotificatedApp("com.twitter.android", "#1da1f2", true),
                    new NotificatedApp("com.android.vending", "#34a853", true)
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
        if( (nApp != null && !nApp.enabled)) {
            return;
        }

        if(nApp == null && !NotificationManager.default_app_state) {
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

        if(NotificationManager.textMatches(text) || NotificationManager.titleMatches(title)) return;

        int color;
        try {
            color = Color.parseColor(nApp.color);
        } catch (Exception e) {
            color = Color.parseColor(default_color);
        }

        Intent msgrcv = new Intent("Msg");
        msgrcv.putExtra("package", pack);
        msgrcv.putExtra("title", title);
        msgrcv.putExtra("text", text);
        msgrcv.putExtra("color", color);

        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(msgrcv);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {}
}