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

        @Override
        public String info() {
            return "Input color";
        }
    },
    output_color {
        @Override
        public String defaultValue() {
            return "#ffffffff";
        }

        @Override
        public String info() {
            return "Output color";
        }
    },
    bg_color {
        @Override
        public String defaultValue() {
            return "#ff000000";
        }

        @Override
        public String info() {
            return "Background color";
        }
    },
    device_color {
        @Override
        public String defaultValue() {
            return "#ffff9800";
        }

        @Override
        public String info() {
            return "Device label color";
        }
    },
    battery_color_high {
        @Override
        public String defaultValue() {
            return "#4CAF50";
        }

        @Override
        public String info() {
            return "Battery label color when the battery level is high";
        }
    },
    battery_color_medium {
        @Override
        public String defaultValue() {
            return "#FFEB3B";
        }

        @Override
        public String info() {
            return "Battery label color when the battery level is medium";
        }
    },
    battery_color_low {
        @Override
        public String defaultValue() {
            return "#FF5722";
        }

        @Override
        public String info() {
            return "Battery label color when the battery level is low";
        }
    },
    time_color {
        @Override
        public String defaultValue() {
            return "#03A9F4";
        }

        @Override
        public String info() {
            return "Time label color";
        }
    },
    storage_color {
        @Override
        public String defaultValue() {
            return "#9C27B0";
        }

        @Override
        public String info() {
            return "Storage label color";
        }
    },
    ram_color {
        @Override
        public String defaultValue() {
            return "#fff44336";
        }

        @Override
        public String info() {
            return "RAM label color";
        }
    },
    network_info_color {
        @Override
        public String defaultValue() {
            return "#FFCA28";
        }

        @Override
        public String info() {
            return "";
        }
    },
    toolbar_bg {
        @Override
        public String defaultValue() {
            return "#00000000";
        }

        @Override
        public String info() {
            return "Toolbar background color";
        }
    },
    toolbar_color {
        @Override
        public String defaultValue() {
            return "#ffff0000";
        }

        @Override
        public String info() {
            return "Toolbar icons color";
        }
    },
    enter_color {
        @Override
        public String defaultValue() {
            return "#ffffffff";
        }

        @Override
        public String info() {
            return "Enter icon color";
        }
    },
    cursor_color {
        @Override
        public String defaultValue() {
            return "#ffffff";
        }

        @Override
        public String info() {
            return "";
        }
    },
    overlay_color {
        @Override
        public String defaultValue() {
            return "#80000000";
        }

        @Override
        public String info() {
            return "The overlay that overlaps to the background (only when system_wallpaper is true)";
        }
    },
    alias_content_color {
        @Override
        public String defaultValue() {
            return "#1DE9B6";
        }

        @Override
        public String info() {
            return "Alias content color";
        }
    },
    statusbar_color {
        @Override
        public String defaultValue() {
            return "#000000";
        }

        @Override
        public String info() {
            return "Status Bar color (5.0+)";
        }
    },
    navigationbar_color {
        @Override
        public String defaultValue() {
            return "#000000";
        }

        @Override
        public String info() {
            return "Navigation Bar color (5.0+)";
        }
    },
    app_installed_color {
        @Override
        public String defaultValue() {
            return "#FF7043";
        }

        @Override
        public String info() {
            return "App installed message color";
        }
    },
    app_uninstalled_color {
        @Override
        public String defaultValue() {
            return "#FF7043";
        }

        @Override
        public String info() {
            return "App uninstalled message color";
        }
    },
    hint_color {
        @Override
        public String defaultValue() {
            return "#4CAF50";
        }

        @Override
        public String info() {
            return "Hint color";
        }
    },
    mark_color {
        @Override
        public String defaultValue() {
            return "#CDDC39";
        }

        @Override
        public String info() {
            return "The background color that will be used as marker";
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

    @Override
    public String type() {
        return XMLPrefsManager.XMLPrefsSave.COLOR;
    }
}