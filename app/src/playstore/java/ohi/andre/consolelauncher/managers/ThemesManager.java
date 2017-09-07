package ohi.andre.consolelauncher.managers;

import android.content.Context;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.tuils.Tuils;

/**
 * Created by francescoandreuzzi on 19/08/2017.
 */

public class ThemesManager {

    public static void apply(final Context context, final String name) {
        final DatabaseReference reference = FirebaseDatabase.getInstance().getReference();

        reference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                DataSnapshot theme = dataSnapshot.child("themes").child(name);
                if(theme != null && theme.exists()) {

                    File currentTheme = new File(Tuils.getFolder(), "theme.xml");
                    File currentSuggestions = new File(Tuils.getFolder(), "suggestions.xml");

                    String themePath = currentTheme.getAbsolutePath();
                    String suggestionsPath = currentSuggestions.getAbsolutePath();

                    Tuils.insertOld(currentTheme);
                    Tuils.insertOld(currentSuggestions);

                    Object o;

                    String author = Tuils.EMPTYSTRING;
                    if(theme.hasChild("author")) {
                        o = theme.child("author").getValue();
                        if(o != null) {
                            author = o.toString();
                        }
                    }

                    if(theme.hasChild("downloads")) {
                        o = theme.child("downloads").getValue();
                        if (o != null) {
                            try {
                                long downloads = Long.parseLong(o.toString());
                                reference.child("themes").child(name).child("downloads").setValue(downloads + 1);
                            } catch (Exception e) {
                                Tuils.log(e);
                            }
                        }
                    }

                    theme = theme.child("files");
                    if(theme == null || !theme.exists()) {
                        Tuils.sendOutput(context, R.string.theme_invalid);
                        return;
                    }

                    DataSnapshot t = theme.child("THEME");
                    DataSnapshot s = theme.child("SUGGESTIONS");

                    if(t == null || !t.exists() || s == null || !s.exists()) {
                        Tuils.sendOutput(context, R.string.theme_invalid);
                        return;
                    }

                    createFile(themePath, "THEME", t);
                    createFile(suggestionsPath, "SUGGESTIONS", s);

                    Tuils.sendOutput(context, "Applied theme " + name + (author.length() > 0 ? " by " + author : Tuils.EMPTYSTRING));
                } else {
                    Tuils.sendOutput(context, R.string.theme_not_found);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Tuils.sendOutput(context, databaseError.getMessage());
            }
        });
    }

    public static void ls(final Context context) {
        FirebaseDatabase.getInstance().getReference().addListenerForSingleValueEvent(new ValueEventListener() {

            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<String> strings = new ArrayList<>();

                DataSnapshot themes = dataSnapshot.child("themes");
                for(DataSnapshot s : themes.getChildren()) {
                    String name = s.getKey();
                    if(name.startsWith("custom_theme")) continue;

                    strings.add(s.getKey());
                }

                Collections.sort(strings);
                Tuils.addPrefix(strings, Tuils.DOUBLE_SPACE);
                Tuils.insertHeaders(strings, false);

                Tuils.sendOutput(context, Tuils.toPlanString(strings, Tuils.NEWLINE));
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });
    }

    private static Pattern rgbaExtractor = Pattern.compile("rgba\\(([0-9]+),([0-9]+),([0-9]+),([0-9]+)\\)");

    private static void createFile(String path, String r, DataSnapshot s) {
        File file = new File(path);
        XMLPrefsManager.resetFile(file, r);

        try {
            Object[] o;
            try {
                o = XMLPrefsManager.buildDocument(file, r);
            } catch (Exception e) {
                return;
            }

            Document d = (Document) o[0];
            Element root = (Element) o[1];

            for(DataSnapshot ds : s.getChildren()) {
                String value = ds.getValue().toString();
                if(value.startsWith("rgba(")) value = rgbaToHex(value);

                Element em = d.createElement(ds.getKey());
                em.setAttribute(XMLPrefsManager.VALUE_ATTRIBUTE, value);
                root.appendChild(em);
            }

            XMLPrefsManager.writeTo(d, file);

        } catch (Exception e) {
            Tuils.log(e);
        }
    }

    private static String rgbaToHex(String value) {
        StringBuilder builder = new StringBuilder();
        builder.append("#");

        Matcher matcher = rgbaExtractor.matcher(value);
        if(matcher.find()) {
            try {
                String redString = matcher.group(1);
                String greenString = matcher.group(2);
                String blueString = matcher.group(3);
                String alphaString = matcher.group(4);

                int alpha = (int) (Float.parseFloat(alphaString) * 255);

                String redHex = Integer.toHexString(Integer.parseInt(redString));
                if(redHex.length() == 1) redHex = "0" + redHex;

                String greenHex = Integer.toHexString(Integer.parseInt(greenString));
                if(greenHex.length() == 1) greenHex = "0" + greenHex;

                String blueHex = Integer.toHexString(Integer.parseInt(blueString));
                if(blueHex.length() == 1) blueHex = "0" + blueHex;

                String alphaHex = Integer.toHexString(alpha);
                if(alphaHex.length() == 1) alphaHex = "0" + alphaHex;

                builder.append(alphaHex).append(redHex).append(greenHex).append(blueHex);
            } catch (Exception e) {
                return null;
            }
        }

        return builder.toString();
    }
}
