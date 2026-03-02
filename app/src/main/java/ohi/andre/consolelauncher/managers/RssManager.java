package ohi.andre.consolelauncher.managers;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.os.Handler;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.text.TextUtils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXParseException;

import java.io.BufferedInputStream;
import java.io.File;
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

import ohi.andre.consolelauncher.MainManager;
import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.managers.xml.XMLPrefsManager;
import ohi.andre.consolelauncher.managers.xml.classes.XMLPrefsElement;
import ohi.andre.consolelauncher.managers.xml.classes.XMLPrefsList;
import ohi.andre.consolelauncher.managers.xml.classes.XMLPrefsSave;
import ohi.andre.consolelauncher.tuils.StoppableThread;
import ohi.andre.consolelauncher.tuils.Tuils;
import ohi.andre.consolelauncher.tuils.html_escape.HtmlEscape;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import static ohi.andre.consolelauncher.managers.xml.XMLPrefsManager.VALUE_ATTRIBUTE;
import static ohi.andre.consolelauncher.managers.xml.XMLPrefsManager.set;
import static ohi.andre.consolelauncher.managers.xml.XMLPrefsManager.writeTo;

/**
 * Created by francescoandreuzzi on 01/10/2017.
 */

public class RssManager implements XMLPrefsElement {

    private final int RSS_CHECK_DELAY = 5000;

    private final String RSS_FOLDER = "rss";

    public static String TIME_ATTRIBUTE  = "updateTimeSec";
    public static String SHOW_ATTRIBUTE = "show";
    public static String URL_ATTRIBUTE = "url";
    public static String LASTCHECKED_ATTRIBUTE = "lastChecked";
    public static String LAST_SHOWN_ITEM_ATTRIBUTE = "lastShownItem";
    public static String ID_ATTRIBUTE = "id", FORMAT_ATTRIBUTE = "format";
    public static String INCLUDE_ATTRIBUTE = "includeIfMatches";
    public static String EXCLUDE_ATTRIBUTE = "excludeIfMatches";
    public static String COLOR_ATTRIBUTE = "color";
    public static String WIFIONLY_ATTRIBUTE = "wifiOnly";
    public static String TIME_FORMAT_ATTRIBUTE = "timeFormat";
    public static String DATE_TAG_ATTRIBUTE = "pubDateTag";
    public static String ENTRY_TAG_ATTRIBUTE = "entryTag";
    public static String ON_ATTRIBUTE = "on";
    public static String CMD_ATTRIBUTE = "cmd";

    public static final String RSS_LABEL = "rss", FORMAT_LABEL = "format", REGEX_CMD_LABEL = "regex";

    private final String PUBDATE_CHILD = "pubDate", ENTRY_CHILD = "item", LINK_CHILD = "link", HREF_ATTRIBUTE = "href";

    private SimpleDateFormat defaultRSSDateFormat = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z");

    private static XMLPrefsList values;

    public static final String PATH = "rss.xml";
    public static final String NAME = "RSS";

    public static XMLPrefsElement instance = null;

    @Override
    public String[] delete() {
        return null;
    }

    @Override
    public XMLPrefsList getValues() {
        return values;
    }

    @Override
    public void write(XMLPrefsSave save, String value) {
        set(new File(Tuils.getFolder(), PATH), save.label(), new String[] {VALUE_ATTRIBUTE}, new String[] {value});
    }

    @Override
    public String path() {
        return PATH;
    }

    private int defaultColor, downloadMessageColor;
    private String defaultFormat, timeFormat, downloadFormat;

    private boolean includeRssDefault, showDownloadMessage, click;

    private Context context;
    private Handler handler;

    private File root, rssIndexFile;

    private List<Rss> feeds;
    private List<XMLPrefsManager.IdValue> formats;
    private List<CmdableRegex> cmdRegexes;

    private OkHttpClient client;

    //    those will obscure the tag and its content
    private Pattern[] hideTagPatterns;

    private Pattern urlPattern, idPattern, bPattern, kbPattern, mbPattern, gbPattern;

    private ConnectivityManager connectivityManager;

    public RssManager(Context context, OkHttpClient client) {
        instance = this;
        this.context = context;

        connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        root = new File(Tuils.getFolder(), RSS_FOLDER);
        rssIndexFile = new File(Tuils.getFolder(), PATH);

        this.client = client;

        prepare();

        values = new XMLPrefsList();

        handler = new Handler();
        refresh();
    }

