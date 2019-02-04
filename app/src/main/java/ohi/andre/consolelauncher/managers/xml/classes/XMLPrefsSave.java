package ohi.andre.consolelauncher.managers.xml.classes;

/**
 * Created by francescoandreuzzi on 06/03/2018.
 */

public interface XMLPrefsSave {
    String APP = "app", INTEGER = "int", BOOLEAN = "boolean", TEXT = "text", COLOR = "color";

    String defaultValue();
    String type();
    String info();
    XMLPrefsElement parent();
    String label();
}
