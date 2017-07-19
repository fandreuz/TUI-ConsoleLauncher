package ohi.andre.consolelauncher.managers.suggestions;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import ohi.andre.comparestring.Compare;
import ohi.andre.consolelauncher.commands.Command;
import ohi.andre.consolelauncher.commands.CommandAbstraction;
import ohi.andre.consolelauncher.commands.CommandTuils;
import ohi.andre.consolelauncher.commands.main.MainPack;
import ohi.andre.consolelauncher.commands.specific.ParamCommand;
import ohi.andre.consolelauncher.commands.specific.PermanentSuggestionCommand;
import ohi.andre.consolelauncher.managers.AliasManager;
import ohi.andre.consolelauncher.managers.AppsManager;
import ohi.andre.consolelauncher.managers.ContactManager;
import ohi.andre.consolelauncher.managers.FileManager;
import ohi.andre.consolelauncher.managers.MusicManager;
import ohi.andre.consolelauncher.managers.XMLPrefsManager;
import ohi.andre.consolelauncher.managers.notifications.NotificationManager;
import ohi.andre.consolelauncher.tuils.Tuils;

import static ohi.andre.consolelauncher.commands.CommandTuils.xmlPrefsEntrys;
import static ohi.andre.consolelauncher.commands.CommandTuils.xmlPrefsFiles;

/**
 * Created by francescoandreuzzi on 25/12/15.
 */
public class SuggestionsManager {

    private final int MIN_COMMAND_PRIORITY = 5;

    private int min_command_rate = 4;
    private int min_apps_rate = 4;
    private int min_contacts_rate = 4;
    private int min_file_rate = 2;
    private int min_songs_rate = 4;

//    use to place something at the top
    private final int MAX_RATE = 100;
    private final int NO_RATE = -1;

    private final int FIRST_INTERVAL = 7;

    private boolean showAlias, showAliasWasSet = false;

