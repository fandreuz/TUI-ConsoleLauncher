package ohi.andre.consolelauncher.managers;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.BufferedInputStream;
import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.managers.xml.XMLPrefsManager;
import ohi.andre.consolelauncher.tuils.InputOutputReceiver;
import ohi.andre.consolelauncher.tuils.TimeManager;
import ohi.andre.consolelauncher.tuils.Tuils;

import static ohi.andre.consolelauncher.managers.xml.XMLPrefsManager.VALUE_ATTRIBUTE;
import static ohi.andre.consolelauncher.managers.xml.XMLPrefsManager.set;
import static ohi.andre.consolelauncher.managers.xml.XMLPrefsManager.writeTo;

/**
 * Created by francescoandreuzzi on 01/10/2017.
 */

public class RssManager implements XMLPrefsManager.XmlPrefsElement {

//    header:
//    last-modified
//    etag
//    last_checked_client

    private final String RSS_FOLDER = "rss";

    public static final String TIME_ATTRIBUTE  = "time", SHOW_ATTRIBUTE = "show", URL_ATTRIBUTE = "url", LASTCHECKED_ATTRIBUTE = "lastChecked", LASTMODIFIED_ATTRIBUTE = "lastModified", ETAG_ATTRIBUTE = "etag",
                            LAST_SHOWN_ITEM_ATTRIBUTE = "lastShownItem", ID_ATTRIBUTE = "id", FORMAT_ATTRIBUTE = "format", INCLUDE_ATTRIBUTE = "includeIfMatches", EXCLUDE_ATTRIBUTE = "excludeIfMatches",
                                COLOR_ATTRIBUTE = "color", WIFIONLY_ATTRIBUTE = "wifiOnly", COMMAND_ATTRIBUTE = "updateCommand";

    public static final String RSS_LABEL = "rss", FORMAT_LABEL = "format";

    private final String SEPARATOR = " - ";
    private final String ID_LABEL = "ID: ", TIME_LABEL = "update time: ", SHOW_LABEL = "show: ", URL_LABEL = "url: ";

    private final String LAST_MODIFIED_FIELD = "Last-Modified", ETAG_FIELD = "ETag", IF_MODIFIED_SINCE_FIELD = "If-Modified-Since", IF_NONE_MATCH_FIELD = "If-None-Match", GET_LABEL = "GET";

    private final String PUBDATE_CHILD = "pubDate";

    private DateFormat defaultRSSDateFormat = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z");

    private static XMLPrefsManager.XMLPrefsList values;

    public static final String PATH = "rss.xml";
    public static final String NAME = "RSS";

    public static XMLPrefsManager.XmlPrefsElement instance = null;

    @Override
    public String[] deleted() {
        return new String[] {};
    }

    @Override
    public XMLPrefsManager.XMLPrefsList getValues() {
        return values;
    }

    @Override
    public void write(XMLPrefsManager.XMLPrefsSave save, String value) {
        set(new File(Tuils.getFolder(), PATH), NAME, save.label(), new String[] {VALUE_ATTRIBUTE}, new String[] {value});
    }

    private int defaultColor;
    private String defaultFormat, timeFormat;
    private boolean includeRssDefault;

    private Context context;
    private Handler handler;

    private File root, rssFile;

    private List<Rss> feeds;
    private List<Format> formats;

//    those will obscure the tag and its content
    private Pattern[] hideTagPatterns;

    private ConnectivityManager connectivityManager;

    public RssManager(Context context) {
        instance = this;
        this.context = context;

        connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        RegexManager.create(context);

        root = new File(Tuils.getFolder(), RSS_FOLDER);
        rssFile = new File(Tuils.getFolder(), PATH);

        prepare();

        values = new XMLPrefsManager.XMLPrefsList();

        refresh();
    }

