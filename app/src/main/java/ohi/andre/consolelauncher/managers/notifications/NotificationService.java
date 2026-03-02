package ohi.andre.consolelauncher.managers.notifications;

/**
 * Created by francescoandreuzzi on 27/04/2017.
 */

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import androidx.core.app.NotificationCompat;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ohi.andre.consolelauncher.managers.TerminalManager;
import ohi.andre.consolelauncher.managers.TimeManager;
import ohi.andre.consolelauncher.managers.notifications.reply.ReplyManager;
import ohi.andre.consolelauncher.managers.xml.XMLPrefsManager;
import ohi.andre.consolelauncher.managers.xml.options.Behavior;
import ohi.andre.consolelauncher.managers.xml.options.Notifications;
import ohi.andre.consolelauncher.tuils.StoppableThread;
import ohi.andre.consolelauncher.tuils.Tuils;


@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class NotificationService extends NotificationListenerService {

    public static final String DESTROY = "destroy";

    private final int UPDATE_TIME = 2000;
    private String LINES_LABEL = "Lines";
    private String ANDROID_LABEL_PREFIX = "android.";
    private String NULL_LABEL = "null";

    HashMap<String, List<Notification>> pastNotifications;
    Handler handler = new Handler();

    String format;
    int color, maxOptionalDepth;
    boolean enabled, click, longClick, active;

    Queue<StatusBarNotification> queue;

    final String PKG = "%pkg", APP = "%app", NEWLINE = "%n";
    final Pattern timePattern = Pattern.compile("^%t[0-9]*$");

    PackageManager manager;
    ReplyManager replyManager;
    NotificationManager notificationManager;

    private final Pattern formatPattern = Pattern.compile("%(?:\\[(\\d+)\\])?(?:\\[([^]]+)\\])?(?:(?:\\{)([a-zA-Z\\.\\:\\s]+)(?:\\})|([a-zA-Z\\.\\:]+))");

    StoppableThread bgThread;

    @Override
    public void onCreate() {
        super.onCreate();

        init();
    }

    private void init() {
        try {
            notificationManager = NotificationManager.create(this);
            XMLPrefsManager.loadCommons(this);
        } catch (Exception e) {
            Tuils.log(e);
            return;
        }

        try {
            replyManager = new ReplyManager(this);
        } catch (VerifyError error) {
            replyManager = null;
        }

        bgThread = new StoppableThread() {
            @Override
            public void run() {
                super.run();

                if(!enabled) return;

                while(true) {
                    if(isInterrupted()) return;

                    if(queue != null) {

                        StatusBarNotification sbn;
                        while ((sbn = queue.poll()) != null) {

                            android.app.Notification notification = sbn.getNotification();
                            if (notification == null) {
                                continue;
                            }

                            String pack = sbn.getPackageName();

                            String appName;
                            try {
                                appName = manager.getApplicationInfo(pack, 0).loadLabel(manager).toString();
                            } catch (PackageManager.NameNotFoundException e) {
                                appName = "null";
                            }

                            NotificationManager.NotificatedApp nApp = notificationManager.getAppState(pack);
                            if ((nApp != null && !nApp.enabled)) {
                                continue;
                            }

                            if (nApp == null && !notificationManager.default_app_state) {
                                continue;
                            }

                            String f;
                            if(nApp != null && nApp.format != null) f = nApp.format;
                            else f = format;

                            int textColor;
                            if(nApp != null && nApp.color != null) textColor = Color.parseColor(nApp.color);
                            else textColor = color;

                            CharSequence s = Tuils.span(f, textColor);

                            Bundle bundle = NotificationCompat.getExtras(notification);

                            if(bundle != null) {
                                Matcher m = formatPattern.matcher(s);
                                String match;
                                while(m.find()) {
                                    match = m.group(0);
                                    if (!match.startsWith(PKG) && !match.startsWith(APP) && !match.startsWith(NEWLINE) && !timePattern.matcher(match).matches()) {
                                        String length = m.group(1);
                                        String color = m.group(2);
                                        String value = m.group(3);

                                        if(value == null || value.length() == 0) value = m.group(4);

                                        if(value != null) value = value.trim();
                                        else continue;

                                        if(value.length() == 0) continue;

                                        if(value.equals("ttl")) value = "title";
                                        else if(value.equals("txt")) value = "text";

                                        String[] temp = value.split(":"), split;
//                                    this is an other way to do what I did in NotesManager for footer/header
                                        if(value.endsWith(":")) {
                                            split = new String[temp.length + 1];
                                            System.arraycopy(temp, 0, split, 0, temp.length);
                                            split[split.length - 1] = Tuils.EMPTYSTRING;
                                        } else split = temp;

//                                    because the last one is the default text, but only if there is more than one label
                                        int stopAt = split.length;
                                        if(stopAt > 1) stopAt--;

                                        CharSequence text = null;
                                        for(int j = 0; j < stopAt; j++) {
                                            if(split[j].contains(LINES_LABEL)) {
                                                CharSequence[] array = bundle.getCharSequenceArray(ANDROID_LABEL_PREFIX + split[j]);
                                                if(array != null) {
                                                    for(CharSequence c : array) {
                                                        if(text == null) text = c;
                                                        else text = TextUtils.concat(text, Tuils.NEWLINE, c);
                                                    }
                                                }
                                            } else {
                                                text = bundle.getCharSequence(ANDROID_LABEL_PREFIX + split[j]);
                                            }

                                            if(text != null && text.length() > 0) break;
                                        }

                                        if(text == null || text.length() == 0) {
                                            text = split.length == 1 ? NULL_LABEL : split[split.length - 1];
                                        }

                                        String stringed = text.toString().trim();

                                        try {
                                            int l = Integer.parseInt(length);
                                            stringed = stringed.substring(0,l);
                                        } catch (Exception e) {}

                                        try {
                                            text = Tuils.span(stringed, Color.parseColor(color));
                                        } catch (Exception e) {
                                            text = stringed;
                                        }

                                        s = TextUtils.replace(s, new String[] {m.group(0)}, new CharSequence[] {text});
                                    }
                                }
                            }

                            String text = s.toString();

                            if(notificationManager.match(text)) continue;

                            int found = isInPastNotifications(pack, text);
//                        if(found == 0) {
//                            Tuils.log("app " + pack, pastNotifications.get(pack).toString());
//                        }

                            if(found == 2) continue;

//                        else
                            Notification n = new Notification(System.currentTimeMillis(), text, pack, notification.contentIntent);

                            if(found == 1) {
                                List<Notification> ns = new ArrayList<>();
                                ns.add(n);
                                pastNotifications.put(pack, ns);
                            } else if(found == 0) {
                                pastNotifications.get(pack).add(n);
                            }

                            s = TextUtils.replace(s, new String[]{PKG, APP, NEWLINE}, new CharSequence[]{pack, appName, Tuils.NEWLINE});
                            String st = s.toString();
                            while (st.contains(NEWLINE)) {
                                s = TextUtils.replace(s,
                                        new String[]{NEWLINE},
                                        new CharSequence[]{Tuils.NEWLINE});
                                st = s.toString();
                            }

                            try {
                                s = TimeManager.instance.replace(s);
                            } catch (Exception e) {
                                Tuils.log(e);
                            }

//                        Tuils.log("text", text);
//                        Tuils.log("--------");

                            Tuils.sendOutput(NotificationService.this.getApplicationContext(), s, TerminalManager.CATEGORY_NO_COLOR, click ? notification.contentIntent : null, longClick ? n : null);

                            if(replyManager != null) replyManager.onNotification(sbn, s);
                        }
                    }

                    try {
                        sleep(UPDATE_TIME);
                    } catch (InterruptedException e) {
                        Tuils.log(e);
                        return;
                    }
                }
            }
        };

        manager = getPackageManager();
        enabled = XMLPrefsManager.getBoolean(Notifications.show_notifications) || XMLPrefsManager.get(Notifications.show_notifications).equalsIgnoreCase("enabled");

        pastNotifications = new HashMap<>();

        format = XMLPrefsManager.get(Notifications.notification_format);
        color = XMLPrefsManager.getColor(Notifications.default_notification_color);

        click = XMLPrefsManager.getBoolean(Notifications.click_notification);
        longClick = XMLPrefsManager.getBoolean(Notifications.long_click_notification);

        maxOptionalDepth = XMLPrefsManager.getInt(Behavior.max_optional_depth);

        handler.post(new Runnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();

                for (Map.Entry<String, List<Notification>> entry : pastNotifications.entrySet()) {
                    List<Notification> notifications = entry.getValue();

                    Iterator<Notification> it = notifications.iterator();
                    while (it.hasNext()) {
                        if (now - it.next().time >= UPDATE_TIME) it.remove();
                    }
                }

                handler.postDelayed(this, UPDATE_TIME);
            }
        });

        queue = new ArrayBlockingQueue<>(5);
        bgThread.start();

        active = true;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent != null) {
            boolean destroy = intent.getBooleanExtra(DESTROY, false);
            if(destroy) dispose();
        }

        if(!active) init();

        return START_STICKY;
    }

    private void dispose() {
        if(replyManager != null) {
            replyManager.dispose(this);
            replyManager = null;
        }

        if(notificationManager != null) {
            notificationManager.dispose();
            notificationManager = null;
        }

        bgThread.interrupt();
        bgThread = null;

        if(pastNotifications != null) {
            pastNotifications.clear();
            pastNotifications = null;
        }

        if(queue != null) {
            queue.clear();
            queue = null;
        }

        active = false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

//        ondestroy won't ever be called
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if(!enabled) return;

        queue.offer(sbn);
    }

