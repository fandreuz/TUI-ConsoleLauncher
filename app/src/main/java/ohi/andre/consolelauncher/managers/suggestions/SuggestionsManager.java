package ohi.andre.consolelauncher.managers.suggestions;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import it.andreuzzi.comparestring2.AlgMap;
import it.andreuzzi.comparestring2.CompareObjects;
import it.andreuzzi.comparestring2.CompareStrings;
import it.andreuzzi.comparestring2.StringableObject;
import it.andreuzzi.comparestring2.algs.interfaces.Algorithm;
import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.UIManager;
import ohi.andre.consolelauncher.commands.Command;
import ohi.andre.consolelauncher.commands.CommandAbstraction;
import ohi.andre.consolelauncher.commands.CommandTuils;
import ohi.andre.consolelauncher.commands.main.MainPack;
import ohi.andre.consolelauncher.commands.main.Param;
import ohi.andre.consolelauncher.commands.main.specific.ParamCommand;
import ohi.andre.consolelauncher.commands.main.specific.PermanentSuggestionCommand;
import ohi.andre.consolelauncher.managers.AliasManager;
import ohi.andre.consolelauncher.managers.AppsManager;
import ohi.andre.consolelauncher.managers.ContactManager;
import ohi.andre.consolelauncher.managers.FileManager;
import ohi.andre.consolelauncher.managers.RssManager;
import ohi.andre.consolelauncher.managers.TerminalManager;
import ohi.andre.consolelauncher.managers.music.Song;
import ohi.andre.consolelauncher.managers.notifications.NotificationManager;
import ohi.andre.consolelauncher.managers.notifications.reply.BoundApp;
import ohi.andre.consolelauncher.managers.notifications.reply.ReplyManager;
import ohi.andre.consolelauncher.managers.xml.XMLPrefsManager;
import ohi.andre.consolelauncher.managers.xml.classes.XMLPrefsSave;
import ohi.andre.consolelauncher.managers.xml.options.Apps;
import ohi.andre.consolelauncher.managers.xml.options.Behavior;
import ohi.andre.consolelauncher.managers.xml.options.Notifications;
import ohi.andre.consolelauncher.managers.xml.options.Reply;
import ohi.andre.consolelauncher.managers.xml.options.Rss;
import ohi.andre.consolelauncher.managers.xml.options.Suggestions;
import ohi.andre.consolelauncher.tuils.StoppableThread;
import ohi.andre.consolelauncher.tuils.Tuils;

import static ohi.andre.consolelauncher.commands.CommandTuils.xmlPrefsEntrys;
import static ohi.andre.consolelauncher.commands.CommandTuils.xmlPrefsFiles;

/**
 * Created by francescoandreuzzi on 25/12/15.
 */
public class SuggestionsManager {

    private HideSuggestionViewValues hideViewValue;

    public static final String SINGLE_QUOTE = "'", DOUBLE_QUOTES = "\"";
    private final int FIRST_INTERVAL = 6;

    private final String[] SPLITTERS = {Tuils.SPACE};
    private final String[] FILE_SPLITTERS = {Tuils.SPACE, "-", "_"};
    private final String[] XML_PREFS_SPLITTERS = {"_"};

    private boolean showAliasDefault, clickToLaunch, showAppsGpDefault, enabled;
    private int minCmdPriority;

    private String multipleCmdSeparator;

    private boolean doubleSpaceFirstSuggestion;
    private LinearLayout suggestionsView;
    private SuggestionRunnable suggestionRunnable;
    private LinearLayout.LayoutParams suggestionViewParams;
    private SuggestionsManager.Suggestion lastFirst;

    private TerminalManager mTerminalAdapter;

    private View.OnClickListener clickListener = v -> {
        Suggestion suggestion = (Suggestion) v.getTag(R.id.suggestion_id);
        clickSuggestion(suggestion);
    };

    private MainPack pack;
    private StoppableThread lastSuggestionThread;
    private Handler handler = new Handler();

    private RemoverRunnable removeAllSuggestions;

    private int[] spaces;

    int[] counts, noInputCounts;

    private Pattern rmQuotes = Pattern.compile("['\"]");

    int suggestionsPerCategory;
    float suggestionsDeadline;

    private CustomComparator comparator;

    private Algorithm algInstance;
    private AlgMap.Alg alg;

    private int quickCompare;

    public SuggestionsManager(LinearLayout suggestionsView, MainPack mainPack, TerminalManager mTerminalAdapter) {
        this.suggestionsView = suggestionsView;
        this.pack = mainPack;
        this.mTerminalAdapter = mTerminalAdapter;

        setAlgorithm(XMLPrefsManager.getInt(Suggestions.suggestions_algorithm));

        quickCompare = XMLPrefsManager.getInt(Suggestions.suggestions_quickcompare_n);

        this.suggestionsPerCategory = XMLPrefsManager.getInt(Suggestions.suggestions_per_category);
        this.suggestionsDeadline = Float.parseFloat(XMLPrefsManager.get(Suggestions.suggestions_deadline));

        this.removeAllSuggestions = new RemoverRunnable(suggestionsView);

        doubleSpaceFirstSuggestion = XMLPrefsManager.getBoolean(Suggestions.double_space_click_first_suggestion);
        SuggestionsManager.Suggestion.appendQuotesBeforeFile = XMLPrefsManager.getBoolean(Behavior.append_quote_before_file);
        multipleCmdSeparator = XMLPrefsManager.get(Behavior.multiple_cmd_separator);

        enabled = true;

        showAliasDefault = XMLPrefsManager.getBoolean(Suggestions.suggest_alias_default);
        showAppsGpDefault = XMLPrefsManager.getBoolean(Suggestions.suggest_appgp_default);
        clickToLaunch = XMLPrefsManager.getBoolean(Suggestions.click_to_launch);

        minCmdPriority = XMLPrefsManager.getInt(Suggestions.noinput_min_command_priority);

        spaces = UIManager.getListOfIntValues(XMLPrefsManager.get(Suggestions.suggestions_spaces), 4, 0);

        try {
            hideViewValue = HideSuggestionViewValues.valueOf(XMLPrefsManager.get(Suggestions.hide_suggestions_when_empty).toUpperCase());
        } catch (Exception e) {
            hideViewValue = HideSuggestionViewValues.valueOf(Suggestions.hide_suggestions_when_empty.defaultValue().toUpperCase());
        }

        String s = XMLPrefsManager.get(Suggestions.suggestions_order);
        Pattern orderPattern = Pattern.compile("(\\d+)\\((\\d+)\\)");
        Matcher m = orderPattern.matcher(s);

        int[] indexes = new int[4];
        counts = new int[4];

        int index = 0;
        while(m.find() && index < indexes.length) {
            int type = Integer.parseInt(m.group(1));

            if(type >= indexes.length) {
                Tuils.sendOutput(Color.RED, pack.context, "Invalid suggestion type: " + type);

                indexes = null;
                counts = null;

                break;
            }

            int count = Integer.parseInt(m.group(2));

            indexes[type] = index;
            counts[type] = count;

            index++;
        }

        s = XMLPrefsManager.get(Suggestions.noinput_suggestions_order);
        orderPattern = Pattern.compile("(\\d+)\\((\\d+)\\)");
        m = orderPattern.matcher(s);

        int[] noInputIndexes = new int[4];
        noInputCounts = new int[4];

        index = 0;
        while(m.find() && index < noInputIndexes.length) {
            int type = Integer.parseInt(m.group(1));

            if(type >= noInputIndexes.length) {
                Tuils.sendOutput(Color.RED, pack.context, "Invalid suggestion type: " + type);

                noInputIndexes = null;
                noInputCounts = null;

                break;
            }

            int count = Integer.parseInt(m.group(2));

            noInputIndexes[type] = index;
            noInputCounts[type] = count;

            index++;
        }

        comparator = new CustomComparator(noInputIndexes, indexes);

        TextView uselessView = getSuggestionView(pack.context);
        uselessView.setVisibility(View.INVISIBLE);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(spaces[0], spaces[1], spaces[2], spaces[3]);

        ((LinearLayout) suggestionsView.getParent()).addView(uselessView, params);
    }