    public void refresh() {
        if(handler != null) {
            handler.removeCallbacksAndMessages(null);
            handler = null;
        }

        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);

                
            }
        };

        if(feeds != null) feeds.clear();
        else feeds = new ArrayList<>();

        if(formats != null) formats.clear();
        else formats = new ArrayList<>();

        new Thread() {
            @Override
            public void run() {
                super.run();

                try {
                    Object[] o = XMLPrefsManager.buildDocument(rssFile, NAME);

                    Document document = (Document) o[0];
                    Element rootElement = (Element) o[1];

                    NodeList nodes = rootElement.getElementsByTagName("*");

                    List<ohi.andre.consolelauncher.managers.xml.options.Rss> enums = new ArrayList<>(Arrays.asList(ohi.andre.consolelauncher.managers.xml.options.Rss.values()));

                    String[] deleted = instance.deleted();
                    boolean needToWrite = false;

                    Main:
                    for(int count = 0; count < nodes.getLength(); count++) {
                        Node node = nodes.item(count);

                        String nn = node.getNodeName();
                        if (Tuils.find(nn, (List) enums) != -1) {
//                              is an enum value
                            values.add(nn, node.getAttributes().getNamedItem(VALUE_ATTRIBUTE).getNodeValue());

                            for (int en = 0; en < enums.size(); en++) {
                                if (enums.get(en).label().equals(nn)) {
                                    enums.remove(en);
                                    break;
                                }
                            }
                        } else {
//                             deleted
                            int index = deleted == null ? -1 : Tuils.find(nn, deleted);
                            if(index != -1) {
                                deleted[index] = null;
                                Element e = (Element) node;
                                rootElement.removeChild(e);

                                needToWrite = true;
                            }

                            else {
                                String name = node.getNodeName();
                                if (name.equals(RSS_LABEL)) {
                                    Element t = (Element) node;

                                    int id;
                                    try {
                                        id = Integer.parseInt(t.getAttribute(ID_ATTRIBUTE));
                                    } catch (Exception exc) {
                                        continue Main;
                                    }

                                    String url = t.getAttribute(URL_ATTRIBUTE);
                                    long updateTime = Long.parseLong(t.getAttribute(TIME_ATTRIBUTE));
                                    boolean show = Boolean.parseBoolean(t.getAttribute(SHOW_ATTRIBUTE));

                                    long lastChecked = t.hasAttribute(LASTCHECKED_ATTRIBUTE) ? Long.parseLong(t.getAttribute(LASTCHECKED_ATTRIBUTE)) : -1;
                                    long lastShown = t.hasAttribute(LAST_SHOWN_ITEM_ATTRIBUTE) ? Long.parseLong(t.getAttribute(LAST_SHOWN_ITEM_ATTRIBUTE)) : -1;
                                    String lastModified = t.hasAttribute(LASTMODIFIED_ATTRIBUTE) ? t.getAttribute(LASTMODIFIED_ATTRIBUTE) : null;
                                    String etag = t.hasAttribute(ETAG_ATTRIBUTE) ? t.getAttribute(ETAG_ATTRIBUTE) : null;

                                    String format = t.hasAttribute(FORMAT_ATTRIBUTE) ? t.getAttribute(FORMAT_ATTRIBUTE) : null;
                                    String includeIfMatches = t.hasAttribute(INCLUDE_ATTRIBUTE) ? t.getAttribute(INCLUDE_ATTRIBUTE) : null;
                                    String excludeIfMatches = t.hasAttribute(EXCLUDE_ATTRIBUTE) ? t.getAttribute(EXCLUDE_ATTRIBUTE) : null;
                                    int color;
                                    try {
                                        color = Color.parseColor(t.getAttribute(COLOR_ATTRIBUTE));
                                    } catch (Exception exc) {
                                        color = Integer.MAX_VALUE;
                                    }

                                    boolean wifiOnly;
                                    try {
                                        wifiOnly = Boolean.parseBoolean(t.getAttribute(WIFIONLY_ATTRIBUTE));
                                    } catch (Exception e) {
                                        wifiOnly = false;
                                    }

                                    String updateCommand = t.hasAttribute(COMMAND_ATTRIBUTE) ? t.getAttribute(COMMAND_ATTRIBUTE) : null;

                                    final Rss r = new Rss(url, updateTime, lastChecked, lastShown, id, show, lastModified, etag, format, includeIfMatches, excludeIfMatches, color, wifiOnly, updateCommand);
                                    feeds.add(r);
                                } else if(name.equals(FORMAT_LABEL)) {
                                        Element e = (Element) node;

                                        int id;
                                        try {
                                            id = Integer.parseInt(e.getAttribute(ID_ATTRIBUTE));
                                        } catch (Exception exc) {
                                            id = -1;
                                        }

                                        if(id == -1) continue;

                                        String format = e.getAttribute(XMLPrefsManager.VALUE_ATTRIBUTE);

                                        Format i = new Format(format, id);
                                        formats.add(i);
                                    }
                                }
                            }
                        }

                    if (enums.size() > 0) {
                        for (XMLPrefsManager.XMLPrefsSave s : enums) {
                            String value = s.defaultValue();

                            Element em = document.createElement(s.label());
                            em.setAttribute(VALUE_ATTRIBUTE, value);
                            rootElement.appendChild(em);

                            values.add(s.label(), value);
                        }

                        writeTo(document, rssFile);
                    } else if (needToWrite) {
                        writeTo(document, rssFile);
                    }
                } catch (Exception e) {
                    Tuils.log(e);
                    Tuils.toFile(e);
                }

                defaultFormat = XMLPrefsManager.get(ohi.andre.consolelauncher.managers.xml.options.Rss.rss_default_format);
                defaultColor = XMLPrefsManager.getColor(ohi.andre.consolelauncher.managers.xml.options.Rss.rss_default_color);
                includeRssDefault = XMLPrefsManager.getBoolean(ohi.andre.consolelauncher.managers.xml.options.Rss.include_rss_default);
                timeFormat = XMLPrefsManager.get(ohi.andre.consolelauncher.managers.xml.options.Rss.rss_time_format);

                String hiddenTags = XMLPrefsManager.get(ohi.andre.consolelauncher.managers.xml.options.Rss.rss_hidden_tags).replaceAll(Tuils.SPACE, Tuils.EMPTYSTRING);
                String[] split = null;
                for(int c = 0; c < hiddenTags.length(); c++) {
                    char ch = hiddenTags.charAt(c);
                    if(Character.isLetter(ch)) continue;
                    split = hiddenTags.split(ch + Tuils.EMPTYSTRING);
                }
                if(split == null) {
                    split = new String[] {hiddenTags};
                }

                hideTagPatterns = new Pattern[split.length * 2];
                for(int c = 0, j = 0; c < split.length; c++, j += 2) {
                    hideTagPatterns[j] = Pattern.compile("<" + split[c] + "[^>]*>[^<]*<\\/" + split[c] + ">");
                    hideTagPatterns[j + 1] = Pattern.compile("<" + split[c] + "[^>]*\\/>");
                }

                for(Rss rss : feeds) {
                    rss.updateFormat();
                    rss.updateIncludeIfMatches();
                    rss.updateExcludeIfMatches();

                    if(rss.color == Integer.MAX_VALUE) rss.color = defaultColor;

                    queue(rss);
                }
            }
        }.start();
    }

    public void dispose() {
        handler.removeCallbacksAndMessages(null);
    }

    public String add(int id, long timeInSeconds, String url) {
        String output = XMLPrefsManager.add(rssFile, NAME, RSS_LABEL, new String[] {ID_ATTRIBUTE, TIME_ATTRIBUTE, SHOW_ATTRIBUTE, URL_ATTRIBUTE},
                new String[] {String.valueOf(id), String.valueOf(timeInSeconds * 1000), String.valueOf(true), url});

        if(output == null) {
            try {
                Rss r = new Rss(url, timeInSeconds * 1000, id, true);

                r.lastShownItem = System.currentTimeMillis();
                r.format = defaultFormat;

                updateRss(r, true);
                feeds.add(r);

                queue(r);
            } catch (Exception e) {
                Tuils.log(e);
                return e.toString();
            }

            return null;
        } else return output;
    }

    public String addFormat(int id, String value) {
        String output = XMLPrefsManager.add(rssFile, NAME, FORMAT_LABEL, new String[] {ID_ATTRIBUTE, XMLPrefsManager.VALUE_ATTRIBUTE}, new String[] {String.valueOf(id), value});
        if(output == null) {
            formats.add(new Format(value, id));
            return null;
        } else return output;
    }

    public String removeFormat(int id) {
        String output = XMLPrefsManager.removeNode(rssFile, NAME, FORMAT_LABEL, new String[] {ID_ATTRIBUTE}, new String[] {String.valueOf(id)});;
        if(output == null) {
            return null;
        } else {
            if (output.length() > 0) return output;
            return context.getString(R.string.id_notfound);
        }
    }

    public String rm(int id) {
        String output = XMLPrefsManager.removeNode(rssFile, NAME, RSS_LABEL, new String[] {ID_ATTRIBUTE}, new String[] {String.valueOf(id)});
        if(output == null) {
            File rss = new File(root, RSS_LABEL + id + ".xml");
            if(rss.exists()) rss.delete();

            removeId(id);
            handler.sendEmptyMessage(id);

            return null;
        }
        else {
            if(output.length() > 0) return output;
            else return context.getString(R.string.rss_not_found);
        }
    }

    public String list() {
        StringBuilder builder = new StringBuilder();
        for(Rss r : feeds) {
            builder.append(ID_LABEL).append(":").append(Tuils.SPACE).append(r.id).append(Tuils.NEWLINE).append(r.url).append(Tuils.NEWLINE);
        }

        return builder.toString().trim();
    }

    public void l(int id) {
        for(Rss feed : feeds) {
            if(feed.id == id) {
                try {
                    parse(feed, false);
                } catch (Exception e) {
                    Tuils.log(e);
                    Tuils.toFile(e);
                }
                break;
            }
        }
    }

    public String setShow(int id, boolean show) {
        String output = XMLPrefsManager.set(rssFile, NAME, RSS_LABEL, new String[] {ID_ATTRIBUTE}, new String[] {String.valueOf(id)}, new String[] {SHOW_ATTRIBUTE}, new String[] {String.valueOf(show)}, false);
        if(output == null) {
            Rss r = findId(id);
            if(r != null) r.show = show;
            return null;
        }
        else {
            if (output.length() > 0) return output;
            return context.getString(R.string.rss_not_found);
        }
    }

    public String setTime(int id, long timeSeconds) {
        String output = XMLPrefsManager.set(rssFile, NAME, RSS_LABEL, new String[] {ID_ATTRIBUTE}, new String[] {String.valueOf(id)}, new String[] {TIME_ATTRIBUTE}, new String[] {String.valueOf(timeSeconds)}, false);
        if(output == null) {
            Rss r = findId(id);
            if(r != null) r.updateTimeSeconds = timeSeconds;
            return null;
        }
        else {
            if (output.length() > 0) return output;
            return context.getString(R.string.rss_not_found);
        }
    }

    public String setFormat(int id, String format) {
        String output = XMLPrefsManager.set(rssFile, NAME, RSS_LABEL, new String[] {ID_ATTRIBUTE}, new String[] {String.valueOf(id)}, new String[] {FORMAT_ATTRIBUTE}, new String[] {format}, false);
        if(output == null) {
            Rss r = findId(id);
            if(r != null) r.setFormat(format);
            return null;
        }
        else {
            if (output.length() > 0) return output;
            return context.getString(R.string.rss_not_found);
        }
    }

    public String setColor(int id, String color) {
        String output = XMLPrefsManager.set(rssFile, NAME, RSS_LABEL, new String[] {ID_ATTRIBUTE}, new String[] {String.valueOf(id)}, new String[] {COLOR_ATTRIBUTE}, new String[] {color}, false);
        if(output == null) {
            Rss r = findId(id);
            if(r != null) r.color = Color.parseColor(color);
            return null;
        }
        else {
            if (output.length() > 0) return output;
            return context.getString(R.string.rss_not_found);
        }
    }

    public String setIncludeIfMatches(int id, String regex) {
        String output = XMLPrefsManager.set(rssFile, NAME, RSS_LABEL, new String[] {ID_ATTRIBUTE}, new String[] {String.valueOf(id)}, new String[] {INCLUDE_ATTRIBUTE}, new String[] {regex}, false);
        if(output == null) {
            Rss r = findId(id);
            if(r != null) r.setIncludeIfMatches(regex);
            return null;
        }
        else {
            if (output.length() > 0) return output;
            return context.getString(R.string.rss_not_found);
        }
    }

    public String setExcludeIfMatches(int id, String regex) {
        String output = XMLPrefsManager.set(rssFile, NAME, RSS_LABEL, new String[] {ID_ATTRIBUTE}, new String[] {String.valueOf(id)}, new String[] {EXCLUDE_ATTRIBUTE}, new String[] {regex}, false);
        if(output == null) {
            Rss r = findId(id);
            if(r != null) r.setExcludeIfMatches(regex);
            return null;
        }
        else {
            if (output.length() > 0) return output;
            return context.getString(R.string.rss_not_found);
        }
    }

    public String setWifiOnly(int id, boolean wifiOnly) {
        String output = XMLPrefsManager.set(rssFile, NAME, RSS_LABEL, new String[] {ID_ATTRIBUTE}, new String[] {String.valueOf(id)}, new String[] {WIFIONLY_ATTRIBUTE}, new String[] {String.valueOf(wifiOnly)},
                false);
        if(output == null) {
            Rss r = findId(id);
            if(r != null) r.wifiOnly = wifiOnly;
            return null;
        }
        else {
            if (output.length() > 0) return output;
            return context.getString(R.string.rss_not_found);
        }
    }

