package ohi.andre.consolelauncher;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import ohi.andre.consolelauncher.commands.Command;
import ohi.andre.consolelauncher.commands.CommandGroup;
import ohi.andre.consolelauncher.commands.CommandTuils;
import ohi.andre.consolelauncher.commands.ExecutePack;
import ohi.andre.consolelauncher.commands.main.MainPack;
import ohi.andre.consolelauncher.commands.specific.RedirectCommand;
import ohi.andre.consolelauncher.managers.AliasManager;
import ohi.andre.consolelauncher.managers.AppsManager;
import ohi.andre.consolelauncher.managers.ContactManager;
import ohi.andre.consolelauncher.managers.MusicManager;
import ohi.andre.consolelauncher.managers.XMLPrefsManager;
import ohi.andre.consolelauncher.managers.notifications.NotificationManager;
import ohi.andre.consolelauncher.tuils.ShellUtils;
import ohi.andre.consolelauncher.tuils.StoppableThread;
import ohi.andre.consolelauncher.tuils.Tuils;
import ohi.andre.consolelauncher.tuils.interfaces.CommandExecuter;
import ohi.andre.consolelauncher.tuils.interfaces.Inputable;
import ohi.andre.consolelauncher.tuils.interfaces.OnRedirectionListener;
import ohi.andre.consolelauncher.tuils.interfaces.Outputable;
import ohi.andre.consolelauncher.tuils.interfaces.Redirectator;

/*Copyright Francesco Andreuzzi

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.*/

public class MainManager {

    private RedirectCommand redirect;
    private Redirectator redirectator = new Redirectator() {
        @Override
        public void prepareRedirection(RedirectCommand cmd) {
            redirect = cmd;

            if(redirectionListener != null) {
                redirectionListener.onRedirectionRequest(cmd);
            }
        }

        @Override
        public void cleanup() {
            if(redirect != null) {
                redirect.beforeObjects.clear();
                redirect.afterObjects.clear();

                if(redirectionListener != null) {
                    redirectionListener.onRedirectionEnd(redirect);
                }

                redirect = null;
            }
        }
    };
    private OnRedirectionListener redirectionListener;
    public void setRedirectionListener(OnRedirectionListener redirectionListener) {
        this.redirectionListener = redirectionListener;
    }

    private final String COMMANDS_PKG = "ohi.andre.consolelauncher.commands.main.raw";

    private CmdTrigger[] triggers = new CmdTrigger[]{
            new AliasTrigger(),
            new TuiCommandTrigger(),
            new AppTrigger(),
//            keep this as last trigger
            new SystemCommandTrigger()
    };
    private MainPack mainPack;

    private Context mContext;

    private Inputable in;
    private Outputable out;

    private boolean showAliasValue;
    private boolean showAppHistory;

    private Handler handler = new Handler();

    private Thread thread;
    private boolean busy = false;
    private void busy() {
        busy = true;

//        i do this because i don't want to change the hint every single time (users would complain)
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(busy) in.changeHint(mContext.getString(R.string.busy_hint));
            }
        }, 650);
    }
    private void notBusy() {
        busy = false;
        in.resetHint();
    }

    protected MainManager(LauncherActivity c, Inputable i, Outputable o, DevicePolicyManager devicePolicyManager, ComponentName componentName) {
        mContext = c;

        in = i;
        out = o;

        showAliasValue = XMLPrefsManager.get(boolean.class, XMLPrefsManager.Behavior.show_alias_content);
        showAppHistory = XMLPrefsManager.get(boolean.class, XMLPrefsManager.Behavior.show_launch_history);

        CommandGroup group = new CommandGroup(mContext, COMMANDS_PKG);

        ContactManager cont = null;
        try {
            cont = new ContactManager(mContext);
        } catch (NullPointerException e) {}

        CommandExecuter executer = new CommandExecuter() {
            @Override
            public String exec(String input, String alias) {
                onCommand(input, alias);
                return null;
            }
        };

        MusicManager music = new MusicManager(mContext, out);

        AppsManager appsMgr = new AppsManager(c, out);
        AliasManager aliasManager = new AliasManager();

        mainPack = new MainPack(mContext, group, aliasManager, appsMgr, music, cont, devicePolicyManager, componentName, c, executer, out, redirectator);
    }