    private void setAlgorithm(int id) {
        switch (id) {
            case 0:
                alg = AlgMap.DistAlg.LCS;
                break;
            case 1:
                alg = AlgMap.DistAlg.OSA;
                break;
            case 2:
                alg = AlgMap.DistAlg.QGRAM;
                break;
            case 4:
                alg = AlgMap.NormDistAlg.COSINE;
                break;
            case 5:
                alg = AlgMap.NormDistAlg.JACCARD;
                break;
            case 6:
                alg = AlgMap.NormDistAlg.JAROWRINKLER;
                break;
            case 7:
                alg = AlgMap.NormDistAlg.METRICLCS;
                break;
            case 8:
                alg = AlgMap.NormDistAlg.NGRAM;
                break;
            case 9:
                alg = AlgMap.NormDistAlg.NLEVENSHTEIN;
                break;
            case 10:
                alg = AlgMap.NormDistAlg.SORENSENDICE;
                break;
            case 11:
                alg = AlgMap.NormSimAlg.COSINE;
                break;
            case 12:
                alg = AlgMap.NormSimAlg.JACCARD;
                break;
            case 13:
                alg = AlgMap.NormSimAlg.JAROWRINKLER;
                break;
            case 14:
                alg = AlgMap.NormSimAlg.NLEVENSHTEIN;
                break;
            case 15:
                alg = AlgMap.NormSimAlg.SORENSENDICE;
                break;
            case 16:
                alg = AlgMap.MetricDistAlg.DAMERAU;
                break;
            case 17:
                alg = AlgMap.MetricDistAlg.JACCARD;
                break;
            case 18:
                alg = AlgMap.MetricDistAlg.LEVENSHTEIN;
                break;
            case 19:
                alg = AlgMap.MetricDistAlg.METRICLCS;
                break;
        }

        algInstance = alg.buildAlg(id);
    }

    public TextView getSuggestionView(Context context) {
        TextView textView = new TextView(context);
        textView.setOnClickListener(clickListener);

        textView.setFocusable(false);
        textView.setLongClickable(false);
        textView.setClickable(true);

        textView.setTypeface(Tuils.getTypeface(context));
        textView.setTextSize(XMLPrefsManager.getInt(Suggestions.suggestions_size));

        textView.setPadding(spaces[2], spaces[3], spaces[2], spaces[3]);

        textView.setLines(1);
        textView.setMaxLines(1);

        return textView;
    }

    private void stop() {
        handler.removeCallbacksAndMessages(null);
        if(lastSuggestionThread != null) lastSuggestionThread.interrupt();
    }

    public void dispose() {
        stop();
    }

    public void clear() {
        stop();
        suggestionsView.removeAllViews();
    }

    Runnable hideRunnable = new Runnable() {
        @Override
        public void run() {
            suggestionsView.setVisibility(View.GONE);

            stop();
        }
    };
    public void hide() {
        if(Looper.getMainLooper() == Looper.myLooper()) {
            hideRunnable.run();
        } else {
            ((Activity) mTerminalAdapter.mContext).runOnUiThread(hideRunnable);
        }
    }

    Runnable showRunnable = new Runnable() {
        @Override
        public void run() {
            suggestionsView.setVisibility(View.VISIBLE);
        }
    };
    public void show() {
        if(Looper.getMainLooper() == Looper.myLooper()) {
            showRunnable.run();
        } else {
            ((Activity) mTerminalAdapter.mContext).runOnUiThread(showRunnable);
        }
    }

    public void enable() {
        enabled = true;

        show();
    }

    public void disable() {
        enabled = false;

        hide();
    }

    public void clickSuggestion(SuggestionsManager.Suggestion suggestion) {
        boolean execOnClick = suggestion.exec;

        String text = suggestion.getText();
        String input = mTerminalAdapter.getInput();

        if(suggestion.type == SuggestionsManager.Suggestion.TYPE_PERMANENT) {
            mTerminalAdapter.setInput(input + text);
        } else {
            boolean addSpace = suggestion.type != SuggestionsManager.Suggestion.TYPE_FILE && suggestion.type != SuggestionsManager.Suggestion.TYPE_COLOR;

            if(multipleCmdSeparator.length() > 0) {
//                try to understand if the user is using a multiple cmd
                String[] split = input.split(multipleCmdSeparator);

//                not using it
                if(split.length == 1) mTerminalAdapter.setInput(text + (addSpace ? Tuils.SPACE : Tuils.EMPTYSTRING), suggestion.object);

//                yes
                else {
                    split[split.length - 1] = Tuils.EMPTYSTRING;

                    String beforeInputs = Tuils.EMPTYSTRING;
                    for(int count = 0; count < split.length - 1; count++) {
                        beforeInputs = beforeInputs + split[count] + multipleCmdSeparator;
                    }

                    mTerminalAdapter.setInput(beforeInputs + text + (addSpace ? Tuils.SPACE : Tuils.EMPTYSTRING), suggestion.object);
                }
            } else {
                mTerminalAdapter.setInput(text + (addSpace ? Tuils.SPACE : Tuils.EMPTYSTRING), suggestion.object);
            }
        }

        if (execOnClick) {
            mTerminalAdapter.simulateEnter();
        } else {
            mTerminalAdapter.focusInputEnd();
        }
    }

