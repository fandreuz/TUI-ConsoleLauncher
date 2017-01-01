package ohi.andre.consolelauncher.commands.raw;

import android.annotation.TargetApi;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.provider.Settings;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.widget.Toast;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.commands.CommandAbstraction;
import ohi.andre.consolelauncher.commands.ExecInfo;
import ohi.andre.consolelauncher.tuils.Tuils;

public class data implements CommandAbstraction {

    @Override
    public String exec(ExecInfo info) {
        boolean active = toggle(info);

        return info.res.getString(R.string.output_data) + " " + Boolean.toString(active);
    }

    private boolean toggle(ExecInfo info) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            if (info.connectivityMgr == null) {
                try {
                    init(info);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            boolean mobileConnected;

            if (info.wifi == null)
                info.wifi = (WifiManager) info.context.getSystemService(Context.WIFI_SERVICE);

            if (info.wifi.isWifiEnabled())
                mobileConnected = true;
            else {
                NetworkInfo mobileInfo = info.connectivityMgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
                State state = mobileInfo.getState();
                mobileConnected = state == NetworkInfo.State.CONNECTED || state == NetworkInfo.State.CONNECTING;
            }

            try {
                info.setMobileDataEnabledMethod.invoke(info.connectMgr, !mobileConnected);
            } catch (Exception e) {
                e.printStackTrace();
            }

            return !mobileConnected;
        } else {
            if (!Tuils.verifyRoot()) {
                Toast.makeText(info.context, R.string.output_nofeature, Toast.LENGTH_SHORT).show();
                return false;
            }

            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.LOLLIPOP) {
                try {
                    return toggleDataLollipop(info.context);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    return toggleDataAboveLollipop(info.context);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    private boolean toggleDataLollipop(Context context) throws Exception {
        String command = null;
        int state = isMobileDataEnabledFromLollipop(context) ? 0 : 1;
        String transactionCode = getTransactionCode(context);
        if (transactionCode != null && transactionCode.length() > 0) {
            command = "service call phone " + transactionCode + " i32 " + state;
            return executeCommandViaSu("-c", command);
        }
        return false;
    }

    private static String getTransactionCode(Context context) throws Exception {
        try {
            final TelephonyManager mTelephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            final Class<?> mTelephonyClass = Class.forName(mTelephonyManager.getClass().getName());
            final Method mTelephonyMethod = mTelephonyClass.getDeclaredMethod("getITelephony");
            mTelephonyMethod.setAccessible(true);
            final Object mTelephonyStub = mTelephonyMethod.invoke(mTelephonyManager);
            final Class<?> mTelephonyStubClass = Class.forName(mTelephonyStub.getClass().getName());
            final Class<?> mClass = mTelephonyStubClass.getDeclaringClass();
            final Field field = mClass.getDeclaredField("TRANSACTION_setDataEnabled");
            field.setAccessible(true);
            return String.valueOf(field.getInt(null));
        } catch (Exception e) {
            throw e;
        }
    }

    private static boolean isMobileDataEnabledFromLollipop(Context context) {
        boolean state = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            state = Settings.Global.getInt(context.getContentResolver(), "mobile_data", 0) == 1;
        }
        return state;
    }

    private static boolean executeCommandViaSu(String option, String command) {
        String su = "su";
        for (int i=0; i < 3; i++) {
            if (i == 1) {
                su = "/system/xbin/su";
            } else if (i == 2) {
                su = "/system/bin/su";
            }
            try {
                Runtime.getRuntime().exec(new String[]{su, option, command});
            } catch (Exception e) {
                return false;
            } finally {
                return true;
            }
        }

        return false;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    private boolean toggleDataAboveLollipop(Context context) throws Exception {
        SubscriptionManager mSubscriptionManager = (SubscriptionManager) context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
        String command = null;
        int state = isMobileDataEnabledFromLollipop(context) ? 0 : 1;
        String transactionCode = getTransactionCode(context);
        for (int i = 0; i < mSubscriptionManager.getActiveSubscriptionInfoCountMax(); i++) {
            if (transactionCode != null && transactionCode.length() > 0) {
                int subscriptionId = mSubscriptionManager.getActiveSubscriptionInfoList().get(i).getSubscriptionId();
                command = "service call phone " + transactionCode + " i32 " + subscriptionId + " i32 " + state;
                return executeCommandViaSu("-c", command);
            }
        }
        return false;
    }

    private void init(ExecInfo info) throws Exception {
        info.connectivityMgr = (ConnectivityManager) info.context.getSystemService(Context.CONNECTIVITY_SERVICE);
        Class<?> conmanClass = Class.forName(info.connectivityMgr.getClass().getName());
        Field iConnectivityManagerField = conmanClass.getDeclaredField("mService");
        iConnectivityManagerField.setAccessible(true);
        info.connectMgr = iConnectivityManagerField.get(info.connectivityMgr);
        Class<?> iConnectivityManagerClass = Class.forName(info.connectMgr.getClass().getName());
        info.setMobileDataEnabledMethod = iConnectivityManagerClass
                .getDeclaredMethod("setMobileDataEnabled", Boolean.TYPE);
        info.setMobileDataEnabledMethod.setAccessible(true);
    }

    @Override
    public int helpRes() {
        return R.string.help_data;
    }

    @Override
    public int minArgs() {
        return 0;
    }

    @Override
    public int maxArgs() {
        return 0;
    }

    @Override
    public int[] argType() {
        return null;
    }

    @Override
    public int priority() {
        return 2;
    }

    @Override
    public String[] parameters() {
        return null;
    }

    @Override
    public String onNotArgEnough(ExecInfo info, int nArgs) {
        return null;
    }

    @Override
    public int notFoundRes() {
        return 0;
    }

}
