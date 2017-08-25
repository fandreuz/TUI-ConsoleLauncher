package ohi.andre.consolelauncher.commands.main.raw;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import java.util.ArrayList;
import java.util.List;

import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.commands.CommandAbstraction;
import ohi.andre.consolelauncher.commands.ExecutePack;
import ohi.andre.consolelauncher.commands.main.MainPack;
import ohi.andre.consolelauncher.commands.specific.ParamCommand;
import ohi.andre.consolelauncher.tuils.Tuils;

public class search extends ParamCommand {

    private static final String YOUTUBE_PREFIX = "https://www.youtube.com/results?search_query=";
    private static final String GOOGLE_PREFIX = "http://www.google.com/#q=";
    private static final String PLAYSTORE_PREFIX = "market://search?q=";
    private static final String PLAYSTORE_BROWSER_PREFIX = "https://play.google.com/store/search?q=";
    private static final String DUCKDUCKGO_PREFIX = "https://duckduckgo.com/?q=";

    private enum Param implements ohi.andre.consolelauncher.commands.main.Param {

        ps {
            @Override
            public String exec(ExecutePack pack) {
                List<String> args = pack.get(ArrayList.class, 1);
                return playstore(args, pack.context);
            }
        },
//        file {
//            @Override
//            public String exec(ExecutePack pack) {
//                List<String> args = pack.get(ArrayList.class, 1);
//                MainPack p = ((MainPack) pack);
//                return file(args, p.currentDirectory, p.res, p.outputable);
//            }
//        },
        gg {
            @Override
            public String exec(ExecutePack pack) {
                List<String> args = pack.get(ArrayList.class, 1);
                return google(args, pack.context);
            }
        },
        yt {
            @Override
            public String exec(ExecutePack pack) {
                List<String> args = pack.get(ArrayList.class, 1);
                return youTube(args, pack.context);
            }
        },
        u {
            @Override
            public String exec(ExecutePack pack) {
                List<String> args = pack.get(ArrayList.class, 1);
                return url(Tuils.toPlanString(args, Tuils.SPACE), pack.context);
            }
        },
        dd {
            @Override
            public String exec(ExecutePack pack) {
                List<String> args = pack.get(ArrayList.class, 1);
                return duckDuck(args, pack.context);
            }
        };

        @Override
        public int[] args() {
            return new int[] {CommandAbstraction.TEXTLIST};
        }

        static Param get(String p) {
            p = p.toLowerCase();
            Param[] ps = values();
            for (Param p1 : ps)
                if (p.endsWith(p1.label()))
                    return p1;
            return null;
        }

        static String[] labels() {
            Param[] ps = values();
            String[] ss = new String[ps.length];

            for (int count = 0; count < ps.length; count++) {
                ss[count] = ps[count].label();
            }

            return ss;
        }

        @Override
        public String label() {
            return Tuils.MINUS + name();
        }

        @Override
        public String onNotArgEnough(ExecutePack pack, int n) {
            return pack.context.getString(R.string.help_search);
        }

        @Override
        public String onArgNotFound(ExecutePack pack, int index) {
            return null;
        }
    }

    @Override
    protected ohi.andre.consolelauncher.commands.main.Param paramForString(MainPack pack, String param) {
        return Param.get(param);
    }

    @Override
    public boolean supportDefaultParam() {
        return true;
    }

    @Override
    protected String doThings(ExecutePack pack) {
        return null;
    }

    @Override
    public String[] params() {
        return Param.labels();
    }

    private static String google(List<String> args, Context c) {
        String toSearch = Tuils.toPlanString(args, "+");

        Uri uri = Uri.parse(GOOGLE_PREFIX + toSearch);
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        c.startActivity(intent);

        return Tuils.EMPTYSTRING;
    }

    private static String playstore(List<String> args, Context c) {
        String toSearch = Tuils.toPlanString(args, "%20");

        try {
            c.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(PLAYSTORE_PREFIX + toSearch)));
        } catch (android.content.ActivityNotFoundException anfe) {
            c.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(PLAYSTORE_BROWSER_PREFIX + toSearch)));
        }

        return Tuils.EMPTYSTRING;
    }

    private static String url(String url, Context c) {
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://" + url;
        }

        Uri uri = Uri.parse(url);
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        c.startActivity(intent);

        return Tuils.EMPTYSTRING;
    }

    private static String duckDuck(List<String> args, Context c) {
        String toSearch = Tuils.toPlanString(args, "+");

        Uri uri = Uri.parse(DUCKDUCKGO_PREFIX + toSearch);
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        c.startActivity(intent);

        return Tuils.EMPTYSTRING;
    }

//    private static String file(final List<String> args, final File cd, final Resources res, final Outputable outputable) {
//        new Thread() {
//            @Override
//            public void run() {
//                super.run();
//
//                String name = Tuils.toPlanString(args);
//                List<String> paths = rightPaths(cd, name);
//                if(paths.size() == 0) {
//                    outputable.onOutput(res.getString(R.string.output_nothing_found));
//                } else {
//                    outputable.onOutput(Tuils.toPlanString(paths, Tuils.NEWLINE));
//                }
//            }
//        };
//
//        return Tuils.EMPTYSTRING;
//    }
//
//    private static List<String> rightPaths(File dir, String name) {
//        File[] files = dir.listFiles();
//        List<String> rightPaths = new ArrayList<>(files.length);
//
//        boolean check = false;
//        for (File file : files) {
//            if (fileMatch(file, name)) {
//                if (!check)
//                    rightPaths.add(dir.getAbsolutePath());
//                check = true;
//                rightPaths.add(Tuils.NEWLINE + Tuils.DOUBLE_SPACE + file.getAbsolutePath());
//            }
//            if (file.isDirectory())
//                rightPaths.addAll(rightPaths(file, name));
//        }
//
//        return rightPaths;
//    }
//
//    private static boolean fileMatch(File f, String name) {
//        return f.getName().equalsIgnoreCase(name);
//    }

    private static String youTube(List<String> args, Context c) {
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
    public int priority() {
        return 4;
    }
}
