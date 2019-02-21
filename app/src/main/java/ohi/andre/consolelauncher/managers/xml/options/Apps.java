package ohi.andre.consolelauncher.managers.xml.options;

import ohi.andre.consolelauncher.managers.AppsManager;
import ohi.andre.consolelauncher.managers.xml.classes.XMLPrefsElement;
import ohi.andre.consolelauncher.managers.xml.classes.XMLPrefsSave;

/**
 * Created by francescoandreuzzi on 24/09/2017.
 */

public enum Apps implements XMLPrefsSave {

    default_app_n1 {
        @Override
        public String defaultValue() {
            return MOST_USED;
        }

        @Override
        public String info() {
            return "The first default-suggested app";
        }
    },
    default_app_n2 {
        @Override
        public String defaultValue() {
            return MOST_USED;
        }

        @Override
        public String info() {
            return "The second default-suggested app";
        }
    },
    default_app_n3 {
        @Override
        public String defaultValue() {
            return "com.android.vending";
        }

        @Override
        public String info() {
            return "The third default-suggested app";
        }
    },
    default_app_n4 {
        @Override
        public String defaultValue() {
            return NULL;
        }

        @Override
        public String info() {
            return "The fourth default-suggested app";
        }
    },
    default_app_n5 {
        @Override
        public String defaultValue() {
            return NULL;
        }

        @Override
        public String info() {
            return "The fifth default-suggested app";
        }
    },
    app_groups_sorting {
        @Override
        public String defaultValue() {
            return "2";
        }

        @Override
        public String info() {
            return "0 = time up->down; 1 = time down->up; 2 = alphabetical up->down; 3 = alphabetical down->up; 4 = most used up->down; 5 = most used down->up";
        }

        @Override
        public String type() {
            return XMLPrefsSave.INTEGER;
        }
    };

    public static final String MOST_USED = "most_used";
    public static final String NULL = "null";

    @Override
    public String label() {
        return name();
    }

    @Override
    public XMLPrefsElement parent() {
        return AppsManager.instance;
    }

    @Override
    public String type() {
        return XMLPrefsSave.APP;
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
