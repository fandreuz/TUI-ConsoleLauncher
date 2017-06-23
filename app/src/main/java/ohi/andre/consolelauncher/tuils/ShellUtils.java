package ohi.andre.consolelauncher.tuils;

import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;

import ohi.andre.consolelauncher.tuils.interfaces.Outputable;

/**
 * Created by francescoandreuzzi on 24/04/2017.
 */

public class ShellUtils {

    public static class CommandResult {
        public int result;
        public String msg;

        public CommandResult(int exit, String msg) {
            this.result = exit;
            this.msg = msg;
        }

        @Override
        public String toString() {
            return msg;
        }
    }

    public static CommandResult execCommand(String cmd , boolean root, String path) {
        return execCommand(new String[] {cmd}, root, path, null);
    }

    public static CommandResult execCommand(String[] cmd , boolean root, String path) {
        return execCommand(cmd, root, path, null);
    }

    public static CommandResult execCommand(String cmd , boolean root, String path, Outputable outputable) {
        return execCommand(new String[] {cmd}, root, path, outputable);
    }

//    custom dir doesnt work, and also multiple commands
    public static CommandResult execCommand(String[] cmds , boolean root, String path, final Outputable outputable) {
//        try {
//            Process process = Runtime.getRuntime().exec(cmds[0]);
//            process.waitFor();
//
//            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
//
//            StringBuilder log = new StringBuilder();
//            String line;
//            while ((line = bufferedReader.readLine()) != null) {
//                Log.e("andre", "uh");
//                log.append(line + "\n");
//            }
//            process.destroy();
//
//            return new CommandResult(0, log.toString());
//        } catch (Exception e) {
//            return new CommandResult(0, e.toString());
//        }

        if(cmds.length > 1 || cmds.length == 0) return null;

        int result = -1;

        BufferedReader errorResult = null;
        StringBuilder errorMsg = null;
        final StringBuilder output = new StringBuilder();

        try {

            final Process process = Runtime.getRuntime().exec((root ? "su -c " : "") + cmds[0], null, path != null ? new File(path) : null);
            final Thread externalThread = Thread.currentThread();

            Thread readerThread = new StoppableThread() {

                @Override
                public void run() {
                    super.run();

                    if (Thread.interrupted() || externalThread.isInterrupted()) return;

                    BufferedReader successResult = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    String s;
                    try {
                        while ((s = successResult.readLine()) != null) {
                            if (Thread.currentThread().isInterrupted() || externalThread.isInterrupted())
                                return;

                            if (outputable != null) outputable.onOutput(Tuils.NEWLINE + s);
                            else {
                                output.append(Tuils.NEWLINE);
                                output.append(s);
                            }
                        }

                        sleep(25);
                    } catch (StackOverflowError | Exception e) {
                        if(outputable != null && ! (e instanceof InterruptedException)) outputable.onOutput(e.toString());

                        try {
                            successResult.close();
                        } catch (IOException e1) {}

                        return;
                    }
                }
            };
            readerThread.start();

            result = process.waitFor();
            readerThread.interrupt();

            if(output.length() == 0) {
                errorMsg = new StringBuilder();

                errorResult = new BufferedReader(new InputStreamReader(process.getErrorStream()));

                String s;
                while ((s = errorResult.readLine()) != null) {
                    if(errorMsg.length() > 0) errorMsg.append(Tuils.NEWLINE);
                    errorMsg.append(s);
                }
            }

            process.destroy();
        }
        catch (Exception e) {}
        finally {
            try {
                if (errorResult != null) {
                    errorResult.close();
                }
            } catch (IOException e) {}
        }

        if(output.length() > 0) {
            return new CommandResult(result, output.toString());
        }
        else if(errorMsg != null) {
            return new CommandResult(result, errorMsg.toString());
        }
        return null;
    }
}
