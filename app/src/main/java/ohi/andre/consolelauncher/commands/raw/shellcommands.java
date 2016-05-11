package ohi.andre.consolelauncher.commands.raw;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import ohi.andre.comparestring.Compare;
import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.commands.CommandAbstraction;
import ohi.andre.consolelauncher.commands.ExecInfo;
import ohi.andre.consolelauncher.tuils.Tuils;

/**
 * Created by francescoandreuzzi on 19/04/16.
 */
public class shellcommands implements CommandAbstraction {

    @Override
    public String exec(ExecInfo info) throws Exception {
        List<String> cmds = Tuils.getOSCommands();

        Collections.sort(cmds, new Comparator<String>() {
            @Override
            public int compare(String lhs, String rhs) {
                return Compare.alphabeticCompare(lhs, rhs);
            }
        });

        Tuils.addPrefix(cmds, Tuils.DOUBLE_SPACE);
        Tuils.addSeparator(cmds, Tuils.TRIBLE_SPACE);
        Tuils.insertHeaders(cmds, true);

        return Tuils.toPlanString(cmds, "");
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
    public int[] argType() {
        return new int[0];
    }

    @Override
    public int priority() {
        return 0;
    }

    @Override
    public int helpRes() {
        return R.string.help_shellcommands;
    }

    @Override
    public int notFoundRes() {
        return 0;
    }

    @Override
    public String onNotArgEnough(ExecInfo info, int nArgs) {
        return null;
    }

    @Override
    public String[] parameters() {
        return new String[0];
    }
}
