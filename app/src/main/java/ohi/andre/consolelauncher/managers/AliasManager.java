package ohi.andre.consolelauncher.managers;

import android.content.Context;
import android.graphics.Color;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.managers.xml.XMLPrefsManager;
import ohi.andre.consolelauncher.managers.xml.options.Behavior;
import ohi.andre.consolelauncher.tuils.Tuils;
import ohi.andre.consolelauncher.tuils.interfaces.Reloadable;

public class AliasManager implements Reloadable {

    public static final String PATH = "alias.txt";

    private List<Map.Entry<String, String>> aliases;
    private String paramMarker, paramSeparator, aliasLabelFormat;

    private Context context;

    public AliasManager(Context c) {
        this.context = c;

        reload();

        paramMarker = Pattern.quote(XMLPrefsManager.get(Behavior.alias_param_marker));
        paramSeparator = Pattern.quote(XMLPrefsManager.get(Behavior.alias_param_separator));
        aliasLabelFormat = XMLPrefsManager.get(Behavior.alias_content_format);
    }

    public String printAliases() {
        String output = Tuils.EMPTYSTRING;
        for (Map.Entry<String, String> entry : aliases) {
            output = output.concat(entry.getKey() + " --> " + entry.getValue() + Tuils.NEWLINE);
        }

        return output.trim();
    }

//    [0] = aliasValue
//    [1] = aliasName
//    [2] = residualString
    public String[] getAlias(String alias, boolean supportSpaces) {
        if(supportSpaces) {

//            String[] split = alias.split(Tuils.SPACE);
//            String name = Tuils.EMPTYSTRING;
//
//            for(int count = 0; count < split.length; count++) {
//                name += Tuils.SPACE + split[count];
//                name = name.trim();
//
//                String a = aliases.get(name);
//
//                if(a != null) {
//                    String residual = Tuils.EMPTYSTRING;
//                    for(int c = count + 1; c < split.length; c++) {
//                        residual += split[c] + Tuils.SPACE;
//                    }
//
//                    return new String[] {a, name, residual.trim()};
//                }
//            }

            String args = Tuils.EMPTYSTRING;

            String aliasValue = null;
            while (true) {
                aliasValue = getALiasFor(alias);
                if(aliasValue != null) break;
                else {
                    int index = alias.lastIndexOf(Tuils.SPACE);
                    if(index == -1) return new String[] {null, null, alias};

                    args = alias.substring(index + 1) + Tuils.SPACE + args;
                    args = args.trim();
                    alias = alias.substring(0,index);
                }
            }

            return new String[] {aliasValue, alias, args};
        } else {
            return new String[] {getALiasFor(alias), alias, Tuils.EMPTYSTRING};
        }
    }

    public String format(String aliasValue, String params) {
        params = params.trim();

        if(params.length() == 0) return aliasValue;
        String[] split = params.split(paramSeparator);

        for(String s : split) {
            aliasValue = aliasValue.replaceFirst(paramMarker, s);
        }

        return aliasValue;
    }

    private String getALiasFor(String name) {
        for(Map.Entry<String, String> entry : aliases) {
            if(name.equals(entry.getKey())) return entry.getValue();
        }

        return null;
    }

    private void removeAliasFor(String name) {
        for(int c = 0; c < aliases.size(); c++) {
            Map.Entry e = aliases.get(c);
            if(name.equals(e.getKey())) {
                aliases.remove(c);
                return;
            }
        }
    }

    private final Pattern pn = Pattern.compile("%n", Pattern.CASE_INSENSITIVE | Pattern.LITERAL);
    private final Pattern pv = Pattern.compile("%v", Pattern.CASE_INSENSITIVE | Pattern.LITERAL);
    private final Pattern pa = Pattern.compile("%a", Pattern.CASE_INSENSITIVE | Pattern.LITERAL);
    public String formatLabel(String aliasName, String aliasValue) {
        String a = aliasLabelFormat;
        a = pn.matcher(a).replaceAll(Matcher.quoteReplacement(Tuils.NEWLINE));
        a = pv.matcher(a).replaceAll(Matcher.quoteReplacement(aliasValue));
        a = pa.matcher(a).replaceAll(Matcher.quoteReplacement(aliasName));
        return a;
    }

    @Override
    public void reload() {
        if(aliases != null) aliases.clear();
        else aliases = new ArrayList<>();

        File root = Tuils.getFolder();
        if(root == null) return;

        File file = new File(root, PATH);

        try {
            if(!file.exists()) file.createNewFile();

            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));

            String line;
            while((line = reader.readLine()) != null) {
                String[] splatted = line.split("=");
                if(splatted.length < 2) continue;

                String name, value = Tuils.EMPTYSTRING;
                name = splatted[0];

                for(int c = 1; c < splatted.length; c++) {
                    value += splatted[c];
                    if(c != splatted.length - 1) value += "=";
                }

                name = name.trim();
                value = value.trim();

                if(name.equalsIgnoreCase(value)) {
                    Tuils.sendOutput(Color.RED, context,
                            context.getString(R.string.output_notaddingalias1) + Tuils.SPACE + name + Tuils.SPACE + context.getString(R.string.output_notaddingalias2));
                } else if(value.startsWith(name + Tuils.SPACE)) {
                    Tuils.sendOutput(Color.RED, context,
                            context.getString(R.string.output_notaddingalias1) + Tuils.SPACE + name + Tuils.SPACE + context.getString(R.string.output_notaddingalias3));
                } else {
                    aliases.add(new AbstractMap.SimpleImmutableEntry<>(name, value));
                }
            }
        } catch (Exception e) {}
    }

    public boolean add(String name, String value) {

        FileOutputStream fos;
        try {
            fos = new FileOutputStream(new File(Tuils.getFolder(), PATH), true);
            fos.write((Tuils.NEWLINE + name + "=" + value).getBytes());
            fos.close();

            aliases.add(new AbstractMap.SimpleImmutableEntry<>(name, value));

            return true;
        } catch (Exception e) {
            return false;
        }

    }

    public boolean remove(String name) {
        reload();

        try {
            File inputFile = new File(Tuils.getFolder(), PATH);
            File tempFile = new File(Tuils.getFolder(), PATH + "2");

            BufferedReader reader = new BufferedReader(new FileReader(inputFile));
            BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile));

            String prefix = name + "=";
            String line;
            while((line = reader.readLine()) != null) {
                if(line.startsWith(prefix)) continue;
                writer.write(line + Tuils.NEWLINE);
            }
            writer.close();
            reader.close();


            removeAliasFor(name);

            return tempFile.renameTo(inputFile);
        } catch (Exception e) {
            return false;
        }
    }

    public List<String> getAliases() {
        List<String> aliasKeys = new ArrayList<>(0);
        if(aliases == null) return aliasKeys;

        for(Map.Entry<String, String> entry : aliases) aliasKeys.add(entry.getKey());

        return aliasKeys;
    }
}