//    base methods

    private void queue(final Rss rss) {
        Runnable rn = new Runnable() {
            @Override
            public void run() {
                try {
                    updateRss(rss, false);
                    queue(rss);
                } catch (Exception e1) {
                    Tuils.log(e1);
                }
            }
        };

        long delay;
        if(rss.lastCheckedClient == -1) delay = 0;
        else delay = rss.updateTimeSeconds - (System.currentTimeMillis() - rss.lastCheckedClient);

        if(delay <= 0) rn.run();
        else {
            handler.postDelayed(rn, delay);
        }
    }

    final String quotes = "\"";

    private void updateRss(final Rss feed, final boolean firstTime) {
        if(feed.wifiOnly && !connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnected()) {

            feed.lastCheckedClient = System.currentTimeMillis();
            feed.updateFile();

            return;
        }

        final Object o = new Object();

        new Thread() {
            @Override
            public void run() {
                super.run();

                try {
                    URL uObj = new URL(feed.url);
                    HttpURLConnection urlConnection = (HttpURLConnection) uObj.openConnection();
                    urlConnection.setRequestMethod(GET_LABEL);

                    if(!firstTime) {
                        urlConnection.setRequestProperty(IF_MODIFIED_SINCE_FIELD, feed.lMod);
                        urlConnection.setRequestProperty(IF_NONE_MATCH_FIELD, quotes + feed.etag + quotes);
                    }

                    urlConnection.connect();

                    if(firstTime || urlConnection.getResponseCode() != 304) {
                        Tuils.download(new BufferedInputStream(uObj.openStream()), new File(root, RSS_LABEL + feed.id + ".xml"));

                        feed.lMod = urlConnection.getHeaderField(LAST_MODIFIED_FIELD);
                        feed.etag = urlConnection.getHeaderField(ETAG_FIELD);
                        if(feed.etag != null) feed.etag = feed.etag.replaceAll("\"", Tuils.EMPTYSTRING);

                        if(parse(feed, true) && feed.updateCommand != null) {
                            Intent intent = new Intent(InputOutputReceiver.ACTION_CMD);
                            intent.putExtra(InputOutputReceiver.SHOW_CONTENT, false);
                            intent.putExtra(InputOutputReceiver.TEXT, feed.updateCommand);
                            context.sendBroadcast(intent);
                        }
                    } else {
//                        not modified
                    }

                    feed.lastCheckedClient = System.currentTimeMillis();
                    feed.updateFile();

                } catch (Exception e) {
                    Tuils.log(e);
                    Tuils.toFile(e);
                }

                synchronized (o) {
                    o.notify();
                }
            }
        }.start();

        synchronized (o) {
            try {
                o.wait();
            } catch (InterruptedException e) {
                Tuils.log(e);
                Tuils.toFile(e);
            }
        }
    }

    private boolean parse(Rss feed, boolean time) throws Exception {
        Tuils.log("parse");

        boolean updated = false;

        File rssFile = new File(root, RSS_LABEL + feed.id + ".xml");
        if(!rssFile.exists()) return updated;

        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(rssFile);

        doc.getDocumentElement().normalize();

        long lastShown = feed.lastShownItem;

        NodeList list = doc.getElementsByTagName("item");
        for(int c = list.getLength(); c >= 0; c--) {
            Tuils.log("index: " + c);

            Element element = (Element) list.item(c);
            if(element == null) {
                continue;
            }

            if(time) {
                Tuils.log("timing");
                NodeList l = element.getElementsByTagName(PUBDATE_CHILD);
                if(l.getLength() == 0) continue;

                String date = l.item(0).getTextContent();
                Date d = defaultRSSDateFormat.parse(date);

                long timeLong = d.getTime();
                if(c == 0) {
                    feed.lastShownItem = d.getTime();
                    feed.updateTime();
                }

                Tuils.log("pubDate: " + timeLong);
                Tuils.log("lastShown: " + lastShown);

                if(lastShown < timeLong) {
                    updated = true;

                    Tuils.log("showing");
                    showItem(feed, element);
                }
            } else {
                updated = true;

                Tuils.log("not timing");
                showItem(feed, element);
            }
        }

        return updated;
    }

    private final Pattern formatPattern = Pattern.compile("%(?:\\[(\\d+)\\])?(?:\\[([^]]+)\\])?([^\\s]+)");
    private final Pattern removeTags = Pattern.compile("<[^>]+>");
    private final String THREE_DOTS = "...";

