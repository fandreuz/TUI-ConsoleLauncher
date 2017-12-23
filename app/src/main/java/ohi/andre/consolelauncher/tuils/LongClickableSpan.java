package ohi.andre.consolelauncher.tuils;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Vibrator;
import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;

import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.managers.notifications.NotificationManager;
import ohi.andre.consolelauncher.managers.notifications.NotificationService;

/**
 * Created by francescoandreuzzi on 22/10/2017.
 */

public class LongClickableSpan extends ClickableSpan {

    public static int longPressVibrateDuration = -1;

    private Object clickO, longClickO;
    private String longIntentKey;

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

    @Override
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

    private static boolean execute(View v, Object o, String intentKey) {
        if(o == null) return false;

        if(o instanceof String) {
            Intent intent = new Intent(intentKey != null ? intentKey : InputOutputReceiver.ACTION_CMD);
            intent.putExtra(InputOutputReceiver.SHOW_CONTENT, false);
            intent.putExtra(InputOutputReceiver.TEXT, (String) o);
            v.getContext().sendBroadcast(intent);
        } else if(o instanceof PendingIntent) {
            PendingIntent pi = (PendingIntent) o;

            try {
                pi.send();
            } catch (PendingIntent.CanceledException e) {
                Tuils.log(e);
            }
        } else if(o instanceof NotificationService.Notification) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
                PopupMenu menu = new PopupMenu(v.getContext().getApplicationContext(), v);
                menu.getMenuInflater().inflate(R.menu.notification_menu, menu.getMenu());

                final NotificationService.Notification n = (NotificationService.Notification) o;

                menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {

                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        int id = item.getItemId();

                        switch (id) {
                            case R.id.exclude_app:
                                NotificationManager.setState(n.pkg, false);
                                break;
                            case R.id.exclude_notification:
                                NotificationManager.addFilter(n.text, -1);
                                break;
                            default:
                                return false;
                        }

                        return true;
                    }
                });

                menu.show();
            }
        }

        return true;
    }
}

