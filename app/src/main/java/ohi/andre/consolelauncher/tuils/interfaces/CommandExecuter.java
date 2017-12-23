package ohi.andre.consolelauncher.tuils.interfaces;

/**
 * Created by francescoandreuzzi on 25/01/16.
 */
public interface CommandExecuter {

    void exec(String aliasValue, String aliasName);
    void exec(String input);
    void exec(String input, boolean needWriteInput);
    void exec(String input, Object obj);
}