//    called when a new element is detected, it could be triggered many times again in some milliseconds
    private void showItem(Rss feed, Element item) {
        String cp = feed.format != null ? feed.format : defaultFormat;

        CharSequence s = Tuils.span(cp, feed.color);

        Matcher m = formatPattern.matcher(cp);
        while(m.find()) {
            if(m.groupCount() == 3) {
                String length = m.group(1);
                String color = m.group(2);
                String tag = m.group(3);

                String value;
                int cl = feed.color;

                NodeList ls = item.getElementsByTagName(tag);
                if(ls.getLength() == 0) value = Tuils.EMPTYSTRING;
                else {
                    value = ls.item(0).getTextContent();

                    if(value.equals(PUBDATE_CHILD)) {
                        Date d;
                        try {
                            d = defaultRSSDateFormat.parse(value);
                        } catch (ParseException e) {
                            continue;
                        }

                        long timeLong = d.getTime();
                        value = TimeManager.replace(timeFormat, timeLong, Integer.MAX_VALUE).toString();

                    } else {
                        for(Pattern p : hideTagPatterns) {
                            value = p.matcher(value).replaceAll(Tuils.EMPTYSTRING);
                        }

                        value = removeTags.matcher(value).replaceAll(Tuils.EMPTYSTRING);

                        try {
                            int l = Integer.parseInt(length);
                            value = value.substring(0, l);
                            value = value.concat(THREE_DOTS);
                        } catch (Exception e) {}

                    }

                    try {
                        cl = Color.parseColor(color);
                    } catch (Exception e) {
                        cl = feed.color;
                    }

                }

                CharSequence replace;
                if(cl != feed.color && value.length() > 0) {
                    replace = Tuils.span(value, cl);
                } else {
                    replace = value;
                }

                s = TextUtils.replace(s, new String[] {m.group(0)}, new CharSequence[] {replace});
            }

        }

        if(includeRssDefault) {
            if(feed.excludeIfMatches != null && feed.excludeIfMatches.length() > 0) {
                Pattern p = Pattern.compile(feed.excludeIfMatches);
                if (p.matcher(s.toString()).find()) {
                    return;
                }
            }
        } else {
            if(feed.includeIfMatches != null && feed.includeIfMatches.length() > 0) {
                Pattern p = Pattern.compile(feed.includeIfMatches);
                if (!p.matcher(s.toString()).find()) {
                    return;
                }
            }
        }

        Tuils.sendOutput(context, s);
    }

    private class Rss {
        String url;
        long updateTimeSeconds, lastCheckedClient, lastShownItem;
        int id;
        boolean show;
        String lMod, etag;

        String format;
        String includeIfMatches, excludeIfMatches;
        String updateCommand;

        int color;

        boolean wifiOnly;

        public Rss(String url, long updateTimeSeconds, int id, boolean show) {
            this(url, updateTimeSeconds, -1, -1, id, show, null, null, null, null, null, Integer.MAX_VALUE, false, null);
        }

        public Rss(String url, long updateTimeSeconds, long lastCheckedClient, long lastShownItem, int id, boolean show, String lMod, String etag, String format,
                   String includeIfMatches, String excludeIfMatches, int color, boolean wifiOnly, String updateCommand) {
            this.url = url;
            this.updateTimeSeconds = updateTimeSeconds;
            this.lastShownItem = lastShownItem;
            this.lastCheckedClient = lastCheckedClient;
            this.id = id;
            this.show = show;
            this.lMod = lMod;
            this.etag = etag;

            this.format = format;
            this.includeIfMatches = includeIfMatches;
            this.excludeIfMatches = excludeIfMatches;

            this.color = color;

            this.wifiOnly = wifiOnly;

            this.updateCommand = updateCommand;
        }

        private void updateFile() {
            XMLPrefsManager.set(rssFile, NAME, RSS_LABEL, new String[] {ID_ATTRIBUTE}, new String[] {String.valueOf(id)},
                    new String[] {LASTCHECKED_ATTRIBUTE, LASTMODIFIED_ATTRIBUTE, ETAG_ATTRIBUTE}, new String[] {String.valueOf(lastCheckedClient), lMod, etag},
                    false);
        }

        @Override
        public String toString() {
            return new StringBuilder().append(ID_LABEL).append(id).append(SEPARATOR).append(TIME_LABEL).append(updateTimeSeconds).append(SEPARATOR).append(SHOW_LABEL).append(show).append(Tuils.NEWLINE)
                    .append(URL_LABEL).append(url).toString();
        }

        public void setFormat(String format) {
            this.format = format;
            updateFormat();
        }

        public void setIncludeIfMatches(String includeIfMatches) {
            this.includeIfMatches = includeIfMatches;
            updateIncludeIfMatches();
        }

        public void setExcludeIfMatches(String excludeIfMatches) {
            this.excludeIfMatches = excludeIfMatches;
            updateExcludeIfMatches();
        }

        public void updateFormat() {
            if(format != null) {
                try {
                    int id = Integer.parseInt(format);
                    for(Format i : formats) {
                        if(id == i.id) format = i.value;
                    }
                } catch (Exception exc) {
//                  the format is personalized --> it can't be casted to int
                }
            }
        }

        public void updateIncludeIfMatches() {
            if(includeIfMatches != null) {
                try {
                    int id = Integer.parseInt(includeIfMatches);
                    includeIfMatches = RegexManager.get(id).value;
                } catch (Exception exc) {}
            }
        }

        public void updateExcludeIfMatches() {
            if(excludeIfMatches != null) {
                try {
                    int id = Integer.parseInt(excludeIfMatches);
                    excludeIfMatches = RegexManager.get(id).value;
                } catch (Exception exc) {}
            }
        }

        @Override
        public boolean equals(Object obj) {
            if(obj instanceof Rss) return id == ((Rss) obj).id;
            if(obj instanceof Integer) return id == (int) obj;
            return false;
        }

        public void updateTime() {
            Tuils.log("updating time: " + XMLPrefsManager.set(rssFile, NAME, RSS_LABEL, new String[] {ID_ATTRIBUTE}, new String[] {String.valueOf(id)}, new String[] {LAST_SHOWN_ITEM_ATTRIBUTE},
                    new String[] {String.valueOf(lastShownItem)}, false));
        }
    }

    private static class Format {
        String value;
        int id;

        public Format(String value, int id) {
            this.value = value;
            this.id = id;
        }
    }

//    utils
    private void removeId(int id) {
        for(int c = 0; c < feeds.size(); c++) {
            if(feeds.get(c).id == id) {
                feeds.remove(c);
                return;
            }
        }
    }

    private Rss findId(int id) {
        for(int c = 0; c < feeds.size(); c++) {
            Rss r = feeds.get(c);
            if(r.id == id) {
                return r;
            }
        }

        return null;
    }

    private boolean prepare() {
        boolean check = true;
        if(!root.isDirectory()) {
            check = false;
            root.mkdir();
        }
        if(!rssFile.exists()) {
            try {
                rssFile.createNewFile();
                XMLPrefsManager.resetFile(rssFile, NAME);
                check = false;

                return check && root.list().length > 1;
            } catch (Exception e) {
                return false;
            }
        }

        return check;
    }
}
