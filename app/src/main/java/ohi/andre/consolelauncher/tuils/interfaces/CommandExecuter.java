package ohi.andre.consolelauncher.tuils.interfaces;

/**
 * Created by francescoandreuzzi on 25/01/16.
 */
public interface CommandExecuter {

    String exec(String aliasValue, String aliasName);
    String exec(String input);
    String exec(String input, boolean needWriteInput);
}
