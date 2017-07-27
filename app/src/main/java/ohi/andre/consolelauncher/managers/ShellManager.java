package ohi.andre.consolelauncher.managers;

import android.os.Environment;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import ohi.andre.consolelauncher.tuils.StoppableThread;
import ohi.andre.consolelauncher.tuils.Tuils;
import ohi.andre.consolelauncher.tuils.interfaces.Outputable;

/**
 * Created by francescoandreuzzi on 25/07/2017.
 */

public class ShellManager {

    Process p;
    Thread t;

    File file;

    InputStream output;
    InputStream error;
    OutputStream cmdStream;

    Outputable outputable;

    public ShellManager(Outputable outputable) {
        this.outputable = outputable;

        init(Environment.getExternalStorageDirectory());
    }

    private void init(File f) {
        try {
            p = Runtime.getRuntime().exec("/system/bin/sh");
        } catch (IOException e) {
            p = null;
            return;
        }

        this.file = f;

        output = p.getInputStream();
        error = p.getErrorStream();
        cmdStream = p.getOutputStream();

        new Thread() {
            @Override
            public void run() {
                super.run();

                cd(file);
            }
        }.start();
    }

    public String cmd(final String cmd, final boolean write) {

        if(t != null && t.isAlive()) {
            try {
                synchronized (t) {
                    t.wait();
                }
            } catch (InterruptedException e) {}
        }

        try {
            cmdStream.write((cmd + Tuils.NEWLINE).getBytes());
            cmdStream.write(("echo EOF" + Tuils.NEWLINE).getBytes());
        } catch (IOException e) {
            return null;
        }

        final String[] outputMsg = {Tuils.EMPTYSTRING};

        t = new StoppableThread() {
            String line = Tuils.EMPTYSTRING;
            String errorMsg = Tuils.EMPTYSTRING;

            @Override
            public void run() {
                super.run();

                do {
                    if(Thread.currentThread().isInterrupted()) return;

                    int n = 0;
                    try {
                        n = output.read();
                    } catch (IOException e) {}

                    if(n == -1) continue;

                    char c = (char) n;
                    if(c == '\n') {
                        if(line.equals("EOF")) break;
                        else {
                            if(write) outputable.onOutput(line);
                            outputMsg[0] = outputMsg[0] + Tuils.EMPTYSTRING + line;
                            line = Tuils.EMPTYSTRING;
                        }
                    } else {
                        line = line + c;
                    }
                } while (true);

                if(write) {
                    try {
                        int available = error.available();
                        for (int i = 0; i < available; i++) errorMsg = errorMsg + (char) error.read();
                    } catch (IOException e) {}

                    if(Thread.currentThread().isInterrupted()) return;

                    outputable.onOutput(errorMsg);
                }


                if(cmd.startsWith("cd ")) {
                    line = Tuils.EMPTYSTRING;

//                    update the current dir
                    try {
                        cmdStream.write(("pwd" + Tuils.NEWLINE).getBytes());
                    } catch (IOException e) {}

                    do {
                        int n = 0;
                        try {
                            n = output.read();
                        } catch (IOException e) {}

                        if(n == -1) continue;
                        char c = (char) n;

                        if(c == '\n') {
                            file = new File(line);
                            break;
                        } else {
                            line = line + c;
                        }
                    } while (true);
                }

                synchronized (outputMsg) {
                    outputMsg.notify();
                }

                synchronized (this) {
                    this.notify();
                }
            }
        };

        t.start();

        try {
            synchronized (outputMsg) {
                outputMsg.wait();
            }
        } catch (InterruptedException e) {}

        return outputMsg[0];
    }

    public void cd(File file) {
        cmd("cd" + Tuils.SPACE + file.getAbsolutePath(), false);
    }

    public File currentDir() {
        return file == null ? Environment.getExternalStorageDirectory() : file;
    }

    public void reset() {

        if(this.p != null) {
            p.destroy();
            p = null;

            try {
                output.close();
                error.close();
                cmdStream.close();
            } catch (IOException e) {}

            output = null;
            error = null;
            cmdStream = null;
        }

        if(this.t != null) {
            t.interrupt();
            t = null;
        }

        init(this.file);
    }

    public void destroy() {
        if(t != null) t.interrupt();
        if(p != null) {
            p.destroy();

            try {
                output.close();
                error.close();
                cmdStream.close();
            } catch (IOException e) {}
        }
    }

//    public boolean sendSigint() {
//
//        if(t != null) {
//            t.interrupt();
//
//            int pid = getPid();
//            if(pid != 0) {
//
//                try {
//                    p.destroy();
//                    Runtime.getRuntime().exec("kill -SIGINT" + Tuils.SPACE + pid);
//                } catch (IOException e) {}
//            }
//
//            try {
//                int av = output.available();
//                for(int i = 0; i < av; i++) {
//                    output.read();
//                }
//            } catch (IOException e) {}
//
//            try {
//                int av = error.available();
//                for(int i = 0; i < av; i++) {
//                    error.read();
//                }
//            } catch (IOException e) {}
//
//            return true;
//        }
//        return false;
//    }
//
//    public int getPid() {
//        String s = p.toString();
//        String sPid = s.replaceAll("Process", Tuils.EMPTYSTRING).replace("[", Tuils.EMPTYSTRING).replace("]", Tuils.EMPTYSTRING).replaceAll("pid=", Tuils.EMPTYSTRING);
//
//        try {
//            return Integer.parseInt(sPid);
//        } catch (Exception e) {
//            return 0;
//        }
//    }
}