    public void requestSuggestion(final String input) {

        if(!enabled) return;

        if (suggestionViewParams == null) {
            suggestionViewParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            suggestionViewParams.setMargins(15, 0, 15, 0);
            suggestionViewParams.gravity = Gravity.CENTER_VERTICAL;
        }

        if(suggestionRunnable == null) {
            suggestionRunnable = new SuggestionRunnable(pack, suggestionsView, suggestionViewParams, (HorizontalScrollView) suggestionsView.getParent().getParent(), spaces);
        }

        if (lastSuggestionThread != null) {
            lastSuggestionThread.interrupt();
            suggestionRunnable.interrupt();
            if(handler != null) {
                handler.removeCallbacks(suggestionRunnable);
            }
        }

        try {
            int l = input.length();
            if (doubleSpaceFirstSuggestion && l > 0 && input.charAt(l - 1) == ' ') {
                if (input.charAt(l - 2) == ' ') {
//                    double space
                    if(lastFirst == null && suggestionsView.getChildCount() > 0) {
                        SuggestionsManager.Suggestion s = (SuggestionsManager.Suggestion) suggestionsView.getChildAt(0).getTag(R.id.suggestion_id);
                        if(!input.trim().endsWith(s.getText())) lastFirst = s;
                    }

                    if(lastFirst != null) {
                        SuggestionsManager.Suggestion s = lastFirst;
                        mTerminalAdapter.setInput(0 == l - 2 ? Tuils.EMPTYSTRING : input.substring(0, l - 2));
                        clickSuggestion(s);
                        return;
                    }
                } else if (suggestionsView.getChildCount() > 0) {
//                    single space
                    lastFirst = (SuggestionsManager.Suggestion) suggestionsView.getChildAt(0).getTag(R.id.suggestion_id);
                    if(lastFirst.getText().equals(input.trim())) {
                        lastFirst = null;
                    }
                }
            } else {
                lastFirst = null;
            }
        } catch (Exception e) {
//            this will trigger an error when there's a single space in the input field, but it's not a problem
            Tuils.log(e);
            Tuils.toFile(e);
        }

        lastSuggestionThread = new StoppableThread() {
            @Override
            public void run() {

                super.run();

                String before, lastWord;
                String lastInput;
                if(multipleCmdSeparator.length() > 0) {
                    String[] split = input.split(multipleCmdSeparator);
                    if(split.length == 0) lastInput = input;
                    else lastInput = split[split.length - 1];
                } else {
                    lastInput = input;
                }

                int lastSpace = lastInput.lastIndexOf(Tuils.SPACE);
                if(lastSpace == -1) {
                    before = Tuils.EMPTYSTRING;
                    lastWord = lastInput;
                } else {
                    before = lastInput.substring(0,lastSpace);
                    lastWord = lastInput.substring(lastSpace + 1,lastInput.length());
                }

                final List<SuggestionsManager.Suggestion> suggestions;
                try {
                    suggestions = getSuggestions(before, lastWord);
                } catch (Exception e) {
                    Tuils.log(e);
                    Tuils.toFile(e);
                    return;
                }

                if(suggestions.size() == 0) {
                    ((Activity) pack.context).runOnUiThread(removeAllSuggestions);
                    removeAllSuggestions.isGoingToRun = true;

                    if(hideViewValue == HideSuggestionViewValues.ALWAYS || (hideViewValue == HideSuggestionViewValues.TRUE && input.length() == 0)) {
                        hide();
                    }

                    return;
                } else {
                    if(removeAllSuggestions.isGoingToRun) {
                        removeAllSuggestions.stop = true;
                    }

                    show();
                }

                if (Thread.interrupted()) {
                    suggestionRunnable.interrupt();
                    return;
                }

                final TextView[] existingViews = new TextView[suggestionsView.getChildCount()];
                for (int count = 0; count < existingViews.length; count++) {
                    existingViews[count] = (TextView) suggestionsView.getChildAt(count);
                }

                if (Thread.interrupted()) {
                    suggestionRunnable.interrupt();
                    return;
                }

                int n = suggestions.size() - existingViews.length;
                TextView[] toAdd = null;
                TextView[] toRecycle = null;
                if (n == 0) {
                    toRecycle = existingViews;
                    toAdd = null;
                } else if (n > 0) {
                    toRecycle = existingViews;
                    toAdd = new TextView[n];
                    for (int count = 0; count < toAdd.length; count++) {
                        toAdd[count] = getSuggestionView(pack.context);
                    }
                } else if (n < 0) {
                    toAdd = null;
                    toRecycle = new TextView[suggestions.size()];
                    System.arraycopy(existingViews, 0, toRecycle, 0, toRecycle.length);
                }

                if (Thread.interrupted()) {
                    suggestionRunnable.interrupt();
                    return;
                }

                suggestionRunnable.setN(n);
                suggestionRunnable.setSuggestions(suggestions);
                suggestionRunnable.setToAdd(toAdd);
                suggestionRunnable.setToRecycle(toRecycle);
                suggestionRunnable.reset();
                ((Activity) pack.context).runOnUiThread(suggestionRunnable);
            }
        };

        try {
            lastSuggestionThread.start();
        } catch (InternalError e) {
            Tuils.log(e);
            Tuils.toFile(e);
        }
    }

//    there's always a space between beforelastspace and lastword
    public List<Suggestion> getSuggestions(String beforeLastSpace, String lastWord) {
        List<Suggestion> suggestionList = new ArrayList<>();

        beforeLastSpace  = beforeLastSpace .trim();
        lastWord = lastWord.trim();

//        lastword = 0
        if (lastWord.length() == 0) {

//            lastword = 0 && beforeLastSpace = 0
            if (beforeLastSpace .length() == 0) {
                comparator.noInput = true;

                AppsManager.LaunchInfo[] apps = pack.appsManager.getSuggestedApps();
                if (apps != null) {
                    for(int count = 0; count < apps.length && count < noInputCounts[Suggestion.TYPE_APP]; count++) {
                        if(apps[count] == null) {
                            continue;
                        }

                        suggestionList.add(new Suggestion(beforeLastSpace , apps[count].publicLabel, clickToLaunch, Suggestion.TYPE_APP, apps[count]));
                    }
                }

                suggestCommand(pack, suggestionList, null);

                if(showAliasDefault) suggestAlias(pack.aliasManager, suggestionList, lastWord);
                if(showAppsGpDefault) suggestAppGroup(pack, suggestionList, lastWord, beforeLastSpace );
            }

//            lastword == 0 && beforeLastSpace > 0
            else {
                comparator.noInput = false;

//                check if this is a command
                Command cmd = null;
                try {
                    cmd = CommandTuils.parse(beforeLastSpace , pack);
                } catch (Exception e) {}

                if (cmd != null) {

                    if(cmd.cmd instanceof PermanentSuggestionCommand) {
                        suggestPermanentSuggestions(suggestionList, (PermanentSuggestionCommand) cmd.cmd);
                    }

                    if (cmd.mArgs != null && cmd.mArgs.length > 0 && cmd.cmd instanceof ParamCommand && cmd.nArgs >= 1 && cmd.mArgs[0] instanceof Param && ((Param) cmd.mArgs[0]).args().length + 1 == cmd.nArgs) {
//                        nothing
                    } else {
                        if(cmd.cmd instanceof ParamCommand && (cmd.mArgs == null || cmd.mArgs.length == 0 || cmd.mArgs[0] instanceof String))
                            suggestParams(pack, suggestionList, (ParamCommand) cmd.cmd, beforeLastSpace, null);
                        else suggestArgs(pack, cmd.nextArg(), suggestionList, beforeLastSpace );
                    }

                } else {
                    String[] split = rmQuotes.matcher(beforeLastSpace).replaceAll(Tuils.EMPTYSTRING).split(Tuils.SPACE);
                    boolean isShellCmd = false;
                    for(String s : split) {
                        if(needsFileSuggestion(s)) {
                            isShellCmd = true;
                            break;
                        }
                    }

                    if(isShellCmd) {
                        suggestFile(pack, suggestionList, Tuils.EMPTYSTRING, beforeLastSpace );
                    } else {
//                        ==> app
                        if(!suggestAppInsideGroup(pack, suggestionList, Tuils.EMPTYSTRING, beforeLastSpace , false)) suggestApp(pack, suggestionList, beforeLastSpace  + Tuils.SPACE, Tuils.EMPTYSTRING);
                    }

                }
            }
        }

//        lastWord > 0
        else {
            comparator.noInput = false;

            if (beforeLastSpace .length() > 0) {
//                lastword > 0 && beforeLastSpace  > 0
                Command cmd = null;
                try {
                    cmd = CommandTuils.parse(beforeLastSpace , pack);
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
                        suggestParams(pack, suggestionList, (ParamCommand) cmd.cmd, beforeLastSpace , lastWord);
                    } else suggestArgs(pack, cmd.nextArg(), suggestionList, lastWord, beforeLastSpace );
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
                        suggestFile(pack, suggestionList, lastWord, beforeLastSpace );
                    } else {
                        if(!suggestAppInsideGroup(pack, suggestionList, lastWord, beforeLastSpace , false)) suggestApp(pack, suggestionList, beforeLastSpace  + Tuils.SPACE + lastWord, Tuils.EMPTYSTRING);
                    }
                }

//                lastword > 0 && beforeLastSpace  = 0
            } else {
                suggestCommand(pack, suggestionList, lastWord, beforeLastSpace );
                suggestAlias(pack.aliasManager, suggestionList, lastWord);
                suggestApp(pack, suggestionList, lastWord, Tuils.EMPTYSTRING);
                suggestAppGroup(pack, suggestionList, lastWord, beforeLastSpace );
            }
        }

        Collections.sort(suggestionList, comparator);
        return suggestionList;
    }

    private boolean needsFileSuggestion(String cmd) {
        return cmd.equalsIgnoreCase("ls") || cmd.equalsIgnoreCase("cd") || cmd.equalsIgnoreCase("mv") || cmd.equalsIgnoreCase("cp") || cmd.equalsIgnoreCase("rm") || cmd.equalsIgnoreCase("cat");
    }

    private void suggestPermanentSuggestions(List<Suggestion> suggestions, PermanentSuggestionCommand cmd) {
        for(String s : cmd.permanentSuggestions()) {
            Suggestion sugg = new Suggestion(null, s, false, Suggestion.TYPE_PERMANENT);
            suggestions.add(sugg);
        }
    }

    private void suggestAlias(AliasManager aliasManager, List<Suggestion> suggestions, String lastWord) {
        int canInsert = lastWord == null || lastWord.length() == 0 ? noInputCounts[Suggestion.TYPE_ALIAS] : counts[Suggestion.TYPE_ALIAS];

        for(AliasManager.Alias a : aliasManager.getAliases(true)) {
            if (lastWord.length() == 0 || a.name.startsWith(lastWord)) {
                if (canInsert == 0) return;
                canInsert--;

                suggestions.add(new Suggestion(Tuils.EMPTYSTRING, a.name, clickToLaunch && !a.isParametrized, Suggestion.TYPE_ALIAS));
            }
        }
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

                suggestions.add(new Suggestion(beforeLastSpace , s, p.args().length == 0 && clickToLaunch, 0));
            }
        } else {
            for (String s : cmd.params()) {
                Param p = cmd.getParam(pack, s).getValue();
                if(p == null) continue;

                if (s.startsWith(lastWord) || s.replace("-", Tuils.EMPTYSTRING).startsWith(lastWord)) {
                    suggestions.add(new Suggestion(beforeLastSpace , s, p.args().length == 0 && clickToLaunch, 0));
                }
            }
        }
    }

    private void suggestArgs(MainPack info, int type, List<Suggestion> suggestions, String afterLastSpace, String beforeLastSpace ) {
        switch (type) {
            case CommandAbstraction.FILE:
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
                suggestAppGroup(info, suggestions, afterLastSpace, beforeLastSpace );
                break;
            case CommandAbstraction.APP_INSIDE_GROUP:
                suggestAppInsideGroup(info, suggestions, afterLastSpace, beforeLastSpace , true);
                break;
            case CommandAbstraction.BOUND_REPLY_APP:
                suggestBoundReplyApp(suggestions, afterLastSpace, beforeLastSpace);
                break;
            case CommandAbstraction.DATASTORE_PATH_TYPE:
                suggestDataStoreType(suggestions, beforeLastSpace);
                break;
        }
    }

    private void suggestArgs(MainPack info, int type, List<Suggestion> suggestions, String beforeLastSpace ) {
        suggestArgs(info, type, suggestions, null, beforeLastSpace );
    }

    private void suggestBoolean(List<Suggestion> suggestions, String beforeLastSpace ) {
        suggestions.add(new Suggestion(beforeLastSpace , "true", clickToLaunch, Suggestion.TYPE_BOOLEAN));
        suggestions.add(new Suggestion(beforeLastSpace , "false", clickToLaunch, Suggestion.TYPE_BOOLEAN));
    }

    private void suggestFile(MainPack info, List<Suggestion> suggestions, String afterLastSpace, String beforeLastSpace) {
        boolean noAfterLastSpace = afterLastSpace == null || afterLastSpace.length() == 0;
        boolean afterLastSpaceNotEndsWithSeparator = noAfterLastSpace || !afterLastSpace.endsWith(File.separator);

        if(noAfterLastSpace || afterLastSpaceNotEndsWithSeparator) {
            suggestions.add(new Suggestion(beforeLastSpace, File.separator, false, Suggestion.TYPE_FILE, afterLastSpace));
        }

        if(Suggestion.appendQuotesBeforeFile && !noAfterLastSpace && !afterLastSpace.endsWith(SINGLE_QUOTE) && !afterLastSpace.endsWith(DOUBLE_QUOTES))
            suggestions.add(new Suggestion(beforeLastSpace, SINGLE_QUOTE, false, Suggestion.TYPE_FILE, afterLastSpace));

        if (noAfterLastSpace) {
            suggestFilesInDir(null, suggestions, info.currentDirectory, beforeLastSpace);
            return;
        }

        if (!afterLastSpace.contains(File.separator)) {
            suggestFilesInDir(suggestions, info.currentDirectory, afterLastSpace, beforeLastSpace, null);
        } else {

//            if it's ../../
            if (!afterLastSpaceNotEndsWithSeparator) {
                String total = beforeLastSpace + Tuils.SPACE + afterLastSpace;
                int quotesCount = total.length() - total.replace(DOUBLE_QUOTES, Tuils.EMPTYSTRING).replace(SINGLE_QUOTE, Tuils.EMPTYSTRING).length();

                if(quotesCount > 0) {
                    int singleQIndex = total.lastIndexOf(SINGLE_QUOTE);
                    int doubleQIndex = total.lastIndexOf(DOUBLE_QUOTES);

                    int lastQuote = Math.max(singleQIndex, doubleQIndex);

                    String file = total.substring(lastQuote + Math.abs(quotesCount % 2 - 2));
                    FileManager.DirInfo dirInfo = FileManager.cd(info.currentDirectory, file);
                    suggestFilesInDir(afterLastSpace, suggestions, dirInfo.file, beforeLastSpace);

                } else {
//                    removes the /
                    afterLastSpace = afterLastSpace.substring(0, afterLastSpace.length() - 1);
                    FileManager.DirInfo dirInfo = FileManager.cd(info.currentDirectory, afterLastSpace);
                    suggestFilesInDir(afterLastSpace + File.separator, suggestions, dirInfo.file, beforeLastSpace);
                }
            }
//            if it's ../..
            else {
                String originalAfterLastSpace = afterLastSpace;
                afterLastSpace = rmQuotes.matcher(afterLastSpace).replaceAll(Tuils.EMPTYSTRING);

                int index = afterLastSpace.lastIndexOf(File.separator);
                FileManager.DirInfo dirInfo = FileManager.cd(info.currentDirectory, afterLastSpace.substring(0,index));

                int originalIndex = originalAfterLastSpace.lastIndexOf(File.separator);

                String alsals = originalAfterLastSpace.substring(0, originalIndex + 1);
                String als = originalAfterLastSpace.substring(originalIndex + 1);
//                beforeLastSpace  = beforeLastSpace + Tuils.SPACE + hold;

                suggestFilesInDir(suggestions, dirInfo.file, als, beforeLastSpace, alsals);
            }
        }
    }

    private void suggestFilesInDir(List<Suggestion> suggestions, File dir, String afterLastSeparator, String beforeLastSpace, String afterLastSpaceWithoutALS) {
        if (dir == null || !dir.isDirectory()) return;

        if (afterLastSeparator == null || afterLastSeparator.length() == 0) {
            suggestFilesInDir(null, suggestions, dir, beforeLastSpace);
            return;
        }

        String[] files = dir.list();
        if(files == null) {
            return;
        }

//        Tuils.log("bls", beforeLastSpace);
//        Tuils.log("als", afterLastSeparator);
//        Tuils.log("alsals", afterLastSpaceWithoutALS);

        String temp = rmQuotes.matcher(afterLastSeparator).replaceAll(Tuils.EMPTYSTRING);

        int counter = quickCompare(temp, files, suggestions, beforeLastSpace, suggestionsPerCategory, false, Suggestion.TYPE_FILE, false);
        if(suggestionsPerCategory - counter <= 0) return;

        String[] fs = CompareStrings.topMatchesWithDeadline(temp, files, suggestionsPerCategory - counter, suggestionsDeadline, FILE_SPLITTERS, algInstance, alg);
        for(String f : fs) {
            suggestions.add(new Suggestion(beforeLastSpace, f, false, Suggestion.TYPE_FILE, afterLastSpaceWithoutALS));
        }
    }

    private int quickCompare(String s1, String[] ss, List<Suggestion> suggestions, String beforeLastSpace, int max, boolean exec, int type, Object tag) {
        if(s1.length() > quickCompare) return 0;

        int counter = 0;

        for(int c = 0; c < ss.length; c++) {
            if(counter >= max) break;

            if(s1.length() <= quickCompare && ss[c].toLowerCase().startsWith(s1)) {
                suggestions.add(new Suggestion(beforeLastSpace, ss[c], exec, type, tag instanceof Boolean ? ((boolean) tag ? ss[c] : null) : tag));

                ss[c] = Tuils.EMPTYSTRING;

                counter++;
            }
        }

        return counter;
    }

    private int quickCompare(String s1, List<? extends StringableObject> ss, List<Suggestion> suggestions, String beforeLastSpace, int max, boolean exec, int type, Object tag) {
        if(s1.length() > quickCompare) return 0;

        int counter = 0;

        Iterator<? extends StringableObject> it = ss.iterator();

        while(it.hasNext()) {
            if(counter >= max) break;

            StringableObject o = it.next();

            if(s1.length() <= quickCompare && o.getLowercaseString().startsWith(s1)) {
                suggestions.add(new Suggestion(beforeLastSpace, o.getString(), exec, type, tag instanceof Boolean ? ((boolean) tag ? o : null) : tag));

                it.remove();

                counter++;
            }
        }

        return counter;
    }

    private void suggestFilesInDir(String afterLastSpaceHolder, List<Suggestion> suggestions, File dir, String beforeLastSpace) {
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
                suggestions.add(new Suggestion(beforeLastSpace , s, false, Suggestion.TYPE_FILE, afterLastSpaceHolder));
            }
        } catch (NullPointerException e) {
            Tuils.log(e);
        }
    }

    private void suggestContact(MainPack info, List<Suggestion> suggestions, String afterLastSpace, String beforeLastSpace ) {
        List<ContactManager.Contact> contacts = info.contacts.getContacts();
        if(contacts == null || contacts.size() == 0) return;

        if (afterLastSpace == null || afterLastSpace.length() == 0) {
            for (ContactManager.Contact contact : contacts) suggestions.add(new Suggestion(beforeLastSpace , contact.name, true, Suggestion.TYPE_CONTACT, contact));
        } else {
            int counter = quickCompare(afterLastSpace, contacts, suggestions, beforeLastSpace, suggestionsPerCategory, true, Suggestion.TYPE_CONTACT, true);
            if(suggestionsPerCategory - counter <= 0) return;

            ContactManager.Contact[] cts = CompareObjects.topMatchesWithDeadline(ContactManager.Contact.class, afterLastSpace, contacts.size(), contacts, suggestionsPerCategory - counter, suggestionsDeadline, SPLITTERS, algInstance, alg);
            for(ContactManager.Contact c : cts) {
                if(c == null) break;
                suggestions.add(new Suggestion(beforeLastSpace , c.name, clickToLaunch, Suggestion.TYPE_CONTACT, c));
            }
        }
    }

    private void suggestDataStoreType(List<Suggestion> suggestions, String beforeLastSpace) {
        suggestions.add(new Suggestion(beforeLastSpace, "json", false, Suggestion.TYPE_BOOLEAN));
        suggestions.add(new Suggestion(beforeLastSpace, "xpath", false, Suggestion.TYPE_BOOLEAN));
        suggestions.add(new Suggestion(beforeLastSpace, "format", false, Suggestion.TYPE_BOOLEAN));
    }

    private void suggestSong(MainPack info, List<Suggestion> suggestions, String afterLastSpace, String beforeLastSpace ) {
        if(info.player == null) return;

        List<Song> songs = info.player.getSongs();
        if(songs == null || songs.size() == 0) return;

        if (afterLastSpace == null || afterLastSpace.length() == 0) {
            for (Song s : songs) {
                suggestions.add(new Suggestion(beforeLastSpace , s.getTitle(), clickToLaunch, Suggestion.TYPE_SONG));
            }
        }
        else {
            int counter = quickCompare(afterLastSpace, songs, suggestions, beforeLastSpace, suggestionsPerCategory, clickToLaunch, Suggestion.TYPE_SONG, false);
            if(suggestionsPerCategory - counter <= 0) return;

            Song[] ss = CompareObjects.topMatchesWithDeadline(Song.class, afterLastSpace, songs.size(), songs, suggestionsPerCategory - counter, suggestionsDeadline, SPLITTERS, algInstance, alg);
            for(Song s : ss) {
                if(s == null) break;
                suggestions.add(new Suggestion(beforeLastSpace , s.getTitle(), clickToLaunch, Suggestion.TYPE_SONG));
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

            int canInsert = counts[Suggestion.TYPE_COMMAND];
            for (String s : cmds) {
                if(canInsert == 0 || Thread.currentThread().isInterrupted()) return;

                if(s.startsWith(afterLastSpace)) {
                    CommandAbstraction cmd = info.commandGroup.getCommandByName(s);
                    int[] args = cmd.argType();
                    boolean exec = args == null || args.length == 0;
                    suggestions.add(new Suggestion(beforeLastSpace , s, exec && clickToLaunch, Suggestion.TYPE_COMMAND));
                    canInsert--;
                }
            }
        }
    }

    private void suggestCommand(MainPack info, List<Suggestion> suggestions, String beforeLastSpace) {
        CommandAbstraction[] cmds = info.commandGroup.getCommands();
        if(cmds == null) return;

//        if there's a beforelastspace -> help ...
        int canInsert = beforeLastSpace != null && beforeLastSpace.length() > 0 ? Integer.MAX_VALUE : noInputCounts[Suggestion.TYPE_COMMAND];

        for (CommandAbstraction cmd : cmds) {
            if(canInsert == 0 || Thread.currentThread().isInterrupted()) return;

            if (info.cmdPrefs.getPriority(cmd) >= minCmdPriority) {
                int[] args = cmd.argType();
                boolean exec = args == null || args.length == 0;

                suggestions.add(new Suggestion(beforeLastSpace , cmd.getClass().getSimpleName(), exec && clickToLaunch, Suggestion.TYPE_COMMAND));
                canInsert--;
            }
        }
    }

    private void suggestColor(List<Suggestion> suggestions, String afterLastSpace, String beforeLastSpace ) {
        if(afterLastSpace == null || afterLastSpace.length() == 0 || (afterLastSpace.length() == 1 && afterLastSpace.charAt(0) != '#')) {
            suggestions.add(new Suggestion(beforeLastSpace , "#", false, Suggestion.TYPE_COLOR));
        }
    }

    private void suggestApp(MainPack info, List<Suggestion> suggestions, String afterLastSpace, String beforeLastSpace) {
        suggestApp(info.appsManager.shownApps(), suggestions, afterLastSpace, beforeLastSpace, true);
    }

    private void suggestHiddenApp(MainPack info, List<Suggestion> suggestions, String afterLastSpace, String beforeLastSpace) {
        suggestApp(info.appsManager.hiddenApps(), suggestions, afterLastSpace, beforeLastSpace, false);
    }

    private void suggestAllPackages(MainPack info, List<Suggestion> suggestions, String afterLastSpace, String beforeLastSpace ) {
        List<AppsManager.LaunchInfo> apps = new ArrayList<>(info.appsManager.shownApps());
        apps.addAll(info.appsManager.hiddenApps());
        suggestApp(apps, suggestions, afterLastSpace, beforeLastSpace, true);
    }

    private void suggestDefaultApp(MainPack info, List<Suggestion> suggestions, String afterLastSpace, String beforeLastSpace ) {
        suggestions.add(new Suggestion(beforeLastSpace , "most_used", false, Suggestion.TYPE_PERMANENT));
        suggestions.add(new Suggestion(beforeLastSpace , "null", false, Suggestion.TYPE_PERMANENT));

        suggestApp(info.appsManager.shownApps(), suggestions, afterLastSpace, beforeLastSpace, true);
    }

    private void suggestApp(List<AppsManager.LaunchInfo> apps, List<Suggestion> suggestions, String afterLastSpace, String beforeLastSpace, boolean canClickToLaunch) {
        if(apps == null || apps.size() == 0) return;

        apps = new ArrayList<>(apps);

        int canInsert = counts[Suggestion.TYPE_APP];
        if (afterLastSpace == null || afterLastSpace.length() == 0) {
            for (AppsManager.LaunchInfo l : apps) {
                if(canInsert == 0) return;
                canInsert--;

                suggestions.add(new Suggestion(beforeLastSpace , l.publicLabel, canClickToLaunch && clickToLaunch, Suggestion.TYPE_APP, l));
            }
        } else {
            int counter = quickCompare(afterLastSpace, apps, suggestions, beforeLastSpace, canInsert, canClickToLaunch && clickToLaunch, Suggestion.TYPE_APP, canClickToLaunch && clickToLaunch);
            if(canInsert - counter <= 0) return;

            AppsManager.LaunchInfo[] infos = CompareObjects.topMatchesWithDeadline(AppsManager.LaunchInfo.class, afterLastSpace, apps.size(), apps, canInsert - counter, suggestionsDeadline, SPLITTERS, algInstance, alg);
            for(AppsManager.LaunchInfo i : infos) {
                if(i == null) break;

                if(canInsert == 0) return;
                canInsert--;

                suggestions.add(new Suggestion(beforeLastSpace , i.publicLabel, canClickToLaunch && clickToLaunch, Suggestion.TYPE_APP, canClickToLaunch && clickToLaunch ? i : null));
            }
        }
    }

    private void suggestConfigEntry(List<Suggestion> suggestions, String afterLastSpace, String beforeLastSpace ) {
        if(xmlPrefsEntrys == null) {
            xmlPrefsEntrys = new ArrayList<>();

            for(XMLPrefsManager.XMLPrefsRoot element : XMLPrefsManager.XMLPrefsRoot.values()) xmlPrefsEntrys.addAll(element.enums);

            Collections.addAll(xmlPrefsEntrys, Apps.values());
            Collections.addAll(xmlPrefsEntrys, Notifications.values());
            Collections.addAll(xmlPrefsEntrys, Rss.values());
            Collections.addAll(xmlPrefsEntrys, Reply.values());
        }

        List<XMLPrefsSave> list = new ArrayList<>(xmlPrefsEntrys);

        if(afterLastSpace == null || afterLastSpace.length() == 0) {
            for(XMLPrefsSave s : list) {
                Suggestion sg = new Suggestion(beforeLastSpace , s.label(), false, Suggestion.TYPE_COMMAND);
                suggestions.add(sg);
            }
        }
        else {
            int counter = quickCompare(afterLastSpace, list, suggestions, beforeLastSpace, suggestionsPerCategory, false, Suggestion.TYPE_COMMAND, false);
            if(suggestionsPerCategory - counter <= 0) return;

            XMLPrefsSave[] saves = CompareObjects.topMatchesWithDeadline(XMLPrefsSave.class, afterLastSpace, list.size(), list, suggestionsPerCategory - counter, suggestionsDeadline, XML_PREFS_SPLITTERS, algInstance, alg);
            for (XMLPrefsSave s : saves) {
                suggestions.add(new Suggestion(beforeLastSpace , s.label(), false, Suggestion.TYPE_COMMAND));
            }
        }
    }

    private void suggestConfigFile(List<Suggestion> suggestions, String afterLastSpace, String beforeLastSpace ) {
        if(xmlPrefsFiles == null) {
            xmlPrefsFiles = new ArrayList<>();
            for(XMLPrefsManager.XMLPrefsRoot element : XMLPrefsManager.XMLPrefsRoot.values())
                xmlPrefsFiles.add(element.path);
            xmlPrefsFiles.add(AppsManager.PATH);
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) xmlPrefsFiles.add(ReplyManager.PATH);
            xmlPrefsFiles.add(NotificationManager.PATH);
            xmlPrefsFiles.add(RssManager.PATH);
        }

        if(afterLastSpace == null || afterLastSpace.length() == 0) {
            for(String s : xmlPrefsFiles) {
                Suggestion sg = new Suggestion(beforeLastSpace , s, false, Suggestion.TYPE_CONFIGFILE, afterLastSpace);
                suggestions.add(sg);
            }
        } else if(afterLastSpace.length() <= FIRST_INTERVAL) {
            afterLastSpace = afterLastSpace.trim().toLowerCase();
            for (String s : xmlPrefsFiles) {
                if(Thread.currentThread().isInterrupted()) return;

                if(s.startsWith(afterLastSpace)) {
                    suggestions.add(new Suggestion(beforeLastSpace , s, false, Suggestion.TYPE_CONFIGFILE, afterLastSpace));
                }
            }
        }
    }

    private void suggestAppGroup(MainPack pack, List<Suggestion> suggestions, String afterLastSpace, String beforeLastSpace ) {
        List<AppsManager.Group> groups = new ArrayList<>(pack.appsManager.groups);
        if(groups.size() == 0) return;

        int canInsert;
        if(afterLastSpace == null || afterLastSpace.length() == 0) {
            canInsert = noInputCounts[Suggestion.TYPE_APPGP];
            for(AppsManager.Group g : groups) {
                if(canInsert == 0) return;
                canInsert--;

                Suggestion sg = new Suggestion(beforeLastSpace , g.name(), false, Suggestion.TYPE_APPGP, g);
                suggestions.add(sg);
            }
        }
        else {
            canInsert = counts[Suggestion.TYPE_APPGP];

            int counter = quickCompare(afterLastSpace, groups, suggestions, beforeLastSpace, canInsert, false, Suggestion.TYPE_APPGP, true);
            if(canInsert - counter <= 0) return;

            AppsManager.Group[] gps = CompareObjects.topMatchesWithDeadline(AppsManager.Group.class, afterLastSpace, groups.size(), groups, canInsert, suggestionsDeadline, SPLITTERS, algInstance, alg);
            for(AppsManager.Group g : gps) {
                if(g == null) break;
                suggestions.add(new Suggestion(beforeLastSpace , g.name(), false, Suggestion.TYPE_APPGP, g));
            }
        }
    }

    private boolean suggestAppInsideGroup(MainPack pack, List<Suggestion> suggestions, String afterLastSpace, String beforeLastSpace, boolean keepGroupName) {
        int index = -1;

        String app = Tuils.EMPTYSTRING;

        if(!beforeLastSpace.contains(Tuils.SPACE)) {
            index = Tuils.find(beforeLastSpace , pack.appsManager.groups);
            app = afterLastSpace;
            if(!keepGroupName) beforeLastSpace  = Tuils.EMPTYSTRING;
        } else {
            String[] split = beforeLastSpace.split(Tuils.SPACE);
            for(int count = 0; count < split.length; count++) {
                index = Tuils.find(split[count], pack.appsManager.groups);
                if(index != -1) {

                    beforeLastSpace = Tuils.EMPTYSTRING;
                    for(int i = 0; (keepGroupName ? i <= count : i < count); i++) {
                        beforeLastSpace = beforeLastSpace + split[i] + Tuils.SPACE;
                    }
                    beforeLastSpace = beforeLastSpace.trim();

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

        AppsManager.Group g = pack.appsManager.groups.get(index);

        List<AppsManager.Group.GroupLaunchInfo> apps = new ArrayList<>((List<AppsManager.Group.GroupLaunchInfo>) g.members());
        if(apps.size() > 0) {
            if (app == null || app.length() == 0) {
                for (AppsManager.Group.GroupLaunchInfo o : apps) {
                    suggestions.add(new Suggestion(beforeLastSpace , o.publicLabel, clickToLaunch, Suggestion.TYPE_APP, o));
                }
            }
            else {
                int counter = quickCompare(app, apps, suggestions, beforeLastSpace, Integer.MAX_VALUE, clickToLaunch, Suggestion.TYPE_APP, true);
                if(counter == apps.size()) return true;

                AppsManager.Group.GroupLaunchInfo[] infos = CompareObjects.topMatchesWithDeadline(AppsManager.Group.GroupLaunchInfo.class, app, apps.size(), apps, apps.size(), suggestionsDeadline, SPLITTERS, algInstance, alg);
                for(AppsManager.Group.GroupLaunchInfo gli : infos) {
                    if(gli == null) break;
                    suggestions.add(new Suggestion(beforeLastSpace, gli.publicLabel, clickToLaunch, Suggestion.TYPE_APP, gli));
                }
            }
        }

        return true;
    }

    private boolean suggestBoundReplyApp(List<Suggestion> suggestions, String afterLastSpace, String beforeLastSpace) {
        List<BoundApp> apps = new ArrayList<>(ReplyManager.boundApps);
        if(apps.size() == 0) return false;

        if (afterLastSpace == null || afterLastSpace.length() == 0) {
            for (BoundApp b : apps) {
                suggestions.add(new Suggestion(beforeLastSpace, b.label, false, Suggestion.TYPE_APP));
            }
        }
        else {
            int counter = quickCompare(afterLastSpace, apps, suggestions, beforeLastSpace, suggestionsPerCategory, false, Suggestion.TYPE_APP, false);
            if(suggestionsPerCategory - counter <= 0) return true;

            BoundApp[] b = CompareObjects.topMatchesWithDeadline(BoundApp.class, afterLastSpace, apps.size(), apps, suggestionsPerCategory - counter, suggestionsDeadline, SPLITTERS, algInstance, alg);
            for(BoundApp ba : b) {
                if(ba == null) break;
                suggestions.add(new Suggestion(beforeLastSpace, ba.label, false, Suggestion.TYPE_APP));
            }
        }

        return true;
    }

    public static class Suggestion {

//        these suggestions will appear together
        public static final int TYPE_APP = 0;
        public static final int TYPE_ALIAS = 1;
        public static final int TYPE_COMMAND = 2;
        public static final int TYPE_APPGP = 3;

//        these suggestions will appear only in some special moments, ALONE
        public static final int TYPE_FILE = 10;
        public static final int TYPE_BOOLEAN = 11;
        public static final int TYPE_SONG = 12;
        public static final int TYPE_CONTACT = 13;
        public static final int TYPE_COLOR = 14;
        public static final int TYPE_PERMANENT = 15;
        public static final int TYPE_CONFIGFILE = 16;

        public String text, textBefore;

        public boolean exec;
        public int type;

        public Object object;

        public static boolean appendQuotesBeforeFile;

        public Suggestion(String beforeLastSpace, String text, boolean exec, int type) {
            this.textBefore = beforeLastSpace;
            this.text = text;

            this.exec = exec;
            this.type = type;

            this.object = null;
        }

        public Suggestion(String beforeLastSpace, String text, boolean exec, int type, Object tag) {
            this.textBefore = beforeLastSpace;
            this.text = text;

            this.exec = exec;
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
            } else if(type == Suggestion.TYPE_FILE) {
                String lastWord = object == null ? null : (String) object;
                if(lastWord == null) {
                    lastWord = Tuils.EMPTYSTRING;
                }

                boolean textIsSpecial = (text.equals(File.separator) || text.equals(DOUBLE_QUOTES) || text.equals(SINGLE_QUOTE));
                boolean appendLastWord = lastWord.endsWith(File.separator) || textIsSpecial;

//                Tuils.log("-------------");
//                Tuils.log("tspe", textIsSpecial);
//                Tuils.log("tbe", textBefore.replaceAll(" ", "#"));
//                Tuils.log("lw", lastWord);
//                Tuils.log("txt", text);

                return textBefore +
                        Tuils.SPACE +
                        (appendLastWord ? lastWord : Tuils.EMPTYSTRING) +
                        (appendQuotesBeforeFile && !appendLastWord ? SINGLE_QUOTE : Tuils.EMPTYSTRING) +
                        text;
            }

            if(textBefore == null || textBefore.length() == 0) {
                return text;
            } else {
                return textBefore + Tuils.SPACE + text;
            }
        }

        @Override
        public String toString() {
            return text;
        }
    }

    private class CustomComparator implements Comparator<Suggestion>  {

        public boolean noInput;

        public int[] noInputIndexes, inputIndexes;

        public CustomComparator(int[] noInputIndexes, int[] inputIndexes) {
            this.noInputIndexes = noInputIndexes;
            this.inputIndexes = inputIndexes;
        }

        @Override
        public int compare(Suggestion o1, Suggestion o2) {
            if(o1.type == o2.type) return 0;

            if(noInput) {
                return noInputIndexes[o1.type] - noInputIndexes[o2.type];
            } else {
                if(o1.type < inputIndexes.length) {
                    if(o2.type < inputIndexes.length) return inputIndexes[o1.type] - inputIndexes[o2.type];
                    else return -1;
                } else {
                    if(o2.type < inputIndexes.length) return 1;
                    else return 0;
                }
            }
        }
    }
}