    public void refresh() {
        if(handler != null) {
            handler.removeCallbacksAndMessages(null);
        }

        if(feeds != null) feeds.clear();
        else feeds = new ArrayList<>();

        if(formats != null) formats.clear();
        else formats = new ArrayList<>();

        if(cmdRegexes != null) cmdRegexes.clear();
        else cmdRegexes = new ArrayList<>();

        new StoppableThread() {
            @Override
            public void run() {
                super.run();

                Object[] o;
                try {
                    o = XMLPrefsManager.buildDocument(rssIndexFile, NAME);
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

                try {

                    Document document = (Document) o[0];
                    Element rootElement = (Element) o[1];

                    NodeList nodes = rootElement.getElementsByTagName("*");

                    List<ohi.andre.consolelauncher.managers.xml.options.Rss> enums = new ArrayList<>(Arrays.asList(ohi.andre.consolelauncher.managers.xml.options.Rss.values()));

                    String[] deleted = instance.delete();
                    boolean needToWrite = false;

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
                                    feeds.add(Rss.fromElement(t));
                                } else if(name.equals(FORMAT_LABEL)) {
                                    Element e = (Element) node;

                                    int id;
                                    try {
                                        id = Integer.parseInt(e.getAttribute(ID_ATTRIBUTE));
                                    } catch (Exception exc) {
                                        id = -1;
                                    }

                                    if(id == -1) continue;

                                    String format = XMLPrefsManager.getStringAttribute(e, XMLPrefsManager.VALUE_ATTRIBUTE);

                                    XMLPrefsManager.IdValue i = new XMLPrefsManager.IdValue(format, id);
                                    formats.add(i);
                                } else if(name.equals(REGEX_CMD_LABEL)) {
                                    Element e = (Element) node;

                                    int id;
                                    try {
                                        id = Integer.parseInt(e.getAttribute(ID_ATTRIBUTE));
                                    } catch (Exception exc) {
                                        continue;
                                    }

                                    String regex = XMLPrefsManager.getStringAttribute(e, XMLPrefsManager.VALUE_ATTRIBUTE);
                                    if(regex == null || regex.length() == 0) continue;

                                    String on = XMLPrefsManager.getStringAttribute(e, ON_ATTRIBUTE);
                                    if(on == null || on.length() == 0) continue;

                                    String cmd = XMLPrefsManager.getStringAttribute(e, CMD_ATTRIBUTE);
                                    if(cmd == null || cmd.length() == 0) continue;

                                    cmdRegexes.add(new CmdableRegex(id, on, regex, cmd));
                                }
                            }
                        }
                    }

                    if (enums.size() > 0) {
                        for (XMLPrefsSave s : enums) {
                            String value = s.defaultValue();

                            Element em = document.createElement(s.label());
                            em.setAttribute(VALUE_ATTRIBUTE, value);
                            rootElement.appendChild(em);

                            values.add(s.label(), value);
                        }

                        writeTo(document, rssIndexFile);
                    } else if (needToWrite) {
                        writeTo(document, rssIndexFile);
                    }
                } catch (Exception e) {
                    Tuils.log(e);
                    Tuils.toFile(e);
                }

                click = XMLPrefsManager.getBoolean(ohi.andre.consolelauncher.managers.xml.options.Rss.click_rss);
//                longClick = XMLPrefsManager.getBoolean(ohi.andre.consolelauncher.managers.xml.options.Rss.long_click_rss);
                defaultFormat = XMLPrefsManager.get(ohi.andre.consolelauncher.managers.xml.options.Rss.rss_default_format);
                defaultColor = XMLPrefsManager.getColor(ohi.andre.consolelauncher.managers.xml.options.Rss.rss_default_color);
                includeRssDefault = XMLPrefsManager.getBoolean(ohi.andre.consolelauncher.managers.xml.options.Rss.include_rss_default);
                timeFormat = XMLPrefsManager.get(ohi.andre.consolelauncher.managers.xml.options.Rss.rss_time_format);
                showDownloadMessage = XMLPrefsManager.getBoolean(ohi.andre.consolelauncher.managers.xml.options.Rss.show_rss_download);
                if(showDownloadMessage) {
                    downloadFormat = XMLPrefsManager.get(ohi.andre.consolelauncher.managers.xml.options.Rss.rss_download_format);

                    String size = "%s";

                    idPattern = Pattern.compile("%id", Pattern.CASE_INSENSITIVE);
                    urlPattern = Pattern.compile("%url", Pattern.CASE_INSENSITIVE);
                    gbPattern = Pattern.compile(size + "gb", Pattern.CASE_INSENSITIVE);
                    mbPattern = Pattern.compile(size + "mb", Pattern.CASE_INSENSITIVE);
                    kbPattern = Pattern.compile(size + "kb", Pattern.CASE_INSENSITIVE);
                    bPattern = Pattern.compile(size + "b", Pattern.CASE_INSENSITIVE);

                    downloadMessageColor = XMLPrefsManager.getColor(ohi.andre.consolelauncher.managers.xml.options.Rss.rss_download_message_color);
                }

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
                    rss.updateFormat(formats);
                    rss.updateIncludeIfMatches();
                    rss.updateExcludeIfMatches();

