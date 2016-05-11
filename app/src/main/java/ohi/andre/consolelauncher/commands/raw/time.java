package ohi.andre.consolelauncher.commands.raw;

import java.util.Calendar;

import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.commands.CommandAbstraction;
import ohi.andre.consolelauncher.commands.ExecInfo;

/**
 * Created by andre on 03/12/15.
 */
public class time implements CommandAbstraction {
    @Override
    public String exec(ExecInfo info) throws Exception {
        Calendar c = Calendar.getInstance();
        int hours = c.get(Calendar.HOUR_OF_DAY);
        int minutes = c.get(Calendar.MINUTE);

        return hours + ":" + minutes;
    }

    @Override
    public int minArgs() {
        return 0;
    }

    @Override
    public int maxArgs() {
        return 0;
    }

    @Override
    public int priority() {
        return 1;
    }

    @Override
    public int[] argType() {
        return null;
    }

    @Override
    public String[] parameters() {
        return null;
    }

    @Override
    public int helpRes() {
        return R.string.help_time;
    }

    @Override
    public int notFoundRes() {
        return 0;
    }

    @Override
    public String onNotArgEnough(ExecInfo info, int nArgs) {
        return null;
    }
}
