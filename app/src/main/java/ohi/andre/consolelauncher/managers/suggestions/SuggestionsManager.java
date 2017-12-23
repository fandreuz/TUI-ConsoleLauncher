package ohi.andre.consolelauncher.managers.suggestions;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import ohi.andre.consolelauncher.commands.Command;
import ohi.andre.consolelauncher.commands.CommandAbstraction;
import ohi.andre.consolelauncher.commands.CommandTuils;
import ohi.andre.consolelauncher.commands.main.MainPack;
import ohi.andre.consolelauncher.commands.main.Param;
import ohi.andre.consolelauncher.commands.specific.ParamCommand;
import ohi.andre.consolelauncher.commands.specific.PermanentSuggestionCommand;
import ohi.andre.consolelauncher.managers.AliasManager;
import ohi.andre.consolelauncher.managers.AppsManager;
import ohi.andre.consolelauncher.managers.ContactManager;
import ohi.andre.consolelauncher.managers.FileManager;
import ohi.andre.consolelauncher.managers.RssManager;
import ohi.andre.consolelauncher.managers.music.Song;
import ohi.andre.consolelauncher.managers.notifications.NotificationManager;
import ohi.andre.consolelauncher.managers.xml.XMLPrefsManager;
import ohi.andre.consolelauncher.managers.xml.options.Apps;
import ohi.andre.consolelauncher.managers.xml.options.Notifications;
import ohi.andre.consolelauncher.managers.xml.options.Rss;
import ohi.andre.consolelauncher.managers.xml.options.Suggestions;
import ohi.andre.consolelauncher.tuils.Compare;
import ohi.andre.consolelauncher.tuils.SimpleMutableEntry;
import ohi.andre.consolelauncher.tuils.Tuils;

import static ohi.andre.consolelauncher.commands.CommandTuils.xmlPrefsEntrys;
import static ohi.andre.consolelauncher.commands.CommandTuils.xmlPrefsFiles;

/**
 * Created by francescoandreuzzi on 25/12/15.
 */
public class SuggestionsManager {

    private final int MIN_COMMAND_PRIORITY = 5;

    private final int MAX_RATE = 100;
    private final int NO_RATE = -1;

    private final int FIRST_INTERVAL = 6;

    private boolean showAliasDefault, set = false, clickToLaunch, showAppsGpDefault;

