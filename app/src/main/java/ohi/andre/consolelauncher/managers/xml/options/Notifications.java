package ohi.andre.consolelauncher.managers.xml.options;

import ohi.andre.consolelauncher.managers.notifications.NotificationManager;
import ohi.andre.consolelauncher.managers.xml.classes.XMLPrefsElement;
import ohi.andre.consolelauncher.managers.xml.classes.XMLPrefsSave;

/**
 * Created by francescoandreuzzi on 24/09/2017.
 */

public enum Notifications implements XMLPrefsSave {

    show_notifications {
        @Override
        public String defaultValue() {
            return "false";
        }

        @Override
        public String info() {
            return "If true, t-ui will show every incoming notification";
        }
    },
    app_notification_enabled_default {
        @Override
        public String defaultValue() {
            return "true";
        }

        @Override
        public String info() {
            return "If true, t-ui will show notifications from all apps, unless they are explicitly excluded. If false, t-ui won't show a notification from a specific app unless it was \texplicitly included";
        }
    },
    default_notification_color {
        @Override
        public String defaultValue() {
            return "#00FF00";
        }

        @Override
        public String type() {
            return XMLPrefsSave.COLOR;
        }

        @Override
        public String info() {
            return "The default color";
        }
    },
    notification_format {
        @Override
        public String defaultValue() {
            return "[%t] %pkg: %[100][teal]title --- %text";
        }

        @Override
        public String type() {
            return XMLPrefsSave.TEXT;
        }

        @Override
        public String info() {
            return "The default format";
        }
    },
    click_notification {
        @Override
        public String defaultValue() {
            return "true";
        }

        @Override
        public String info() {
            return "If true, T-UI will perform the operation associated with the original notification when you click it";
        }
    },
    long_click_notification {
        @Override
        public String defaultValue() {
            return "true";
        }

        @Override
        public String info() {
            return "If true, you will be able to perform some quick operations long-clicking a notification";
        }
    },
    notification_popup_exclude_app {
        @Override
        public String defaultValue() {
            return "true";
        }

        @Override
        public String info() {
            return "If false, the \"Exclude app\" option won\'t be shown in the long click popup menu";
        }

        @Override
        public String type() {
            return XMLPrefsSave.BOOLEAN;
        }
    },
    notification_popup_exclude_notification {
        @Override
        public String defaultValue() {
            return "true";
        }

        @Override
        public String info() {
            return "If false, the \"Exclude notification\" option won\'t be shown in the long click popup menu";
        }

        @Override
        public String type() {
            return XMLPrefsSave.BOOLEAN;
        }
    },
    notification_popup_reply {
        @Override
        public String defaultValue() {
            return "true";
        }

        @Override
        public String info() {
            return "If false, the \"Reply to the last notification\" option won\'t be shown in the long click popup menu";
        }

        @Override
        public String type() {
            return XMLPrefsSave.BOOLEAN;
        }
    };

    @Override
    public XMLPrefsElement parent() {
        return NotificationManager.instance;
    }

    @Override
    public String label() {
        return name();
    }

    @Override
    public String type() {
        return XMLPrefsSave.BOOLEAN;
    }

    @Override
    public String[] invalidValues() {
        return null;
    }

    @Override
    public String getLowercaseString() {
        return label();
    }

    @Override
    public String getString() {
        return label();
    }
}
