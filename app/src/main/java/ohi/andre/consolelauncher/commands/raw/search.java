package ohi.andre.consolelauncher.commands.raw;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import ohi.andre.comparestring.Compare;
import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.commands.CommandAbstraction;
import ohi.andre.consolelauncher.commands.CommandsPreferences;
import ohi.andre.consolelauncher.commands.ExecInfo;
import ohi.andre.consolelauncher.managers.FileManager;
import ohi.andre.consolelauncher.tuils.Tuils;

public class search implements CommandAbstraction {

    private final String YOUTUBE_PREFIX = "https://www.youtube.com/results?search_query=";
    private final String GOOGLE_PREFIX = "http://www.google.com/#q=";
    private final String PLAYSTORE_PREFIX = "market://search?q=";
    private final String PLAYSTORE_BROWSER_PREFIX = "https://play.google.com/store/search?q=";

    private final String PLAYSTORE_PARAM = "-p";
    private final String FILE_PARAM = "-f";
    private final String GOOGLE_PARAM = "-g";
    private final String YOUTUBE_PARAM = "-y";
    private final String URL_PARAM = "-u";

    private final int MIN_FILE_RATE = 6;

    @Override
    public String exec(ExecInfo info) {
        List<String> args = info.get(ArrayList.class, 1);

        String param = info.get(String.class, 0);
        if(param == null) {
            return info.context.getString(R.string.output_nothing_found);
        }
        if(!param.startsWith("-")) {
            if(args == null) {
                args = new ArrayList<>();
            }
            args.add(0,param);
            param = info.cmdPrefs.forCommand("search").get(CommandsPreferences.DEFAULT_PARAM);
        }

        switch (param) {
            case PLAYSTORE_PARAM:
                return playstore(args, info.context, info.res);
            case YOUTUBE_PARAM:
                return youTube(args, info.context, info.res);
            case FILE_PARAM:
                return file(args, info.currentDirectory, info.res);
            case GOOGLE_PARAM:
                return google(args, info.context, info.res);
            case URL_PARAM:
                return url(args.get(0), info.context, info.res);
            default:
                return info.res.getString(R.string.output_invalid_param) + Tuils.SPACE + param;
        }
    }

    private String google(List<String> args, Context c, Resources res) {
        String toSearch = Tuils.toPlanString(args, "+");

        Uri uri = Uri.parse(GOOGLE_PREFIX + toSearch);
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        c.startActivity(intent);

        return res.getString(R.string.output_searchinggoogle) + Tuils.SPACE + flat(args);
    }

    private String playstore(List<String> args, Context c, Resources res) {
        String toSearch = Tuils.toPlanString(args, "%20");

        try {
            c.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(PLAYSTORE_PREFIX + toSearch)));
        } catch (android.content.ActivityNotFoundException anfe) {
            c.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(PLAYSTORE_BROWSER_PREFIX + toSearch)));
        }

        return res.getString(R.string.output_searchingplaystore) + " " + flat(args);
    }

    private String url(String url, Context c, Resources res) {
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://" + url;
        }

        Uri uri = Uri.parse(url);
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        c.startActivity(intent);

        return res.getString(R.string.output_search_url) + Tuils.SPACE + url;
    }

    private String file(List<String> args, File cd, Resources res) {
        String header = res.getString(R.string.output_search_file) + Tuils.SPACE + cd.getAbsolutePath();

        String name = Tuils.toPlanString(args);
        String found = Tuils.toPlanString(rightPaths(cd, name, FileManager.USE_SCROLL_COMPARE), "\n");

        if (found.length() > 1)
            return header.concat(Tuils.NEWLINE + found);
        else
            return header.concat(Tuils.NEWLINE + res.getString(R.string.output_nothing_found));
    }

    private List<String> rightPaths(File dir, String name, boolean scrollCompare) {
        File[] files = dir.listFiles();
        List<String> rightPaths = new ArrayList<>(files.length);

        boolean check = false;
        for (File file : files) {
            if (fileMatch(file, name, scrollCompare)) {
                if (!check)
                    rightPaths.add(dir.getAbsolutePath());
                check = true;
                rightPaths.add("\n\t" + file.getAbsolutePath());
            }
            if (file.isDirectory())
                rightPaths.addAll(rightPaths(file, name, scrollCompare));
        }

        return rightPaths;
    }

    private boolean fileMatch(File f, String name, boolean scrollCompare) {
        if (scrollCompare)
            return Compare.scrollComparison(f.getName(), name) >= MIN_FILE_RATE;
        else
            return Compare.linearComparison(f.getName(), name) >= MIN_FILE_RATE;
    }

    private String youTube(List<String> args, Context c, Resources res) {
        String toSearch = Tuils.toPlanString(args, "+");
        Uri uri = Uri.parse(YOUTUBE_PREFIX + toSearch);
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        c.startActivity(intent);

        return res.getString(R.string.output_search_youtube) + Tuils.SPACE + flat(args);
    }

    private String flat(List<String> args) {
        String flat = "";
        for (String s : args)
            flat = flat.concat(s + Tuils.SPACE);
        return flat;
    }

    @Override
    public int helpRes() {
        return R.string.help_search;
    }

    @Override
    public int minArgs() {
        return 1;
    }

    @Override
    public int maxArgs() {
        return CommandAbstraction.UNDEFINIED;
    }

    @Override
    public int[] argType() {
        return new int[] {CommandAbstraction.PARAM, CommandAbstraction.TEXTLIST};
    }

    @Override
    public int priority() {
        return 4;
    }

    @Override
    public String[] parameters() {
        return new String[]{
                PLAYSTORE_PARAM,
                FILE_PARAM,
                GOOGLE_PARAM,
                YOUTUBE_PARAM,
                URL_PARAM
        };
    }

    @Override
    public String onNotArgEnough(ExecInfo info, int nArgs) {
        return info.res.getString(helpRes());
    }

    @Override
    public int notFoundRes() {
        return helpRes();
    }

}