    public Suggestion[] getSuggestions(MainPack info, String beforeLastSpace , String lastWord) {

        if(!set) {
            showAliasDefault = XMLPrefsManager.getBoolean(Suggestions.suggest_alias_default);
            showAppsGpDefault = XMLPrefsManager.getBoolean(Suggestions.suggest_appgp_default);
            set = true;
            clickToLaunch = XMLPrefsManager.getBoolean(Suggestions.click_to_launch);
        }

        List<Suggestion> suggestionList = new ArrayList<>();

        beforeLastSpace  = beforeLastSpace .trim();
        lastWord = lastWord.trim();

//        lastword = 0
        if (lastWord.length() == 0) {
//            lastword = 0 && beforeLastSpace  = 0

            if (beforeLastSpace .length() == 0) {
                AppsManager.LaunchInfo[] apps = info.appsManager.getSuggestedApps();
                if (apps != null) {
                    for(int count = 0; count < apps.length; count++) {
                        if(apps[count] == null) {
                            continue;
                        }

                        float shift = count + 1;
                        float rate = 1f / shift;
                        suggestionList.add(new Suggestion(beforeLastSpace , apps[count].getString(), clickToLaunch, (int) Math.ceil(rate), Suggestion.TYPE_APP, apps[count]));
                    }
                }

                if(showAliasDefault) suggestAlias(info.aliasManager, suggestionList, lastWord);
                if(showAppsGpDefault) suggestAppGroup(suggestionList, lastWord, beforeLastSpace );

                return suggestionList.toArray(new Suggestion[suggestionList.size()]);
            }

//            lastword == 0 && beforeLastSpace  > 0
            else {
//                check if this is a command
                Command cmd = null;
                try {
                    cmd = CommandTuils.parse(beforeLastSpace , info, true);
                } catch (Exception e) {}

                if (cmd != null) {

                    if(cmd.cmd instanceof PermanentSuggestionCommand) {
                        suggestPermanentSuggestions(suggestionList, (PermanentSuggestionCommand) cmd.cmd);
                    }

                    if (cmd.mArgs != null && cmd.mArgs.length > 0 && cmd.cmd instanceof ParamCommand && cmd.nArgs >= 1 && cmd.mArgs[0] instanceof Param &&
                            ((Param) cmd.mArgs[0]).args().length + 1 == cmd.nArgs) {
                        return new Suggestion[0];
                    }

//                    if( ( !(cmd.cmd instanceof ParamCommand) && cmd.nArgs == cmd.cmd.maxArgs() - 1 && cmd.indexNotFound == cmd.cmd.maxArgs() - 1) ||
//                            (cmd.mArgs != null && cmd.mArgs.length > 0 && cmd.cmd instanceof ParamCommand && cmd.nArgs >= 1 && ((ParamCommand) cmd.cmd).argsForParam((String) cmd.mArgs[0]).length == cmd.nArgs
//                                    && cmd.indexNotFound == ((ParamCommand) cmd.cmd).argsForParam((String) cmd.mArgs[0]).length)) {
////                        the last arg wasnt found
//                        suggestArgs(info, cmd.cmd instanceof ParamCommand ? ((ParamCommand) cmd.cmd).argsForParam((String) cmd.mArgs[0])[cmd.nArgs - 1] : cmd.cmd.argType()[cmd.nArgs], suggestionList, lastWord, beforeLastSpace );
//                    }

                    if(cmd.cmd instanceof ParamCommand && (cmd.mArgs == null || cmd.mArgs.length == 0 || cmd.mArgs[0] instanceof String)) {
                        suggestParams(info, suggestionList, (ParamCommand) cmd.cmd, beforeLastSpace , null);
                    }
                    else suggestArgs(info, cmd.nextArg(), suggestionList, beforeLastSpace );

                } else {

                    String[] split = beforeLastSpace .replaceAll("['\"]", Tuils.EMPTYSTRING).split(Tuils.SPACE);
                    boolean isShellCmd = false;
                    for(String s : split) {
                        if(needsFileSuggestion(s)) {
                            isShellCmd = true;
                            break;
                        }
                    }

                    if(isShellCmd) {
                        suggestFile(info, suggestionList, Tuils.EMPTYSTRING, beforeLastSpace );
                    } else {
//                        ==> app
                        if(!suggestAppInsideGroup(suggestionList, Tuils.EMPTYSTRING, beforeLastSpace , false)) suggestApp(info, suggestionList, beforeLastSpace  + Tuils.SPACE, Tuils.EMPTYSTRING);
                    }

                }
            }
        }

//        lastWord > 0
        else {

            if (beforeLastSpace .length() > 0) {
//                lastword > 0 && beforeLastSpace  > 0
                Command cmd = null;
                try {
                    cmd = CommandTuils.parse(beforeLastSpace , info, true);
                } catch (Exception e) {}

                if (cmd != null) {
                    if(cmd.cmd instanceof PermanentSuggestionCommand) {
                        suggestPermanentSuggestions(suggestionList, (PermanentSuggestionCommand) cmd.cmd);
                    }

//                    if (cmd.cmd.maxArgs() == 1 && beforeLastSpace .contains(Tuils.SPACE)) {
//                        int index = cmd.cmd.getClass().getSimpleName().length() + 1;
//
//                        lastWord = beforeLastSpace .substring(index) + lastWord;
//                    }

                    if(cmd.cmd instanceof ParamCommand && (cmd.mArgs == null || cmd.mArgs.length == 0 || cmd.mArgs[0] instanceof String)) {
                        suggestParams(info, suggestionList, (ParamCommand) cmd.cmd, beforeLastSpace , lastWord);
                    } else suggestArgs(info, cmd.nextArg(), suggestionList, lastWord, beforeLastSpace );
                } else {

                    String[] split = beforeLastSpace .replaceAll("['\"]", Tuils.EMPTYSTRING).split(Tuils.SPACE);
                    boolean isShellCmd = false;
                    for(String s : split) {
                        if(needsFileSuggestion(s)) {
                            isShellCmd = true;
                            break;
                        }
                    }

                    if(isShellCmd) {
                        suggestFile(info, suggestionList, lastWord, beforeLastSpace );
                    } else {
                        if(!suggestAppInsideGroup(suggestionList, lastWord, beforeLastSpace , false)) suggestApp(info, suggestionList, beforeLastSpace  + Tuils.SPACE + lastWord, Tuils.EMPTYSTRING);
                    }
                }

            } else {
//                lastword > 0 && beforeLastSpace  = 0
                suggestCommand(info, suggestionList, lastWord, beforeLastSpace );
                suggestAlias(info.aliasManager, suggestionList, lastWord);
                suggestApp(info, suggestionList, lastWord, Tuils.EMPTYSTRING);
                suggestAppGroup(suggestionList, lastWord, beforeLastSpace );
            }
        }

        Collections.sort(suggestionList);
        Suggestion[] array = new Suggestion[suggestionList.size()];
        return suggestionList.toArray(array);
    }

