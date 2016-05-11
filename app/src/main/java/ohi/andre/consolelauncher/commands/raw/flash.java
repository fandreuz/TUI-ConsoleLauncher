package ohi.andre.consolelauncher.commands.raw;

import android.hardware.Camera.Parameters;

import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.commands.CommandAbstraction;
import ohi.andre.consolelauncher.commands.ExecInfo;

@SuppressWarnings("deprecation")
public class flash implements CommandAbstraction {

    @Override
    public String exec(ExecInfo info) {
        if (!info.canUseFlash)
            return info.res.getString(R.string.output_flashlightnotavailable);

        final ExecInfo execInfo = info;
        new Thread() {
            @Override
            public void run() {
                super.run();

                if (execInfo.camera == null)
                    execInfo.initCamera();

                if (!execInfo.isFlashOn) {
                    execInfo.parameters.setFlashMode(Parameters.FLASH_MODE_TORCH);
                    execInfo.camera.setParameters(execInfo.parameters);
                    execInfo.camera.startPreview();
                    execInfo.isFlashOn = true;
                } else {
                    execInfo.parameters.setFlashMode(Parameters.FLASH_MODE_OFF);
                    execInfo.camera.setParameters(execInfo.parameters);
                    execInfo.camera.stopPreview();
                    execInfo.isFlashOn = false;
                }
            }
        }.start();

        if (!execInfo.isFlashOn)
            return info.res.getString(R.string.output_flashon);
        else
            return info.res.getString(R.string.output_flashoff);
    }

    @Override
    public int helpRes() {
        return R.string.help_flash;
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
        return 4;
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