    public Suggestion[] getSuggestions(MainPack info, String before, String lastWord) {

        if(!showAliasWasSet) {
            showAlias = XMLPrefsManager.get(boolean.class, XMLPrefsManager.Behavior.show_alias_suggestions);
            showAliasWasSet = true;
        }

        List<Suggestion> suggestionList = new ArrayList<>();

        before = before.trim();
        lastWord = lastWord.trim();

//        lastword = 0
        if (lastWord.length() == 0) {
//            lastword = 0 && before = 0
            if (before.length() == 0) {
                String[] apps = info.appsManager.getSuggestedApps();
                if (apps != null) {
                    for(int count = 0; count < apps.length; count++) {
                        if(apps[count] == null) {
                            continue;
                        }

                        float shift = count + 1;
                        float rate = 1f / shift;
                        suggestionList.add(new Suggestion(before, apps[count], true, (int) Math.ceil(rate), Suggestion.TYPE_APP));
                    }
                }

                if(showAlias) suggestAlias(info.aliasManager, suggestionList, lastWord);

                return suggestionList.toArray(new Suggestion[suggestionList.size()]);
            }
//            lastword == 0 && before > 0
            else {
//                check if this is a command
                Command cmd = null;
                try {
                    cmd = CommandTuils.parse(before, info, true);
                } catch (Exception e) {}

                if (cmd != null) {

                    if(cmd.cmd instanceof PermanentSuggestionCommand) {
                        suggestPermanentSuggestions(suggestionList, (PermanentSuggestionCommand) cmd.cmd);
                    }

                    if (cmd.nArgs == cmd.cmd.maxArgs() ||
                            (cmd.mArgs != null && cmd.mArgs.length > 0 && cmd.cmd instanceof ParamCommand && cmd.nArgs >= 1 &&
                                    ((ParamCommand) cmd.cmd).argsForParam((String) cmd.mArgs[0]) != null && ((ParamCommand) cmd.cmd).argsForParam((String) cmd.mArgs[0]).length + 1 == cmd.nArgs)) {
                        return new Suggestion[0];
                    }

//                    if( ( !(cmd.cmd instanceof ParamCommand) && cmd.nArgs == cmd.cmd.maxArgs() - 1 && cmd.indexNotFound == cmd.cmd.maxArgs() - 1) ||
//                            (cmd.mArgs != null && cmd.mArgs.length > 0 && cmd.cmd instanceof ParamCommand && cmd.nArgs >= 1 && ((ParamCommand) cmd.cmd).argsForParam((String) cmd.mArgs[0]).length == cmd.nArgs
//                                    && cmd.indexNotFound == ((ParamCommand) cmd.cmd).argsForParam((String) cmd.mArgs[0]).length)) {
////                        the last arg wasnt found
//                        suggestArgs(info, cmd.cmd instanceof ParamCommand ? ((ParamCommand) cmd.cmd).argsForParam((String) cmd.mArgs[0])[cmd.nArgs - 1] : cmd.cmd.argType()[cmd.nArgs], suggestionList, lastWord, before);
//                    }

                    if(cmd.cmd instanceof ParamCommand && (cmd.mArgs == null || cmd.mArgs.length == 0)) suggestParams(suggestionList, (ParamCommand) cmd.cmd, before, null);
                    else suggestArgs(info, cmd.nextArg(), suggestionList, before);

                } else {
//                    >>word
//                    not a command
//                    ==> app
                    suggestApp(info, suggestionList, before, Tuils.EMPTYSTRING);
                }
            }
        }
//        lastWord > 0
        else {
            if (before.length() > 0) {
//                lastword > 0 && before > 0
                Command cmd = null;
                try {
                    cmd = CommandTuils.parse(before, info, true);
                } catch (Exception e) {}

                if (cmd != null) {
                    if(cmd.cmd instanceof PermanentSuggestionCommand) {
                        suggestPermanentSuggestions(suggestionList, (PermanentSuggestionCommand) cmd.cmd);
                    }

                    if (cmd.cmd.maxArgs() == 1 && before.contains(Tuils.SPACE)) {
                        int index = cmd.cmd.getClass().getSimpleName().length() + 1;

                        lastWord = before.substring(index) + lastWord;
                    }

                    if(cmd.cmd instanceof ParamCommand && (cmd.mArgs == null || cmd.mArgs.length == 0)) {
                        suggestParams(suggestionList, (ParamCommand) cmd.cmd, before, lastWord);
                    } else suggestArgs(info, cmd.nextArg(), suggestionList, lastWord, before);
                } else {
//                    not a command
//                    ==> app
                    suggestApp(info, suggestionList, before + lastWord, Tuils.EMPTYSTRING);
                }
            } else {
//                lastword > 0 && before = 0
                suggestCommand(info, suggestionList, lastWord, before);
                if(showAlias) suggestAlias(info.aliasManager, suggestionList, lastWord);
                suggestApp(info, suggestionList, lastWord, Tuils.EMPTYSTRING);
            }
        }

        Collections.sort(suggestionList);
        Suggestion[] array = new Suggestion[suggestionList.size()];
        return suggestionList.toArray(array);
    }

    private void suggestPermanentSuggestions(List<Suggestion> suggestions, PermanentSuggestionCommand cmd) {
        for(String s : cmd.permanentSuggestions()) {
            Suggestion sugg = new Suggestion(null, s, false, NO_RATE, Suggestion.TYPE_PERMANENT);
            suggestions.add(sugg);
        }
    }

    private void suggestAlias(AliasManager aliasManager, List<Suggestion> suggestions, String lastWord) {
        if(lastWord.length() == 0) for(String s : aliasManager.getAliases()) suggestions.add(new Suggestion(Tuils.EMPTYSTRING, s, true, NO_RATE, Suggestion.TYPE_ALIAS));
        else for(String s : aliasManager.getAliases()) if(s.startsWith(lastWord)) suggestions.add(new Suggestion(Tuils.EMPTYSTRING, s, true, NO_RATE, Suggestion.TYPE_ALIAS));
    }

