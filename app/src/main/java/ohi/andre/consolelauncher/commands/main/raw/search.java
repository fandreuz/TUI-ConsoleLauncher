package ohi.andre.consolelauncher.commands.main.raw;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ohi.andre.comparestring.Compare;
import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.commands.CommandAbstraction;
import ohi.andre.consolelauncher.commands.CommandsPreferences;
import ohi.andre.consolelauncher.commands.ExecutePack;
import ohi.andre.consolelauncher.commands.main.MainPack;
import ohi.andre.consolelauncher.managers.FileManager;
import ohi.andre.consolelauncher.tuils.Tuils;
import ohi.andre.consolelauncher.tuils.interfaces.Outputable;

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

    private final int MIN_FILE_RATE = 4;

    @Override
    public String exec(ExecutePack pack) {
        MainPack info = (MainPack) pack;

        List<String> args = info.get(ArrayList.class, 1);
        String param = info.get(String.class, 0);

        switch (param) {
            case PLAYSTORE_PARAM:
                return playstore(args, info.context);
            case YOUTUBE_PARAM:
                return youTube(args, info.context);
            case FILE_PARAM:
                return file(args, info.currentDirectory, info.res, info.outputable);
            case GOOGLE_PARAM:
                return google(args, info.context);
            case URL_PARAM:
                return url(args.get(0), info.context);
            default:
                return info.res.getString(R.string.output_invalid_param) + Tuils.SPACE + param;
        }
    }

    private String google(List<String> args, Context c) {
        String toSearch = Tuils.toPlanString(args, "+");

        Uri uri = Uri.parse(GOOGLE_PREFIX + toSearch);
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        c.startActivity(intent);

        return Tuils.EMPTYSTRING;
    }

    private String playstore(List<String> args, Context c) {
        String toSearch = Tuils.toPlanString(args, "%20");

        try {
            c.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(PLAYSTORE_PREFIX + toSearch)));
        } catch (android.content.ActivityNotFoundException anfe) {
            c.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(PLAYSTORE_BROWSER_PREFIX + toSearch)));
        }

        return Tuils.EMPTYSTRING;
    }
    
    private String url(String url, Context c) {
         if (!url.startsWith("http://") && !url.startsWith("https://")) {
             url = "http://" + url;
         }
 
         Uri uri = Uri.parse(url);
         Intent intent = new Intent(Intent.ACTION_VIEW, uri);
         c.startActivity(intent);
 
         return Tuils.EMPTYSTRING;
     }

    private String file(final List<String> args, final File cd, final Resources res, final Outputable outputable) {
        new Thread() {
            @Override
            public void run() {
                super.run();

                String name = Tuils.toPlanString(args);
                List<String> paths = rightPaths(cd, name, FileManager.USE_SCROLL_COMPARE);
                if(paths.size() == 0) {
                    outputable.onOutput(res.getString(R.string.output_nothing_found));
                } else {
                    outputable.onOutput(Tuils.toPlanString(paths, Tuils.NEWLINE));
                }
            }
        };

        return Tuils.EMPTYSTRING;
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
                rightPaths.add(Tuils.NEWLINE + Tuils.DOUBLE_SPACE + file.getAbsolutePath());
            }
            if (file.isDirectory())
                rightPaths.addAll(rightPaths(file, name, scrollCompare));
        }

        return rightPaths;
    }

    private boolean fileMatch(File f, String name, boolean scrollCompare) {
        if (scrollCompare) {
            return Compare.scrollComparison(f.getName(), name) >= MIN_FILE_RATE;
        } else {
            return Compare.linearComparison(f.getName(), name) >= MIN_FILE_RATE;
        }
    }

    private String youTube(List<String> args, Context c) {
        String toSearch = Tuils.toPlanString(args, "+");
        Uri uri = Uri.parse(YOUTUBE_PREFIX + toSearch);
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        c.startActivity(intent);

        return Tuils.EMPTYSTRING;
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
    public String onNotArgEnough(ExecutePack pack, int nArgs) {
        MainPack info = (MainPack) pack;
        return info.res.getString(R.string.output_nothing_found);
    }

    @Override
    public String onArgNotFound(ExecutePack pack) {
        MainPack info = (MainPack) pack;
        List<String> toSearch = Arrays.asList((String[]) info.args);
        String param = info.cmdPrefs.forCommand(getClass().getSimpleName()).get(CommandsPreferences.DEFAULT_PARAM);

        switch (param) {
            case PLAYSTORE_PARAM:
                return playstore(toSearch, info.context);
            case YOUTUBE_PARAM:
                return youTube(toSearch, info.context);
            case FILE_PARAM:
                return file(toSearch, info.currentDirectory, info.res, info.outputable);
            case GOOGLE_PARAM:
                return google(toSearch, info.context);
            case URL_PARAM:
                return url(toSearch.get(0), info.context);
            default:
                return info.res.getString(R.string.output_invalid_param) + Tuils.SPACE + param;
        }
    }

}
