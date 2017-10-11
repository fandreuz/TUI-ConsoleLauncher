package ohi.andre.consolelauncher.managers.xml.options;

import ohi.andre.consolelauncher.managers.notifications.NotificationManager;
import ohi.andre.consolelauncher.managers.xml.XMLPrefsManager;

/**
 * Created by francescoandreuzzi on 24/09/2017.
 */

public enum Notifications implements XMLPrefsManager.XMLPrefsSave {

    show_notifications {
        @Override
        public String defaultValue() {
            return "false";
        }
    },
    app_notification_enabled_default {
        @Override
        public String defaultValue() {
            return "true";
        }
    },
    default_notification_color {
        @Override
        public String defaultValue() {
            return "#00FF00";
        }
    },
    notification_format {
        @Override
        public String defaultValue() {
            return "[%t] %pkg: %ttl --- %txt";
        }
    };

    @Override
    public XMLPrefsManager.XmlPrefsElement parent() {
        return NotificationManager.instance;
    }

    @Override
    public String label() {
        return name();
    }

    @Override
    public boolean is(String s) {
        return name().equals(s);
    }
}
