package ohi.andre.consolelauncher.managers.notifications;

/**
 * Created by francescoandreuzzi on 27/04/2017.
 */

import android.annotation.TargetApi;
import android.app.Notification;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ohi.andre.consolelauncher.managers.XMLPrefsManager;
import ohi.andre.consolelauncher.tuils.TimeManager;
import ohi.andre.consolelauncher.tuils.Tuils;

import static ohi.andre.consolelauncher.managers.notifications.NotificationManager.NotificatedApp;
import static ohi.andre.consolelauncher.managers.notifications.NotificationManager.default_color;


@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class NotificationService extends NotificationListenerService {

    private final int UPDATE_TIME = 1500;

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
                    if(time - entry.getValue() > 1500) recentNotifications.remove(entry.getKey());
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
    public void onDestroy() {
        super.onDestroy();
    }

    String format;
    int timeColor;

    final Pattern patternPkg = Pattern.compile("%pkg", Pattern.CASE_INSENSITIVE | Pattern.LITERAL);
    final Pattern patternText = Pattern.compile("%txt", Pattern.CASE_INSENSITIVE | Pattern.LITERAL);
    final Pattern patternTitle = Pattern.compile("%ttl", Pattern.CASE_INSENSITIVE | Pattern.LITERAL);
    final Pattern patternAppname = Pattern.compile("%app", Pattern.CASE_INSENSITIVE | Pattern.LITERAL);
    final Pattern patternNewline = Pattern.compile("%n", Pattern.CASE_INSENSITIVE | Pattern.LITERAL);

    PackageManager manager;

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {

        if(recentNotifications.containsKey(sbn.getPackageName())) return;
        recentNotifications.put(sbn.getPackageName(), System.currentTimeMillis());

        Notification notification = sbn.getNotification();
        if (notification == null) {
            return;
        }

        if(format == null) {
            manager = getPackageManager();
            format = NotificationManager.getFormat();
            timeColor = XMLPrefsManager.getColor(XMLPrefsManager.Theme.time_color);
        }

        String pack = sbn.getPackageName();

        String appName;
        try {
            appName = manager.getApplicationInfo(pack, 0).loadLabel(manager).toString();
        } catch (PackageManager.NameNotFoundException e) {
            appName = "null";
        }

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

        if(title == null) title = "null";
        if(text == null) text = "null";

        if(NotificationManager.match(pack, text, title)) return;

        int color;
        try {
            color = Color.parseColor(nApp.color);
        } catch (Exception e) {
            color = Color.parseColor(default_color);
        }

        String finalText = format;
        finalText = patternPkg.matcher(finalText).replaceAll(Matcher.quoteReplacement(pack));
        finalText = patternAppname.matcher(finalText).replaceAll(Matcher.quoteReplacement(appName));
        finalText = patternText.matcher(finalText).replaceAll(Matcher.quoteReplacement(text));
        finalText = patternTitle.matcher(finalText).replaceAll(Matcher.quoteReplacement(title));
        finalText = patternNewline.matcher(finalText).replaceAll(Matcher.quoteReplacement(Tuils.NEWLINE));

        SpannableString spannableString = new SpannableString(finalText);
        spannableString.setSpan(new ForegroundColorSpan(color), 0, finalText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        CharSequence s;
        try {
            s = TimeManager.replace(spannableString, timeColor);
        } catch (Exception e) {
            return;
        }

        Intent msgrcv = new Intent("Msg");
        msgrcv.putExtra("text", s);

        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(msgrcv);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {}
}