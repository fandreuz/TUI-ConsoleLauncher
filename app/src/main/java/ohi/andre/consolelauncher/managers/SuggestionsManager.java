package ohi.andre.consolelauncher.managers;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import ohi.andre.comparestring.Compare;
import ohi.andre.consolelauncher.commands.Command;
import ohi.andre.consolelauncher.commands.CommandAbstraction;
import ohi.andre.consolelauncher.commands.CommandTuils;
import ohi.andre.consolelauncher.commands.ExecInfo;
import ohi.andre.consolelauncher.tuils.Tuils;

/**
 * Created by francescoandreuzzi on 25/12/15.
 */
public class SuggestionsManager {

    private static final int MIN_COMMAND_RATE = 1;
    private static final int MIN_COMMAND_PRIORITY = 5;

    private static final int MIN_APPS_RATE = 3;

    private static final int MIN_CONTACTS_RATE = 2;

    private static final int MIN_FILE_RATE = 2;

    private static final int MIN_SONGS_RATE = 2;

    //    use to place something at the top
    private static final int MAX_RATE = 100;

    public static String[] getSuggestions(ExecInfo info, String before, String lastWord) {

        List<Compare.CompareInfo> suggestionList = new ArrayList<>();

        before = Tuils.trimSpaces(before);
        lastWord = Tuils.trimSpaces(lastWord);

//        lastword = 0
        if (lastWord.length() == 0) {
//            lastword = 0 && before = 0
            if (before.length() == 0) {
                suggestAlias(info, suggestionList);
                suggestCommand(info, suggestionList);
                return toSuggestionArray(suggestionList);
            }
//            lastword = 0 && before > 0
            else {
//                check if this is a command
                Command cmd = null;
                try {
                    cmd = CommandTuils.parse(before, info, true);
                } catch (Exception e) {
                }

                if (cmd != null) {
                    if (cmd.nArgs == cmd.cmd.maxArgs())
                        return new String[0];

                    int nextArg = cmd.nextArg();
                    if (nextArg == CommandAbstraction.PARAM)
                        suggestParams(suggestionList, cmd.cmd);
                    else
                        suggestArgs(info, cmd.nextArg(), suggestionList);
                } else {
//                    something typed
//                    but contains spaces ==> not command
                    suggestApp(info, suggestionList, before);
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
                    if (cmd.cmd.maxArgs() == 1 && before.contains(" ")) {
                        int index = cmd.cmd.getClass().getSimpleName().length() + 1;

                        StringBuilder builder = new StringBuilder();
                        builder.append(before.substring(index));
                        builder.append(lastWord);
                        lastWord = builder.toString();
                    }

                    suggestArgs(info, cmd.nextArg(), suggestionList, lastWord);
                } else {
//                    not a command
                    suggestApp(info, suggestionList, before.concat(lastWord));
                }
            } else {
//                lastword > 0 && before = 0
                suggestCommand(info, suggestionList, lastWord);
                suggestApp(info, suggestionList, lastWord);
            }
        }

        Collections.sort(suggestionList);
        return toSuggestionArray(suggestionList);
    }

    private static void suggestAlias(ExecInfo info, List<Compare.CompareInfo> suggestions) {
        Set<String> alias = info.aliasManager.getAliass();
        for (String s : alias)
            suggestions.add(new Compare.CompareInfo(s, -1));
    }

    private static void suggestParams(List<Compare.CompareInfo> suggestions, CommandAbstraction cmd) {
        String[] params = cmd.parameters();
        if (params == null)
            return;
        for (String s : cmd.parameters())
            suggestions.add(new Compare.CompareInfo(s, -1));
    }

    private static void suggestArgs(ExecInfo info, int type, List<Compare.CompareInfo> suggestions, String prev) {
        switch (type) {
            case CommandAbstraction.FILE:
            case CommandAbstraction.FILE_LIST:
                suggestFile(info, suggestions, prev);
                break;
            case CommandAbstraction.PACKAGE:
                suggestApp(info, suggestions, prev);
                break;
            case CommandAbstraction.COMMAND:
                suggestCommand(info, suggestions, prev);
                break;
            case CommandAbstraction.CONTACTNUMBER:
                suggestContact(info, suggestions, prev);
                break;
            case CommandAbstraction.SONG:
                suggestSong(info, suggestions, prev);
                break;
        }
    }

    private static void suggestArgs(ExecInfo info, int type, List<Compare.CompareInfo> suggestions) {
        suggestArgs(info, type, suggestions, null);
    }

