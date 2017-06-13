package ohi.andre.consolelauncher.commands;

import android.content.res.Resources;

import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.commands.specific.ParamCommand;
import ohi.andre.consolelauncher.tuils.Tuils;

public class Command {

    public CommandAbstraction cmd;
    public Object[] mArgs;
    public int nArgs;

    public int indexNotFound = -1;

    public String exec(Resources resources, ExecutePack info) throws Exception {
        info.set(mArgs);

        if (indexNotFound != -1) {
            return cmd.onArgNotFound(info, indexNotFound);
        }
        if (nArgs < cmd.minArgs() || (mArgs == null && cmd.minArgs() > 0) ||
                (cmd instanceof ParamCommand && mArgs != null && mArgs.length > 0 && ((ParamCommand) cmd).argsForParam((String) mArgs[0]) != null &&
                        ((ParamCommand) cmd).argsForParam((String) mArgs[0]).length + 1 > nArgs)) {
            return cmd.onNotArgEnough(info, nArgs);
        }
        if(cmd instanceof ParamCommand && ((ParamCommand) cmd).argsForParam((String) mArgs[0]) == null) {
            return info.context.getString(R.string.output_invalid_param) + Tuils.SPACE + mArgs[0];
        }
        if (cmd.maxArgs() != CommandAbstraction.UNDEFINIED && nArgs > cmd.maxArgs()) {
            return resources.getString(R.string.output_toomanyargs);
        }

        String output = cmd.exec(info);

        info.clear();

        return output;
    }

    public int nextArg() {
        boolean useParamArgs = cmd instanceof ParamCommand && mArgs != null && mArgs.length >= 1;

        int[] args;
        if (useParamArgs) {
            args = ((ParamCommand) cmd).argsForParam((String) mArgs[0]);
        } else {
            args = cmd.argType();
        }

        if (args == null) {
            return 0;
        }

        try {
            return args[useParamArgs ? nArgs - 1 : nArgs];
        } catch (ArrayIndexOutOfBoundsException e) {
            nArgs -= 1;
            return nextArg();
        }
    }
}
