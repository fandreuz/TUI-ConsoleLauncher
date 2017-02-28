package ohi.andre.consolelauncher.commands;

import ohi.andre.consolelauncher.commands.main.MainPack;

public interface CommandAbstraction {

    //	undefinied n of arguments
    int UNDEFINIED = -1;

    //	arg type
    int PLAIN_TEXT = 10;
    int FILE = 11;
    int PACKAGE = 12;
    int CONTACTNUMBER = 13;
    int TEXTLIST = 14;
    int SONG = 15;
    int FILE_LIST = 16;
    int COMMAND = 17;
    int PARAM = 18;
    int BOOLEAN = 19;

    String exec(ExecutePack pack) throws Exception;

    int minArgs();

    int maxArgs();

    int[] argType();

    int priority();

    int helpRes();

    String onArgNotFound(ExecutePack pack);

    String onNotArgEnough(ExecutePack pack, int nArgs);

    String[] parameters();
}
