package ohi.andre.consolelauncher.managers.xml.options;

import ohi.andre.consolelauncher.managers.xml.XMLPrefsManager;
import ohi.andre.consolelauncher.managers.xml.classes.XMLPrefsElement;
import ohi.andre.consolelauncher.managers.xml.classes.XMLPrefsSave;

/**
 * Created by francescoandreuzzi on 24/09/2017.
 */

public enum Suggestions implements XMLPrefsSave {

    show_suggestions {
        @Override
        public String defaultValue() {
            return "true";
        }

        @Override
        public String type() {
            return XMLPrefsSave.BOOLEAN;
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
            return XMLPrefsSave.BOOLEAN;
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
            return XMLPrefsSave.BOOLEAN;
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
            return XMLPrefsSave.BOOLEAN;
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
            return XMLPrefsSave.BOOLEAN;
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
            return XMLPrefsSave.INTEGER;
        }

        @Override
        public String info() {
            return "The text size of the suggestions";
        }
    },
    double_space_click_first_suggestion {
        @Override
        public String defaultValue() {
            return "true";
        }

        @Override
        public String type() {
            return BOOLEAN;
        }

        @Override
        public String info() {
            return "If true, t-ui will simulate a click on the current first suggestion if you double-click the space bar";
        }
    },
    noinput_suggestions_order {
        @Override
        public String defaultValue() {
            return "0(5)1(5)2(2)3(5)";
        }

        @Override
        public String type() {
            return XMLPrefsSave.TEXT;
        }

        @Override
        public String info() {
            return "The order and the number of suggestions that appears on-screen when the input field is empty. 0=apps, 1=alias, 2=cmds, 3=app groups. Put between round brackets the maximum number of suggestions of the leading type";
        }
    },
    suggestions_order {
        @Override
        public String defaultValue() {
            return "2(2)0(5)1(5)3(3)";
        }

        @Override
        public String type() {
            return XMLPrefsSave.TEXT;
        }

        @Override
        public String info() {
            return "The order and the number of suggestions that appears on-screen. 0=apps, 1=alias, 2=cmds, 3=app groups. Put between round brackets the maximum number of suggestions of the leading type";
        }
    },
    noinput_min_command_priority {
        @Override
        public String defaultValue() {
            return "5";
        }

        @Override
        public String type() {
            return XMLPrefsSave.INTEGER;
        }

        @Override
        public String info() {
            return "The minimum priority of a command shown when the input field is empty";
        }
    },
    suggestions_per_category {
        @Override
        public String defaultValue() {
            return "5";
        }

        @Override
        public String type() {
            return XMLPrefsSave.INTEGER;
        }

        @Override
        public String info() {
            return "The number of suggestions shown per category. This doesn\'t affect \"noinput_suggestions_order\" and \"suggestions_order\"";
        }
    },
    suggestions_deadline {
        @Override
        public String defaultValue() {
            return "0.45";
        }

        @Override
        public String type() {
            return XMLPrefsSave.TEXT;
        }

        @Override
        public String info() {
            return "The min/max rank that a suggestion needs to get in order to be shown. min/max depends on the comparison algorithm";
        }
    },
    suggestions_algorithm {
        @Override
        public String defaultValue() {
            return "13";
        }

        @Override
        public String type() {
            return XMLPrefsSave.INTEGER;
        }

        @Override
        public String info() {
            return "The algorithm used to compare strings";
        }
    },
    suggestions_quickcompare_n {
        @Override
        public String defaultValue() {
            return "3";
        }

        @Override
        public String type() {
            return INTEGER;
        }

        @Override
        public String info() {
            return "If the input is shorter than n characters, t-ui will try to show you the entries which start with those characters";
        }
    },
    hide_suggestions_when_empty {
        @Override
        public String defaultValue() {
            return "always";
        }

        @Override
        public String type() {
            return XMLPrefsSave.TEXT;
        }

        @Override
        public String info() {
            return "If \"always\" the suggestion area will be hidden when there are no suggestions. If \"true\" it will be hidden only if also the input area is empty. \"false\" disables the feature";
        }
    },
    suggestions_spaces {
        @Override
        public String defaultValue() {
            return "15,15,25,20";
        }

        @Override
        public String type() {
            return XMLPrefsSave.TEXT;
        }

        @Override
        public String info() {
            return "[External horizontal margin],[E. vertical margin],[Internal horizontal margin],[I. vertical margin]";
        }
    };

    @Override
    public XMLPrefsElement parent() {
        return XMLPrefsManager.XMLPrefsRoot.SUGGESTIONS;
    }

    @Override
    public String label() {
        return name();
    }

    @Override
    public String type() {
        return XMLPrefsSave.COLOR;
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