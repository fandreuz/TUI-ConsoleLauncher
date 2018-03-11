package ohi.andre.consolelauncher.managers;

import android.content.Context;
import android.graphics.Color;
import android.text.SpannableString;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXParseException;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.managers.xml.XMLPrefsManager;
import ohi.andre.consolelauncher.managers.xml.options.Theme;
import ohi.andre.consolelauncher.tuils.StoppableThread;
import ohi.andre.consolelauncher.tuils.Tuils;

/**
 * Created by francescoandreuzzi on 04/10/2017.
 */

public class RegexManager {

    private final String PATH = "regex.xml", ROOT = "REGEX";
    private final String REGEX_LABEL = "regex", ID_ATTRIBUTE = "id";

    private List<Regex> regexes;

    public static RegexManager instance;

    public RegexManager(final Context context) {
        if(regexes != null) regexes.clear();
        else regexes = new ArrayList<>();

        new StoppableThread() {

            @Override
            public void run() {
                super.run();

                try {
                    File root = Tuils.getFolder();
                    if(root == null) {
                        Tuils.sendOutput(Color.RED, context, R.string.tuinotfound_rss);
                        return;
                    }

                    File file = new File(root, PATH);
                    if(!file.exists()) {
                        file.createNewFile();
                        XMLPrefsManager.resetFile(file, ROOT);
                    }

                    Object[] o;
                    try {
                        o = XMLPrefsManager.buildDocument(file, ROOT);
                        if(o == null) {
                            Tuils.sendXMLParseError(context, PATH);
                            return;
                        }
                    } catch (SAXParseException e) {
                        Tuils.sendXMLParseError(context, PATH, e);
                        return;
                    } catch (Exception e) {
                        Tuils.log(e);
                        return;
                    }

                    Element el = (Element) o[1];

                    List<Integer> busyIds = new ArrayList<>();

                    NodeList nodeList = el.getElementsByTagName(REGEX_LABEL);

                    Out:
                    for(int c = 0; c < nodeList.getLength(); c++) {
                        Element e = (Element) nodeList.item(c);

                        if(!e.hasAttribute(XMLPrefsManager.VALUE_ATTRIBUTE)) continue;
                        String value = e.getAttribute(XMLPrefsManager.VALUE_ATTRIBUTE);

                        int id;
                        try {
                            id = Integer.parseInt(e.getAttribute(ID_ATTRIBUTE));
                        } catch (Exception exc) {
                            continue;
                        }

                        for(int j = 0; j < busyIds.size(); j++) {
                            if((int) busyIds.get(j) == id) continue Out;
                        }

                        busyIds.add(id);

                        if(value != null && value.length() > 0) {
                            regexes.add(new Regex(value, id));
                        }
                    }
                } catch (Exception e) {
                    Tuils.log(e);
                    Tuils.toFile(e);
                    return;
                }
            }
        }.start();

        instance = this;
    }

    public Regex get(int id) {
        for(Regex r : regexes) {
            if(r.id == id) return r;
        }

        return null;
    }

    private void rmFromList(int id) {
        Iterator<Regex> iterator = regexes.iterator();
        while (iterator.hasNext()) {
            Regex r = iterator.next();
            if(r.id == id) {
                iterator.remove();
            }
        }
    }

//    null: all good
//    "": used id
    public String add(int id, String value) {
        for(int c = 0; c < regexes.size(); c++) {
            if(regexes.get(c).id == id) return Tuils.EMPTYSTRING;
        }

        regexes.add(new Regex(value, id));

        File file = new File(Tuils.getFolder(), PATH);

        return XMLPrefsManager.add(file, REGEX_LABEL, new String[] {ID_ATTRIBUTE, XMLPrefsManager.VALUE_ATTRIBUTE}, new String[] {String.valueOf(id), value});
    }

//    null: all good
//    "": not found
    public String rm(int id) {
        try {
            File file = new File(Tuils.getFolder(), PATH);

            Object[] o = XMLPrefsManager.buildDocument(file, null);
            if(o == null) {
                return null;
            }

            Document d = (Document) o[0];
            Element el = (Element) o[1];

            boolean needToWrite = false;

            NodeList nodeList = el.getElementsByTagName(REGEX_LABEL);
            for(int c = 0; c < nodeList.getLength(); c++) {
                Element e = (Element) nodeList.item(c);

                int cId = Integer.MAX_VALUE;
                try {
                    id = Integer.parseInt(e.getAttribute(ID_ATTRIBUTE));
                } catch (Exception exc) {
                    continue;
                }

                if(cId == id) {
                    needToWrite = true;
                    el.removeChild(e);
                }
            }

            if(needToWrite) {
                XMLPrefsManager.writeTo(d, file);
                rmFromList(id);
                return null;
            }
            else return Tuils.EMPTYSTRING;
        } catch (Exception e) {
            return e.toString();
        }
    }

    public CharSequence test(int id, String test) {
        Regex regex = get(id);
        if(regex == null) return Tuils.EMPTYSTRING;

        if(regex.regex == null) return "null";
        Matcher m = regex.regex.matcher(test);

        int color = XMLPrefsManager.getColor(Theme.mark_color);
        int outputColor = XMLPrefsManager.getColor(Theme.output_color);

        if(m.matches()) {
            return Tuils.span(color, outputColor, test);
        }

        int last = 0;
        SpannableString s = Tuils.span(test, outputColor);
        while(m.find()) {
            String g0 = m.group(0);
            last = Tuils.span(color, s, g0, last);
        }

        return s;
    }

    public void dispose() {
        if(regexes != null) {
            regexes.clear();
            regexes = null;
        }

        instance = null;
    }

    public static class Regex {
        public Pattern regex;
        public String literalPattern;
        public int id;

        public Regex() {}

        public Regex(String value, int id) {
            this.regex = Pattern.compile(value);
            this.id = id;
        }
    }
}