    private boolean needsFileSuggestion(String cmd) {
        return cmd.equalsIgnoreCase("ls") || cmd.equalsIgnoreCase("cd") || cmd.equalsIgnoreCase("mv") || cmd.equalsIgnoreCase("cp") || cmd.equalsIgnoreCase("rm") || cmd.equalsIgnoreCase("cat");
    }

    private void suggestPermanentSuggestions(List<Suggestion> suggestions, PermanentSuggestionCommand cmd) {
        for(String s : cmd.permanentSuggestions()) {
            Suggestion sugg = new Suggestion(null, s, false, NO_RATE, Suggestion.TYPE_PERMANENT);
            suggestions.add(sugg);
        }
    }

    private void suggestAlias(AliasManager aliasManager, List<Suggestion> suggestions, String lastWord) {
        if(lastWord.length() == 0) for(String s : aliasManager.getAliases()) suggestions.add(new Suggestion(Tuils.EMPTYSTRING, s, clickToLaunch, NO_RATE, Suggestion.TYPE_ALIAS));
        else for(String s : aliasManager.getAliases()) if(s.startsWith(lastWord)) suggestions.add(new Suggestion(Tuils.EMPTYSTRING, s, clickToLaunch, NO_RATE, Suggestion.TYPE_ALIAS));
    }

    private void suggestParams(MainPack pack, List<Suggestion> suggestions, ParamCommand cmd, String beforeLastSpace , String lastWord) {
        String[] params = cmd.params();
        if (params == null) {
            return;
        }

        if(lastWord == null || lastWord.length() == 0) {
            for (String s : cmd.params()) {
                Param p = cmd.getParam(pack, s).getValue();
                if(p == null) continue;

                suggestions.add(new Suggestion(beforeLastSpace , s, p.args().length == 0 && clickToLaunch, NO_RATE, 0));
            }
        }
        else {
            for (String s : cmd.params()) {
                Param p = cmd.getParam(pack, s).getValue();
                if(p == null) continue;

                if (s.startsWith(lastWord) || s.replace("-", Tuils.EMPTYSTRING).startsWith(lastWord)) {
                    suggestions.add(new Suggestion(beforeLastSpace , s, p.args().length == 0 && clickToLaunch, NO_RATE, 0));
                }
            }
        }
    }

