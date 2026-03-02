package ohi.andre.consolelauncher.managers.notifications.reply;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.RemoteInput;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXParseException;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ohi.andre.consolelauncher.BuildConfig;
import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.managers.xml.XMLPrefsManager;
import ohi.andre.consolelauncher.managers.xml.classes.XMLPrefsElement;
import ohi.andre.consolelauncher.managers.xml.classes.XMLPrefsList;
import ohi.andre.consolelauncher.managers.xml.classes.XMLPrefsSave;
import ohi.andre.consolelauncher.managers.xml.options.Reply;
import ohi.andre.consolelauncher.tuils.PrivateIOReceiver;
import ohi.andre.consolelauncher.tuils.Tuils;

import static ohi.andre.consolelauncher.managers.xml.XMLPrefsManager.VALUE_ATTRIBUTE;
import static ohi.andre.consolelauncher.managers.xml.XMLPrefsManager.set;
import static ohi.andre.consolelauncher.managers.xml.XMLPrefsManager.writeTo;

/**
 * Created by francescoandreuzzi on 17/01/2018.
 */

public class ReplyManager implements XMLPrefsElement {

    public static String PATH = "reply.xml";
    public static String NAME = "REPLY";
    public static String ACTION = BuildConfig.APPLICATION_ID + ".reply";
    public static String ID = "id";
    public static String WHAT = "what";
    public static String ACTION_UPDATE = BuildConfig.APPLICATION_ID + ".update";
    public static String ACTION_LS = BuildConfig.APPLICATION_ID + ".lsreplies";

    private static final String ID_ATTRIBUTE = "id";

    private Set<NotificationWear> notificationWears;
    public static List<BoundApp> boundApps;

    private BroadcastReceiver receiver;

    public static ReplyManager instance;
    private XMLPrefsList values;

    private boolean enabled;

    private Context context;

    public static int nextUsableId;

    @Override
    public String path() {
        return PATH;
    }

    public ReplyManager(Context context) {
        enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH;
        if(!enabled) return;

        notificationWears = new HashSet<>();
        values = new XMLPrefsList();
        this.context = context;

        instance = this;

        load(true);

        enabled = Boolean.parseBoolean(values.get(Reply.reply_enabled).value);
        if(!enabled) {
            notificationWears = null;
            boundApps = null;
        } else {
            IntentFilter filter = new IntentFilter();
            filter.addAction(ACTION);
            filter.addAction(ACTION_UPDATE);
            filter.addAction(ACTION_LS);

            receiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if(intent.getAction().equals(ACTION)) {
                        String app = intent.getStringExtra(ID);
                        String what = intent.getStringExtra(WHAT);

                        int id;
                        try {
                            id = Integer.parseInt(app);
                        } catch (Exception e) {
                            BoundApp bapp = findApp(app);
                            if(bapp == null) {
                                Tuils.sendOutput(context, context.getString(R.string.reply_app_not_found) + Tuils.SPACE + app);
                                return;
                            }

                            id = bapp.applicationId;
                        }

                        if(what == null) {
                            check(id);
                        } else {
                            if(id == -1) return;
                            replyTo(ReplyManager.this.context, id, what);
                        }
                    } else if(intent.getAction().equals(ACTION_UPDATE)) {
                        load(false);
                    } else if(intent.getAction().equals(ACTION_LS)) {
                        ls(context);
                    }
                }
            };

