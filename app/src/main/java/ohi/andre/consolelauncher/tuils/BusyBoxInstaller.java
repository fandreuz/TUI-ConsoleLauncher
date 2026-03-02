package ohi.andre.consolelauncher.tuils;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;

public class BusyBoxInstaller {

    private static final String PREFS_NAME = "busybox_prefs";
    private static final String ENABLED_KEY = "busybox_enabled";
    private static final String BUSYBOX_BIN = "busybox.so"; // Using .so suffix as a common workaround

    private static class PkgInfo {
        String url;
        String sha256;
        PkgInfo(String url, String sha256) {
            this.url = url;
            this.sha256 = sha256;
        }
    }

    private static PkgInfo getPkgInfo() {
        for (String abi : Build.SUPPORTED_ABIS) {
            if (abi.contains("arm64") || abi.contains("aarch64")) {
                return new PkgInfo("https://github.com/EXALAB/Busybox-static/raw/refs/heads/main/busybox_arm64", "f3154aad70b2928e3a57be3e4e966d2574904425fd63e717b9aa55933cfaf08c");
            } else if (abi.contains("armeabi") || abi.contains("arm")) {
                return new PkgInfo("https://github.com/EXALAB/Busybox-static/raw/refs/heads/main/busybox_arm", "03ac599984d511f41c1571814a85ad41a92ddd4d21691bff239b32f5e74dc49b");
            } else if (abi.contains("x86_64") || abi.contains("amd64")) {
                return new PkgInfo("https://github.com/EXALAB/Busybox-static/raw/refs/heads/main/busybox_amd64", "559dd743bf2f8841405fb9025cba1079966294ab9bbce80678935bce4770d54d");
            } else if (abi.contains("x86") || abi.contains("i386")) {
                return new PkgInfo("https://github.com/EXALAB/Busybox-static/raw/refs/heads/main/busybox_x86", "9ae241aabf08d6607aa37d2225d6ad76a0cdbfac0a3a611f216c8e816f09ebc2");
            }
        }
        return null;
    }

    public static boolean isInstalled(Context context) {
        File busyboxFile = new File(new File(context.getFilesDir(), "bin"), BUSYBOX_BIN);
        return busyboxFile.exists() && busyboxFile.canExecute();
    }

    public interface InstallationCallback {
        void onSuccess();
        void onError(String error);
    }

    public static void install(final Context context, final InstallationCallback callback) {
        new Thread(() -> {
            File binDir = new File(context.getFilesDir(), "bin");
            if (!binDir.exists()) binDir.mkdir();
            File outputFile = new File(binDir, BUSYBOX_BIN);

            try {
                PkgInfo info = getPkgInfo();
                if (info == null) throw new Exception("Unsupported architecture");

                URL url = new URL(info.url);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setInstanceFollowRedirects(true);
                connection.connect();

                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    throw new Exception("Download failed: HTTP " + connection.getResponseCode());
                }

                InputStream is = new BufferedInputStream(url.openStream());
                FileOutputStream os = new FileOutputStream(outputFile);
                byte[] buffer = new byte[8192];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
                os.flush();
                os.close();
                is.close();

                // Verification
                String downloadedHash = getFileSHA256(outputFile);
                if (!downloadedHash.equalsIgnoreCase(info.sha256)) {
                    outputFile.delete();
                    throw new Exception("Security Alert: Hash mismatch!");
                }

                outputFile.setExecutable(true, false);

                SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
                editor.putBoolean(ENABLED_KEY, true);
                editor.apply();

                if (callback != null) callback.onSuccess();
            } catch (Exception e) {
                if (outputFile.exists()) outputFile.delete();
                if (callback != null) callback.onError(e.getMessage());
            }
        }).start();
    }

    public static void uninstall(Context context) {
        SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
        editor.putBoolean(ENABLED_KEY, false);
        editor.apply();

        File binDir = new File(context.getFilesDir(), "bin");
        if (binDir.exists()) {
            File[] files = binDir.listFiles();
            if (files != null) {
                for (File f : files) f.delete();
            }
        }
    }

    public static String getBusyboxPath(Context context) {
        File busyboxFile = new File(new File(context.getFilesDir(), "bin"), BUSYBOX_BIN);
        return busyboxFile.exists() ? busyboxFile.getAbsolutePath() : null;
    }

    private static String getFileSHA256(File file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        InputStream fis = new FileInputStream(file);
        byte[] buffer = new byte[8192];
        int count;
        while ((count = fis.read(buffer)) != -1) digest.update(buffer, 0, count);
        fis.close();
        byte[] bytes = digest.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
