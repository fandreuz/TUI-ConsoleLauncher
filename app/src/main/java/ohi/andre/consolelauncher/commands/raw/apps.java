package ohi.andre.consolelauncher.commands.raw;

import android.app.Activity;
import android.content.SharedPreferences;

import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.commands.CommandAbstraction;
import ohi.andre.consolelauncher.commands.ExecInfo;

public class apps implements CommandAbstraction {

    private final String HIDE_PARAM = "-h";
    private final String UNHIDE_PARAM = "-uh";
    private final String SHOWHIDDEN_PARAM = "-sh";

    @Override
    public String exec(ExecInfo info) {
        String param = info.get(String.class, 0);
        String app = info.get(String.class, 1);
        if (app == null)
            return info.res.getString(helpRes());

        if (param.equals(HIDE_PARAM))
            return hideApp(info, app);
        else if (param.equals(UNHIDE_PARAM))
            return unHideApp(info, app);
        else
            return info.res.getString(helpRes());
    }

    private String hideApp(ExecInfo info, String app) {
        SharedPreferences.Editor editor = ((Activity) info.context).getPreferences(0).edit();
        String result = info.appsManager.hideApp(editor, app);
        if (result != null) {
            editor.commit();
            return result + " " + info.res.getString(R.string.output_hideapp);
        } else
            return info.res.getString(R.string.output_appnotfound);
    }

    private String unHideApp(ExecInfo info, String app) {
        SharedPreferences.Editor editor = ((Activity) info.context).getPreferences(0).edit();
        String result = info.appsManager.unhideApp(editor, app);
        if (result != null) {
            editor.commit();
            return result + " " + info.res.getString(R.string.output_unhideapp);
        } else
            return info.res.getString(R.string.output_appnotfound);
    }

    private String showHiddenApps(ExecInfo info) {
        return info.appsManager.printHiddenApps();
    }

    @Override
    public int helpRes() {
        return R.string.help_apps;
    }

    @Override
    public int minArgs() {
        return 2;
    }

    @Override
    public int maxArgs() {
        return 2;
    }

    @Override
    public int[] argType() {
        return new int[]{CommandAbstraction.PARAM, CommandAbstraction.PACKAGE};
    }

    @Override
    public int priority() {
        return 2;
    }

    @Override
    public String[] parameters() {
        return new String[]{
                HIDE_PARAM,
                UNHIDE_PARAM,
                SHOWHIDDEN_PARAM
        };
    }

    @Override
    public String onNotArgEnough(ExecInfo info, int nArgs) {
        if (nArgs > 0) {
            if (info.get(String.class, 0).equals(SHOWHIDDEN_PARAM))
                return showHiddenApps(info);
            else
                return info.res.getString(helpRes());
        } else
            return info.appsManager.printApps();
    }

    @Override
    public int notFoundRes() {
        return R.string.output_appnotfound;
    }

}