                    if(rss.color == Integer.MAX_VALUE) rss.color = defaultColor;
                }

                for(CmdableRegex rg : cmdRegexes) {
                    try {
                        int id = Integer.parseInt(rg.literalPattern);
                        rg.regex = RegexManager.instance.get(id).regex;
                    } catch (Exception exc) {
                        try {
                            rg.regex = Pattern.compile(rg.literalPattern);
                        } catch (Exception e) {
                            Tuils.sendOutput(Color.RED, context, context.getString(R.string.invalid_regex) + Tuils.SPACE + rg.literalPattern);
                            rg.regex = null;
                        }
                    }
                }

                handler.post(updateRunnable);
            }
        }.start();
    }

    public void dispose() {
        if(handler != null) handler.removeCallbacksAndMessages(null);
    }

    public String add(int id, long timeInSeconds, String url) {
        String output = XMLPrefsManager.add(rssIndexFile, RSS_LABEL, new String[] {ID_ATTRIBUTE, TIME_ATTRIBUTE, SHOW_ATTRIBUTE, URL_ATTRIBUTE},
                new String[] {String.valueOf(id), String.valueOf(timeInSeconds), String.valueOf(true), url});

        if(output == null) {
            try {
                Rss r = new Rss(url, timeInSeconds, id, true);

                r.lastShownItem = System.currentTimeMillis();
                r.format = defaultFormat;

                updateRss(r, true);
                feeds.add(r);
            } catch (Exception e) {
                Tuils.log(e);
                return e.toString();
            }

            return null;
        } else return output;
    }

    public String addFormat(int id, String value) {
        String output = XMLPrefsManager.add(rssIndexFile, FORMAT_LABEL, new String[] {ID_ATTRIBUTE, XMLPrefsManager.VALUE_ATTRIBUTE}, new String[] {String.valueOf(id), value});
        if(output == null) {
            formats.add(new XMLPrefsManager.IdValue(value, id));
            return null;
        } else return output;
    }

    public String removeFormat(int id) {
        String output = XMLPrefsManager.removeNode(rssIndexFile, FORMAT_LABEL, new String[] {ID_ATTRIBUTE}, new String[] {String.valueOf(id)});;
        if(output == null) {
            return null;
        } else {
            if (output.length() > 0) return output;
            return context.getString(R.string.id_notfound);
        }
    }

    public String rm(int id) {
        String output = XMLPrefsManager.removeNode(rssIndexFile, RSS_LABEL, new String[] {ID_ATTRIBUTE}, new String[] {String.valueOf(id)});
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
            builder.append(Rss.ID_LABEL).append(":").append(Tuils.SPACE).append(r.id).append(Tuils.NEWLINE).append(r.url).append(Tuils.NEWLINE);
        }

        String output = builder.toString().trim();
        if(output.length() == 0) return "[]";
        return output;
    }

    public String l(int id) {
        for(Rss feed : feeds) {
            if(feed.id == id) {
                try {
                    parse(feed, false);
                    return null;
                } catch (Exception e) {
                    Tuils.log(e);
                    Tuils.toFile(e);
                    return e.toString();
                }
            }
        }

        return context.getString(R.string.rss_not_found);
    }

    public String setShow(int id, boolean show) {
        String output = XMLPrefsManager.set(rssIndexFile, RSS_LABEL, new String[] {ID_ATTRIBUTE}, new String[] {String.valueOf(id)}, new String[] {SHOW_ATTRIBUTE}, new String[] {String.valueOf(show)}, false);
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
        String output = XMLPrefsManager.set(rssIndexFile, RSS_LABEL, new String[] {ID_ATTRIBUTE}, new String[] {String.valueOf(id)}, new String[] {TIME_ATTRIBUTE}, new String[] {String.valueOf(timeSeconds)}, false);
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

    public String setTimeFormat(int id, String format) {
        String output = XMLPrefsManager.set(rssIndexFile, RSS_LABEL, new String[] {ID_ATTRIBUTE}, new String[] {String.valueOf(id)}, new String[] {TIME_FORMAT_ATTRIBUTE}, new String[] {format}, false);
        if(output == null) {
            Rss r = findId(id);
            if(r != null) r.timeFormat = new SimpleDateFormat(format);
            return null;
        }
        else {
            if (output.length() > 0) return output;
            return context.getString(R.string.rss_not_found);
        }
    }

    public String setFormat(int id, String format) {
        String output = XMLPrefsManager.set(rssIndexFile, RSS_LABEL, new String[] {ID_ATTRIBUTE}, new String[] {String.valueOf(id)}, new String[] {FORMAT_ATTRIBUTE}, new String[] {format}, false);
        if(output == null) {
            Rss r = findId(id);
            if(r != null) r.setFormat(formats, format);
            return null;
        }
        else {
            if (output.length() > 0) return output;
            return context.getString(R.string.rss_not_found);
        }
    }

    public String setColor(int id, String color) {
        String output = XMLPrefsManager.set(rssIndexFile, RSS_LABEL, new String[] {ID_ATTRIBUTE}, new String[] {String.valueOf(id)}, new String[] {COLOR_ATTRIBUTE}, new String[] {color}, false);
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

    public String setDateTag(int id, String tag) {
        String output = XMLPrefsManager.set(rssIndexFile, RSS_LABEL, new String[] {ID_ATTRIBUTE}, new String[] {String.valueOf(id)}, new String[] {DATE_TAG_ATTRIBUTE}, new String[] {tag}, false);
        if(output == null) {
            Rss r = findId(id);
            if(r != null) r.dateTag = tag;
            return null;
        }
        else {
            if (output.length() > 0) return output;
            return context.getString(R.string.rss_not_found);
        }
    }

    public String setEntryTag(int id, String tag) {
        String output = XMLPrefsManager.set(rssIndexFile, RSS_LABEL, new String[] {ID_ATTRIBUTE}, new String[] {String.valueOf(id)}, new String[] {ENTRY_TAG_ATTRIBUTE}, new String[] {tag}, false);
        if(output == null) {
            Rss r = findId(id);
            if(r != null) r.entryTag = tag;
            return null;
        }
        else {
            if (output.length() > 0) return output;
            return context.getString(R.string.rss_not_found);
        }
    }

    public String setIncludeIfMatches(int id, String regex) {
        String output = XMLPrefsManager.set(rssIndexFile, RSS_LABEL, new String[] {ID_ATTRIBUTE}, new String[] {String.valueOf(id)}, new String[] {INCLUDE_ATTRIBUTE}, new String[] {regex}, false);
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
        String output = XMLPrefsManager.set(rssIndexFile, RSS_LABEL, new String[] {ID_ATTRIBUTE}, new String[] {String.valueOf(id)}, new String[] {EXCLUDE_ATTRIBUTE}, new String[] {regex}, false);
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
        String output = XMLPrefsManager.set(rssIndexFile, RSS_LABEL, new String[] {ID_ATTRIBUTE}, new String[] {String.valueOf(id)}, new String[] {WIFIONLY_ATTRIBUTE}, new String[] {String.valueOf(wifiOnly)},
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

    public String addRegexCommand(int id, String on, String regex, String cmd) {
        String output = XMLPrefsManager.add(rssIndexFile, REGEX_CMD_LABEL, new String[] {ID_ATTRIBUTE, ON_ATTRIBUTE, XMLPrefsManager.VALUE_ATTRIBUTE, CMD_ATTRIBUTE},
                new String[] {String.valueOf(id), on, regex, cmd});
        if(output == null) {
            cmdRegexes.add(new CmdableRegex(id, on, regex, cmd));
            return null;
        } else {
            if(output.length() > 0) return output;
            return context.getString(R.string.output_error);
        }
    }

    public String rmRegexCommand(int id) {
        String output = XMLPrefsManager.removeNode(rssIndexFile, REGEX_CMD_LABEL, new String[] {ID_ATTRIBUTE}, new String[] {String.valueOf(id)});
        if(output == null) {
            for(int i = 0; i < cmdRegexes.size(); i++) {
                if(cmdRegexes.get(i).id == id) cmdRegexes.remove(i);
            }

            return null;
        } else {
            if(output.length() > 0) return output;
            return context.getString(R.string.id_notfound);
        }
    }

//    base methods

    private Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            for(Rss feed : feeds) {
                if(feed.needUpdate()) updateRss(feed, false);
            }

            handler.postDelayed(this, RSS_CHECK_DELAY);
        }
    };

    private void updateRss(final Rss feed, final boolean firstTime) {
        updateRss(feed, firstTime, false);
    }

    public boolean updateRss(int feed, final boolean firstTime, boolean force) {
        Rss rss = findId(feed);
        if(rss == null) return false;

        updateRss(rss, firstTime, force);
        return true;
    }

    private void updateRss(final Rss feed, final boolean firstTime, final boolean force) {
        if(!force && feed.wifiOnly && !connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnected()) {
            feed.lastCheckedClient = System.currentTimeMillis();
            feed.updateFile(rssIndexFile);

            return;
        }

        new StoppableThread() {
            @Override
            public void run() {
                super.run();

                if(!Tuils.hasInternetAccess()) {
                    if(force) Tuils.sendOutput(Color.RED, context, R.string.no_internet);
                    return;
                }

                try {
                    Request.Builder builder = new Request.Builder()
                            .url(feed.url)
                            .get();

//                    if(!firstTime && feed.lMod != null && feed.etag != null) {
//                        builder.addHeader(IF_MODIFIED_SINCE_FIELD, feed.lMod);
//                        builder.addHeader(IF_NONE_MATCH_FIELD, quotes + feed.etag + quotes);
//                    }

                    Response response = client.newCall(builder.build()).execute();

                    if(response.isSuccessful() && (firstTime || response.code() != 304)) {
                        ResponseBody body = response.body();

                        long bytes = 0;
                        if(body != null) bytes = Tuils.download(new BufferedInputStream(body.byteStream()), new File(root, RSS_LABEL + feed.id + ".xml"));

                        if(showDownloadMessage) {
                            CharSequence c = Tuils.span(downloadFormat, downloadMessageColor);

                            double kb = (double) bytes / (double) 1024;
                            double mb = kb / (double) 1024;
                            double gb = mb / (double) 1024;

                            kb = Tuils.round(kb, 2);
                            mb = Tuils.round(mb, 2);
                            gb = Tuils.round(gb, 2);

                            c = urlPattern.matcher(c).replaceAll(feed.url);
                            c = idPattern.matcher(c).replaceAll(String.valueOf(feed.id));
                            c = gbPattern.matcher(c).replaceAll(String.valueOf(gb));
                            c = mbPattern.matcher(c).replaceAll(String.valueOf(mb));
                            c = kbPattern.matcher(c).replaceAll(String.valueOf(kb));
                            c = bPattern.matcher(c).replaceAll(String.valueOf(bytes));

                            c = TimeManager.instance.replace(c);

                            Tuils.sendOutput(downloadMessageColor, context, c);
                        }

                        if(bytes == 0) {
                            Tuils.sendOutput(Color.RED, context, context.getString(R.string.rss_invalid_empty) + Tuils.SPACE + feed.id);
                            return;
                        }

//                        feed.lMod = response.header(LAST_MODIFIED_FIELD);
//                        feed.etag = response.header(ETAG_FIELD);
//                        if(feed.etag != null) feed.etag = feed.etag.replaceAll("\"", Tuils.EMPTYSTRING);

                        response.close();

                        if(feed.show) parse(feed, true);
                    } else {
//                        not modified
                    }

                    feed.lastCheckedClient = System.currentTimeMillis();
                    feed.updateFile(rssIndexFile);

                } catch (Exception e) {
                    Tuils.log(e);
                    Tuils.toFile(e);
                }
            }
        }.start();
    }

    private boolean parse(Rss feed, boolean time) throws Exception {
        boolean updated = false;

        File rssFile = new File(root, RSS_LABEL + feed.id + ".xml");
        if(!rssFile.exists()) return false;

        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();

        Document doc;
        try {
            doc = dBuilder.parse(rssFile);
        } catch (SAXParseException e) {
            Tuils.sendXMLParseError(context, PATH, e);
            return false;
        }

        doc.getDocumentElement().normalize();

        long greatestTime = -1;
        boolean foundOneDateAtLeast = false;

        String entryTag = feed.entryTag != null ? feed.entryTag : ENTRY_CHILD;
        String dateTag = feed.dateTag != null ? feed.dateTag : PUBDATE_CHILD;

        NodeList list = doc.getElementsByTagName(entryTag);
        if(list.getLength() == 0) {
            Tuils.sendOutput(Color.RED, context, context.getString(R.string.rss_invalid_entry_tag) + Tuils.SPACE + (entryTag));
            return false;
        }

        for(int c = list.getLength(); c >= 0; c--) {

            Element element = (Element) list.item(c);
            if(element == null) {
                continue;
            }

            if(time) {
                NodeList l = element.getElementsByTagName(dateTag);
                if(l.getLength() == 0) continue;

                foundOneDateAtLeast = true;

                String date = l.item(0).getTextContent();

                Date d;
                try {
                    d = feed.timeFormat != null ? feed.timeFormat.parse(date) : defaultRSSDateFormat.parse(date);
                } catch (Exception e) {
                    Tuils.sendOutput(Color.RED, context, rssFile.getName() + ": " + context.getString(R.string.rss_invalid_timeformat));
                    return false;
                }

                long timeLong = d.getTime();
                greatestTime = Math.max(greatestTime, timeLong);

                if(feed.lastShownItem < timeLong) {
                    updated = true;
                    showItem(feed, element, false);
                }
            } else {
//                user - requested
                updated = true;
                showItem(feed, element, true);
            }
        }

        if(time && !foundOneDateAtLeast) {
            Tuils.sendOutput(Color.RED, context, context.getString(R.string.rss_invalid_date) + Tuils.SPACE + (dateTag));
        } else if(greatestTime != -1) {
            feed.lastShownItem = greatestTime;
            feed.updateLastShownItem(rssIndexFile);
        }

        return updated;
    }

    private final Pattern formatPattern = Pattern.compile("%(?:\\[(\\d+)\\])?(?:\\[([^]]+)\\])?([a-zA-Z]+)");
    private final Pattern removeTags = Pattern.compile("<[^>]+>");
    private final String THREE_DOTS = "...";

    private final String OPEN_URL = "search -u ";
    private final String PERCENTAGE = "%";

//    called when a new element is detected, it could be triggered many times again in some milliseconds
    private void showItem(Rss feed, Element item, boolean userRequested) {
        if(item == null) return;

        String cp = feed.format != null ? feed.format : defaultFormat;
        cp = Tuils.patternNewline.matcher(cp).replaceAll(Tuils.NEWLINE);

        CharSequence s = Tuils.span(cp, feed.color);

        String dateTag = feed.dateTag == null ? PUBDATE_CHILD : feed.dateTag;

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
                    if(value != null) value = value.trim();
                    else value = Tuils.EMPTYSTRING;

                    if(tag.equals(dateTag)) {
                        Date d;
                        try {
                            d = feed.timeFormat != null ? feed.timeFormat.parse(value) : defaultRSSDateFormat.parse(value);
                        } catch (ParseException e) {
                            Tuils.log(e);
                            continue;
                        }

                        long timeLong = d.getTime();
                        value = TimeManager.instance.replace(timeFormat, timeLong, Integer.MAX_VALUE).toString();
                    } else {
                        value = HtmlEscape.unescapeHtml(value);

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
            if(feed.excludeIfMatches != null && feed.excludeIfMatches.matcher(s.toString()).find()) return;
        } else {
            if(feed.includeIfMatches != null && feed.includeIfMatches.matcher(s.toString()).find()) return;
        }

        if(!userRequested) {
            for(CmdableRegex r : cmdRegexes) {
                if(r.regex == null || !Tuils.arrayContains(r.on, feed.id)) continue;

                String cmd = r.cmd;

                Matcher rssMatcher = r.regex.matcher(s.toString());
                if(rssMatcher.matches() || rssMatcher.find()) {

                    for(int c = 1; c < rssMatcher.groupCount() + 1; c++) {
                        cmd = cmd.replaceAll(PERCENTAGE + c, rssMatcher.group(c));
                    }

                    Intent intent = new Intent(MainManager.ACTION_EXEC);
                    intent.putExtra(MainManager.NEED_WRITE_INPUT, false);
                    intent.putExtra(MainManager.CMD, cmd);
                    intent.putExtra(MainManager.CMD_COUNT, MainManager.commandCount);
                    LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                }
            }
        }

        String url = null;
        NodeList list = item.getElementsByTagName(LINK_CHILD);
        if(list.getLength() != 0) {
            Node n = list.item(0);
            url = n.getTextContent();

            if(n.getNodeType() == Node.ELEMENT_NODE && (url == null || url.length() == 0)) {
                url = ((Element) n).getAttribute(HREF_ATTRIBUTE);
            }
        }

        String action;
        if(url == null || url.length() == 0) action = null;
        else action = OPEN_URL + url;

        Tuils.sendOutput(context, s, TerminalManager.CATEGORY_NO_COLOR, click ? action : null);
    }

    public static class Rss {
        private static final String ID_LABEL = "ID", URL_LABEL = "URL", UPDATE_TIME_LABEL = "update time", SHOW_LABEL = "show";

        public String url;
        public long updateTimeSeconds, lastCheckedClient, lastShownItem;
        public int id;
        public boolean show;
//        public String lMod, etag;

        public String entryTag, dateTag;

        public String format;

        public Pattern includeIfMatches, excludeIfMatches;
        public String tempInclude, tempExclude;

        public int color;

        public boolean wifiOnly;

        public SimpleDateFormat timeFormat;

        public Rss(String url, long updateTimeSeconds, int id, boolean show) {
            this(url, updateTimeSeconds, -1, -1, id, show, null, null, null, Integer.MAX_VALUE, false, null, null, null);
        }

        public Rss(String url, long updateTimeSeconds, long lastCheckedClient, long lastShownItem, int id, boolean show, String format,
                   String includeIfMatches, String excludeIfMatches, int color, boolean wifiOnly, String timeFormat, String rootNode, String timeNode) {
            setAll(url, updateTimeSeconds, lastCheckedClient, lastShownItem, id, show, format, includeIfMatches, excludeIfMatches, color, wifiOnly, timeFormat, rootNode, timeNode);
        }

        public static Rss fromElement(Element t) {
            int id;
            try {
                id = Integer.parseInt(t.getAttribute(ID_ATTRIBUTE));
            } catch (Exception exc) {
                return null;
            }

            String url = t.getAttribute(URL_ATTRIBUTE);
            if(url == null) return null;

            long updateTime;
            try {
                updateTime = Long.parseLong(t.getAttribute(TIME_ATTRIBUTE));
            } catch (Exception e) {
//                default: 1/2 h
                updateTime = 60 * 30;
            }

            boolean show = true;
            try {
                show = Boolean.parseBoolean(t.getAttribute(SHOW_ATTRIBUTE));
            } catch (Exception e) {}

            long lastChecked = XMLPrefsManager.getLongAttribute(t, LASTCHECKED_ATTRIBUTE);
            long lastShown = XMLPrefsManager.getLongAttribute(t, LAST_SHOWN_ITEM_ATTRIBUTE);

            String format = XMLPrefsManager.getStringAttribute(t, FORMAT_ATTRIBUTE);
            String includeIfMatches = XMLPrefsManager.getStringAttribute(t, INCLUDE_ATTRIBUTE);
            String excludeIfMatches = XMLPrefsManager.getStringAttribute(t, EXCLUDE_ATTRIBUTE);
            int color;
            try {
                color = Color.parseColor(t.getAttribute(COLOR_ATTRIBUTE));
            } catch (Exception exc) {
                color = Integer.MAX_VALUE;
            }

            boolean wifiOnly = false;
            try {
                wifiOnly = Boolean.parseBoolean(t.getAttribute(WIFIONLY_ATTRIBUTE));
            } catch (Exception e) {}

            String timeFormat = XMLPrefsManager.getStringAttribute(t, TIME_FORMAT_ATTRIBUTE);

            String rootNode = XMLPrefsManager.getStringAttribute(t, ENTRY_TAG_ATTRIBUTE);
            String timeNode = XMLPrefsManager.getStringAttribute(t, DATE_TAG_ATTRIBUTE);

            return new Rss(url, updateTime, lastChecked, lastShown, id, show, format, includeIfMatches, excludeIfMatches, color, wifiOnly, timeFormat, rootNode, timeNode);
        }

        private void setAll(String url, long updateTimeSeconds, long lastCheckedClient, long lastShownItem, int id, boolean show, String format,
                            String includeIfMatches, String excludeIfMatches, int color, boolean wifiOnly, String timeFormat, String rootNode, String timeNode) {
            this.url = url;
            this.updateTimeSeconds = updateTimeSeconds;
            this.lastShownItem = lastShownItem;
            this.lastCheckedClient = lastCheckedClient;
            this.id = id;
            this.show = show;
//            this.lMod = lMod;
//            this.etag = etag;

            this.format = format;

            this.tempInclude = includeIfMatches;
            this.tempExclude = excludeIfMatches;

            this.color = color;

            this.wifiOnly = wifiOnly;

            if(timeFormat == null) this.timeFormat = null;
            else try {
                this.timeFormat = new SimpleDateFormat(timeFormat);
            } catch (Exception e) {
                this.timeFormat = null;
            }

            this.entryTag = rootNode;
            this.dateTag = timeNode;
        }

        public boolean needUpdate() {
//            Tuils.log("lc", lastCheckedClient);
//            Tuils.log(System.currentTimeMillis() - lastCheckedClient);
//            Tuils.log("up", updateTimeSeconds * 1000);
            return System.currentTimeMillis() - lastCheckedClient >= (updateTimeSeconds * 1000);
        }

        public void updateLastShownItem(File rssFile) {
            XMLPrefsManager.set(rssFile, RSS_LABEL, new String[] {ID_ATTRIBUTE}, new String[] {String.valueOf(id)}, new String[] {LAST_SHOWN_ITEM_ATTRIBUTE},
                    new String[] {String.valueOf(lastShownItem)}, false);
        }

        public void updateIncludeIfMatches() {
            if(includeIfMatches != null) {
                try {
                    int id = Integer.parseInt(tempInclude);
                    includeIfMatches = RegexManager.instance.get(id).regex;
                } catch (Exception exc) {
                    includeIfMatches = Pattern.compile(tempInclude);
                }
            }
        }

        public void updateExcludeIfMatches() {
            if(excludeIfMatches != null) {
                try {
                    int id = Integer.parseInt(tempExclude);
                    excludeIfMatches = RegexManager.instance.get(id).regex;
                } catch (Exception exc) {
                    includeIfMatches = Pattern.compile(tempExclude);
                }
            }
        }

        private void updateFile(File rssFile) {
            XMLPrefsManager.set(rssFile, RSS_LABEL, new String[] {ID_ATTRIBUTE}, new String[] {String.valueOf(id)},
                    new String[] {LASTCHECKED_ATTRIBUTE/*, LASTMODIFIED_ATTRIBUTE, ETAG_ATTRIBUTE*/}, new String[] {String.valueOf(lastCheckedClient)/*, lMod, etag*/},
                    false);
        }

        public void updateFormat(List<XMLPrefsManager.IdValue> formats) {
            if(format != null) {
                try {
                    int id = Integer.parseInt(format);
                    for(XMLPrefsManager.IdValue i : formats) {
                        if(id == i.id) format = i.value;
                    }
                } catch (Exception exc) {
//                  the format is personalized --> it can't be casted to int
                }
            }
        }

        public void setFormat(List<XMLPrefsManager.IdValue> formats, String format) {
            this.format = format;
            updateFormat(formats);
        }

        public void setIncludeIfMatches(String includeIfMatches) {
            tempInclude = includeIfMatches;
            updateIncludeIfMatches();
        }

        public void setExcludeIfMatches(String excludeIfMatches) {
            tempExclude = excludeIfMatches;
            updateExcludeIfMatches();
        }

        @Override
        public boolean equals(Object obj) {
            if(obj instanceof Rss) return id == ((Rss) obj).id;
            if(obj instanceof Integer) return id == (int) obj;
            return false;
        }

        @Override
        public String toString() {
            String dots = ":";
            return new StringBuilder().append(ID_LABEL).append(dots).append(Tuils.SPACE).append(id).append(Tuils.SPACE)
                    .append(URL_LABEL).append(dots).append(Tuils.SPACE).append(url).append(Tuils.SPACE)
                    .append(UPDATE_TIME_LABEL).append(dots).append(Tuils.SPACE).append(updateTimeSeconds).append(Tuils.SPACE)
                    .append(SHOW_LABEL).append(dots).append(Tuils.SPACE).append(show)
                    .toString();
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

    public Rss findId(int id) {
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
        if(!rssIndexFile.exists()) {
            try {
                rssIndexFile.createNewFile();
                XMLPrefsManager.resetFile(rssIndexFile, NAME);
                check = false;

                return check && root.list().length > 1;
            } catch (Exception e) {
                return false;
            }
        }

        return check;
    }

    private class CmdableRegex extends RegexManager.Regex {
        int[] on;
        String cmd;

        public CmdableRegex(int id, String on, String regex, String cmd) {
            this.id = id;

            this.literalPattern = regex;
            this.cmd = cmd;

            char separator = Tuils.firstNonDigit(on);
            if(separator == 0) {
                try {
                    this.on = new int[] {Integer.parseInt(on)};
                } catch (Exception e) {
                    Tuils.log(e);
                }
            } else {
                if(separator == ' ') {
                    char s2 = Tuils.firstNonDigit(Tuils.removeSpaces(on));
                    if(s2 != 0) {
                        on = Tuils.removeSpaces(on);
                        separator = s2;
                    }
                }

                String[] split = on.split(Pattern.quote(separator + Tuils.EMPTYSTRING));
                this.on = new int[split.length];

                for(int c = 0; c < split.length; c++) {
                    try {
                        this.on[c] = Integer.parseInt(split[c]);
                    } catch (Exception e) {
                        Tuils.log(e);
                        this.on[c] = Integer.MAX_VALUE;
                    }
                }
            }
        }
    }
}
