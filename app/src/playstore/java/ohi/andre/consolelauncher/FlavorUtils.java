package ohi.andre.consolelauncher;

import com.github.anrwatchdog.ANRError;
import com.github.anrwatchdog.ANRWatchDog;

import ohi.andre.consolelauncher.tuils.Tuils;

public class FlavorUtils {

    public static void startANR () {
        new ANRWatchDog(5000)
                .setANRListener(new ANRWatchDog.ANRListener() {
                    @Override
                    public void onAppNotResponding(ANRError anrError) {
                        Tuils.log(anrError);
                        Tuils.toFile(anrError);
                    }
                })
                .setReportMainThreadOnly()
                .start();
    }
}