    private static void suggestFile(ExecInfo info, List<Compare.CompareInfo> suggestions, String prev) {
        suggestions.add(new Compare.CompareInfo(File.separator, MAX_RATE));

        if (prev == null || prev.length() == 0) {
            suggestFilesInDir(suggestions, info.currentDirectory);
            return;
        }

        if (!prev.contains(File.separator) && prev.length() > 0) {
            suggestFilesInDir(suggestions, info.currentDirectory, prev);
        } else if (prev.length() > 0) {
            if (prev.endsWith(File.separator)) {
                prev = prev.substring(0, prev.length() - 1);
                FileManager.DirInfo dirInfo = FileManager.cd(info.currentDirectory, prev);
                if (dirInfo.file.isDirectory()) {
                    for (String s : dirInfo.file.list())
                        suggestions.add(new Compare.CompareInfo(s, -1));
                }
            } else {
                FileManager.DirInfo dirInfo = FileManager.cd(info.currentDirectory, prev);
                if (dirInfo.file.isDirectory()) {
                    prev = prev.substring(prev.indexOf(File.separator) + 1);
                    Compare.getCompareInfo(suggestions, dirInfo.file.list(), prev, MIN_FILE_RATE, FileManager.USE_SCROLL_COMPARE);
                }
            }
        }
    }

    private static void suggestContact(ExecInfo info, List<Compare.CompareInfo> suggestions, String prev) {
        if (prev == null || prev.length() == 0) {
            for (String s : info.contacts.names())
                suggestions.add(new Compare.CompareInfo(s, -1));
        } else
            Compare.getCompareInfo(suggestions, info.contacts.names(), prev, MIN_CONTACTS_RATE, ContactManager.USE_SCROLL_COMPARE);
    }

    private static void suggestSong(ExecInfo info, List<Compare.CompareInfo> suggestions, String prev) {
        if (prev == null || prev.length() == 0) {
            for (String s : info.player.getNames())
                suggestions.add(new Compare.CompareInfo(s, -1));
        } else
            Compare.getCompareInfo(suggestions, info.player.getNames(), prev, MIN_SONGS_RATE, MusicManager.USE_SCROLL_COMPARE);
    }

    //    help...
    private static void suggestCommand(ExecInfo info, List<Compare.CompareInfo> suggestions, String prev) {
        if (prev == null || prev.length() == 0) {
            suggestCommand(info, suggestions);
            return;
        }

        Compare.getCompareInfo(suggestions, info.commandGroup.getCommands(), prev, MIN_COMMAND_RATE, false);
    }

    //    use when suggesting random commands
    private static void suggestCommand(ExecInfo info, List<Compare.CompareInfo> suggestions) {
        for (String s : info.commandGroup.getCommands()) {
            CommandAbstraction cmd = null;
            try {
                cmd = info.commandGroup.getCommandByName(s);
            } catch (Exception e) {
            }

            if (cmd != null && cmd.priority() >= MIN_COMMAND_PRIORITY)
                suggestions.add(new Compare.CompareInfo(s, cmd.priority()));
        }
    }

    private static void suggestApp(ExecInfo info, List<Compare.CompareInfo> suggestions, String prev) {
        if (prev == null || prev.length() == 0) {
            for (String s : info.appsManager.getAppsLabels())
                suggestions.add(new Compare.CompareInfo(s, -1));
        } else
            Compare.getCompareInfo(suggestions, info.appsManager.getAppsLabels(), prev, MIN_APPS_RATE, AppsManager.USE_SCROLL_COMPARE);
    }

    private static void suggestFilesInDir(List<Compare.CompareInfo> suggestions, File dir, String prev) {
        if (dir == null || !dir.isDirectory())
            return;

        if (prev == null || prev.length() == 0) {
            suggestFilesInDir(suggestions, dir);
            return;
        }

        Compare.getCompareInfo(suggestions, dir.list(), prev, MIN_FILE_RATE, FileManager.USE_SCROLL_COMPARE);
    }

    private static void suggestFilesInDir(List<Compare.CompareInfo> suggestions, File dir) {
        if (dir == null || !dir.isDirectory())
            return;

        try {
            for (String s : dir.list())
                suggestions.add(new Compare.CompareInfo(s, -1));
        } catch (NullPointerException e) {
        }
    }

    private static String[] toSuggestionArray(List<Compare.CompareInfo> list) {
        String[] strings = new String[list.size()];
        for (int count = 0; count < strings.length; count++)
            strings[count] = list.get(count).s;
        return strings;
    }
}
