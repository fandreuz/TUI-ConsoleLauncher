package ohi.andre.consolelauncher.managers.xml.options;

import ohi.andre.consolelauncher.managers.xml.XMLPrefsManager;

/**
 * Created by francescoandreuzzi on 24/09/2017.
 */

public enum Suggestions implements XMLPrefsManager.XMLPrefsSave {

    show_suggestions {
        @Override
        public String defaultValue() {
            return "true";
        }
    },
    transparent_suggestions {
        @Override
        public String defaultValue() {
            return "false";
        }
    },
    default_text_color {
        @Override
        public String defaultValue() {
            return "#000000";
        }
    },
    default_bg_color {
        @Override
        public String defaultValue() {
            return "#ffffff";
        }
    },
    apps_text_color {
        @Override
        public String defaultValue() {
            return "";
        }
    },
    apps_bg_color {
        @Override
        public String defaultValue() {
            return "#00897B";
        }
    },
    alias_text_color {
        @Override
        public String defaultValue() {
            return "";
        }
    },
    alias_bg_color {
        @Override
        public String defaultValue() {
            return "#FF5722";
        }
    },
    cmd_text_color {
        @Override
        public String defaultValue() {
            return "";
        }
    },
    cmd_bg_color {
        @Override
        public String defaultValue() {
            return "#76FF03";
        }
    },
    song_text_color {
        @Override
        public String defaultValue() {
            return "";
        }
    },
    song_bg_color {
        @Override
        public String defaultValue() {
            return "#EEFF41";
        }
    },
    contact_text_color {
        @Override
        public String defaultValue() {
            return "";
        }
    },
    contact_bg_color {
        @Override
        public String defaultValue() {
            return "#64FFDA";
        }
    },
    file_text_color {
        @Override
        public String defaultValue() {
            return "";
        }
    },
    file_bg_color {
        @Override
        public String defaultValue() {
            return "#03A9F4";
        }
    },
    suggest_alias_default {
        @Override
        public String defaultValue() {
            return "true";
        }
    },
    suggest_appgp_default {
        @Override
        public String defaultValue() {
            return "true";
        }
    },
    click_to_launch {
        @Override
        public String defaultValue() {
            return "true";
        }
    },
    suggestions_size {
        @Override
        public String defaultValue() {
            return "12";
        }
    };

    @Override
    public XMLPrefsManager.XmlPrefsElement parent() {
        return XMLPrefsManager.XMLPrefsRoot.SUGGESTIONS;
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