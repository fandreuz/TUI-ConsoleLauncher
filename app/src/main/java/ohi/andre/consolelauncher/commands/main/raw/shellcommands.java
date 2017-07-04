package ohi.andre.consolelauncher.commands.main.raw;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ohi.andre.comparestring.Compare;
import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.commands.CommandAbstraction;
import ohi.andre.consolelauncher.commands.ExecutePack;
import ohi.andre.consolelauncher.tuils.Tuils;

/**
 * Created by francescoandreuzzi on 19/04/16.
 */
public class shellcommands implements CommandAbstraction {

    @Override
    public String exec(ExecutePack pack) {
        Collection<String> cmds = getOSCommands();
        List<String> commands = new ArrayList<>(cmds);

        Collections.sort(commands, new Comparator<String>() {
            @Override
            public int compare(String lhs, String rhs) {
                return Compare.alphabeticCompare(lhs, rhs);
            }
        });

        Tuils.addPrefix(commands, Tuils.DOUBLE_SPACE);
        Tuils.addSeparator(commands, Tuils.TRIBLE_SPACE);
        Tuils.insertHeaders(commands, true);

        return Tuils.toPlanString(commands, Tuils.EMPTYSTRING);
    }

    private final String[] path = {
            "/system/bin",
            "/system/xbin"
    };

    private Set<String> getOSCommands() {
        Set<String> commands = new HashSet<>();

        for (String s : path) {
            commands.addAll(Arrays.asList(new File(s).list()));
        }

        return commands;
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
    public String onArgNotFound(ExecutePack info, int index) {
        return null;
    }

    @Override
    public String onNotArgEnough(ExecutePack info, int nArgs) {
        return null;
    }
}
