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

        @Override
        public String type() {
            return XMLPrefsManager.XMLPrefsSave.BOOLEAN;
        }

        @Override
        public String info() {
            return "If false, suggestions won't be shown";
        }
    },
    transparent_suggestions {
        @Override
        public String defaultValue() {
            return "false";
        }

        @Override
        public String type() {
            return XMLPrefsManager.XMLPrefsSave.BOOLEAN;
        }

        @Override
        public String info() {
            return "If true, the background will be transparent";
        }
    },
    default_text_color {
        @Override
        public String defaultValue() {
            return "#000000";
        }

        @Override
        public String info() {
            return "The default text color";
        }
    },
    default_bg_color {
        @Override
        public String defaultValue() {
            return "#ffffff";
        }

        @Override
        public String info() {
            return "The default background color";
        }
    },
    apps_text_color {
        @Override
        public String defaultValue() {
            return "";
        }

        @Override
        public String info() {
            return "Apps suggestions text color";
        }
    },
    apps_bg_color {
        @Override
        public String defaultValue() {
            return "#00897B";
        }

        @Override
        public String info() {
            return "Apps suggestions background color";
        }
    },
    alias_text_color {
        @Override
        public String defaultValue() {
            return "";
        }

        @Override
        public String info() {
            return "Aliases suggestions text color";
        }
    },
    alias_bg_color {
        @Override
        public String defaultValue() {
            return "#FF5722";
        }

        @Override
        public String info() {
            return "Aliases suggestions background color";
        }
    },
    cmd_text_color {
        @Override
        public String defaultValue() {
            return "";
        }

        @Override
        public String info() {
            return "Commands suggestions text color";
        }
    },
    cmd_bg_color {
        @Override
        public String defaultValue() {
            return "#76FF03";
        }

        @Override
        public String info() {
            return "Commands suggestions background color";
        }
    },
    song_text_color {
        @Override
        public String defaultValue() {
            return "";
        }

        @Override
        public String info() {
            return "Songs suggestions text color";
        }
    },
    song_bg_color {
        @Override
        public String defaultValue() {
            return "#EEFF41";
        }

        @Override
        public String info() {
            return "Songs suggestions background color";
        }
    },
    contact_text_color {
        @Override
        public String defaultValue() {
            return "";
        }

        @Override
        public String info() {
            return "Contacts suggestions text color";
        }
    },
    contact_bg_color {
        @Override
        public String defaultValue() {
            return "#64FFDA";
        }

        @Override
        public String info() {
            return "Contacts suggestions background color";
        }
    },
    file_text_color {
        @Override
        public String defaultValue() {
            return "";
        }

        @Override
        public String info() {
            return "Files suggestions text color";
        }
    },
    file_bg_color {
        @Override
        public String defaultValue() {
            return "#03A9F4";
        }

        @Override
        public String info() {
            return "Files suggestions background color";
        }
    },
    suggest_alias_default {
        @Override
        public String defaultValue() {
            return "true";
        }

        @Override
        public String type() {
            return XMLPrefsManager.XMLPrefsSave.BOOLEAN;
        }

        @Override
        public String info() {
            return "If true, your alias will be shown when the input field is empty";
        }
    },
    suggest_appgp_default {
        @Override
        public String defaultValue() {
            return "true";
        }

        @Override
        public String type() {
            return XMLPrefsManager.XMLPrefsSave.BOOLEAN;
        }

        @Override
        public String info() {
            return "If true, your app groups will be shown when the input field is empty";
        }
    },
    click_to_launch {
        @Override
        public String defaultValue() {
            return "true";
        }

        @Override
        public String type() {
            return XMLPrefsManager.XMLPrefsSave.BOOLEAN;
        }

        @Override
        public String info() {
            return "If true, some suggestions will be executed as soon as you click them";
        }
    },
    suggestions_size {
        @Override
        public String defaultValue() {
            return "12";
        }

        @Override
        public String type() {
            return XMLPrefsManager.XMLPrefsSave.INTEGER;
        }

        @Override
        public String info() {
            return "The text size of the suggestions";
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

    @Override
    public String type() {
        return XMLPrefsManager.XMLPrefsSave.COLOR;
    }
}