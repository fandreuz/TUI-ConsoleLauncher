package ohi.andre.consolelauncher.commands;

public interface CommandAbstraction {

    //	arg type
    int PLAIN_TEXT = 10;
    int FILE = 11;
    int VISIBLE_PACKAGE = 12;
    int CONTACTNUMBER = 13;
    int TEXTLIST = 14;
    int SONG = 15;
    int COMMAND = 17;
    int PARAM = 18;
    int BOOLEAN = 19;
    int HIDDEN_PACKAGE = 20;
    int COLOR = 21;
    int CONFIG_FILE = 22;
    int CONFIG_ENTRY = 23;
    int INT = 24;
    int DEFAULT_APP = 25;
    int ALL_PACKAGES = 26;
    int NO_SPACE_STRING = 27;
    int APP_GROUP = 28;
    int APP_INSIDE_GROUP = 29;
    int LONG = 30;
    int BOUND_REPLY_APP = 31;
    int DATASTORE_PATH_TYPE = 32;

    String exec(ExecutePack pack) throws Exception;
    int[] argType();
    int priority();
    int helpRes();
    String onArgNotFound(ExecutePack pack, int indexNotFound);
    String onNotArgEnough(ExecutePack pack, int nArgs);
}
