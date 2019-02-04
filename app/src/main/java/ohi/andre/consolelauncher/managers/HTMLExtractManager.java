package ohi.andre.consolelauncher.managers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.Uri;
import android.support.v4.content.LocalBroadcastManager;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;

import com.jayway.jsonpath.JsonPath;

import net.minidev.json.JSONArray;

import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.jsoup.Jsoup;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXParseException;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ohi.andre.consolelauncher.BuildConfig;
import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.UIManager;
import ohi.andre.consolelauncher.managers.xml.XMLPrefsManager;
import ohi.andre.consolelauncher.managers.xml.options.Behavior;
import ohi.andre.consolelauncher.managers.xml.options.Theme;
import ohi.andre.consolelauncher.tuils.LongClickableSpan;
import ohi.andre.consolelauncher.tuils.Tuils;
import okhttp3.CacheControl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static ohi.andre.consolelauncher.managers.xml.XMLPrefsManager.resetFile;

/**
 * Created by francescoandreuzzi on 29/03/2018.
 */

public class HTMLExtractManager {

    public static String ACTION_ADD = BuildConfig.APPLICATION_ID + ".htmlextract_add";
    public static String ACTION_RM = BuildConfig.APPLICATION_ID + ".htmlextract_rm";
    public static String ACTION_EDIT = BuildConfig.APPLICATION_ID + ".htmlextract_edit";
    public static String ACTION_LS = BuildConfig.APPLICATION_ID + ".htmlextract_ls";

    public static String ACTION_QUERY = BuildConfig.APPLICATION_ID + ".htmlextract_query";
    public static String ACTION_WEATHER = BuildConfig.APPLICATION_ID + ".htmlextract_weather";

    public static String ID = "id";
    public static String FORMAT_ID = "formatId";
    public static String TAG_NAME = "tag";
    public static String WEATHER_AREA = "wArea";

    public static String BROADCAST_COUNT = "broadcastCount";

    public static String PATH = "htmlextract.xml", NAME = "HTMLEXTRACT";

    private List<StoreableValue> xpaths, jsons, formats;

    private OkHttpClient client;
    private BroadcastReceiver receiver;

    public static int broadcastCount;

    String defaultFormat, weatherFormat;
    int weatherColor;

