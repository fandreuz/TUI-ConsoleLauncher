package ohi.andre.consolelauncher.managers.xml.options;

import ohi.andre.consolelauncher.managers.AppsManager;
import ohi.andre.consolelauncher.managers.xml.XMLPrefsManager;

/**
 * Created by francescoandreuzzi on 24/09/2017.
 */

public enum Apps implements XMLPrefsManager.XMLPrefsSave {

    default_app_n1 {
        @Override
        public String defaultValue() {
            return MOST_USED;
        }
    },
    default_app_n2 {
        @Override
        public String defaultValue() {
            return MOST_USED;
        }
    },
    default_app_n3 {
        @Override
        public String defaultValue() {
            return "com.android.vending";
        }
    },
    default_app_n4 {
        @Override
        public String defaultValue() {
            return NULL;
        }
    },
    default_app_n5 {
        @Override
        public String defaultValue() {
            return NULL;
        }
    };

    public static final String MOST_USED = "most_used";
    public static final String NULL = "null";

    @Override
    public String label() {
        return name();
    }

    @Override
    public XMLPrefsManager.XmlPrefsElement parent() {
        return AppsManager.instance;
    }

    @Override
    public boolean is(String s) {
        return name().equals(s);
    }
}
