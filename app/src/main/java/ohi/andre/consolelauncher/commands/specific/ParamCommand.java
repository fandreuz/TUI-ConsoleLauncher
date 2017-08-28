package ohi.andre.consolelauncher.commands.specific;

import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.commands.CommandAbstraction;
import ohi.andre.consolelauncher.commands.CommandsPreferences;
import ohi.andre.consolelauncher.commands.ExecutePack;
import ohi.andre.consolelauncher.commands.main.MainPack;
import ohi.andre.consolelauncher.commands.main.Param;
import ohi.andre.consolelauncher.tuils.SimpleMutableEntry;
import ohi.andre.consolelauncher.tuils.Tuils;

/**
 * Created by francescoandreuzzi on 01/05/2017.
 */

public abstract class ParamCommand implements CommandAbstraction {

    @Override
    public final int[] argType() {
        return new int[] {CommandAbstraction.PARAM};
    }

    @Override
    public int minArgs() {
        return -1;
    }

    @Override
    public final String exec(ExecutePack pack) throws Exception {
        String o = doThings(pack);
        if(o != null) return o;

        Param param = pack.get(Param.class, 0);
        if(param == null) return pack.context.getString(R.string.output_invalid_param) + Tuils.SPACE + pack.get(String.class, 0);
        return param.exec(pack);
    }

    public SimpleMutableEntry<Boolean, Param> getParam(MainPack pack, String param) {
        Param p = paramForString(pack, param);
        if(p == null && supportDefaultParam()) {
            return new SimpleMutableEntry<>(true, paramForString(pack, defaultParam(pack)));
        }
        return new SimpleMutableEntry<>(false, p);
    }

    public String defaultParam(MainPack pack) {
        String def = pack.cmdPrefs.forCommand(getClass().getSimpleName()).get(CommandsPreferences.DEFAULT_PARAM);
        if(!def.startsWith("-")) def = "-" + def;
        return def;
    }

    @Override
    public String onNotArgEnough(ExecutePack pack, int nArgs) {
        return pack.context.getString(helpRes());
    }

    @Override
    public String onArgNotFound(ExecutePack pack, int indexNotFound) {
        return pack.context.getString(helpRes());
    }

    public abstract String[] params();
    protected abstract Param paramForString(MainPack pack, String param);
    protected abstract String doThings(ExecutePack pack);
    public boolean supportDefaultParam() {
        return false;
    }
}