//    command manager
    public void onCommand(String input, String alias) {

        if(busy) {
            if(input.equalsIgnoreCase("ctrlc")) {
                thread.interrupt();
                notBusy();
                return;
            }

            out.onOutput(mContext.getString(R.string.busy));
            return;
        }

        input = Tuils.removeUnncesarySpaces(input);

        if(redirect != null) {
            if(!redirect.isWaitingPermission()) {
                redirect.afterObjects.add(input);
            }
            String output = redirect.onRedirect(mainPack);
            out.onOutput(output);

            return;
        }

        if(alias != null && showAliasValue) {
            out.onOutput(alias + " --> " + "[" + input + "]");
        }

        for (CmdTrigger trigger : triggers) {
            boolean r;
            try {
                r = trigger.trigger(mainPack, input);
            } catch (Exception e) {
                out.onOutput(Tuils.getStackTrace(e));
                return;
            }
            if (r) {
                return;
            } else {}
        }
    }

    public void onLongBack() {
        in.in(Tuils.EMPTYSTRING);
    }

    public void sendPermissionNotGrantedWarning() {
        redirectator.cleanup();
    }

    //    dispose
    public void dispose() {
        mainPack.dispose();
    }

    public void destroy() {
        mainPack.destroy();
    }

    public MainPack getMainPack() {
        return mainPack;
    }

    interface CmdTrigger {
        boolean trigger(ExecutePack info, String input) throws Exception;
    }

    private class AliasTrigger implements CmdTrigger {
        @Override
        public boolean trigger(ExecutePack info, String alias) {
            String aliasValue = mainPack.aliasManager.getAlias(alias);
            if (aliasValue == null) {
                return false;
            }

            mainPack.executer.exec(aliasValue, alias);

            return true;
        }
    }

    //    this must be the last trigger
    private class SystemCommandTrigger implements CmdTrigger {

        final int COMMAND_NOTFOUND = 127;

        @Override
        public boolean trigger(final ExecutePack info, final String input) throws Exception {

//            this is the last trigger, it has to say "command not found"

//            boolean su = false;
//            if (CommandTuils.isSuCommand(input)) {
//                su = true;
//                input = input.substring(3);
//                if (input.startsWith(ShellUtils.COMMAND_SU_ADD + Tuils.SPACE)) {
//                    input = input.substring(3);
//                }
//            }

            final String cmd = input;
            final boolean useSU = false;

            thread = new StoppableThread() {
                @Override
                public void run() {
                    super.run();

                    busy();

                    if(Thread.interrupted()) return;
                    ShellUtils.CommandResult result = ShellUtils.execCommand(new String[] {cmd}, useSU, mainPack.currentDirectory.getAbsolutePath(), out);
                    if(Thread.interrupted()) return;

                    if (result == null || result.result == COMMAND_NOTFOUND || result.result == -1) {
                        out.onOutput(mContext.getString(R.string.output_commandnotfound));
                    } else {
                        String output = result.toString();
                        if(output != null) {
                            output = output.trim();
                            if(output.length() == 0 ) {
                                output = mainPack.res.getString(R.string.output_commandexitvalue) + Tuils.SPACE + result.result;
                            }
                        }
                        out.onOutput(output);
                    }

                    notBusy();
                }
            };
            thread.start();

            return true;
        }
    }

    private class AppTrigger implements CmdTrigger {

        @Override
        public boolean trigger(ExecutePack info, String input) {
            String packageName = mainPack.appsManager.findPackage(input, AppsManager.SHOWN_APPS);
            if (packageName == null) {
                return false;
            }

            Intent intent = mainPack.appsManager.getIntent(packageName);
            if (intent == null) {
                return false;
            }

            if(showAppHistory) out.onOutput("-->" + Tuils.SPACE + intent.getComponent().getClassName());

            mContext.startActivity(intent);

            return true;
        }
    }

    private class TuiCommandTrigger implements CmdTrigger {

        @Override
        public boolean trigger(final ExecutePack info, final String input) throws Exception {

            final boolean[] returnValue = new boolean[1];
            thread = new StoppableThread() {
                @Override
                public void run() {
                    super.run();

                    busy();

                    Looper.prepare();

                    mainPack.lastCommand = input;

                    try {
                        Command command = CommandTuils.parse(input, info, false);

                        synchronized (returnValue) {
                            returnValue[0] = command != null;
                            returnValue.notify();
                        }

                        if (returnValue[0]) {
                            if(Thread.interrupted()) return;
                            String output = command.exec(mContext.getResources(), info);
                            if(Thread.interrupted()) return;

                            if(output != null) {
                                out.onOutput(output);
                            }
                        }
                    } catch (Exception e) {
                        out.onOutput(e.toString());
                        Log.e("andre", "", e);
                    }

                    notBusy();
                }
            };

            thread.start();

            synchronized (returnValue) {
                returnValue.wait();
                return returnValue[0];
            }
        }
    }
}