            LocalBroadcastManager.getInstance(context.getApplicationContext()).registerReceiver(receiver, filter);
        }
    }

    private void load(boolean loadPrefs) {
        if(boundApps != null) boundApps.clear();
        else boundApps = new ArrayList<>();

        List<Reply> enums = new ArrayList<>(Arrays.asList(Reply.values()));

        File file = new File(Tuils.getFolder(), PATH);

        Object[] o;
        try {
            o = XMLPrefsManager.buildDocument(file, NAME);
            if(o == null) {
                Tuils.sendXMLParseError(context, PATH);
                return;
            }
        } catch (SAXParseException e) {
            Tuils.sendXMLParseError(context, PATH, e);
            return;
        } catch (Exception e) {
            Tuils.log(e);
            return;
        }

        Document d = (Document) o[0];
        Element root = (Element) o[1];

        NodeList nodes = root.getElementsByTagName("*");

        PackageManager mgr = context.getPackageManager();

        try {
            for (int count = 0; count < nodes.getLength(); count++) {
                final Node node = nodes.item(count);
                String nn = node.getNodeName();

                if (Tuils.find(nn, enums) != -1) {
                    if(loadPrefs) {
                        values.add(nn, node.getAttributes().getNamedItem(VALUE_ATTRIBUTE).getNodeValue());

                        for (int en = 0; en < enums.size(); en++) {
                            if (enums.get(en).label().equals(nn)) {
                                enums.remove(en);
                                break;
                            }
                        }
                    }
                } else {
                    int id = XMLPrefsManager.getIntAttribute((Element) node, ID_ATTRIBUTE);

                    ApplicationInfo info;
                    try {
                        info = mgr.getApplicationInfo(nn, 0);
                    } catch (Exception e) {
                        Tuils.log(e);
                        continue;
                    }

                    String label = info.loadLabel(mgr).toString();
                    if (id != -1) boundApps.add(new BoundApp(id, nn, label));
                }
            }

            if (loadPrefs && enums.size() > 0) {
                for (XMLPrefsSave s : enums) {
                    String value = s.defaultValue();

                    Element em = d.createElement(s.label());
                    em.setAttribute(VALUE_ATTRIBUTE, value);
                    root.appendChild(em);

                    values.add(s.label(), value);
                }

                writeTo(d, file);
            }
        } catch (Exception e) {
            Tuils.log(e);
        }

        nextUsableId = nextUsableId();
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void onNotification(StatusBarNotification notification, CharSequence text) {
        if(!enabled) return;

        BoundApp app = findApp(notification.getPackageName());
        if(app == null) return;

        NotificationWear w = extractWearNotification(notification);
        if(w == null) return;

        NotificationWear old = findNotificationWear(app);

        if(old != null && (w.pendingIntent == null || w.remoteInputs == null || w.remoteInputs.length == 0)) return;
        if(old != null) notificationWears.remove(old);

        w.text = text;
        w.app = app;

        notificationWears.add(w);
    }

    private void replyTo(Context context, int applicationId, String what) {
        if(!enabled) return;

        BoundApp app = findApp(applicationId);
        if(app == null) {
            Tuils.sendOutput(context, context.getString(R.string.reply_id_not_found) + Tuils.SPACE + applicationId);
            return;
        }

        NotificationWear wear = findNotificationWear(applicationId);
        if(wear != null) replyTo(context, wear, what);
        else Tuils.sendOutput(context, R.string.reply_notification_not_found);
    }

    @TargetApi(Build.VERSION_CODES.KITKAT_WATCH)
    private void replyTo(Context context, NotificationWear notificationWear, String what) {
        RemoteInput[] remoteInputs = notificationWear.remoteInputs;

        Bundle localBundle = notificationWear.bundle;

        Intent i = new Intent(PrivateIOReceiver.ACTION_REPLY);
        i.putExtra(PrivateIOReceiver.BUNDLE, localBundle);
        i.putExtra(PrivateIOReceiver.REMOTE_INPUTS, remoteInputs);
        i.putExtra(PrivateIOReceiver.TEXT, what);
        i.putExtra(PrivateIOReceiver.PENDING_INTENT, notificationWear.pendingIntent);
        i.putExtra(PrivateIOReceiver.ID, notificationWear.id);
        i.putExtra(PrivateIOReceiver.CURRENT_ID, PrivateIOReceiver.currentId);

        LocalBroadcastManager.getInstance(context.getApplicationContext()).sendBroadcast(i);
    }

    @TargetApi(Build.VERSION_CODES.KITKAT_WATCH)
    private NotificationWear extractWearNotification(StatusBarNotification statusBarNotification) {
        NotificationWear notificationWear = new NotificationWear();

        Notification.WearableExtender wearableExtender = new Notification.WearableExtender(statusBarNotification.getNotification());
        for(Notification.Action action : wearableExtender.getActions()) {
            RemoteInput[] rs = action.getRemoteInputs();
            if(rs != null && rs.length > 0) {
                notificationWear.remoteInputs = rs;
//                Actually I assume that there's only one action
                notificationWear.pendingIntent = action.actionIntent;
                break;
            }
        }

        notificationWear.bundle = statusBarNotification.getNotification().extras;
        notificationWear.id = statusBarNotification.getId();

        return notificationWear;
    }

    private BoundApp findApp(int applicationId) {
        if(boundApps != null) {
            for(BoundApp a : boundApps) {
                if(a.applicationId == applicationId) return a;
            }
        }

        return null;
    }

    private BoundApp findApp(String pkg) {
        if(boundApps != null) {
            for(BoundApp a : boundApps) {
                if(a.packageName.equals(pkg)) return a;
            }
        }

        return null;
    }

    private NotificationWear findNotificationWear(BoundApp bapp) {
        for(NotificationWear h : notificationWears) {
            if(h.app != null && h.app.packageName.equals(bapp.packageName)) return h;
        }
        return null;
    }

    private NotificationWear findNotificationWear(int id) {
        for(NotificationWear h : notificationWears) {
            if(h.app != null && h.app.applicationId == id) return h;
        }
        return null;
    }

    public void dispose(Context context) {
        try {
            LocalBroadcastManager.getInstance(context.getApplicationContext()).unregisterReceiver(receiver);
        } catch (Exception e) {}

        if(notificationWears != null) {
            notificationWears.clear();
            notificationWears = null;
        }
        if(boundApps != null) {
            boundApps.clear();
            boundApps = null;
        }
        if(values != null) {
            values.list.clear();
            values = null;
        }

        instance = null;
    }

    @Override
    public XMLPrefsList getValues() {
        return values;
    }

    @Override
    public void write(XMLPrefsSave save, String value) {
        set(new File(Tuils.getFolder(), PATH), save.label(), new String[] {VALUE_ATTRIBUTE}, new String[] {value});
    }

    @Override
    public String[] delete() {
        return null;
    }

    public void check(int id) {
        if(!enabled) return;

        BoundApp app = findApp(id);
        if(app == null) {
            Tuils.sendOutput(context, context.getString(R.string.reply_id_not_found) + Tuils.SPACE + id);
            return;
        }

        NotificationWear wear = findNotificationWear(app);
        if(wear == null) {
            Tuils.sendOutput(context, R.string.reply_notification_not_found);
            return;
        }

        Tuils.sendOutput(context, wear.text);
    }

    public static String bind(String pkg) {
        return XMLPrefsManager.set(new File(Tuils.getFolder(), PATH), pkg, new String[] {ID_ATTRIBUTE}, new String[] {String.valueOf(nextUsableId)});
    }

    public static String unbind(String pkg) {
        return XMLPrefsManager.removeNode(new File(Tuils.getFolder(), PATH), pkg);
    }

    private int nextUsableId() {
        int nextUsableID = 0;
        while (true) {
            boolean shouldRestart = false;

            for(BoundApp b : boundApps) {
                if(b.applicationId == nextUsableID) {
                    shouldRestart = true;
                    break;
                }
            }

            if(!shouldRestart) return nextUsableID;

            nextUsableID++;
        }
    }

    public void ls(Context c) {
        if(!enabled) return;

        StringBuilder builder = new StringBuilder();
        if(instance != null) {
            for(BoundApp a : boundApps) builder.append(a.packageName).append(" -> ").append(a.applicationId).append(Tuils.NEWLINE);
        }
        String s = builder.toString();
        if(s.length() == 0) s = "[]";

        Tuils.sendOutput(context, s);
    }

//    private static class NotificationHolder {
//        BindedApp app;
//
//        List<RemoteInput> remoteInputs;
//        Bundle bundle;
//        PendingIntent pendingIntent;
//
//        public NotificationHolder(BindedApp app, List<RemoteInput> remoteInputs, Bundle bundle, PendingIntent pendingIntent) {
//            this.app = app;
//            this.remoteInputs = remoteInputs;
//            this.bundle = bundle;
//            this.pendingIntent = pendingIntent;
//        }
//
//        @Override
//        public boolean equals(Object obj) {
//            NotificationHolder h = (NotificationHolder) obj;
//            return h.app.equals(app);
//        }
//    }
}
