package ohi.andre.consolelauncher.managers.xml.options;

import ohi.andre.consolelauncher.managers.RssManager;
import ohi.andre.consolelauncher.managers.xml.classes.XMLPrefsElement;
import ohi.andre.consolelauncher.managers.xml.classes.XMLPrefsSave;

/**
 * Created by francescoandreuzzi on 03/10/2017.
 */

public enum Rss implements XMLPrefsSave {

    rss_default_color {
        @Override
        public String defaultValue() {
            return "#f44336";
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
    rss_default_format {
        @Override
        public String defaultValue() {
            return "%[50][green]title ### %[100][teal]description (%pubDate)";
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
    include_rss_default {
        @Override
        public String defaultValue() {
            return "true";
        }

        @Override
        public String type() {
            return XMLPrefsSave.COLOR;
        }

        @Override
        public String info() {
            return "If true, a filter will exclude an item if it matches. If false, a filter will include an item if it matches";
        }
    },
    rss_hidden_tags {
        @Override
        public String defaultValue() {
            return "img";
        }

        @Override
        public String type() {
            return XMLPrefsSave.TEXT;
        }

        @Override
        public String info() {
            return "A list of excluded tags (separated by comma)";
        }
    },
    rss_time_format {
        @Override
        public String defaultValue() {
            return "%t0";
        }

        @Override
        public String type() {
            return XMLPrefsSave.TEXT;
        }

        @Override
        public String info() {
            return "The time format used by RSS items";
        }
    },
    show_rss_download {
        @Override
        public String defaultValue() {
            return "true";
        }

        @Override
        public String type() {
            return XMLPrefsSave.COLOR;
        }

        @Override
        public String info() {
            return "If true, you will see a message when T-UI downloads a feed";
        }
    },
    rss_download_format {
        @Override
        public String defaultValue() {
            return "RSS: %id --- Downloaded %sb bytes";
        }

        @Override
        public String type() {
            return XMLPrefsSave.TEXT;
        }

        @Override
        public String info() {
            return "The message shown when an RSS feed is downloaded";
        }
    },
    rss_download_message_color {
        @Override
        public String defaultValue() {
            return "aqua";
        }

        @Override
        public String type() {
            return XMLPrefsSave.COLOR;
        }

        @Override
        public String info() {
            return "The color of the download message";
        }
    },
    click_rss {
        @Override
        public String defaultValue() {
            return "true";
        }

        @Override
        public String type() {
            return XMLPrefsSave.COLOR;
        }

        @Override
        public String info() {
            return "If true, you will be able to click on an RSS item to open the associated webpage";
        }
    },
//    long_click_rss {
//        @Override
//        public String defaultValue() {
//            return "true";
//        }
//    }
    ;

    @Override
    public XMLPrefsElement parent() {
        return RssManager.instance;
    }

    @Override
    public String label() {
        return name();
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