    public HTMLExtractManager(Context context, OkHttpClient client) {
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                if(intent.getIntExtra(BROADCAST_COUNT, 0) < broadcastCount) return;
                broadcastCount++;

                String action = intent.getAction();

                if(action.equals(ACTION_ADD)) {
                    int id = intent.getIntExtra(ID, Integer.MAX_VALUE);
                    String tag = intent.getStringExtra(TAG_NAME);
                    String path = intent.getStringExtra(XMLPrefsManager.VALUE_ATTRIBUTE);

                    if(tag.equals(StoreableValue.Type.format.name())) {
                        for(int c = 0; c < formats.size(); c++) {
                            if(formats.get(c).id == id) {
                                Tuils.sendOutput(context, R.string.id_already);
                                return;
                            }
                        }
                    } else {
                        for(int c = 0; c < xpaths.size(); c++) {
                            if(xpaths.get(c).id == id) {
                                Tuils.sendOutput(context, R.string.id_already);
                                return;
                            }
                        }

                        for(int c = 0; c < jsons.size(); c++) {
                            if(jsons.get(c).id == id) {
                                Tuils.sendOutput(context, R.string.id_already);
                                return;
                            }
                        }
                    }

                    List<StoreableValue> values;
                    try {
                        StoreableValue.Type p = StoreableValue.Type.valueOf(tag);
                        values = getListFromType(p);
                    } catch (Exception e) {
                        return;
                    }

                    StoreableValue v = StoreableValue.create(values, context, tag, path, id);
                    if(v != null) values.add(v);
                } else if(action.equals(ACTION_RM)) {
                    int id = intent.getIntExtra(ID, Integer.MAX_VALUE);

                    boolean check = false;
                    for(int c = 0; c < xpaths.size(); c++) {
                        if(xpaths.get(c).id == id) {
                            xpaths.remove(c).remove(context);
                            check = true;
                            break;
                        }
                    }

                    for(int c = 0; c < jsons.size(); c++) {
                        if(jsons.get(c).id == id) {
                            jsons.remove(c).remove(context);
                            check = true;
                            break;
                        }
                    }

                    for(int c = 0; c < formats.size(); c++) {
                        if(formats.get(c).id == id) {
                            formats.remove(c).remove(context);
                            check = true;
                            break;
                        }
                    }

                    if(!check) Tuils.sendOutput(context, R.string.id_notfound);
                } else if(action.equals(ACTION_EDIT)) {
                    int id = intent.getIntExtra(ID, Integer.MAX_VALUE);
                    String newExpression = intent.getStringExtra(XMLPrefsManager.VALUE_ATTRIBUTE);
                    if(newExpression == null || newExpression.length() == 0) return;

                    for(int c = 0; c < xpaths.size(); c++) {
                        if(xpaths.get(c).id == id) {
                            xpaths.get(c).edit(context, newExpression);
                            return;
                        }
                    }

                    for(int c = 0; c < jsons.size(); c++) {
                        if(jsons.get(c).id == id) {
                            jsons.get(c).edit(context, newExpression);
                            return;
                        }
                    }

                    for(int c = 0; c < formats.size(); c++) {
                        if(formats.get(c).id == id) {
                            formats.get(c).edit(context, newExpression);
                            return;
                        }
                    }

                    Tuils.sendOutput(context, R.string.id_notfound);
                } else if(action.equals(ACTION_LS)) {
                    String tag = intent.getStringExtra(TAG_NAME);

                    List<StoreableValue> values;
                    StringBuilder builder = new StringBuilder();
                    try {
                        StoreableValue.Type p = StoreableValue.Type.valueOf(tag);
                        values = getListFromType(p);

                        for(StoreableValue v : values) {
                            builder.append("- ID: ").append(v.id).append(" -> ").append(v.value).append(Tuils.NEWLINE);
                        }
                    } catch (Exception e) {
                        builder.append("XPaths:").append(Tuils.NEWLINE);
                        if(xpaths.size() == 0) builder.append("[]").append(Tuils.NEWLINE);
                        else {
                            for(StoreableValue v : xpaths) {
                                builder.append(Tuils.DOUBLE_SPACE).append("- ID: ").append(v.id).append(" -> ").append(v.value).append(Tuils.NEWLINE);
                            }
                        }

                        builder.append("JsonPaths:").append(Tuils.NEWLINE);
                        if(jsons.size() == 0) builder.append("[]").append(Tuils.NEWLINE);
                        else {
                            for(StoreableValue v : jsons) {
                                builder.append(Tuils.DOUBLE_SPACE).append("- ID: ").append(v.id).append(" -> ").append(v.value).append(Tuils.NEWLINE);
                            }
                        }

                        builder.append("Formats:").append(Tuils.NEWLINE);
                        if(formats.size() == 0) builder.append("[]").append(Tuils.NEWLINE);
                        else {
                            for(StoreableValue v : formats) {
                                builder.append(Tuils.DOUBLE_SPACE).append("- ID: ").append(v.id).append(" -> ").append(v.value).append(Tuils.NEWLINE);
                            }
                        }
                    }

                    String text = builder.toString().trim();
                    if(text.length() == 0) text = "[]";
                    Tuils.sendOutput(context, text);
                } else if(action.equals(ACTION_QUERY)) {
                    String website = intent.getStringExtra(XMLPrefsManager.VALUE_ATTRIBUTE);
                    boolean weatherArea = intent.getBooleanExtra(WEATHER_AREA, false);

                    String path = intent.getStringExtra(ID);
                    String format = intent.getStringExtra(FORMAT_ID);

                    if(format == null) {
                        int formatId = intent.getIntExtra(FORMAT_ID, Integer.MAX_VALUE);

                        if(formatId == Integer.MAX_VALUE) {
//                            use the default format
                            format = null;
                        } else {
                            for(StoreableValue f : formats) {
                                if(f.id == formatId) {
                                    format = f.value;
                                    break;
                                }
                            }

                            if(format == null) {
                                Tuils.sendOutput(context, context.getString(R.string.id_notfound) + ": " + formatId + "(" + StoreableValue.Type.format.name() + ")");
                            }
                        }
                    }

                    StoreableValue.Type pathType = StoreableValue.Type.json;
                    if(path == null) {
                        int pathId = intent.getIntExtra(ID, Integer.MAX_VALUE);

                        for(StoreableValue p : xpaths) {
                            if (p.id == pathId) {
                                path = p.value;
                                pathType = p.type;
                                break;
                            }
                        }

                        for(StoreableValue p : jsons) {
                            if (p.id == pathId) {
                                path = p.value;
                                pathType = p.type;
                                break;
                            }
                        }

                        if(path == null) {
                            Tuils.sendOutput(context, context.getString(R.string.id_notfound) + ": " + pathId);
                            return;
                        }
                    }

                    query(context, path, pathType, format, website, weatherArea);
                } else if(action.equals(ACTION_WEATHER)) {
                    String url = intent.getStringExtra(XMLPrefsManager.VALUE_ATTRIBUTE);
                    query(context, weatherFormat, url);
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_ADD);
        filter.addAction(ACTION_RM);
        filter.addAction(ACTION_EDIT);
        filter.addAction(ACTION_LS);
        filter.addAction(ACTION_QUERY);
        filter.addAction(ACTION_WEATHER);

        this.client = client;

        linkColor = XMLPrefsManager.getColor(Theme.link_color);
        outputColor = XMLPrefsManager.getColor(Theme.output_color);
        weatherColor = XMLPrefsManager.getColor(Theme.weather_color);
        defaultFormat = XMLPrefsManager.get(Behavior.htmlextractor_default_format);
        optionalValueSeparator = XMLPrefsManager.get(Behavior.optional_values_separator);
        weatherFormat = XMLPrefsManager.get(Behavior.weather_format);

        LocalBroadcastManager.getInstance(context.getApplicationContext()).registerReceiver(receiver, filter);
        broadcastCount = 0;

        xpaths = new ArrayList<>();
        jsons = new ArrayList<>();
        formats = new ArrayList<>();

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
        } catch (SAXParseException e) {
            Tuils.sendXMLParseError(context, PATH, e);
            return;
        } catch (Exception e) {
            Tuils.log(e);
            return;
        }

//        Document d = (Document) o[0];
        Element root = (Element) o[1];

