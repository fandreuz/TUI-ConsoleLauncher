package ohi.andre.consolelauncher.commands;

import ohi.andre.consolelauncher.R;

public class Command {

    public static int ARG_NOTFOUND = -1;

    public CommandAbstraction cmd;
    public Object[] mArgs;
    public int nArgs;

    public String exec(ExecInfo info) throws Exception {
        info.set(mArgs);

        if (nArgs < cmd.minArgs() && nArgs != ARG_NOTFOUND)
            return cmd.onNotArgEnough(info, nArgs);
        if (nArgs == Command.ARG_NOTFOUND)
            return info.res.getString(cmd.notFoundRes());
        if (cmd.maxArgs() != CommandAbstraction.UNDEFINIED && nArgs > cmd.maxArgs())
            return info.res.getString(R.string.output_toomanyargs);

        String output = cmd.exec(info);

        info.clear();

        return output;
    }

    public int nextArg() {
        int[] args = cmd.argType();
        if (args == null)
            return 0;

        if (nArgs == -1)
            nArgs = 0;
        try {
            return args[nArgs];
        } catch (ArrayIndexOutOfBoundsException e) {
            nArgs -= 1;
            return nextArg();
        }
    }
}
