package ohi.andre.consolelauncher.managers;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.text.style.UnderlineSpan;
import android.widget.TextView;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXParseException;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ohi.andre.consolelauncher.BuildConfig;
import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.commands.main.raw.shortcut;
import ohi.andre.consolelauncher.managers.xml.XMLPrefsManager;
import ohi.andre.consolelauncher.managers.xml.options.Behavior;
import ohi.andre.consolelauncher.managers.xml.options.Theme;
import ohi.andre.consolelauncher.managers.xml.options.Ui;
import ohi.andre.consolelauncher.tuils.LongClickableSpan;
import ohi.andre.consolelauncher.tuils.Tuils;

import static android.content.Context.CLIPBOARD_SERVICE;
import static ohi.andre.consolelauncher.managers.xml.XMLPrefsManager.VALUE_ATTRIBUTE;
import static ohi.andre.consolelauncher.managers.xml.XMLPrefsManager.resetFile;

/**
 * Created by francescoandreuzzi on 12/02/2018.
 */

public class NotesManager {

    public static String ACTION_RM = BuildConfig.APPLICATION_ID + ".rm_note";
    public static String ACTION_ADD = BuildConfig.APPLICATION_ID + ".add_note";
    public static String ACTION_CLEAR = BuildConfig.APPLICATION_ID + ".clear_notes";
    public static String ACTION_LS = BuildConfig.APPLICATION_ID + ".ls_notes";
    public static String ACTION_LOCK = BuildConfig.APPLICATION_ID + ".lock_notes";
    public static String ACTION_CP = BuildConfig.APPLICATION_ID + ".cp_notes";

    public static String BROADCAST_COUNT = "broadcastCount";
    public static String CREATION_TIME = "creationTime", TEXT = "text", LOCK = "lock";

    private final String PATH = "notes.xml", NAME = "NOTES", NOTE_NODE = "note";

    CharSequence oldNotes;
    public boolean hasChanged;

    Set<Class> classes;
    List<Note> notes;

    Pattern optionalPattern;
    String footer, header, divider;
    int color, lockedColor;

    boolean allowLink;
    int linkColor;

    BroadcastReceiver receiver;

    PackageManager packageManager;

    Context mContext;

    public static int broadcastCount;

//    noteview can't be changed too much, it may be shared
    public NotesManager(Context context, TextView noteView) {
        classes = new HashSet<>();
        notes = new ArrayList<>();

        broadcastCount = 0;

        this.mContext = context;

        packageManager = context.getPackageManager();

        String optionalSeparator = "\\" + XMLPrefsManager.get(Behavior.optional_values_separator);
        String optional = "%\\(([^" + optionalSeparator + "]*)" + optionalSeparator + "([^)]*)\\)";
        optionalPattern = Pattern.compile(optional, Pattern.CASE_INSENSITIVE);

        color = XMLPrefsManager.getColor(Theme.notes_color);
        lockedColor = XMLPrefsManager.getColor(Theme.notes_locked_color);

        footer = XMLPrefsManager.get(Ui.notes_footer);
        header = XMLPrefsManager.get(Ui.notes_header);
        divider = XMLPrefsManager.get(Ui.notes_divider);
        divider = Tuils.patternNewline.matcher(divider).replaceAll(Tuils.NEWLINE);

        allowLink = XMLPrefsManager.getBoolean(Behavior.notes_allow_link);
        if(allowLink && noteView != null) {
            noteView.setMovementMethod(new LinkMovementMethod());
            linkColor = XMLPrefsManager.getColor(Theme.link_color);
        }

        Note.sorting = XMLPrefsManager.getInt(Behavior.notes_sorting);

        load(context, true);

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_ADD);
        filter.addAction(ACTION_RM);
        filter.addAction(ACTION_CLEAR);
        filter.addAction(ACTION_LS);
        filter.addAction(ACTION_LOCK);
        filter.addAction(ACTION_CP);

        receiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent.getIntExtra(BROADCAST_COUNT, 0) < broadcastCount) return;
                broadcastCount++;

                if(intent.getAction().equals(ACTION_ADD)) {
                    String text = intent.getStringExtra(TEXT);
                    if(text == null) return;

                    boolean lock = false;
                    String[] split = text.split(Tuils.SPACE);
                    int startAt = 0;

                    String beforeSpace = split.length >= 2 ? split[0] : null;
                    if(beforeSpace != null) {
                        if((beforeSpace.equals("true") || beforeSpace.equals("false"))) {
                            lock = Boolean.parseBoolean(beforeSpace);
                            startAt++;
                        }

                        String[] ar = new String[split.length - startAt];
                        System.arraycopy(split, startAt, ar, 0, ar.length);
                        text = Tuils.toPlanString(ar, Tuils.SPACE);
                    }

                    addNote(text, lock);
                } else if(intent.getAction().equals(ACTION_RM)) {
                    String s = intent.getStringExtra(TEXT);
                    if(s == null) return;

                    rmNote(s);
                } else if(intent.getAction().equals(ACTION_CLEAR)) {
                    clearNotes(context);
                } else if(intent.getAction().equals(ACTION_LS)) {
                    lsNotes(context);
                } else if(intent.getAction().equals(ACTION_LOCK)) {
                    String text = intent.getStringExtra(TEXT);
                    boolean lock = intent.getBooleanExtra(LOCK, false);

                    lockNote(context, text, lock);
                } else if(intent.getAction().equals(ACTION_CP)) {
                    String s = intent.getStringExtra(TEXT);
                    if(s == null) return;

                    cpNote(s);
                }
            }
        };

        LocalBroadcastManager.getInstance(context.getApplicationContext()).registerReceiver(receiver, filter);
    }

    private void load(Context context, boolean loadClasses) {
        if(loadClasses) classes.clear();
        notes.clear();

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

        Element root = (Element) o[1];

        NodeList nodes = root.getElementsByTagName("*");

        for (int count = 0; count < nodes.getLength(); count++) {
            final Node node = nodes.item(count);

            if (node.getNodeType() == Node.ELEMENT_NODE) {
                final Element e = (Element) node;
                final String name = e.getNodeName();

                if(name.equals(NOTE_NODE)) {
                    long time = XMLPrefsManager.getLongAttribute(e, CREATION_TIME);
                    String text = XMLPrefsManager.getStringAttribute(e, XMLPrefsManager.VALUE_ATTRIBUTE);
                    boolean lock = XMLPrefsManager.getBooleanAttribute(e, LOCK);

                    notes.add(new Note(time, text, lock));
                } else if(loadClasses) {
                    int id;
                    try {
                        id = Integer.parseInt(name);
                    } catch (Exception ex) {
                        continue;
                    }

                    int color;
                    try {
                        color = Color.parseColor(XMLPrefsManager.getStringAttribute(e, VALUE_ATTRIBUTE));
                    } catch (Exception ex) {
                        continue;
                    }

                    classes.add(new Class(id, color));
                }
            }
        }

        Collections.sort(notes);

        invalidateNotes();
    }

    Pattern colorPattern = Pattern.compile("(\\d+|#[\\da-zA-Z]{6,8})\\(([^)]*)\\)");
    Pattern countPattern = Pattern.compile("%c", Pattern.CASE_INSENSITIVE);
    Pattern lockPattern = Pattern.compile("%l", Pattern.CASE_INSENSITIVE);
    Pattern rowPattern = Pattern.compile("%r", Pattern.CASE_INSENSITIVE);
    Pattern uriPattern = Pattern.compile("(http[s]?:[^\\s]+|www\\.[^\\s]*)\\.[a-z]+");

    private void invalidateNotes() {
        String header = this.header;
        Matcher mh = optionalPattern.matcher(header);
        while(mh.find()) {
            header = header.replace(mh.group(0), mh.groupCount() == 2 ? mh.group(notes.size() > 0 ? 1 : 2) : Tuils.EMPTYSTRING);
        }

        if(header.length() > 0) {
            String h = countPattern.matcher(header).replaceAll(String.valueOf(notes.size()));
            h = Tuils.patternNewline.matcher(h).replaceAll(Tuils.NEWLINE);
            oldNotes = Tuils.span(h, this.color);
        } else {
            oldNotes = Tuils.EMPTYSTRING;
        }

        CharSequence ns = Tuils.EMPTYSTRING;
        for(int j = 0; j < notes.size(); j++) {
            Note n = notes.get(j);

            CharSequence t = n.text;
            t = lockPattern.matcher(t).replaceAll(String.valueOf(n.lock));
            t = rowPattern.matcher(t).replaceAll(String.valueOf(j + 1));
            t = countPattern.matcher(t).replaceAll(String.valueOf(notes.size()));

            t = Tuils.span(t, n.lock ? lockedColor : this.color);

            t = TimeManager.instance.replace(t, n.creationTime);

            if(allowLink) {
                Matcher m = uriPattern.matcher(t);
                while(m.find()) {
                    String g = m.group();

//                    www.
                    if(g.startsWith("w")) {
                        g = "http://" + g;
                    }

                    Uri u = Uri.parse(g);
                    if(u == null) continue;

                    SpannableString sp = new SpannableString(m.group());
                    sp.setSpan(new LongClickableSpan(u), 0, sp.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    sp.setSpan(new ForegroundColorSpan(linkColor), 0, sp.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                    t = TextUtils.replace(t, new String[] {m.group()}, new CharSequence[] {sp});
                }
            }

            ns = TextUtils.concat(ns, t, j != notes.size() - 1 ? divider : Tuils.EMPTYSTRING);
        }

        oldNotes = TextUtils.concat(oldNotes, ns);

        String footer = this.footer;
        Matcher mf = optionalPattern.matcher(footer);
        while(mf.find()) {
            footer = footer.replace(mf.group(0), mf.groupCount() == 2 ? mf.group(notes.size() > 0 ? 1 : 2) : Tuils.EMPTYSTRING);
        }

        if(footer.length() > 0) {
            String h = countPattern.matcher(footer).replaceAll(String.valueOf(notes.size()));
            h = Tuils.patternNewline.matcher(h).replaceAll(Tuils.NEWLINE);
            oldNotes = TextUtils.concat(oldNotes, Tuils.span(h, this.color));
        } else {}

        Matcher m = colorPattern.matcher(oldNotes);
        while(m.find()) {
            String match = m.group();
            String idColor = m.group(1);
            CharSequence t = m.group(2);

            int color;
            if(idColor.startsWith("#")) {
//                    color
                try {
                    color = Color.parseColor(idColor);
                } catch (Exception e) {
                    color = Color.RED;
                }
            } else {
//                    id
                try {
                    int id = Integer.parseInt(idColor);
                    Class c = findClass(id);
                    color = c.color;
                } catch (Exception e) {
                    color = Color.RED;
                }
            }

            t = Tuils.span(t.toString(), color);
            oldNotes = TextUtils.replace(oldNotes, new String[] {match}, new CharSequence[] {t});
        }

        hasChanged = true;
    }

    public CharSequence getNotes() {
        hasChanged = false;
        return oldNotes;
    }

    private void addNote(String s, boolean lock) {
        long t = System.currentTimeMillis();

        notes.add(new Note(t, s, lock));
        Collections.sort(notes);

        File file = new File(Tuils.getFolder(), PATH);
        if(!file.exists()) {
            resetFile(file, NAME);
        }

        String output = XMLPrefsManager.add(file, NOTE_NODE, new String[] {CREATION_TIME, VALUE_ATTRIBUTE, LOCK}, new String[] {String.valueOf(t), s, String.valueOf(lock)});
        if(output != null) {
            if(output.length() > 0) Tuils.sendOutput(mContext, output);
            else Tuils.sendOutput(mContext, R.string.output_error);
        }

        invalidateNotes();
    }

    private void rmNote(String s) {
        int index = findNote(s);
        if(index == -1) {
            Tuils.sendOutput(mContext, R.string.note_not_found);
            return;
        }

        long time = notes.remove(index).creationTime;

        File file = new File(Tuils.getFolder(), PATH);
        if(!file.exists()) {
            resetFile(file, NAME);
        }

        String output = XMLPrefsManager.removeNode(file, new String[] {CREATION_TIME}, new String[] {String.valueOf(time)});
        if(output != null) {
            if(output.length() > 0) Tuils.sendOutput(mContext, output);
        }

        invalidateNotes();
    }

    private void cpNote(String s) {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB) {
            Tuils.sendOutput(mContext, R.string.api_low);
            return;
        }

        int index = findNote(s);
        if(index == -1) {
            Tuils.sendOutput(mContext, R.string.note_not_found);
            return;
        }

        final String text = notes.get(index).text;

        ((Activity) mContext).runOnUiThread(() -> {
            final ClipboardManager clipboard = (ClipboardManager) mContext.getSystemService(CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("note", text);
            clipboard.setPrimaryClip(clip);
            Tuils.sendOutput(mContext, mContext.getString(R.string.copied) + Tuils.SPACE + text);
        });
    }

    private void clearNotes(Context context) {
        Iterator<Note> iterator = notes.iterator();
        while(iterator.hasNext()) {
            Note n = iterator.next();
            if(!n.lock) iterator.remove();
        }

        File file = new File(Tuils.getFolder(), PATH);
        if(!file.exists()) resetFile(file, NAME);

        String output = XMLPrefsManager.removeNode(file, new String[] {LOCK}, new String[] {String.valueOf(false)}, true, true);
        if(output != null && output.length() > 0) Tuils.sendOutput(Color.RED, context, output);

        invalidateNotes();
    }

    private void lsNotes(Context c) {
        StringBuilder builder = new StringBuilder();

        for(int j = 0; j < notes.size(); j++) {
            Note n = notes.get(j);
            builder.append(" - ").append(j + 1).append(n.lock ? " [locked]" : Tuils.EMPTYSTRING).append(" -> ").append(n.text).append(Tuils.NEWLINE);
        }

        Tuils.sendOutput(c, builder.toString().trim());
    }

    private void lockNote(Context context, String s, boolean lock) {
        int index = findNote(s);
        if(index == -1) {
            Tuils.sendOutput(context, R.string.note_not_found);
            return;
        }

        Note n = notes.get(index);
        n.lock = lock;
        Collections.sort(notes);

        long time = n.creationTime;

        File file = new File(Tuils.getFolder(), PATH);
        if(!file.exists()) {
            resetFile(file, NAME);
        }

        String output = XMLPrefsManager.set(file, NOTE_NODE, new String[] {CREATION_TIME}, new String[] {String.valueOf(time)}, new String[] {LOCK}, new String[] {String.valueOf(lock)}, true);
        if(output != null && output.length() > 0) Tuils.sendOutput(context, output);

        invalidateNotes();
    }

    private int findNote(String s) {
        try {
            int index = Integer.parseInt(s) - 1;
            if(index < 0 || index >= notes.size()) return -1;
            return index;
        } catch (Exception e) {}

        s = s.toLowerCase().trim();

        CharSequence note;
        int c = 0;
        for(; c < notes.size(); c++) {
            Note n = notes.get(c);

            String text = n.text;

            text = lockPattern.matcher(text).replaceAll(String.valueOf(n.lock));
            text = rowPattern.matcher(text).replaceAll(String.valueOf(c + 1));
            text = countPattern.matcher(text).replaceAll(String.valueOf(notes.size()));

            note = text;

            Matcher m = colorPattern.matcher(notes.get(c).text);
            while(m.find()) {
                String match = m.group();
                String idColor = m.group(1);
                CharSequence t = m.group(2);

                int color;
                if(idColor.startsWith("#")) {
//                    color
                    try {
                        color = Color.parseColor(idColor);
                    } catch (Exception e) {
                        color = Color.RED;
                    }
                } else {
//                    id
                    try {
                        int id = Integer.parseInt(idColor);
                        Class cl = findClass(id);
                        color = cl.color;
                    } catch (Exception e) {
                        color = Color.RED;
                    }
                }

                t = Tuils.span(t.toString(), color);
                note = TextUtils.replace(note, new String[] {match}, new CharSequence[] {t});
            }

            if(note.toString().toLowerCase().startsWith(s)) break;
        }

        if(c == notes.size()) {
            return -1;
        }
        return c;
    }

    private Class findClass(int id) {
        Iterator<Class> classIterator = classes.iterator();
        while(classIterator.hasNext()) {
            Class cl = classIterator.next();
            if(cl.id == id) return cl;
        }

        return null;
    }

    public void dispose(Context context) {
        LocalBroadcastManager.getInstance(context.getApplicationContext()).unregisterReceiver(receiver);
    }

    private class Class {
        int id;
        int color;

        public Class(int id, int color) {
            this.id = id;
            this.color = color;
        }
    }

    private static class Note implements Comparable<Note> {
        private static final int SORTING_TIME_UPDOWN = 0;
        private static final int SORTING_TIME_DOWNUP = 1;
        private static final int SORTING_ALPHA_UPDOWN = 2;
        private static final int SORTING_ALPHA_DOWNUP = 3;
        private static final int SORTING_LOCK_BEFORE = 4;
        private static final int SORTING_UNLOCK_BEFORE = 5;

        long creationTime;
        String text;
        boolean lock;

        public static int sorting = Integer.MAX_VALUE;

        public Note(long time, String text, boolean lock) {
            this.creationTime = time;
            this.text = text;
            this.lock = lock;
        }

        @Override
        public int compareTo(@NonNull Note o) {
            switch (sorting) {
                case SORTING_TIME_UPDOWN:
                    return (int) (creationTime - o.creationTime);
                case SORTING_TIME_DOWNUP:
                    return (int) (o.creationTime - creationTime);
                case SORTING_ALPHA_UPDOWN:
                    return Tuils.alphabeticCompare(text, o.text);
                case SORTING_ALPHA_DOWNUP:
                    return Tuils.alphabeticCompare(o.text, text);
                case SORTING_LOCK_BEFORE:
                    if(lock) {
                        if(o.lock) return 0;
                        return -1;
                    } else {
                        if(o.lock) return 1;
                        return 0;
                    }
                case SORTING_UNLOCK_BEFORE:
                    if(lock) {
                        if(o.lock) return 0;
                        return 1;
                    } else {
                        if(o.lock) return -1;
                        return 0;
                    }
                default:
                    return 1;
            }
        }

        @Override
        public String toString() {
            return creationTime + " : " + text;
        }
    }
}