        NodeList nodes = root.getElementsByTagName("*");

        for (int count = 0; count < nodes.getLength(); count++) {
            Node n = nodes.item(count);

            try {
                StoreableValue v = StoreableValue.fromNode((Element) n);
                if(v != null) {
                    getListFromType(v.type).add(v);
                }
            } catch (Exception e) {}
        }
    }

    private List<StoreableValue> getListFromType(StoreableValue.Type t) {
        if(t == StoreableValue.Type.xpath) return xpaths;
        else if(t == StoreableValue.Type.json) return jsons;
        else return formats;
    }

    public void dispose(Context context) {
        LocalBroadcastManager.getInstance(context).unregisterReceiver(receiver);
    }

    Pattern weatherFormatPattern = Pattern.compile("%([a-z_]+)(\\d)*(?:\\$\\(([\\.\\+\\-\\*\\/\\^\\d]+)\\))?");

    private void query(final Context context, final String path, final StoreableValue.Type pathType, final String format, final String url, final boolean weatherArea) {
        new Thread() {
            @Override
            public void run() {
                super.run();

                if (!Tuils.hasInternetAccess()) {
                    output(R.string.no_internet, context, weatherArea);
                    return;
                }

                try {
                    Request.Builder builder = new Request.Builder()
                            .url(url)
                            .cacheControl(CacheControl.FORCE_NETWORK)
                            .addHeader("User-Agent", "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:15.0) Gecko/20100101 Firefox/15.0.1")
                            .get();

                    Response response = client.newCall(builder.build()).execute();

                    if(response.code() == 429 && weatherArea) {
                        Intent i = new Intent(UIManager.ACTION_WEATHER_DELAY);
                        LocalBroadcastManager.getInstance(context.getApplicationContext()).sendBroadcast(i);

                        return;
                    } else if(!response.isSuccessful()) {
                        String message = context.getString(R.string.internet_error) + Tuils.SPACE + response.code();

                        if(weatherArea) {
                            Intent i = new Intent(UIManager.ACTION_WEATHER);
                            i.putExtra(XMLPrefsManager.VALUE_ATTRIBUTE, message);
                            LocalBroadcastManager.getInstance(context.getApplicationContext()).sendBroadcast(i);
                        } else {
                            output(message, context, false);
                        }

                        return;
                    }

                    InputStream inputStream = response.body().byteStream();

                    CharSequence output = Tuils.span(Tuils.EMPTYSTRING, outputColor);

                    if(weatherArea) {
                        String json = Tuils.inputStreamToString(inputStream);
//                        json = json.replaceAll("\"temp\":([\\d\\.]*)", "\"temp\":-4.3");

                        CharSequence o = Tuils.span(weatherFormat, weatherColor);

                        Matcher m = weatherFormatPattern.matcher(weatherFormat);
                        while(m.find()) {
                            String name = m.group(1);
                            String delay = m.group(2);
                            if(delay == null || delay.length() == 0) delay = "1";
                            String converter = m.group(3);

                            int stopAt = Integer.parseInt(delay);

                            Pattern p = Pattern.compile("\"" + name + "\":(?:\"([^\"]+)\"|(-?\\d+\\.?\\d*))");
                            Matcher m1 = p.matcher(json);
                            int c = 1;
                            while(m1.find()) {
                                if(c == stopAt) {
                                    String value = m1.group(1);
                                    if(value == null || value.length() == 0) value = m1.group(2);

                                    if(converter != null && converter.length() > 0) {
                                        try {
                                            double d = Double.parseDouble(value);
                                            d = Tuils.textCalculus(d, converter);
                                            value = String.format("%.2f", d);
                                        } catch (Exception e) {
                                            Tuils.log(e);
                                        }
                                    }

                                    o = TextUtils.replace(o, new String[] {m.group(0)}, new String[] {delimiterStart + value + delimiterEnd});

                                    break;
                                } else c++;
                            }
                        }

                        o = replaceLinkColorReplace(context, o, url);
                        o = removeDelimiter(o);

                        Intent i = new Intent(UIManager.ACTION_WEATHER);
                        i.putExtra(XMLPrefsManager.VALUE_ATTRIBUTE, o);
                        LocalBroadcastManager.getInstance(context.getApplicationContext()).sendBroadcast(i);
                    } else if(pathType == StoreableValue.Type.xpath) {
                        HtmlCleaner cleaner = new HtmlCleaner();
                        CleanerProperties props = cleaner.getProperties();
                        props.setOmitComments(true);

                        TagNode node = cleaner.clean(inputStream);
                        Object[] nodes = node.evaluateXPath(path);
                        if(nodes.length == 0) {
                            Tuils.sendOutput(context, R.string.no_result);
                            return;
                        }

                        for(int c = 0; c < nodes.length; c++) {
                            node = (TagNode) nodes[c];

                            String f = format == null ? defaultFormat : format;
                            CharSequence copy = Tuils.span(f, outputColor);

                            copy = replaceAllAttributesString(copy, node.getAttributes().entrySet());
                            copy = replaceTagNameString(copy, node.getName(), node.getAttributes());
                            copy = replaceNodeValue(copy, node.getText().toString());
                            copy = replaceNewline(copy);
                            copy = replaceLinkColorReplace(context, copy, url);
                            copy = removeDelimiter(copy);

                            if(copy.toString().trim().length() > 0) output = TextUtils.concat(output, (c != 0 ? Tuils.NEWLINE + Tuils.NEWLINE : Tuils.EMPTYSTRING), copy);
                        }

                        output(output, context, weatherArea, TerminalManager.CATEGORY_NO_COLOR);
                    } else {
                        Object o = JsonPath.read(inputStream, path);

                        if(o instanceof Map) {
//                            this should be a single JSON object

                            String f = format == null ? defaultFormat : format;
                            CharSequence copy = Tuils.span(f, outputColor);

                            copy = replaceAllAttributesObject(copy, ((Map) o).entrySet());
                            copy = replaceTagNameObject(copy, null, (Map<String, Object>) o);
                            copy = replaceNewline(copy);
                            copy = replaceLinkColorReplace(context, copy, url);
                            copy = removeDelimiter(copy);

                            output = copy;

                            output(output, context, weatherArea, TerminalManager.CATEGORY_NO_COLOR);
                        } else if(o instanceof List) {
//                            this is an array of JSON objects
                            JSONArray a = (JSONArray) o;

                            for(int c = 0; c < a.size(); c++) {
                                String f = format == null ? defaultFormat : format;
                                CharSequence copy = Tuils.span(f, outputColor);

                                LinkedHashMap<String,Object> m = (LinkedHashMap<String, Object>) a.get(c);

                                copy = replaceAllAttributesObject(copy, m.entrySet());
                                copy = replaceTagNameObject(copy, null, m);
                                copy = replaceNewline(copy);
                                copy = replaceLinkColorReplace(context, copy, url);
                                copy = removeDelimiter(copy);

                                if(copy.toString().trim().length() > 0) output = TextUtils.concat(output, (c != 0 ? Tuils.NEWLINE + Tuils.NEWLINE : Tuils.EMPTYSTRING), copy);
                            }

                            output(output, context, weatherArea, TerminalManager.CATEGORY_NO_COLOR);
                        } else if(o instanceof String) {
                            output = Tuils.span(o.toString(), outputColor);
                            output(output, context, weatherArea, TerminalManager.CATEGORY_NO_COLOR);
                        } else {
                            Tuils.sendOutput(outputColor, context, o.toString());
                        }
                    }
                } catch (Exception e) {
                    output(e.toString(), context, weatherArea);
                    Tuils.toFile(e);
                    Tuils.log(e);
                }
            }
        }.start();
    }

    private void query(final Context context, final String format, final String url) {
        query(context, null, null, format, url, true);
    }

    private static void output(CharSequence s, Context context, boolean weatherArea) {
        output(s, context, weatherArea, Integer.MAX_VALUE);
    }

    private static void output(int string, Context context, boolean weatherArea) {
        output(context.getString(string), context, weatherArea);
    }

    private static void output(CharSequence s, Context context, boolean weatherArea, int category) {
        if(weatherArea) {
            Intent i = new Intent(UIManager.ACTION_WEATHER);
            i.putExtra(XMLPrefsManager.VALUE_ATTRIBUTE, s);
            LocalBroadcastManager.getInstance(context.getApplicationContext()).sendBroadcast(i);
        } else {
            if(category != Integer.MAX_VALUE) Tuils.sendOutput(context, s, category);
            else Tuils.sendOutput(context, s);
        }
    }

    private static void output(int string, Context context, boolean weatherArea, int category) {
        output(context.getString(string), context, weatherArea, category);
    }

    static Pattern tagName = Pattern.compile("%t(?:\\(([^)]*)\\))?", Pattern.CASE_INSENSITIVE);
    static String nodeValuePattern = "%v";

    static Pattern allAttributes = Pattern.compile("%a\\(([^\\)]*)\\)\\(([^\\)]*)\\)", Pattern.CASE_INSENSITIVE);
    static Pattern attributeName = Pattern.compile("%an", Pattern.CASE_INSENSITIVE);
    static Pattern attributeValue = Pattern.compile("%av", Pattern.CASE_INSENSITIVE);

    static int linkColor, outputColor;

    public static CharSequence removeDelimiter(CharSequence original) {
        CharSequence newSequence = original;
        do {
            original = newSequence;
            newSequence = TextUtils.replace(original, delimiterArray, delimiterReplacementArray);
        } while (newSequence.length() < original.length());

        return newSequence;
    }

    public static CharSequence replaceAllAttributesObject(CharSequence original, Set<Map.Entry<String, Object>> set) {
        Matcher allAttributesMatcher = allAttributes.matcher(original);
        if (allAttributesMatcher.find()) {
            List<Map.Entry<String, Object>> l = new ArrayList<>(set);

            String first = allAttributesMatcher.group(1);
            String separator = allAttributesMatcher.group(2);

            StringBuilder b = new StringBuilder();
            b.append(delimiterStart);
            for (int c = 0; c < l.size(); c++) {
                Map.Entry<String, Object> e = l.get(c);

                String temp = first;
                temp = attributeName.matcher(temp).replaceAll(e.getKey());
                temp = attributeValue.matcher(temp).replaceAll(Tuils.removeUnncesarySpaces(e.getValue().toString().trim()));

                b.append(temp);
                if(c != l.size() - 1) b.append(separator);
            }
            b.append(delimiterEnd);

            original = TextUtils.replace(original, new String[] {allAttributesMatcher.group()}, new CharSequence[] {b.toString().trim()});
        }

        return original;
    }

    public static CharSequence replaceTagNameObject(CharSequence original, String tag, Map<String, Object> attributes) {
        Matcher tagMatcher = tagName.matcher(original);
        while(tagMatcher.find()) {
            String attribute = tagMatcher.group(1);

            if(tag == null) tag = "null";

            String replace = "null";
            if(attribute == null || attribute.length() == 0) {
                replace = tag;
            } else if(attributes != null) {
                replace = attributes.get(attribute).toString();
                if(replace == null || replace.length() == 0) replace = "null";
            }

            return TextUtils.replace(original, new String[] {tagMatcher.group()}, new CharSequence[] {delimiterStart + replace + delimiterEnd});
        }

        return original;
    }

    public static CharSequence replaceAllAttributesString(CharSequence original, Set<Map.Entry<String, String>> set) {
        Matcher allAttributesMatcher = allAttributes.matcher(original);
        if (allAttributesMatcher.find()) {
            List<Map.Entry<String, String>> l = new ArrayList<>(set);

            String first = allAttributesMatcher.group(1);
            String separator = allAttributesMatcher.group(2);

            StringBuilder b = new StringBuilder();
            b.append(delimiterStart);
            for (int c = 0; c < l.size(); c++) {
                Map.Entry<String, String> e = l.get(c);

                String temp = first;
                temp = attributeName.matcher(temp).replaceAll(e.getKey());
                temp = attributeValue.matcher(temp).replaceAll(Tuils.removeUnncesarySpaces(e.getValue().trim()));

                b.append(temp);
                if(c != l.size() - 1) b.append(separator);
            }
            b.append(delimiterEnd);

            original = TextUtils.replace(original, new String[] {allAttributesMatcher.group()}, new CharSequence[] {b.toString().trim()});
        }

        return original;
    }

    public static CharSequence replaceTagNameString(CharSequence original, String tag, Map<String, String> attributes) {
        Matcher tagMatcher = tagName.matcher(original);
        while(tagMatcher.find()) {
            String attribute = tagMatcher.group(1);

            if(tag == null) tag = "null";

            String replace = "null";
            if(attribute == null || attribute.length() == 0) {
                replace = tag;
            } else if(attributes != null) {
                replace = attributes.get(attribute);
                if(replace == null || replace.length() == 0) replace = "null";
            }

            return TextUtils.replace(original, new String[] {tagMatcher.group()}, new CharSequence[] {delimiterStart + replace + delimiterEnd});
        }

        return original;
    }

    public static CharSequence replaceNodeValue(CharSequence original, String nodeValue) {
        nodeValue = Jsoup.parse(nodeValue).text();
        return TextUtils.replace(original, new String[] {nodeValuePattern}, new CharSequence[] {delimiterStart + Tuils.removeUnncesarySpaces(nodeValue).trim() + delimiterEnd});
    }

    public static CharSequence replaceNewline(CharSequence original) {
        int before;
        do {
            before = original.length();
            original = TextUtils.replace(original, new String[] {Tuils.patternNewline.pattern()}, new CharSequence[] {Tuils.NEWLINE});;
        } while (original.length() < before);

        return original;
    }

    private enum What {
        COLOR,
        LINK,
        REPLACE
    }

