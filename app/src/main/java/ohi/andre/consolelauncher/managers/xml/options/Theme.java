package ohi.andre.consolelauncher.managers.xml.options;

import ohi.andre.consolelauncher.managers.xml.XMLPrefsManager;

/**
 * Created by francescoandreuzzi on 24/09/2017.
 */

public enum Theme implements XMLPrefsManager.XMLPrefsSave {

    input_color {
        @Override
        public String defaultValue() {
            return "#ff00ff00";
        }
    },
    output_color {
        @Override
        public String defaultValue() {
            return "#ffffffff";
        }
    },
    bg_color {
        @Override
        public String defaultValue() {
            return "#ff000000";
        }
    },
    device_color {
        @Override
        public String defaultValue() {
            return "#ffff9800";
        }
    },
    battery_color_high {
        @Override
        public String defaultValue() {
            return "#4CAF50";
        }
    },
    battery_color_medium {
        @Override
        public String defaultValue() {
            return "#FFEB3B";
        }
    },
    battery_color_low {
        @Override
        public String defaultValue() {
            return "#FF5722";
        }
    },
    time_color {
        @Override
        public String defaultValue() {
            return "#03A9F4";
        }
    },
    storage_color {
        @Override
        public String defaultValue() {
            return "#9C27B0";
        }
    },
    ram_color {
        @Override
        public String defaultValue() {
            return "#fff44336";
        }
    },
    network_info_color {
        @Override
        public String defaultValue() {
            return "#FFCA28";
        }
    },
    toolbar_bg {
        @Override
        public String defaultValue() {
            return "#00000000";
        }
    },
    toolbar_color {
        @Override
        public String defaultValue() {
            return "#ffff0000";
        }
    },
    enter_color {
        @Override
        public String defaultValue() {
            return "#ffffffff";
        }
    },
    overlay_color {
        @Override
        public String defaultValue() {
            return "#80000000";
        }
    },
    alias_content_color {
        @Override
        public String defaultValue() {
            return "#1DE9B6";
        }
    },
    statusbar_color {
        @Override
        public String defaultValue() {
            return "#000000";
        }
    },
    navigationbar_color {
        @Override
        public String defaultValue() {
            return "#000000";
        }
    },
    app_installed_color {
        @Override
        public String defaultValue() {
            return "#FF7043";
        }
    },
    app_uninstalled_color {
        @Override
        public String defaultValue() {
            return "#FF7043";
        }
    },
    hint_color {
        @Override
        public String defaultValue() {
            return "#4CAF50";
        }
    },
    rss_color {
        @Override
        public String defaultValue() {
            return null;
        }
    },
    mark_color {
        @Override
        public String defaultValue() {
            return "#CDDC39";
        }
    };

    @Override
    public XMLPrefsManager.XmlPrefsElement parent() {
        return XMLPrefsManager.XMLPrefsRoot.THEME;
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