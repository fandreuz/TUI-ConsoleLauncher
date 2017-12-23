package ohi.andre.consolelauncher.managers.notifications;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ohi.andre.consolelauncher.managers.RegexManager;
import ohi.andre.consolelauncher.managers.xml.XMLPrefsManager;
import ohi.andre.consolelauncher.managers.xml.options.Notifications;
import ohi.andre.consolelauncher.tuils.Tuils;

import static ohi.andre.consolelauncher.managers.xml.XMLPrefsManager.VALUE_ATTRIBUTE;
import static ohi.andre.consolelauncher.managers.xml.XMLPrefsManager.resetFile;
import static ohi.andre.consolelauncher.managers.xml.XMLPrefsManager.set;
import static ohi.andre.consolelauncher.managers.xml.XMLPrefsManager.writeTo;

/**
 * Created by francescoandreuzzi on 29/04/2017.
 */

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class NotificationManager implements XMLPrefsManager.XmlPrefsElement {

    private static final String COLOR_ATTRIBUTE = "color", ENABLED_ATTRIBUTE = "enabled", ID_ATTRIBUTE = "id", FORMAT_ATTRIBUTE = "format", FILTER_ATTRIBUTE = "filter";

    public static final String PATH = "notifications.xml";
    private static final String NAME = "NOTIFICATIONS";

    public static XMLPrefsManager.XmlPrefsElement instance = null;

    public static boolean default_app_state;
    public static String default_color;

    @Override
    public String[] deleted() {
        return new String[] {"enabled", "default_color", "default_app_state"};
    }

    @Override
    public XMLPrefsManager.XMLPrefsList getValues() {
        return values;
    }

    @Override
    public void write(XMLPrefsManager.XMLPrefsSave save, String value) {
        set(new File(Tuils.getFolder(), PATH), save.label(), new String[] {VALUE_ATTRIBUTE}, new String[] {value});
    }

    private static XMLPrefsManager.XMLPrefsList values;
    private static List<NotificatedApp> apps;
    private static List<Pattern> filters;
    private static List<XMLPrefsManager.IdValue> formats;

    private NotificationManager() {}

    public static void create(Context context) {

        if(instance != null) {
            return;
        }
        instance = new NotificationManager();

        apps = new ArrayList<>();
        filters = new ArrayList<>();
        formats = new ArrayList<>();
        values = new XMLPrefsManager.XMLPrefsList();

        try {
            File file = new File(Tuils.getFolder(), PATH);
            if(!file.exists()) {
                resetFile(file, NAME);
            }

            Object[] o;
            try {
                o = XMLPrefsManager.buildDocument(file, NAME);
                if(o == null) {
                    Tuils.sendXMLParseError(context, PATH);
                    return;
                }
            } catch (Exception e) {
                Tuils.sendXMLParseError(context, PATH, e);
                return;
            }

            Document d = (Document) o[0];
            Element root = (Element) o[1];

            List<Notifications> enums = new ArrayList<>(Arrays.asList(Notifications.values()));
            NodeList nodes = root.getElementsByTagName("*");

            String[] deleted = instance.deleted();
            boolean needToWrite = false;

            for(int count = 0; count < nodes.getLength(); count++) {
                Node node = nodes.item(count);

                String nn = node.getNodeName();
                if (Tuils.find(nn, (List) enums) != -1) {
                    values.add(nn, node.getAttributes().getNamedItem(VALUE_ATTRIBUTE).getNodeValue());

                    for (int en = 0; en < enums.size(); en++) {
                        if (enums.get(en).label().equals(nn)) {
                            enums.remove(en);
                            break;
                        }
                    }
                } else if (nn.equals(FILTER_ATTRIBUTE)) {
                    if (node.getNodeType() == Node.ELEMENT_NODE) {
                        Element e = (Element) node;

                        Pattern pattern;

                        String regex = XMLPrefsManager.getStringAttribute(e, VALUE_ATTRIBUTE);
                        if (regex == null) continue;
                        try {
                            int id = Integer.parseInt(regex);
                            pattern = RegexManager.get(id).regex;
                        } catch (Exception exc) {
                            try {
                                pattern = Pattern.compile(regex);
                            } catch (Exception exc2) {
                                pattern = Pattern.compile(regex, Pattern.LITERAL);
                            }
                        }

                        filters.add(pattern);
                    }
                } else if(nn.equals(FORMAT_ATTRIBUTE)) {
                    if (node.getNodeType() == Node.ELEMENT_NODE) {
                        Element e = (Element) node;

                        String format = XMLPrefsManager.getStringAttribute(e, VALUE_ATTRIBUTE);
                        if(format == null) continue;

                        int id;
                        try {
                            id = e.hasAttribute(ID_ATTRIBUTE) ? Integer.parseInt(e.getAttribute(ID_ATTRIBUTE)) : -1;
                        } catch (NumberFormatException f) {
                            continue;
                        }

                        formats.add(new XMLPrefsManager.IdValue(format, id));
                    }
                } else {
                    int index = deleted == null ? -1 : Tuils.find(nn, deleted);
                    if(index != -1) {
                        deleted[index] = null;
                        Element e = (Element) node;
                        root.removeChild(e);

                        needToWrite = true;
                    }

                    if (node.getNodeType() == Node.ELEMENT_NODE) {
                        Element e = (Element) node;

                        NotificatedApp app;

                        boolean enabled = XMLPrefsManager.getBooleanAttribute(e, ENABLED_ATTRIBUTE);
                        String color = XMLPrefsManager.getStringAttribute(e, COLOR_ATTRIBUTE);
                        String format = XMLPrefsManager.getStringAttribute(e, FORMAT_ATTRIBUTE);

                        app = new NotificatedApp(nn, color, format, enabled);
                        apps.add(app);
                    }
                }
            }

            if (enums.size() > 0) {
                for (XMLPrefsManager.XMLPrefsSave s : enums) {
                    String value = s.defaultValue();

                    Element em = d.createElement(s.label());
                    em.setAttribute(VALUE_ATTRIBUTE, value);
                    root.appendChild(em);

                    values.add(s.label(), value);
                }

                writeTo(d, file);
            } else if (needToWrite) {
                writeTo(d, file);
            }
        } catch (Exception e) {
            Tuils.log(e);
            Tuils.toFile(e);
        }

        for(NotificatedApp app : apps) {
            try {
                int formatID = Integer.parseInt(app.format);

                for(XMLPrefsManager.IdValue idValue : formats) {
                    if(idValue.id == formatID) {
                        app.format = idValue.value;
                        break;
                    }
                }
            } catch (Exception e) {}
        }

        default_app_state = XMLPrefsManager.getBoolean(Notifications.app_notification_enabled_default);
        default_color = XMLPrefsManager.get(Notifications.default_notification_color);
    }

    public static String setState(String pkg, boolean state) {
        return XMLPrefsManager.set(new File(Tuils.getFolder(), PATH), pkg, new String[] {ENABLED_ATTRIBUTE}, new String[] {String.valueOf(state)});
    }

    public static String setColor(String pkg, String color) {
        return XMLPrefsManager.set(new File(Tuils.getFolder(), PATH), pkg, new String[] {ENABLED_ATTRIBUTE, COLOR_ATTRIBUTE}, new String[] {String.valueOf(true), color});
    }

    public static String setFormat(String pkg, String format) {
        return XMLPrefsManager.set(new File(Tuils.getFolder(), PATH), pkg, new String[] {FORMAT_ATTRIBUTE}, new String[] {format});
    }

    public static String addFilter(String pattern, int id) {
        return XMLPrefsManager.add(new File(Tuils.getFolder(), PATH), FILTER_ATTRIBUTE, new String[] {ID_ATTRIBUTE, VALUE_ATTRIBUTE}, new String[] {String.valueOf(id), pattern});
    }

    public static String addFormat(String format, int id) {
        return XMLPrefsManager.add(new File(Tuils.getFolder(), PATH), FORMAT_ATTRIBUTE, new String[] {ID_ATTRIBUTE, VALUE_ATTRIBUTE}, new String[] {String.valueOf(id), format});
    }

    public static String rmFilter(int id) {
        return XMLPrefsManager.removeNode(new File(Tuils.getFolder(), PATH), FILTER_ATTRIBUTE, new String[] {ID_ATTRIBUTE}, new String[] {String.valueOf(id)});
    }

    public static String rmFormat(int id) {
        return XMLPrefsManager.removeNode(new File(Tuils.getFolder(), PATH), FORMAT_ATTRIBUTE, new String[] {ID_ATTRIBUTE}, new String[] {String.valueOf(id)});
    }

    public static boolean match(String pkg, String text) {
//        if(pkg.equals(BuildConfig.APPLICATION_ID)) return true;

        for(Pattern f : filters) {
            Matcher m = f.matcher(text);
            if(m.matches() || m.find()) return true;
        }

        return false;
    }

    public static int apps() {
        return apps.size();
    }

    public static NotificatedApp getAppState(String pkg) {
        int index = Tuils.find(pkg, apps);
        if(index == -1) return null;
        return apps.get(index);

    }

    public static class NotificatedApp {
        String pkg, color, format;
        boolean enabled;

        public NotificatedApp(String pkg, String color, String format, boolean enabled) {
            this.pkg = pkg;
            this.color = color;
            this.enabled = enabled;
            this.format = format;
        }

        @Override
        public boolean equals(Object obj) {
            return this.toString().equals(obj.toString());
        }

        @Override
        public String toString() {
            return pkg;
        }
    }
}
