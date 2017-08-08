package ohi.andre.consolelauncher.managers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import ohi.andre.consolelauncher.tuils.Tuils;
import ohi.andre.consolelauncher.tuils.interfaces.Reloadable;

public class AliasManager implements Reloadable {

    public static final String PATH = "alias.txt";

    private Map<String, String> aliases;

    private String paramMarker, paramSeparator, aliasLabelFormat;

    public AliasManager() {
        reload();

        paramMarker = Pattern.quote(XMLPrefsManager.get(String.class, XMLPrefsManager.Behavior.alias_param_marker));
        paramSeparator = Pattern.quote(XMLPrefsManager.get(String.class, XMLPrefsManager.Behavior.alias_param_separator));
        aliasLabelFormat = XMLPrefsManager.get(String.class, XMLPrefsManager.Behavior.alias_content_format);
    }

    public String printAliases() {
        String output = Tuils.EMPTYSTRING;
        for (Map.Entry<String, String> entry : aliases.entrySet()) {
            output = output.concat(entry.getKey() + " --> " + entry.getValue() + Tuils.NEWLINE);
        }

        return output.trim();
    }

//    [0] = aliasValue
//    [1] = aliasName
//    [2] = residualString
    public String[] getAlias(String alias, boolean supportSpaces) {
        if(supportSpaces) {

            String[] split = alias.split(Tuils.SPACE);
            String name = Tuils.EMPTYSTRING;

            for(int count = 0; count < split.length; count++) {
                name += Tuils.SPACE + split[count];
                name = name.trim();

                String a = aliases.get(name);

                if(a != null) {
                    String residual = Tuils.EMPTYSTRING;
                    for(int c = count + 1; c < split.length; c++) {
                        residual += split[c] + Tuils.SPACE;
                    }

                    return new String[] {a, name, residual.trim()};
                }
            }

            return new String[] {null, null, alias};
        } else {
            return new String[] {aliases.get(alias), alias, Tuils.EMPTYSTRING};
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

    private final Pattern pn = Pattern.compile("%n", Pattern.CASE_INSENSITIVE);
    private final Pattern pv = Pattern.compile("%v", Pattern.CASE_INSENSITIVE);
    private final Pattern pa = Pattern.compile("%a", Pattern.CASE_INSENSITIVE);
    public String formatLabel(String aliasName, String aliasValue) {
        String a = aliasLabelFormat;
        a = pn.matcher(a).replaceAll(Tuils.NEWLINE);
        a = pv.matcher(a).replaceAll(aliasValue);
        a = pa.matcher(a).replaceAll(aliasName);
        return a;
    }

    @Override
    public void reload() {
        if(aliases != null) aliases.clear();
        else aliases = new HashMap();

        File file = new File(Tuils.getFolder(), PATH);

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

                aliases.put(name, value);
            }
        } catch (Exception e) {}
    }

    public boolean add(String name, String value) {

        FileOutputStream fos;
        try {
            fos = new FileOutputStream(new File(Tuils.getFolder(), PATH), true);
            fos.write((Tuils.NEWLINE + name + "=" + value).getBytes());
            fos.close();

            aliases.put(name, value);
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


            aliases.remove(name);

            return tempFile.renameTo(inputFile);
        } catch (Exception e) {
            return false;
        }
    }

    public List<String> getAliases() {
        if(aliases == null) return new ArrayList<>(0);
        return new ArrayList<>(aliases.keySet());
    }
}