    private void suggestParams(List<Suggestion> suggestions, ParamCommand cmd, String before, String lastWord) {
        String[] params = cmd.params();
        if (params == null) {
            return;
        }

        if(lastWord == null || lastWord.length() == 0) {
            for (String s : cmd.params()) {
                int[] args = cmd.argsForParam(s);
                suggestions.add(new Suggestion(before, s, args == null || args.length == 0, NO_RATE, 0));
            }
        }
        else {
            for (String s : cmd.params()) {
                int[] args = cmd.argsForParam(s);
                if (s.startsWith(lastWord) || s.replace("-", Tuils.EMPTYSTRING).startsWith(lastWord)) suggestions.add(new Suggestion(before, s, args == null || args.length == 0, NO_RATE, 0));
            }
        }
    }

    private void suggestArgs(MainPack info, int type, List<Suggestion> suggestions, String prev, String before) {
        switch (type) {
            case CommandAbstraction.FILE:
            case CommandAbstraction.FILE_LIST:
                suggestFile(info, suggestions, prev, before);
                break;
            case CommandAbstraction.VISIBLE_PACKAGE:
                suggestApp(info, suggestions, prev, before);
                break;
            case CommandAbstraction.COMMAND:
                suggestCommand(info, suggestions, prev, before);
                break;
            case CommandAbstraction.CONTACTNUMBER:
                suggestContact(info, suggestions, prev, before);
                break;
            case CommandAbstraction.SONG:
                suggestSong(info, suggestions, prev, before);
                break;
            case CommandAbstraction.BOOLEAN:
                suggestBoolean(suggestions, before);
                break;
            case CommandAbstraction.HIDDEN_PACKAGE:
                suggestHiddenApp(info, suggestions, prev, before);
                break;
            case CommandAbstraction.COLOR:
                suggestColor(suggestions, prev, before);
                break;
            case CommandAbstraction.CONFIG_ENTRY:
                suggestConfigEntry(suggestions, prev, before);
                break;
            case CommandAbstraction.CONFIG_FILE:
                suggestConfigFile(suggestions, prev, before);
                break;
        }
    }

    private void suggestArgs(MainPack info, int type, List<Suggestion> suggestions, String before) {
        suggestArgs(info, type, suggestions, null, before);
    }

    private void suggestBoolean(List<Suggestion> suggestions, String before) {
        suggestions.add(new Suggestion(before, "true", true, NO_RATE, Suggestion.TYPE_BOOLEAN));
        suggestions.add(new Suggestion(before, "false", true, NO_RATE, Suggestion.TYPE_BOOLEAN));
    }

    private void suggestFile(MainPack info, List<Suggestion> suggestions, String prev, String before) {
        if(prev == null || !prev.endsWith(File.separator)) {
            suggestions.add(new Suggestion(before + Tuils.SPACE + (prev != null ? prev : Tuils.EMPTYSTRING), File.separator, false, MAX_RATE, Suggestion.TYPE_FILE));
        }

        if (prev == null || prev.length() == 0) {
            suggestFilesInDir(suggestions, info.currentDirectory, before);
            return;
        }

        if (!prev.contains(File.separator) && prev.length() > 0) {
            suggestFilesInDir(suggestions, info.currentDirectory, prev, before);
        } else if (prev.length() > 0) {
            if (prev.endsWith(File.separator)) {
                prev = prev.substring(0, prev.length() - 1);
                before = before + Tuils.SPACE + prev + File.separator;

                FileManager.DirInfo dirInfo = FileManager.cd(info.currentDirectory, prev);
                suggestFilesInDir(suggestions, dirInfo.file, before);
            } else {
//                contains / but doesn't end with it
                FileManager.DirInfo dirInfo = FileManager.cd(info.currentDirectory, prev.substring(0,prev.lastIndexOf(File.separator)));

                int index = prev.lastIndexOf(File.separator);
                String hold = prev.substring(0, index + 1);
                prev = prev.substring(index + 1);
                before = before + Tuils.SPACE + hold;

                suggestFilesInDir(suggestions, dirInfo.file, prev, before);
            }
        }
    }

