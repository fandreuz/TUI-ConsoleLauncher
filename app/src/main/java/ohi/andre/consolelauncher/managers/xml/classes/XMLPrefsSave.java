package ohi.andre.consolelauncher.managers.xml.classes;

import it.andreuzzi.comparestring2.StringableObject;

/**
 * Created by francescoandreuzzi on 06/03/2018.
 */

public interface XMLPrefsSave extends StringableObject {
    String APP = "app", INTEGER = "int", BOOLEAN = "boolean", TEXT = "text", COLOR = "color";

    String defaultValue();
    String type();
    String info();
    XMLPrefsElement parent();
    String label();
    String[] invalidValues();
}
