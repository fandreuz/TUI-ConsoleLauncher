package ohi.andre.consolelauncher.tuils;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Vibrator;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;

import ohi.andre.consolelauncher.MainManager;
import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.managers.notifications.NotificationManager;
import ohi.andre.consolelauncher.managers.notifications.NotificationService;
import ohi.andre.consolelauncher.managers.xml.XMLPrefsManager;
import ohi.andre.consolelauncher.managers.xml.options.Notifications;

/**
 * Created by francescoandreuzzi on 22/10/2017.
 */

public class LongClickableSpan extends ClickableSpan {

    public static int longPressVibrateDuration = -1;

    private Object clickO, longClickO;
    private String longIntentKey;

    private static boolean set = false, showMenu;
    private static boolean showExcludeApp, showExcludeNotification, showReply;

    public LongClickableSpan(Object clickAction, Object longClickAction) {
        this.clickO = clickAction;
        this.longClickO = longClickAction;
        this.longIntentKey = null;
    }

    public LongClickableSpan(Object clickAction) {
        this.clickO = clickAction;
        this.longClickO = null;
        this.longIntentKey = null;
    }

    public LongClickableSpan(Object clickAction, Object longClickAction, String longIntentKey) {
        this.clickO = clickAction;
        this.longClickO = longClickAction;
        this.longIntentKey = longIntentKey;
    }

    public LongClickableSpan(Object clickAction, String longIntentKey) {
        this.clickO = clickAction;
        this.longClickO = null;
        this.longIntentKey = longIntentKey;
    }

    public LongClickableSpan(String longIntentKey) {
        this.clickO = null;
        this.longClickO = null;
        this.longIntentKey = longIntentKey;
    }

    public void updateDrawState(TextPaint ds) {}

    @Override
    public void onClick(View widget) {
        execute(widget, clickO);
    }

    public void onLongClick(View widget) {
        if(execute(widget, longClickO, longIntentKey) && longPressVibrateDuration > 0) ((Vibrator) widget.getContext().getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE)).vibrate(longPressVibrateDuration);
    }

    private static boolean execute(View v, Object o) {
        return execute(v, o, null);
    }

    private static boolean execute(final View v, Object o, String intentKey) {
        if(o == null) return false;

        if(!set) {
            set = true;

            showExcludeApp = XMLPrefsManager.getBoolean(Notifications.notification_popup_exclude_app);
            showExcludeNotification = XMLPrefsManager.getBoolean(Notifications.notification_popup_exclude_notification);
            showReply = XMLPrefsManager.getBoolean(Notifications.notification_popup_reply);

            showMenu = (showExcludeApp && showExcludeNotification) || (showExcludeApp && showReply) || (showExcludeNotification && showReply);
        }

        if(o instanceof String) {
            Intent intent = new Intent(intentKey != null ? intentKey : MainManager.ACTION_EXEC);
            intent.putExtra(PrivateIOReceiver.TEXT, (String) o);

            if(intentKey == null || intentKey.equals(MainManager.ACTION_EXEC)) {
                intent.putExtra(MainManager.NEED_WRITE_INPUT, false);
                intent.putExtra(MainManager.CMD_COUNT, MainManager.commandCount);
            }

            LocalBroadcastManager.getInstance(v.getContext().getApplicationContext()).sendBroadcast(intent);
        } else if(o instanceof PendingIntent) {
            PendingIntent pi = (PendingIntent) o;

            try {
                pi.send();
            } catch (PendingIntent.CanceledException e) {
                Tuils.log(e);
            }
        } else if(o instanceof Uri) {
            Intent i = new Intent(Intent.ACTION_VIEW, (Uri) o);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            try {
                v.getContext().startActivity(i);
            } catch (Exception e) {
                Tuils.sendOutput(Color.RED, v.getContext(), e.toString());
            }
        } else if(o instanceof NotificationService.Notification) {
            final NotificationService.Notification n = (NotificationService.Notification) o;

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
                if(showMenu) {
                    PopupMenu menu = new PopupMenu(v.getContext().getApplicationContext(), v);
                    menu.getMenuInflater().inflate(R.menu.notification_menu, menu.getMenu());

                    menu.getMenu().findItem(R.id.exclude_app).setVisible(showExcludeApp);
                    menu.getMenu().findItem(R.id.exclude_notification).setVisible(showExcludeNotification);
                    menu.getMenu().findItem(R.id.reply_notification).setVisible(showReply);

                    menu.setOnMenuItemClickListener(item -> {
                        int id = item.getItemId();

                        switch (id) {
                            case R.id.exclude_app:
                                NotificationManager.setState(n.pkg, false);
                                break;
                            case R.id.exclude_notification:
                                Tuils.log(n.text);
                                NotificationManager.addFilter(n.text, -1);
                                break;
                            case R.id.reply_notification:
                                Intent intent = new Intent(PrivateIOReceiver.ACTION_INPUT);
                                intent.putExtra(PrivateIOReceiver.TEXT, "reply -to " + n.pkg + Tuils.SPACE);

                                LocalBroadcastManager.getInstance(v.getContext().getApplicationContext()).sendBroadcast(intent);
                            default:
                                return false;
                        }

                        return true;
                    });

                    menu.show();
                } else {
                    if(showReply) {
                        Intent intent = new Intent(PrivateIOReceiver.ACTION_INPUT);
                        intent.putExtra(PrivateIOReceiver.TEXT, "reply -to " + n.pkg + Tuils.SPACE);

                        LocalBroadcastManager.getInstance(v.getContext().getApplicationContext()).sendBroadcast(intent);
                    }
                    else if(showExcludeNotification) NotificationManager.addFilter(n.text, -1);
                    else if(showExcludeApp) NotificationManager.setState(n.pkg, false);
                }
            }
        }

        return true;
    }
}

