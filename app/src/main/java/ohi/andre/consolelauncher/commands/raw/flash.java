package ohi.andre.consolelauncher.commands.raw;

import android.annotation.TargetApi;
import android.content.Context;
import android.hardware.Camera;
import android.graphics.SurfaceTexture;
import android.hardware.Camera.Parameters;
import android.hardware.camera2.CameraManager;
import android.os.Build;

import java.io.IOException;

import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.commands.CommandAbstraction;
import ohi.andre.consolelauncher.commands.ExecInfo;

@SuppressWarnings("deprecation")
public class flash implements CommandAbstraction {

    @Override
    public String exec(final ExecInfo info) {
        if (!info.canUseFlash) {
            return info.res.getString(R.string.output_flashlightnotavailable);
        }

        final ExecInfo execInfo = info;
        final boolean flashOn = info.isFlashOn;

        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            new Thread() {
                @Override
                public void run() {
                    super.run();

                    if (execInfo.camera == null)
                        execInfo.initCamera();

                    if (!flashOn) {
                        execInfo.parameters.setFlashMode(Parameters.FLASH_MODE_TORCH);
                        execInfo.camera.setParameters(execInfo.parameters);

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                            setSurfaceTexture(info.camera);
                        }

                        execInfo.camera.startPreview();
                    } else {
                        execInfo.parameters.setFlashMode(Parameters.FLASH_MODE_OFF);
                        execInfo.camera.setParameters(execInfo.parameters);

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                            detachSurfaceTexture(info.camera);
                        }

                        execInfo.camera.stopPreview();
                    }
                }
            }.start();
        } else {
            if(!flashOn) {
                flashOnMarshy(info.context);
            } else {
                flashOffMarshy(info.context);
            }
        }

        execInfo.isFlashOn = !flashOn;
        if (execInfo.isFlashOn) {
            return info.res.getString(R.string.output_flashon);
        } else {
            return info.res.getString(R.string.output_flashoff);
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static void setSurfaceTexture(Camera camera) {
        try {
            camera.setPreviewTexture(new SurfaceTexture(0));
        } catch (IOException e) {}
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static void detachSurfaceTexture(Camera camera) {
        try {
            camera.setPreviewTexture(null);
        } catch (IOException e) {}
    }

    @TargetApi(Build.VERSION_CODES.M)
    private boolean flashOnMarshy(Context context) {
        try {
            CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
            manager.setTorchMode(manager.getCameraIdList()[0], true);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private boolean flashOffMarshy(Context context) {
        try {
            CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
            manager.setTorchMode(manager.getCameraIdList()[0], false);
            return true;
        } catch (Exception e) {
            return false;
        }
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