    private void suggestFilesInDir(List<Suggestion> suggestions, File dir, String prev, String before) {
        if (dir == null || !dir.isDirectory())
            return;

        if (prev == null || prev.length() == 0) {
            suggestFilesInDir(suggestions, dir, before);
            return;
        }

        String[] files = dir.list();
        if(files == null) {
            return;
        }
        Arrays.sort(files);
        List<Compare.CompareInfo> infos = Compare.compareInfo(files, prev, min_file_rate,
                FileManager.USE_SCROLL_COMPARE);

        for(Compare.CompareInfo i : infos) {
            suggestions.add(new Suggestion(before, i.s, false, i.rate, Suggestion.TYPE_FILE));
        }
    }

    private void suggestFilesInDir(List<Suggestion> suggestions, File dir, String before) {
        if (dir == null || !dir.isDirectory()) {
            return;
        }

        try {
            String[] files = dir.list();
            if(files == null) {
                return;
            }
            Arrays.sort(files);
            for (String s : files) {
                suggestions.add(new Suggestion(before, s, false, NO_RATE, Suggestion.TYPE_FILE));
            }
        } catch (NullPointerException e) {}
    }

    private void suggestContact(MainPack info, List<Suggestion> suggestions, String prev, String before) {
        if (prev == null || prev.length() == 0) {
            for (ContactManager.Contact contact : info.contacts.getContacts())
                suggestions.add(new Suggestion(before, contact.name, true, NO_RATE, Suggestion.TYPE_CONTACT, contact));
        }

        else if(prev.length() <= FIRST_INTERVAL) {
            prev = prev.trim().toLowerCase();

            for (ContactManager.Contact contact : info.contacts.getContacts())
                if(contact.name.toLowerCase().trim().startsWith(prev)) {
                    suggestions.add(new Suggestion(before, contact.name, true, NO_RATE, Suggestion.TYPE_CONTACT, contact));
                }
        }

        else {
            for(ContactManager.Contact contact : info.contacts.getContacts()) {
                int rate = ContactManager.USE_SCROLL_COMPARE ? Compare.scrollComparison(contact.name, prev) : Compare.linearComparison(contact.name, prev);
                if(rate >= min_contacts_rate) {
                    suggestions.add(new Suggestion(before, contact.name, true, NO_RATE, Suggestion.TYPE_CONTACT, contact));
                }
            }
        }
    }

    private void suggestSong(MainPack info, List<Suggestion> suggestions, String prev, String before) {
        if (prev == null || prev.length() == 0) {
            for (String s : info.player.getNames())
                suggestions.add(new Suggestion(before, s, true, NO_RATE, Suggestion.TYPE_SONG));
        } else if(prev.length() <= FIRST_INTERVAL) {
            prev = prev.trim().toLowerCase();
            List<String> names = info.player.getNames();
            for (String n : names) {
                if(n.toLowerCase().trim().startsWith(prev)) {
                    suggestions.add(new Suggestion(before, n, true, MAX_RATE, Suggestion.TYPE_SONG));
                }
            }
        } else {
            List<Compare.CompareInfo> infos = Compare.compareInfo(info.player.getNames(), prev, min_songs_rate,
                    MusicManager.USE_SCROLL_COMPARE);
            for(Compare.CompareInfo i : infos) {
                suggestions.add(new Suggestion(before, i.s, true, i.rate, Suggestion.TYPE_SONG));
            }
        }
    }