    private void suggestArgs(MainPack info, int type, List<Suggestion> suggestions, String afterLastSpace, String beforeLastSpace ) {
        switch (type) {
            case CommandAbstraction.FILE:
            case CommandAbstraction.FILE_LIST:
                suggestFile(info, suggestions, afterLastSpace, beforeLastSpace );
                break;
            case CommandAbstraction.VISIBLE_PACKAGE:
                suggestApp(info, suggestions, afterLastSpace, beforeLastSpace );
                break;
            case CommandAbstraction.COMMAND:
                suggestCommand(info, suggestions, afterLastSpace, beforeLastSpace );
                break;
            case CommandAbstraction.CONTACTNUMBER:
                suggestContact(info, suggestions, afterLastSpace, beforeLastSpace );
                break;
            case CommandAbstraction.SONG:
                suggestSong(info, suggestions, afterLastSpace, beforeLastSpace );
                break;
            case CommandAbstraction.BOOLEAN:
                suggestBoolean(suggestions, beforeLastSpace );
                break;
            case CommandAbstraction.HIDDEN_PACKAGE:
                suggestHiddenApp(info, suggestions, afterLastSpace, beforeLastSpace );
                break;
            case CommandAbstraction.COLOR:
                suggestColor(suggestions, afterLastSpace, beforeLastSpace );
                break;
            case CommandAbstraction.CONFIG_ENTRY:
                suggestConfigEntry(suggestions, afterLastSpace, beforeLastSpace );
                break;
            case CommandAbstraction.CONFIG_FILE:
                suggestConfigFile(suggestions, afterLastSpace, beforeLastSpace );
                break;
            case CommandAbstraction.DEFAULT_APP:
                suggestDefaultApp(info, suggestions, afterLastSpace, beforeLastSpace );
                break;
            case CommandAbstraction.ALL_PACKAGES:
                suggestAllPackages(info, suggestions, afterLastSpace, beforeLastSpace );
                break;
            case CommandAbstraction.APP_GROUP:
                suggestAppGroup(suggestions, afterLastSpace, beforeLastSpace );
                break;
            case CommandAbstraction.APP_INSIDE_GROUP:
                suggestAppInsideGroup(suggestions, afterLastSpace, beforeLastSpace , true);
                break;
        }
    }

    private void suggestArgs(MainPack info, int type, List<Suggestion> suggestions, String beforeLastSpace ) {
        suggestArgs(info, type, suggestions, null, beforeLastSpace );
    }

    private void suggestBoolean(List<Suggestion> suggestions, String beforeLastSpace ) {
        suggestions.add(new Suggestion(beforeLastSpace , "true", clickToLaunch, NO_RATE, Suggestion.TYPE_BOOLEAN));
        suggestions.add(new Suggestion(beforeLastSpace , "false", clickToLaunch, NO_RATE, Suggestion.TYPE_BOOLEAN));
    }

    private void suggestFile(MainPack info, List<Suggestion> suggestions, String afterLastSpace, String beforeLastSpace ) {
        if(afterLastSpace == null || !afterLastSpace.endsWith(File.separator)) {
            suggestions.add(new Suggestion(beforeLastSpace  + Tuils.SPACE + (afterLastSpace != null ? afterLastSpace : Tuils.EMPTYSTRING), File.separator, false, MAX_RATE, Suggestion.TYPE_FILE));
        }

        if (afterLastSpace == null || afterLastSpace.length() == 0) {
            suggestFilesInDir(suggestions, info.currentDirectory, beforeLastSpace );
            return;
        }

        if (!afterLastSpace.contains(File.separator) && afterLastSpace.length() > 0) {
            suggestFilesInDir(suggestions, info.currentDirectory, afterLastSpace, beforeLastSpace );
        } else if (afterLastSpace.length() > 0) {
            if (afterLastSpace.endsWith(File.separator)) {
                afterLastSpace = afterLastSpace.substring(0, afterLastSpace.length() - 1);
                beforeLastSpace  = beforeLastSpace  + Tuils.SPACE + afterLastSpace + File.separator;

                FileManager.DirInfo dirInfo = FileManager.cd(info.currentDirectory, afterLastSpace);
                suggestFilesInDir(suggestions, dirInfo.file, beforeLastSpace );
            } else {
//                contains / but doesn't end with it
                FileManager.DirInfo dirInfo = FileManager.cd(info.currentDirectory, afterLastSpace.substring(0,afterLastSpace.lastIndexOf(File.separator)));

                int index = afterLastSpace.lastIndexOf(File.separator);
                String hold = afterLastSpace.substring(0, index + 1);
                afterLastSpace = afterLastSpace.substring(index + 1);
                beforeLastSpace  = beforeLastSpace  + Tuils.SPACE + hold;

                suggestFilesInDir(suggestions, dirInfo.file, afterLastSpace, beforeLastSpace );
            }
        }
    }

