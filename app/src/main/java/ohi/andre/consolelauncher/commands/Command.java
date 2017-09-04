package ohi.andre.consolelauncher.commands;

import android.content.res.Resources;

import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.commands.main.Param;
import ohi.andre.consolelauncher.commands.specific.ParamCommand;
import ohi.andre.consolelauncher.tuils.Tuils;

public class Command {

    public CommandAbstraction cmd;
    public Object[] mArgs;
    public int nArgs;

    public int indexNotFound = -1;

    public String exec(Resources resources, ExecutePack info) throws Exception {
        info.set(mArgs);

        if(cmd instanceof ParamCommand) {
            if(indexNotFound == 0) {
                return cmd.onArgNotFound(info, indexNotFound);
            }

            ParamCommand pCmd = (ParamCommand) cmd;

            if(mArgs == null || mArgs.length == 0) {
                return cmd.onNotArgEnough(info, 0);
            }

            int[] args = info.get(Param.class, 0).args();
            if(args == null || !(mArgs[0] instanceof Param)) return resources.getString(R.string.output_invalid_param) + Tuils.SPACE + mArgs[0];

            Param param = (Param) mArgs[0];

            if(indexNotFound != -1) {
                param.onArgNotFound(info, indexNotFound);
            }

            if(pCmd.supportDefaultParam()) {
                if(args.length > nArgs) {
                    return param.onNotArgEnough(info, nArgs);
                }
            } else {
                if(args.length + 1 > nArgs) {
                    return param.onNotArgEnough(info, nArgs);
                }
            }
        } else if(indexNotFound != -1) {
            return cmd.onArgNotFound(info, indexNotFound);
        }
        else if (nArgs < cmd.minArgs() || (mArgs == null && cmd.minArgs() > 0)) {
            return cmd.onNotArgEnough(info, nArgs);
        }

        String output = cmd.exec(info);
        info.clear();
        return output;
    }

    public int nextArg() {
        boolean useParamArgs = cmd instanceof ParamCommand && mArgs != null && mArgs.length >= 1;

        int[] args;
        if (useParamArgs) {
            args = ((Param) mArgs[0]).args();
        } else {
            args = cmd.argType();
        }

        if (args == null || args.length == 0) {
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