    private void suggestCommand(MainPack info, List<Suggestion> suggestions, String prev, String before) {
        if (prev == null || prev.length() == 0) {
            suggestCommand(info, suggestions, before);
            return;
        }

        if(prev.length() <= FIRST_INTERVAL) {
            prev = prev.toLowerCase().trim();
            String[] cmds = info.commandGroup.getCommandNames();
            for (String s : cmds) {
                if(s.startsWith(prev)) {
                    CommandAbstraction cmd = info.commandGroup.getCommandByName(s);
                    int[] args = cmd.argType();
                    boolean exec = args == null || args.length == 0;
                    suggestions.add(new Suggestion(before, s, exec, MAX_RATE, Suggestion.TYPE_COMMAND));
                }
            }
            return;
        }

        List<Compare.CompareInfo> infos = Compare.compareInfo(info.commandGroup.getCommandNames(), prev, min_command_rate, false);
        for(Compare.CompareInfo i : infos) {
            CommandAbstraction cmd = info.commandGroup.getCommandByName(i.s);
            int[] args = cmd.argType();
            boolean exec = args == null || args.length == 0;
            suggestions.add(new Suggestion(before, i.s, exec, i.rate, Suggestion.TYPE_COMMAND));
        }
    }

    private void suggestColor(List<Suggestion> suggestions, String prev, String before) {
        if(prev == null || prev.length() == 0 || (prev.length() == 1 && prev.charAt(0) != '#')) {
            suggestions.add(new Suggestion(before, "#", false, MAX_RATE, Suggestion.TYPE_COLOR));
        }
    }

    private void suggestCommand(MainPack info, List<Suggestion> suggestions, String before) {
        for (String s : info.commandGroup.getCommandNames()) {
            CommandAbstraction cmd = info.commandGroup.getCommandByName(s);
            if (cmd != null && cmd.priority() >= MIN_COMMAND_PRIORITY) {
                int[] args = cmd.argType();
                boolean exec = args == null || args.length == 0;
                suggestions.add(new Suggestion(before, s, exec, cmd.priority(), Suggestion.TYPE_COMMAND));
            }
        }
    }

    private void suggestApp(MainPack info, List<Suggestion> suggestions, String prev, String before) {
        List<String> names = info.appsManager.getAppLabels();
        if (prev == null || prev.length() == 0) {
            for (String s : names) {
                suggestions.add(new Suggestion(before, s, true, NO_RATE, Suggestion.TYPE_APP));
            }
        } else if(prev.length() <= FIRST_INTERVAL) {
            prev = prev.trim().toLowerCase();
            for (String n : names) {
                if(n.toLowerCase().trim().startsWith(prev)) {
                    suggestions.add(new Suggestion(before, n, true, MAX_RATE, Suggestion.TYPE_APP));
                }
            }
        } else {
            List<Compare.CompareInfo> infos = Compare.compareInfo(names, prev, min_apps_rate,
                    AppsManager.USE_SCROLL_COMPARE);
            for(Compare.CompareInfo i : infos) {
                suggestions.add(new Suggestion(before, i.s, true, i.rate, Suggestion.TYPE_APP));
            }
        }
    }

    private void suggestHiddenApp(MainPack info, List<Suggestion> suggestions, String prev, String before) {
        List<String> names = info.appsManager.getHiddenAppsLabels();
        if (prev == null || prev.length() == 0) {
            for (String s : names) {
                suggestions.add(new Suggestion(before, s, true, NO_RATE, Suggestion.TYPE_APP));
            }
        } else if(prev.length() <= FIRST_INTERVAL) {
            prev = prev.trim().toLowerCase();
            for (String n : names) {
                if(n.toLowerCase().trim().startsWith(prev)) {
                    suggestions.add(new Suggestion(before, n, true, MAX_RATE, Suggestion.TYPE_APP));
                }
            }
        } else {
            List<Compare.CompareInfo> infos = Compare.compareInfo(names, prev, min_apps_rate,
                    AppsManager.USE_SCROLL_COMPARE);
            for(Compare.CompareInfo i : infos) {
                suggestions.add(new Suggestion(before, i.s, true, i.rate, Suggestion.TYPE_APP));
            }
        }
    }