    private void suggestFilesInDir(List<Suggestion> suggestions, File dir, String afterLastSpace, String beforeLastSpace ) {
        if (dir == null || !dir.isDirectory())
            return;

        if (afterLastSpace == null || afterLastSpace.length() == 0) {
            suggestFilesInDir(suggestions, dir, beforeLastSpace );
            return;
        }

        String[] files = dir.list();
        if(files == null) {
            return;
        }

        for(SimpleMutableEntry<String, Integer> s : Compare.matchesWithRate(files, afterLastSpace, false)) {
            suggestions.add(new Suggestion(beforeLastSpace , s.getKey(), false, s.getValue(), Suggestion.TYPE_FILE));
        }
    }

    private void suggestFilesInDir(List<Suggestion> suggestions, File dir, String beforeLastSpace ) {
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
                suggestions.add(new Suggestion(beforeLastSpace , s, false, NO_RATE, Suggestion.TYPE_FILE));
            }
        } catch (NullPointerException e) {}
    }

    private void suggestContact(MainPack info, List<Suggestion> suggestions, String afterLastSpace, String beforeLastSpace ) {
        List<ContactManager.Contact> contacts = info.contacts.getContacts();
        if(contacts == null) return;

        if (afterLastSpace == null || afterLastSpace.length() == 0) {
            for (ContactManager.Contact contact : contacts) suggestions.add(new Suggestion(beforeLastSpace , contact.name, true, NO_RATE, Suggestion.TYPE_CONTACT, contact));
        }
        else {
            for(ContactManager.Contact contact : contacts) {
                if(Thread.currentThread().isInterrupted()) return;

                int rate = Compare.matches(contact.name, afterLastSpace, true);
                if(rate != -1) {
                    suggestions.add(new Suggestion(beforeLastSpace , contact.name, true, rate, Suggestion.TYPE_CONTACT, contact));
                }
            }
        }
    }

    private void suggestSong(MainPack info, List<Suggestion> suggestions, String afterLastSpace, String beforeLastSpace ) {
        if(info.player == null) return;

        List<Song> songs = info.player.getSongs();
        if(songs == null) return;

        if (afterLastSpace == null || afterLastSpace.length() == 0) {
            for (Song s : songs) {
                suggestions.add(new Suggestion(beforeLastSpace , s.getTitle(), clickToLaunch, NO_RATE, Suggestion.TYPE_SONG));
            }
        }
        else {
            List<SimpleMutableEntry<Compare.Stringable, Integer>> infos = Compare.matchesWithRate(songs, true, afterLastSpace);
            for(SimpleMutableEntry<Compare.Stringable, Integer> i : infos) {
                suggestions.add(new Suggestion(beforeLastSpace , i.getKey().getString(), clickToLaunch, i.getValue(), Suggestion.TYPE_SONG));
            }
        }
    }

    private void suggestCommand(MainPack info, List<Suggestion> suggestions, String afterLastSpace, String beforeLastSpace ) {
        if (afterLastSpace == null || afterLastSpace.length() == 0) {
            suggestCommand(info, suggestions, beforeLastSpace );
            return;
        }

        if(afterLastSpace.length() <= FIRST_INTERVAL) {
            afterLastSpace = afterLastSpace.toLowerCase().trim();

            String[] cmds = info.commandGroup.getCommandNames();
            if(cmds == null) return;

            for (String s : cmds) {
                if(Thread.currentThread().isInterrupted()) return;

                if(s.startsWith(afterLastSpace)) {
                    CommandAbstraction cmd = info.commandGroup.getCommandByName(s);
                    int[] args = cmd.argType();
                    boolean exec = args == null || args.length == 0;
                    suggestions.add(new Suggestion(beforeLastSpace , s, exec && clickToLaunch, MAX_RATE, Suggestion.TYPE_COMMAND));
                }
            }
        }
    }

    private void suggestColor(List<Suggestion> suggestions, String afterLastSpace, String beforeLastSpace ) {
        if(afterLastSpace == null || afterLastSpace.length() == 0 || (afterLastSpace.length() == 1 && afterLastSpace.charAt(0) != '#')) {
            suggestions.add(new Suggestion(beforeLastSpace , "#", false, MAX_RATE, Suggestion.TYPE_COLOR));
        }
    }

    private void suggestCommand(MainPack info, List<Suggestion> suggestions, String beforeLastSpace ) {
        String[] cmds = info.commandGroup.getCommandNames();
        if(cmds == null) return;

        for (String s : cmds) {
            if(Thread.currentThread().isInterrupted()) return;

            CommandAbstraction cmd = info.commandGroup.getCommandByName(s);
            if (cmd != null && cmd.priority() >= MIN_COMMAND_PRIORITY) {
                int[] args = cmd.argType();
                boolean exec = args == null || args.length == 0;
                suggestions.add(new Suggestion(beforeLastSpace , s, exec && clickToLaunch, cmd.priority(), Suggestion.TYPE_COMMAND));
            }
        }
    }

    private void suggestApp(MainPack info, List<Suggestion> suggestions, String afterLastSpace, String beforeLastSpace) {
        List<AppsManager.LaunchInfo> apps = info.appsManager.shownApps();
        if(apps == null) return;

        if (afterLastSpace == null || afterLastSpace.length() == 0) {
            for (AppsManager.LaunchInfo l : apps) {
                suggestions.add(new Suggestion(beforeLastSpace , l.publicLabel, clickToLaunch, NO_RATE, Suggestion.TYPE_APP, l));
            }
        }
        else {
            List<SimpleMutableEntry<Compare.Stringable, Integer>> infos = Compare.matchesWithRate(apps, true, afterLastSpace);
            for(SimpleMutableEntry<Compare.Stringable, Integer> i : infos) {
                suggestions.add(new Suggestion(beforeLastSpace , i.getKey().getString(), clickToLaunch, i.getValue(), Suggestion.TYPE_APP, i.getKey()));
            }
        }
    }

    private void suggestHiddenApp(MainPack info, List<Suggestion> suggestions, String afterLastSpace, String beforeLastSpace) {
        List<AppsManager.LaunchInfo> apps = info.appsManager.hiddenApps();
        if(apps == null) return;

        if (afterLastSpace == null || afterLastSpace.length() == 0) {
            for (AppsManager.LaunchInfo a : apps) {
                suggestions.add(new Suggestion(beforeLastSpace , a.publicLabel, false, NO_RATE, Suggestion.TYPE_APP));
            }
        }
        else {
            List<SimpleMutableEntry<Compare.Stringable, Integer>> infos = Compare.matchesWithRate(apps, true, afterLastSpace);
            for(SimpleMutableEntry<Compare.Stringable, Integer> i : infos) {
                suggestions.add(new Suggestion(beforeLastSpace , i.getKey().getString(), false, i.getValue(), Suggestion.TYPE_APP));
            }
        }
    }

    private void suggestAllPackages(MainPack info, List<Suggestion> suggestions, String afterLastSpace, String beforeLastSpace ) {
        List<AppsManager.LaunchInfo> apps = new ArrayList<>(info.appsManager.shownApps());
        apps.addAll(info.appsManager.hiddenApps());

        if (afterLastSpace == null || afterLastSpace.length() == 0) {
            for (AppsManager.LaunchInfo a : apps) {
                suggestions.add(new Suggestion(beforeLastSpace , a.publicLabel, clickToLaunch, NO_RATE, Suggestion.TYPE_APP, a));
            }
        } else {
            List<SimpleMutableEntry<Compare.Stringable, Integer>> infos = Compare.matchesWithRate(apps, true, afterLastSpace);
            for(SimpleMutableEntry<Compare.Stringable, Integer> i : infos) {
                suggestions.add(new Suggestion(beforeLastSpace , i.getKey().getString(), clickToLaunch, i.getValue(), Suggestion.TYPE_APP, i.getKey()));
            }
        }
    }

    private void suggestDefaultApp(MainPack info, List<Suggestion> suggestions, String afterLastSpace, String beforeLastSpace ) {
        suggestions.add(new Suggestion(beforeLastSpace , "most_used", false, MAX_RATE, Suggestion.TYPE_PERMANENT));
        suggestions.add(new Suggestion(beforeLastSpace , "null", false, MAX_RATE, Suggestion.TYPE_PERMANENT));

        List<AppsManager.LaunchInfo> apps = info.appsManager.shownApps();
        if(apps == null) return;

        if (afterLastSpace == null || afterLastSpace.length() == 0) {
            for (AppsManager.LaunchInfo a : apps) {
                suggestions.add(new Suggestion(beforeLastSpace , a.publicLabel, clickToLaunch, NO_RATE, Suggestion.TYPE_APP, a));
            }
        }
        else {
            List<SimpleMutableEntry<Compare.Stringable, Integer>> infos = Compare.matchesWithRate(apps, true, afterLastSpace);
            for(SimpleMutableEntry<Compare.Stringable, Integer> i : infos) {
                suggestions.add(new Suggestion(beforeLastSpace , i.getKey().getString(), clickToLaunch, i.getValue(), Suggestion.TYPE_APP, i.getKey()));
            }
        }
    }

    private void suggestConfigEntry(List<Suggestion> suggestions, String afterLastSpace, String beforeLastSpace ) {
        if(xmlPrefsEntrys == null) {
            xmlPrefsEntrys = new ArrayList<>();

            for(XMLPrefsManager.XMLPrefsRoot element : XMLPrefsManager.XMLPrefsRoot.values()) xmlPrefsEntrys.addAll(element.copy);

            Collections.addAll(xmlPrefsEntrys, Apps.values());
            Collections.addAll(xmlPrefsEntrys, Notifications.values());
            Collections.addAll(xmlPrefsEntrys, Rss.values());
        }

        if(afterLastSpace == null || afterLastSpace.length() == 0) {
            for(XMLPrefsManager.XMLPrefsSave s : xmlPrefsEntrys) {
                Suggestion sg = new Suggestion(beforeLastSpace , s.label(), false, NO_RATE, Suggestion.TYPE_COMMAND);
                suggestions.add(sg);
            }
        }
        else {
            for (XMLPrefsManager.XMLPrefsSave s : xmlPrefsEntrys) {
                if(Thread.currentThread().isInterrupted()) return;

                String label = s.label();
                int rate = Compare.matches(label, afterLastSpace, true);
                if(rate != -1) {
                    suggestions.add(new Suggestion(beforeLastSpace , label, false, rate, Suggestion.TYPE_COMMAND));
                }
            }
        }
    }

    private void suggestConfigFile(List<Suggestion> suggestions, String afterLastSpace, String beforeLastSpace ) {
        if(xmlPrefsFiles == null) {
            xmlPrefsFiles = new ArrayList<>();
            for(XMLPrefsManager.XMLPrefsRoot element : XMLPrefsManager.XMLPrefsRoot.values())
                xmlPrefsFiles.add(element.path);
            xmlPrefsFiles.add(AppsManager.PATH);
            xmlPrefsFiles.add(NotificationManager.PATH);
            xmlPrefsFiles.add(RssManager.PATH);
        }

        if(afterLastSpace == null || afterLastSpace.length() == 0) {
            for(String s : xmlPrefsFiles) {
                Suggestion sg = new Suggestion(beforeLastSpace , s, false, NO_RATE, Suggestion.TYPE_FILE);
                suggestions.add(sg);
            }
        } else if(afterLastSpace.length() <= FIRST_INTERVAL) {
            afterLastSpace = afterLastSpace.trim().toLowerCase();
            for (String s : xmlPrefsFiles) {
                if(Thread.currentThread().isInterrupted()) return;

                if(s.startsWith(afterLastSpace)) {
                    suggestions.add(new Suggestion(beforeLastSpace , s, false, MAX_RATE, Suggestion.TYPE_FILE));
                }
            }
        }
    }

    private void suggestAppGroup(List<Suggestion> suggestions, String afterLastSpace, String beforeLastSpace ) {
        List<AppsManager.Group> groups = AppsManager.groups;

        if(afterLastSpace == null || afterLastSpace.length() == 0) {
            for(AppsManager.Group g : groups) {
                Suggestion sg = new Suggestion(beforeLastSpace , g.getName(), false, NO_RATE, Suggestion.TYPE_APP, g);
                suggestions.add(sg);
            }
        }
        else {
            for(AppsManager.Group g : groups) {
                String label = g.getName();
                int rate = Compare.matches(label, afterLastSpace, true);
                if(rate != -1) {
                    suggestions.add(new Suggestion(beforeLastSpace , label, false, rate, Suggestion.TYPE_APP, g));
                }
            }
        }
    }

    private boolean suggestAppInsideGroup(List<Suggestion> suggestions, String afterLastSpace, String beforeLastSpace , boolean keepGroupName) {
        int index = -1;

        String app = Tuils.EMPTYSTRING;

        if(!beforeLastSpace .contains(Tuils.SPACE)) {
            index = Tuils.find(beforeLastSpace , AppsManager.groups);
            app = afterLastSpace;
            if(!keepGroupName) beforeLastSpace  = Tuils.EMPTYSTRING;
        } else {
            String[] split = beforeLastSpace .split(Tuils.SPACE);
            for(int count = 0; count < split.length; count++) {
                index = Tuils.find(split[count], AppsManager.groups);
                if(index != -1) {

                    beforeLastSpace  = Tuils.EMPTYSTRING;
                    for(int i = 0; (keepGroupName ? i <= count : i < count); i++) {
                        beforeLastSpace  = beforeLastSpace  + split[i] + Tuils.SPACE;
                    }
                    beforeLastSpace  = beforeLastSpace .trim();

                    count += 1;
                    for(; count < split.length; count++) {
                        app = app + split[count] + Tuils.SPACE;
                    }
                    if(afterLastSpace != null) app = app + Tuils.SPACE + afterLastSpace;
                    app = app.trim();

                    break;
                }
            }
        }

        if(index == -1) return false;

        AppsManager.Group g = AppsManager.groups.get(index);

        List<? extends Compare.Stringable> apps = g.members();
        if(apps != null && apps.size() > 0) {
            if (app == null || app.length() == 0) {
                for (Compare.Stringable s : apps) {
                    suggestions.add(new Suggestion(beforeLastSpace , s.getString(), clickToLaunch, NO_RATE, Suggestion.TYPE_APP, s));
                }
            }
            else {
                List<SimpleMutableEntry<Compare.Stringable, Integer>> infos = Compare.matchesWithRate(apps, true, app);
                for(SimpleMutableEntry<Compare.Stringable, Integer> i : infos) {
                    suggestions.add(new Suggestion(beforeLastSpace , i.getKey().getString(), clickToLaunch, i.getValue(), Suggestion.TYPE_APP, i.getKey()));
                }
            }
        }

        return true;
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

        public Suggestion(String beforeLastSpace , String text, boolean exec, int rate, int type) {
            this(beforeLastSpace , text, exec, rate, type, null);
        }

        public Suggestion(String beforeLastSpace , String text, boolean exec, int rate, int type, Object tag) {
            this.textBefore = beforeLastSpace ;
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
