package ohi.andre.consolelauncher.managers;

import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

    public static final int MIN_APPS_RATE = 3;

    public static final int MIN_CONTACTS_RATE = 2;

    public static final int MIN_FILE_RATE = 2;

    public static final int MIN_SONGS_RATE = 2;

//    use to place something at the top
    private static final int MAX_RATE = 100;
    private static final int NO_RATE = -1;

    public static Suggestion[] getSuggestions(ExecInfo info, String before, String lastWord) {

        List<Suggestion> suggestionList = new ArrayList<>();

        before = before.trim();
        lastWord = lastWord.trim();

//        lastword = 0
        if (lastWord.length() == 0) {
//            lastword = 0 && before = 0
            if (before.length() == 0) {
                String[] apps = info.appsManager.getSuggestedApps();
                for(int count = 0; count < apps.length; count++) {
                    if(apps[count] == null) {
                        continue;
                    }

                    float shift = count + 1;
                    float rate = 1f / shift;
                    suggestionList.add(new Suggestion(apps[count], true, (int) Math.ceil(rate), Suggestion.TYPE_APP));
                }
                return suggestionList.toArray(new Suggestion[suggestionList.size()]);
            }
//            lastword = 0 && before > 0
            else {
//                check if this is a command
                Command cmd = null;
                try {
                    cmd = CommandTuils.parse(before, info, true);
                } catch (Exception e) {}

                if (cmd != null) {
                    if (cmd.nArgs == cmd.cmd.maxArgs())
                        return new Suggestion[0];

                    int nextArg = cmd.nextArg();
                    if (nextArg == CommandAbstraction.PARAM) {
                        suggestParams(suggestionList, cmd.cmd);
                    }
                    else {
                        suggestArgs(info, cmd.nextArg(), suggestionList);
                    }
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
                    if (cmd.cmd.maxArgs() == 1 && before.contains(Tuils.SPACE)) {
                        int index = cmd.cmd.getClass().getSimpleName().length() + 1;

                        lastWord = before.substring(index) + lastWord;
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
        Suggestion[] array = new Suggestion[suggestionList.size()];
        return suggestionList.toArray(array);
    }

    private static void suggestParams(List<Suggestion> suggestions, CommandAbstraction cmd) {
        String[] params = cmd.parameters();
        if (params == null) {
            return;
        }

        int[] args = cmd.argType();
        boolean exec = args[args.length - 1] == CommandAbstraction.PARAM;
        for (String s : cmd.parameters()) {
            suggestions.add(new Suggestion(s, exec, NO_RATE, 0));
        }
    }

    private static void suggestArgs(ExecInfo info, int type, List<Suggestion> suggestions, String prev) {
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

    private static void suggestArgs(ExecInfo info, int type, List<Suggestion> suggestions) {
        suggestArgs(info, type, suggestions, null);
    }

    private static void suggestFile(ExecInfo info, List<Suggestion> suggestions, String prev) {
        suggestions.add(new Suggestion(File.separator, false, MAX_RATE, Suggestion.TYPE_FILE));

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
                    for (String s : dirInfo.file.list()) {
                        suggestions.add(new Suggestion(s, false, NO_RATE, Suggestion.TYPE_FILE));
                    }
                }
            } else {
                FileManager.DirInfo dirInfo = FileManager.cd(info.currentDirectory, prev);
                if (dirInfo.file.isDirectory()) {
                    prev = prev.substring(prev.indexOf(File.separator) + 1);
                    List<Compare.CompareInfo> infos = Compare.compareInfo(dirInfo.file.list(), prev, MIN_FILE_RATE,
                            FileManager.USE_SCROLL_COMPARE);
                    for(Compare.CompareInfo i : infos) {
                        suggestions.add(new Suggestion(i.s, false, i.rate, Suggestion.TYPE_FILE));
                    }
                }
            }
        }
    }

    private static void suggestContact(ExecInfo info, List<Suggestion> suggestions, String prev) {
        if (prev == null || prev.length() == 0) {
            for (String s : info.contacts.names())
                suggestions.add(new Suggestion(s, true, NO_RATE, Suggestion.TYPE_CONTACT));
        } else {
            List<Compare.CompareInfo> infos = Compare.compareInfo(info.contacts.names(), prev, MIN_CONTACTS_RATE,
                    ContactManager.USE_SCROLL_COMPARE);
            for(Compare.CompareInfo i : infos) {
                suggestions.add(new Suggestion(i.s, true, i.rate, Suggestion.TYPE_CONTACT));
            }
        }
    }

    private static void suggestSong(ExecInfo info, List<Suggestion> suggestions, String prev) {
        if (prev == null || prev.length() == 0) {
            for (String s : info.player.getNames())
                suggestions.add(new Suggestion(s, true, NO_RATE, Suggestion.TYPE_SONG));
        } else {
            List<Compare.CompareInfo> infos = Compare.compareInfo(info.player.getNames(), prev, MIN_SONGS_RATE,
                    MusicManager.USE_SCROLL_COMPARE);
            for(Compare.CompareInfo i : infos) {
                suggestions.add(new Suggestion(i.s, true, i.rate, Suggestion.TYPE_SONG));
            }
        }
    }

    private static void suggestCommand(ExecInfo info, List<Suggestion> suggestions, String prev) {
        if (prev == null || prev.length() == 0) {
            suggestCommand(info, suggestions);
            return;
        }

        List<Compare.CompareInfo> infos = Compare.compareInfo(info.commandGroup.getCommandNames(), prev, MIN_COMMAND_RATE, false);
        for(Compare.CompareInfo i : infos) {
            CommandAbstraction cmd = info.commandGroup.getCommandByName(i.s);
            int[] args = cmd.argType();
            boolean exec = args == null || args.length == 0;
            suggestions.add(new Suggestion(i.s, exec, i.rate, Suggestion.TYPE_COMMAND));
        }
    }

    private static void suggestCommand(ExecInfo info, List<Suggestion> suggestions) {
        for (String s : info.commandGroup.getCommandNames()) {
            CommandAbstraction cmd = info.commandGroup.getCommandByName(s);
            if (cmd != null && cmd.priority() >= MIN_COMMAND_PRIORITY) {
                int[] args = cmd.argType();
                boolean exec = args == null || args.length == 0;
                suggestions.add(new Suggestion(s, exec, cmd.priority(), Suggestion.TYPE_COMMAND));
            }
        }
    }

    private static void suggestApp(ExecInfo info, List<Suggestion> suggestions, String prev) {
        if (prev == null || prev.length() == 0) {
            for (String s : info.appsManager.getAppLabels()) {
                suggestions.add(new Suggestion(s, true, NO_RATE, Suggestion.TYPE_APP));
            }
        } else {
            List<Compare.CompareInfo> infos = Compare.compareInfo(info.appsManager.getAppLabels(), prev, MIN_APPS_RATE,
                    AppsManager.USE_SCROLL_COMPARE);
            for(Compare.CompareInfo i : infos) {
                suggestions.add(new Suggestion(i.s, true, i.rate, Suggestion.TYPE_APP));
            }
        }
    }

    private static void suggestFilesInDir(List<Suggestion> suggestions, File dir, String prev) {
        if (dir == null || !dir.isDirectory())
            return;

        if (prev == null || prev.length() == 0) {
            suggestFilesInDir(suggestions, dir);
            return;
        }

        List<Compare.CompareInfo> infos = Compare.compareInfo(dir.list(), prev, MIN_FILE_RATE,
                FileManager.USE_SCROLL_COMPARE);

        for(Compare.CompareInfo i : infos) {
            suggestions.add(new Suggestion(i.s, false, i.rate, Suggestion.TYPE_FILE));
        }
    }

    private static void suggestFilesInDir(List<Suggestion> suggestions, File dir) {
        if (dir == null || !dir.isDirectory()) {
            return;
        }

        try {
            for (String s : dir.list()) {
                suggestions.add(new Suggestion(s, false, NO_RATE, Suggestion.TYPE_FILE));
            }
        } catch (NullPointerException e) {}
    }

    public static class Suggestion implements Comparable<Suggestion> {

        public static final int TYPE_APP = 10;
        public static final int TYPE_FILE = 11;
        public static final int TYPE_ALIAS = 12;
        public static final int TYPE_COMMAND = 13;
        public static final int TYPE_SONG = 14;
        public static final int TYPE_CONTACT = 15;

        public String text;
        public boolean exec;
        public int rate;
        public int type;

        public Suggestion(String text, boolean exec, int rate, int type) {
            this.text = text;
            this.exec = exec;
            this.rate = rate;
            this.type = type;
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
