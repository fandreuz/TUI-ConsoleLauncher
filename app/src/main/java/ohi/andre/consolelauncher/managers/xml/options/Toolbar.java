package ohi.andre.consolelauncher.managers.xml.options;

import ohi.andre.consolelauncher.managers.xml.XMLPrefsManager;

/**
 * Created by francescoandreuzzi on 24/09/2017.
 */

public enum Toolbar implements XMLPrefsManager.XMLPrefsSave {

    show_toolbar {
        @Override
        public String defaultValue() {
            return "true";
        }

        @Override
        public String info() {
            return "If false, the toolbar is hidden";
        }

        @Override
        public String type() {
            return XMLPrefsManager.XMLPrefsSave.BOOLEAN;
        }
    };

    @Override
    public XMLPrefsManager.XmlPrefsElement parent() {
        return XMLPrefsManager.XMLPrefsRoot.TOOLBAR;
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