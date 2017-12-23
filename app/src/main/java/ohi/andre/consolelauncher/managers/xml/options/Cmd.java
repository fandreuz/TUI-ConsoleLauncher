package ohi.andre.consolelauncher.managers.xml.options;

import ohi.andre.consolelauncher.managers.xml.XMLPrefsManager;

/**
 * Created by francescoandreuzzi on 24/09/2017.
 */

public enum Cmd implements XMLPrefsManager.XMLPrefsSave {

    default_search {
        @Override
        public String defaultValue() {
            return "-gg";
        }

        @Override
        public String info() {
            return "The param that will be used if you type \"search apples\" instead of \"search -param apples\"";
        }

        @Override
        public String type() {
            return XMLPrefsManager.XMLPrefsSave.TEXT;
        }
    };

    @Override
    public XMLPrefsManager.XmlPrefsElement parent() {
        return XMLPrefsManager.XMLPrefsRoot.CMD;
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
