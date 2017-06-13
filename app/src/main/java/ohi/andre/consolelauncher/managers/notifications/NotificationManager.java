package ohi.andre.consolelauncher.managers.notifications;

import android.annotation.TargetApi;
import android.graphics.Color;
import android.os.Build;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import ohi.andre.consolelauncher.managers.XMLPrefsManager;
import ohi.andre.consolelauncher.tuils.Tuils;

import static ohi.andre.consolelauncher.managers.XMLPrefsManager.VALUE_ATTRIBUTE;
import static ohi.andre.consolelauncher.managers.XMLPrefsManager.XML_DEFAULT;
import static ohi.andre.consolelauncher.managers.XMLPrefsManager.resetFile;
import static ohi.andre.consolelauncher.managers.XMLPrefsManager.set;
import static ohi.andre.consolelauncher.managers.XMLPrefsManager.setMany;
import static ohi.andre.consolelauncher.managers.XMLPrefsManager.transform;
import static ohi.andre.consolelauncher.managers.XMLPrefsManager.writeTo;

/**
 * Created by francescoandreuzzi on 29/04/2017.
 */

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class NotificationManager implements XMLPrefsManager.XmlPrefsElement {

    private static final String COLOR_ATTRIBUTE = "color";
    private static final String ENABLED_ATTRIBUTE = "enabled";

    public static final String PATH = "notifications.xml";
    private static final String NAME = "NOTIFICATIONS";

    public enum Options implements XMLPrefsManager.XMLPrefsSave {

        enabled {
            @Override
            public String defaultValue() {
                return "true";
            }
        },
        default_color {
            @Override
            public String defaultValue() {
                return "#FF0000";
            }
        };

        @Override
        public XMLPrefsManager.XMLPrefsRoot parent() {
            return null;
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

    @Override
    public XMLPrefsManager.XMLPrefsList getValues() {
        return null;
    }

    @Override
    public void write(XMLPrefsManager.XMLPrefsSave save, String value) {
        set(new File(Tuils.getFolder(), PATH), NAME, save.label(), new String[] {VALUE_ATTRIBUTE}, new String[] {value});
    }

    private static File folder;
    private static XMLPrefsManager.XMLPrefsList values;
    private static Map<String, Integer> colors;
    private static boolean created = false;

    private NotificationManager() {}

    public static void create() {
        if(created) return;
        created = true;

        folder = Tuils.getFolder();

        colors = new HashMap<>();
        values = new XMLPrefsManager.XMLPrefsList();

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();

            File file = new File(folder, PATH);
            if(!file.exists() && !file.createNewFile()) return;

            Document d;
            try {
                d = builder.parse(file);
            } catch (Exception e) {
                resetFile(file, NAME);

                d = builder.parse(file);
            }

            List<Options> enums = new ArrayList<>(Arrays.asList(Options.values()));

            Element root = (Element) d.getElementsByTagName(NAME).item(0);
            if(root == null) {
                resetFile(file, NAME);
                d = builder.parse(file);
                root = (Element) d.getElementsByTagName(NAME).item(0);
            }
            NodeList nodes = root.getElementsByTagName("*");

            for(int count = 0; count < nodes.getLength(); count++) {
                Node node = nodes.item(count);

                String nn = node.getNodeName();
                if(Tuils.find(nn, (List) enums) != -1) {
                    values.add(nn, node.getAttributes().getNamedItem(VALUE_ATTRIBUTE).getNodeValue());

                    for(int en = 0; en < enums.size(); en++) {
                        if(enums.get(en).label().equals(nn)) {
                            enums.remove(en);
                            break;
                        }
                    }
                } else {
                    if(node.getNodeType() == Node.ELEMENT_NODE) {
                        Element e = (Element) node;

                        boolean enabled = !e.hasAttribute(ENABLED_ATTRIBUTE) || Boolean.parseBoolean(e.getAttribute(ENABLED_ATTRIBUTE));
                        if(enabled) {
                            int color = e.hasAttribute(COLOR_ATTRIBUTE) ? Color.parseColor(e.getAttribute(COLOR_ATTRIBUTE)) : getColor(Options.default_color);
                            colors.put(nn, color);
                        }
                    }
                }
            }

            if(enums.size() == 0) return;

            for(XMLPrefsManager.XMLPrefsSave s : enums) {
                Element em = d.createElement(s.label());
                em.setAttribute(VALUE_ATTRIBUTE, s.defaultValue());
                root.appendChild(em);

                values.add(s.label(), s.defaultValue());
            }

            writeTo(d, file);
        } catch (Exception e) {}
    }

    public static void notificationsChangeFor(NotificatedApp app) {
        notificationsChangeFor(new ArrayList<>(Collections.singletonList(app)));
    }

    public static void notificationsChangeFor(List<NotificatedApp> apps) {
        String[] names = new String[apps.size()];
        final String[] attrNames = {ENABLED_ATTRIBUTE, COLOR_ATTRIBUTE};
        String[][] values = new String[names.length][attrNames.length];

        for(int count = 0; count < apps.size(); count++) {
            NotificatedApp app = apps.get(count);
            names[count] = app.pkg;
            values[count][0] = app.enabled + Tuils.EMPTYSTRING;
            values[count][1] = app.color + Tuils.EMPTYSTRING;
        }

        setMany(new File(folder, PATH), NAME, names, attrNames, values);
    }

    public static NotificatedApp getAppState(String pkg) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();

            File file = new File(folder, PATH);

            Document d;
            try {
                d = builder.parse(file);
            } catch (Exception e) {
                FileOutputStream stream = new FileOutputStream(file);
                stream.write(XML_DEFAULT.getBytes());
                stream.flush();
                stream.close();

                d = builder.parse(file);
            }

            Element root = (Element) d.getElementsByTagName(NAME).item(0);
            NodeList nodes = root.getElementsByTagName("*");

            for (int count = 0; count < nodes.getLength(); count++) {
                Node node = nodes.item(count);

                if(!pkg.equals(node.getNodeName())) continue;

                Element e = (Element) node;
                boolean enabled = !e.hasAttribute(ENABLED_ATTRIBUTE) || Boolean.parseBoolean(e.getAttribute(ENABLED_ATTRIBUTE));
                int color = e.hasAttribute(COLOR_ATTRIBUTE) ? Color.parseColor(e.getAttribute(COLOR_ATTRIBUTE)) : getColor(Options.default_color);

                return new NotificatedApp(pkg, color, enabled);
            }
        } catch (Exception e) {
            return null;
        }

//        default!!!
        return new NotificatedApp(pkg, getColor(Options.default_color), true);
    }

    public static <T> T get(Class<T> c, XMLPrefsManager.XMLPrefsSave prefsSave) {
        if(prefsSave != null) {
            try {
                return (T) transform(values.get(prefsSave).value, c);
            } catch (Exception e) {
                return (T) transform(prefsSave.defaultValue(), c);
            }
        }
        return null;
    }

    public static int getColor(XMLPrefsManager.XMLPrefsSave prefsSave) {
        if(prefsSave != null) {
            try {
                return (int) transform(values.get(prefsSave).value, Color.class);
            } catch (Exception e) {
                String def = prefsSave.defaultValue();
                if(def == null || def.length() == 0) {
                    return -1;
                }
                return (int) transform(def, Color.class);
            }
        }
        return 0;
    }

    public static int colorsLength() {
        return colors.size();
    }

    public static class NotificatedApp {
        String pkg;
        int color;
        boolean enabled;

        public NotificatedApp(String pkg, String color, boolean enabled) throws Exception {
            this(pkg, Color.parseColor(color), enabled);
        }

        public NotificatedApp(String pkg, int color, boolean enabled) {
            this.pkg = pkg;
            this.color = color;
            this.enabled = enabled;
        }

        @Override
        public boolean equals(Object obj) {
            if(obj instanceof NotificatedApp) return pkg.equals(((NotificatedApp) obj).pkg);
            return this == obj;
        }
    }
}
