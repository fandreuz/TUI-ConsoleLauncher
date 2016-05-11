package ohi.andre.consolelauncher.commands.raw;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import ohi.andre.comparestring.Compare;
import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.commands.CommandAbstraction;
import ohi.andre.consolelauncher.commands.ExecInfo;
import ohi.andre.consolelauncher.tuils.Tuils;

public class help implements CommandAbstraction {

    @Override
    public String exec(ExecInfo info) throws Exception {
        CommandAbstraction cmd = info.get(CommandAbstraction.class, 0);
        int res = cmd == null ? R.string.output_commandnotfound : cmd.helpRes();
        return info.res.getString(res);
    }

    @Override
    public int helpRes() {
        return R.string.help_help;
    }

    @Override
    public int minArgs() {
        return 1;
    }

    @Override
    public int maxArgs() {
        return 1;
    }

    @Override
    public int[] argType() {
        return new int[]{CommandAbstraction.COMMAND};
    }

    @Override
    public int priority() {
        return 5;
    }

    @Override
    public String[] parameters() {
        return null;
    }

    @Override
    public String onNotArgEnough(ExecInfo info, int nArgs) {
        List<String> toPrint = new ArrayList<>(info.commandGroup.getCommands());

        Collections.sort(toPrint, new Comparator<String>() {
            @Override
            public int compare(String lhs, String rhs) {
                return Compare.alphabeticCompare(lhs, rhs);
            }
        });

        Tuils.addPrefix(toPrint, Tuils.DOUBLE_SPACE);
        Tuils.addSeparator(toPrint, Tuils.TRIBLE_SPACE);
        Tuils.insertHeaders(toPrint, true);

        return Tuils.toPlanString(toPrint, "");
    }

    @Override
    public int notFoundRes() {
        return R.string.output_commandnotfound;
    }

}
