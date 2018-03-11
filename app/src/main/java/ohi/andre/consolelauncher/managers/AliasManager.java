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
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.managers.xml.XMLPrefsManager;
import ohi.andre.consolelauncher.managers.xml.options.Behavior;
import ohi.andre.consolelauncher.tuils.Tuils;

public class AliasManager {

    public static final String PATH = "alias.txt";

    private List<Alias> aliases;
    private String paramSeparator, aliasLabelFormat;
    private boolean replaceAllMarkers;

    private Context context;

    private Pattern parameterPattern;

    public AliasManager(Context c) {
        this.context = c;

        parameterPattern = Pattern.compile(Pattern.quote(XMLPrefsManager.get(Behavior.alias_param_marker)));
        paramSeparator = Pattern.quote(XMLPrefsManager.get(Behavior.alias_param_separator));
        aliasLabelFormat = XMLPrefsManager.get(Behavior.alias_content_format);
        replaceAllMarkers = XMLPrefsManager.getBoolean(Behavior.alias_replace_all_markers);

        reload();
    }

    public String printAliases() {
        String output = Tuils.EMPTYSTRING;
        for (Alias a : aliases) {
            output = output.concat(a.name + " --> " + a.value + Tuils.NEWLINE);
        }

        return output.trim();
    }

//    [0] = aliasValue
//    [1] = aliasName
//    [2] = residualString
    public String[] getAlias(String alias, boolean supportSpaces) {
        if(supportSpaces) {
            String args = Tuils.EMPTYSTRING;

            String aliasValue = null;
            while (true) {
                aliasValue = getALias(alias);
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
            return new String[] {getALias(alias), alias, Tuils.EMPTYSTRING};
        }
    }

    public String format(String aliasValue, String params) {
        params = params.trim();

        if(params.length() == 0) return aliasValue;
        String[] split = params.split(paramSeparator);

        for(String s : split) {
            aliasValue = parameterPattern.matcher(aliasValue).replaceFirst(s);
        }

        if(replaceAllMarkers) aliasValue = parameterPattern.matcher(aliasValue).replaceAll(split[0]);

        return aliasValue;
    }

    private String getALias(String name) {
        for(Alias a : aliases) {
            if(name.equals(a.name)) return a.value;
        }

        return null;
    }

    private void removeAlias(String name) {
        for(int c = 0; c < aliases.size(); c++) {
            Alias a = aliases.get(c);
            if(name.equals(a.name)) {
                aliases.remove(c);
                return;
            }
        }
    }

    private final Pattern pv = Pattern.compile("%v", Pattern.CASE_INSENSITIVE | Pattern.LITERAL);
    private final Pattern pa = Pattern.compile("%a", Pattern.CASE_INSENSITIVE | Pattern.LITERAL);
    public String formatLabel(String aliasName, String aliasValue) {
        String a = aliasLabelFormat;
        a = Tuils.patternNewline.matcher(a).replaceAll(Matcher.quoteReplacement(Tuils.NEWLINE));
        a = pv.matcher(a).replaceAll(Matcher.quoteReplacement(aliasValue));
        a = pa.matcher(a).replaceAll(Matcher.quoteReplacement(aliasName));
        return a;
    }

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
                    aliases.add(new Alias(name, value, parameterPattern));
                }
            }
        } catch (Exception e) {
            Tuils.log(e);
        }
    }

    public boolean add(String name, String value) {

        FileOutputStream fos;
        try {
            fos = new FileOutputStream(new File(Tuils.getFolder(), PATH), true);
            fos.write((Tuils.NEWLINE + name + "=" + value).getBytes());
            fos.close();

            aliases.add(new Alias(name, value, parameterPattern));

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


            removeAlias(name);

            return tempFile.renameTo(inputFile);
        } catch (Exception e) {
            return false;
        }
    }

    public List<Alias> getAliases(boolean excludeEmtpy) {
        List<Alias> l = new ArrayList<>(aliases);
        if(excludeEmtpy) {
            for(int c = 0; c < l.size(); c++) {
                if(l.get(c).name.length() == 0) {
                    l.remove(c);
                    break;
                }
            }
        }

        return l;
    }

    public static class Alias {
        public String name, value;
        public boolean isParametrized;

        public Alias(String name, String value, Pattern parameterPattern) {
            this.name = name;
            this.value = value;

            isParametrized = parameterPattern.matcher(value).find();
        }

        @Override
        public boolean equals(Object obj) {
            return (obj instanceof Alias && ((Alias) obj).name.equals(name)) || obj.equals(name);
        }
    }
}