//    static Pattern linkColorReplace = Pattern.compile("#([a-zA-Z0-9]{6})?(?:\\[([^\\]]*)\\](@#&.*@#&)|\\[([^\\]]+)\\])", Pattern.CASE_INSENSITIVE);
    static Pattern colorPattern = Pattern.compile("(#[a-fA-F0-9]{6})\\[([^\\]]+)\\]");
    static Pattern linkPattern = Pattern.compile("#\\[((?:(?:http(?:s)?)|(?:www\\.))[^\\]]+)\\]");
    static Pattern replacePattern = Pattern.compile("#(\\[.+?\\])@#&(.+?)&#@");

    static Pattern extractUrl = Pattern.compile("(.*\\.[^\\/]{2,})\\/", Pattern.CASE_INSENSITIVE);

//    this is used to know where a group begins and when it ends
    static String delimiterStart = "@#&", delimiterEnd = new StringBuilder(delimiterStart).reverse().toString(), optionalValueSeparator;
    static String[] delimiterArray = {delimiterStart, delimiterEnd}, delimiterReplacementArray = {Tuils.EMPTYSTRING, Tuils.EMPTYSTRING};

    public static CharSequence replaceLinkColorReplace(Context context, CharSequence original, String url) {
        Matcher m = colorPattern.matcher(original);
        while(m.find()) {
            try {
                int cl = Color.parseColor(m.group(1));
                original = TextUtils.replace(original, new String[] {m.group()}, new CharSequence[] {Tuils.span(m.group(2), cl)});
            } catch (Exception e) {
                Tuils.sendOutput(context, context.getString(R.string.output_invalidcolor) + ": " + m.group(1));
            }
        }

        m = linkPattern.matcher(original);
        while(m.find()) {
            String text = m.group(1);

//            fix relative links
            if(text.startsWith("/")) {
                Matcher m1 = extractUrl.matcher(url);
                if(m1.find()) {
                    text = m1.group(1) + text;
                }
            }

            SpannableString sp = new SpannableString(text);
            sp.setSpan(new LongClickableSpan(Uri.parse(text)), 0, sp.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            sp.setSpan(new ForegroundColorSpan(linkColor), 0, sp.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            original = TextUtils.replace(original, new String[] {m.group()}, new CharSequence[] {sp});
        }

        m = replacePattern.matcher(original);
        while(m.find()) {
            String replaceGroups = m.group(1);
            String text = m.group(2);

            String[] groups = replaceGroups.split("]");
            for(int c = 0; c < groups.length; c++) {
                groups[c] = groups[c].replaceAll("[\\[\\]]", Tuils.EMPTYSTRING);

                String[] split = groups[c].split(optionalValueSeparator);
                if(split.length == 0) continue;

                text = text.replaceAll(split[0], split[1]);
            }

            original = TextUtils.replace(original, new String[] {m.group()}, new CharSequence[] {text});
        }

        return original;
    }

    public static class StoreableValue {

        public enum Type {
            xpath,
            json,
            format;
        }

        int id;
        String value;

        Type type;

        public StoreableValue(int id, String value, Type type) {
            this.id = id;
            this.value = value;
            this.type = type;
        }

        private StoreableValue(int id, String value, String type) {
            this.id = id;
            this.value = value;
            this.type = Type.valueOf(type);
        }

        public static StoreableValue fromNode(Element e) {
            String nn = e.getNodeName();

            int id = XMLPrefsManager.getIntAttribute(e, ID);
            String value = XMLPrefsManager.getStringAttribute(e, XMLPrefsManager.VALUE_ATTRIBUTE);

            try {
                return new StoreableValue(id, value, nn);
            } catch (Exception e1) {
                return null;
            }
        }

        public static StoreableValue create(List<StoreableValue> values, Context context, String tag, String path, int id) {
            for(int c = 0; c < values.size(); c++) {
                if(values.get(c).id == id) {
                    Tuils.sendOutput(context, R.string.id_already);
                    return null;
                }
            }

            File file = new File(Tuils.getFolder(), PATH);
            if(!file.exists()) {
                resetFile(file, NAME);
            }

            String output = XMLPrefsManager.add(file, tag, new String[] {ID, XMLPrefsManager.VALUE_ATTRIBUTE}, new String[] {String.valueOf(id), path});
            if(output != null) {
                if(output.length() > 0) Tuils.sendOutput(Color.RED, context, output);
                else Tuils.sendOutput(Color.RED, context, R.string.output_error);
                return null;
            }

            return new StoreableValue(id, path, tag);
        }

        public void remove(Context context) {
            File file = new File(Tuils.getFolder(), PATH);
            if(!file.exists()) {
                resetFile(file, NAME);
            }

            String output = XMLPrefsManager.removeNode(file, type.name(), new String[] {ID}, new String[] {String.valueOf(id)});
            if(output != null) {
                if(output.length() > 0) Tuils.sendOutput(Color.RED, context, output);
                else {
                    Tuils.sendOutput(Color.RED, context, R.string.id_notfound);
                }
            }
        }

        public void edit(Context context, String newExpression) {
            File file = new File(Tuils.getFolder(), PATH);
            if(!file.exists()) {
                resetFile(file, NAME);
            }

            String output = XMLPrefsManager.set(file, type.name(), new String[] {ID}, new String[] {String.valueOf(id)}, new String[] {XMLPrefsManager.VALUE_ATTRIBUTE}, new String[] {newExpression}, false);
            if(output != null) {
                if(output.length() > 0) Tuils.sendOutput(Color.RED, context, output);
                else {
                    Tuils.sendOutput(Color.RED, context, R.string.id_notfound);
                }
            } else {
                this.value = newExpression;
            }
        }
    }
}
