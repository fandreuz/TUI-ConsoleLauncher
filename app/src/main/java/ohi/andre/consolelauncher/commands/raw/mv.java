package ohi.andre.consolelauncher.commands.raw;

import java.io.File;
import java.util.ArrayList;

import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.commands.CommandAbstraction;
import ohi.andre.consolelauncher.commands.ExecInfo;
import ohi.andre.consolelauncher.managers.FileManager;

/**
 * Created by andre on 03/12/15.
 */
public class mv implements CommandAbstraction {

    @Override
    public String exec(ExecInfo info) throws Exception {

        ArrayList<File> args = info.get(ArrayList.class, 0);

        File where = args.remove(args.size() - 1);
        File[] files = new File[args.size()];
        args.toArray(files);

        int result = FileManager.mv(files, where, info.getSu());
        if (result == 0)
            return info.res.getString(R.string.output_operationdone);
        if (result == FileManager.ISDIRECTORY)
            return info.res.getString(R.string.output_isdirectory);
        if (result == FileManager.ISFILE)
            return info.res.getString(R.string.output_isfile);
        if (result == FileManager.IOERROR)
            return info.res.getString(R.string.output_ioerror);
        return null;
    }

    @Override
    public int minArgs() {
        return 2;
    }

    @Override
    public int maxArgs() {
        return CommandAbstraction.UNDEFINIED;
    }

    @Override
    public int[] argType() {
        return new int[]{CommandAbstraction.FILE_LIST};
    }

    @Override
    public int priority() {
        return 4;
    }

    @Override
    public String[] parameters() {
        return null;
    }

    @Override
    public int helpRes() {
        return R.string.help_mv;
    }

    @Override
    public int notFoundRes() {
        return R.string.output_filenotfound;
    }

    @Override
    public String onNotArgEnough(ExecInfo info, int nArgs) {
        return info.res.getString(R.string.output_filenotfound);
    }
}