    private void suggestConfigEntry(List<Suggestion> suggestions, String prev, String before) {
        if(xmlPrefsEntrys == null) {
            xmlPrefsEntrys = new ArrayList<>();
            for(XMLPrefsManager.XMLPrefsRoot element : XMLPrefsManager.XMLPrefsRoot.values()) {
                for(XMLPrefsManager.XMLPrefsSave save : element.copy)
                    xmlPrefsEntrys.add(save);
            }
            for(XMLPrefsManager.XMLPrefsSave save : AppsManager.Options.values()) xmlPrefsEntrys.add(save);
            for(XMLPrefsManager.XMLPrefsSave save : NotificationManager.Options.values()) xmlPrefsEntrys.add(save);
        }

        if(prev == null || prev.length() == 0) {
            for(XMLPrefsManager.XMLPrefsSave s : xmlPrefsEntrys) {
                Suggestion sg = new Suggestion(before, s.label(), false, NO_RATE, Suggestion.TYPE_COMMAND);
                suggestions.add(sg);
            }
        } else if(prev.length() <= FIRST_INTERVAL) {
            prev = prev.trim().toLowerCase();
            for (XMLPrefsManager.XMLPrefsSave s : xmlPrefsEntrys) {
                String label = s.label();
                if(label.startsWith(prev)) {
                    suggestions.add(new Suggestion(before, label, false, MAX_RATE, Suggestion.TYPE_COMMAND));
                }
            }
        }
    }

    private void suggestConfigFile(List<Suggestion> suggestions, String prev, String before) {
        if(xmlPrefsFiles == null) {
            xmlPrefsFiles = new ArrayList<>();
            for(XMLPrefsManager.XMLPrefsRoot element : XMLPrefsManager.XMLPrefsRoot.values())
                xmlPrefsFiles.add(element.path);
            xmlPrefsFiles.add(AppsManager.PATH);
            xmlPrefsFiles.add(NotificationManager.PATH);
        }

        if(prev == null || prev.length() == 0) {
            for(String s : xmlPrefsFiles) {
                Suggestion sg = new Suggestion(before, s, false, NO_RATE, Suggestion.TYPE_FILE);
                suggestions.add(sg);
            }
        } else if(prev.length() <= FIRST_INTERVAL) {
            prev = prev.trim().toLowerCase();
            for (String s : xmlPrefsFiles) {
                if(s.startsWith(prev)) {
                    suggestions.add(new Suggestion(before, s, false, MAX_RATE, Suggestion.TYPE_FILE));
                }
            }
        }
    }

    public class Suggestion implements Comparable<Suggestion> {

        public static final int TYPE_APP = 10;
        public static final int TYPE_FILE = 11;
        public static final int TYPE_ALIAS = 12;
        public static final int TYPE_COMMAND = 13;
        public static final int TYPE_SONG = 14;
        public static final int TYPE_CONTACT = 15;
        public static final int TYPE_BOOLEAN = 16;
        public static final int TYPE_COLOR = 17;
        public static final int TYPE_PERMANENT = 18;

        public String text;
        public String textBefore;

        public boolean exec;
        public int rate;
        public int type;

        public Object object;

        public Suggestion(String before, String text, boolean exec, int rate, int type) {
            this(before, text, exec, rate, type, null);
        }

        public Suggestion(String before, String text, boolean exec, int rate, int type, Object tag) {
            this.textBefore = before;
            this.text = text;

            this.exec = exec;
            this.rate = rate;
            this.type = type;

            this.object = tag;
        }

        public String getText() {
            if(type == Suggestion.TYPE_CONTACT) {
                ContactManager.Contact c = (ContactManager.Contact) object;

                if(c.numbers.size() <= c.getSelectedNumber()) c.setSelectedNumber(0);

                return textBefore + Tuils.SPACE + c.numbers.get(c.getSelectedNumber());
            } else if(type == Suggestion.TYPE_PERMANENT) {
                return text;
            }

            if(textBefore == null || textBefore.length() == 0) {
                return text;
            } else if((text.startsWith(File.separator) || textBefore.endsWith(File.separator)) && type == TYPE_FILE) {
                return textBefore + text;
            } else {
                return textBefore + Tuils.SPACE + text;
            }
        }

        @Override
        public int compareTo(Suggestion another) {
            return this.rate > another.rate? -1 : (this.rate == another.rate ? 0 : 1);
        }

        @Override
        public String toString() {
            return text;
        }
    }
}
