package ohi.andre.consolelauncher.managers;

import android.content.Context;
import android.content.Intent;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.tuils.InputOutputReceiver;
import ohi.andre.consolelauncher.tuils.Tuils;

/**
 * Created by francescoandreuzzi on 19/08/2017.
 */

public class ThemesManager {

    static DocumentBuilderFactory factory;
    static DocumentBuilder builder;

    static {
        factory = DocumentBuilderFactory.newInstance();
        try {
            builder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {}
    }

    public static void apply(final Context context, final String name) {
        FirebaseDatabase.getInstance().getReference().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                DataSnapshot theme = dataSnapshot.child("themes").child(name);
                if(theme != null && theme.exists()) {

                    File currentTheme = new File(Tuils.getFolder(), "theme.xml");
                    File currentSuggestions = new File(Tuils.getFolder(), "suggestions.xml");

                    String themePath = currentTheme.getAbsolutePath();
                    String suggestionsPath = currentSuggestions.getAbsolutePath();

                    File old = new File(Tuils.getFolder(), "old");
                    if(old.exists()) Tuils.delete(old);
                    old.mkdir();

                    currentTheme.renameTo(new File(old, "theme.xml"));
                    currentSuggestions.renameTo(new File(old, "suggestions.xml"));

                    theme = theme.child("files");
                    DataSnapshot t = theme.child("THEME");
                    DataSnapshot s = theme.child("SUGGESTIONS");

                    createFile(themePath, "THEME", t);
                    createFile(suggestionsPath, "SUGGESTIONS", s);

                    Intent intent = new Intent(InputOutputReceiver.ACTION_OUTPUT);
                    intent.putExtra(InputOutputReceiver.TEXT, context.getString(R.string.theme_done));
                    context.sendBroadcast(intent);
                } else {
                    Intent intent = new Intent(InputOutputReceiver.ACTION_OUTPUT);
                    intent.putExtra(InputOutputReceiver.TEXT, context.getString(R.string.theme_not_found));
                    context.sendBroadcast(intent);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });
    }

    private static Pattern rgbaExtractor = Pattern.compile("rgba\\(([0-9]+),([0-9]+),([0-9]+),([0-9]+)\\)");

    private static void createFile(String path, String root, DataSnapshot s) {
        File file = new File(path);
        XMLPrefsManager.resetFile(file, root);

        try {
            Document document = builder.parse(file);
            Element r = (Element) document.getElementsByTagName(root).item(0);

            for(DataSnapshot ds : s.getChildren()) {
                String value = ds.getValue().toString();
                if(value.startsWith("rgba(")) value = rgbaToHex(value);

                Element em = document.createElement(ds.getKey());
                em.setAttribute(XMLPrefsManager.VALUE_ATTRIBUTE, value);
                r.appendChild(em);
            }

            XMLPrefsManager.writeTo(document, file);

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
