package ohi.andre.consolelauncher.managers.xml.options;

import ohi.andre.consolelauncher.managers.RssManager;
import ohi.andre.consolelauncher.managers.xml.XMLPrefsManager;

/**
 * Created by francescoandreuzzi on 03/10/2017.
 */

public enum Rss implements XMLPrefsManager.XMLPrefsSave {

    rss_default_color {
        @Override
        public String defaultValue() {
            return "#f44336";
        }
    },
    rss_default_format {
        @Override
        public String defaultValue() {
            return "%[50]title --- %[100]description";
        }
    },
    include_rss_default {
        @Override
        public String defaultValue() {
            return "true";
        }
    },
    rss_hidden_tags {
        @Override
        public String defaultValue() {
            return "img";
        }
    },
    rss_time_format {
        @Override
        public String defaultValue() {
            return "%t0";
        }
    };

    @Override
    public XMLPrefsManager.XmlPrefsElement parent() {
        return RssManager.instance;
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