//    0 = not found
//    1 = the app wasnt found -> this is the first notification from this app
//    2 = found
    private int isInPastNotifications(String pkg, String text) {
        try {
            List<Notification> notifications = pastNotifications.get(pkg);
            if(notifications == null) return 1;
            for(Notification n : notifications) if(n.text.equals(text)) return 2;
        } catch (ConcurrentModificationException e) {}
        return 0;
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {}

    public static class Notification implements Parcelable {
        public long time;
        public String text, pkg;
        public PendingIntent pendingIntent;

        public Notification(long time, String text, String pkg, PendingIntent pi) {
            this.time = time;
            this.text = text;
            this.pkg = pkg;
            this.pendingIntent = pi;
        }

        protected Notification(Parcel in) {
            time = in.readLong();
            text = in.readString();
            pkg = in.readString();
            pendingIntent = in.readParcelable(PendingIntent.class.getClassLoader());
        }

        public static final Creator<Notification> CREATOR = new Creator<Notification>() {
            @Override
            public Notification createFromParcel(Parcel in) {
                return new Notification(in);
            }

            @Override
            public Notification[] newArray(int size) {
                return new Notification[size];
            }
        };

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeLong(time);
            dest.writeString(text);
            dest.writeString(pkg);
            dest.writeParcelable(pendingIntent, flags);
        }
    }
}