package ohi.andre.consolelauncher.commands.main.raw;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Build;
import android.provider.Settings;

import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.commands.CommandAbstraction;
import ohi.andre.consolelauncher.commands.ExecutePack;
import ohi.andre.consolelauncher.commands.main.MainPack;
import ohi.andre.consolelauncher.commands.main.specific.ParamCommand;
import ohi.andre.consolelauncher.tuils.Tuils;

/**
 * Created by francescoandreuzzi on 27/03/2018.
 */

public class volume extends ParamCommand {

    private enum Param implements ohi.andre.consolelauncher.commands.main.Param {

        set {
            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.INT, CommandAbstraction.INT};
            }

            @Override
            public String exec(ExecutePack pack) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    NotificationManager mNotificationManager = (NotificationManager) pack.context.getSystemService(Context.NOTIFICATION_SERVICE);
                    if (!mNotificationManager.isNotificationPolicyAccessGranted()) {
                        Intent intent = new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
                        pack.context.startActivity(intent);
                        return pack.context.getString(R.string.output_waitingpermission);
                    }
                }

                int type = pack.getInt();
                int volume = pack.getInt();

                if(volume < 0) volume = 0;
                else if(volume > 100) volume = 100;

                AudioManager manager = (AudioManager) pack.context.getSystemService(Context.AUDIO_SERVICE);
                int maxIndex = manager.getStreamMaxVolume(type);

                volume = volume * maxIndex / 100;

                manager.setStreamVolume(type, volume, 0);

                return null;
            }

            @Override
            public String onArgNotFound(ExecutePack pack, int index) {
                return pack.context.getString(R.string.invalid_integer);
            }
        },
        profile {
            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.INT};
            }

            @Override
            public String exec(ExecutePack pack) {

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    NotificationManager mNotificationManager = (NotificationManager) pack.context.getSystemService(Context.NOTIFICATION_SERVICE);
                    if (!mNotificationManager.isNotificationPolicyAccessGranted()) {
                        Intent intent = new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
                        pack.context.startActivity(intent);
                        return pack.context.getString(R.string.output_waitingpermission);
                    }
                }

                AudioManager manager = (AudioManager) pack.context.getSystemService(Context.AUDIO_SERVICE);
                manager.setRingerMode(pack.getInt());

                return null;
            }

            @Override
            public String onArgNotFound(ExecutePack pack, int index) {
                return pack.context.getString(R.string.invalid_integer);
            }
        },
        get {
            String[] labels = {"Voice call", "System", "Ring", "Media", "Alarm", "Notifications"};

            private void appendInfo(StringBuilder builder, AudioManager manager, int stream) {
                builder.append(labels[stream]).append(":").append(Tuils.SPACE).append(manager.getStreamVolume(stream) * 100 / manager.getStreamMaxVolume(stream)).append("%").append(Tuils.NEWLINE);
            }

            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.INT};
            }

            @Override
            public String exec(ExecutePack pack) {
                AudioManager manager = (AudioManager) pack.context.getSystemService(Context.AUDIO_SERVICE);

                int c = pack.getInt();

                StringBuilder builder = new StringBuilder();
                appendInfo(builder, manager, c);

                return builder.toString().trim();
            }

            @Override
            public String onArgNotFound(ExecutePack pack, int index) {
                return pack.context.getString(R.string.invalid_integer);
            }

            @Override
            public String onNotArgEnough(ExecutePack pack, int n) {
                AudioManager manager = (AudioManager) pack.context.getSystemService(Context.AUDIO_SERVICE);

                StringBuilder builder = new StringBuilder();
                for(int c = 0; c < labels.length; c++) {
                    appendInfo(builder, manager, c);
                }

                return builder.toString().trim();
            }
        };

        static Param get(String p) {
            p = p.toLowerCase();
            Param[] ps = values();
            for (Param p1 : ps) if (p.endsWith(p1.label())) return p1;
            return null;
        }

        static String[] labels() {
            Param[] ps = values();
            String[] ss = new String[ps.length];

            for (int count = 0; count < ps.length; count++) {
                ss[count] = ps[count].label();
            }

            return ss;
        }

        @Override
        public String label() {
            return Tuils.MINUS + name();
        }

        @Override
        public String onNotArgEnough(ExecutePack pack, int n) {
            return pack.context.getString(R.string.help_volume);
        }
    }

    @Override
    public String[] params() {
        return Param.labels();
    }

    @Override
    protected ohi.andre.consolelauncher.commands.main.Param paramForString(MainPack pack, String param) {
        return Param.get(param);
    }

    @Override
    public int priority() {
        return 3;
    }

    @Override
    public int helpRes() {
        return R.string.help_volume;
    }

    @Override
    protected String doThings(ExecutePack pack) {
        return null;
    }
}
