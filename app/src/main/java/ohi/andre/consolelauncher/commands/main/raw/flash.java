package ohi.andre.consolelauncher.commands.main.raw;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import ohi.andre.consolelauncher.LauncherActivity;
import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.commands.CommandAbstraction;
import ohi.andre.consolelauncher.commands.ExecutePack;
import ohi.andre.consolelauncher.managers.flashlight.TorchManager;

public class flash implements CommandAbstraction {

    @Override
    public String exec(ExecutePack pack) {
        if (ContextCompat.checkSelfPermission(pack.context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions((Activity) pack.context, new String[]{Manifest.permission.CAMERA}, LauncherActivity.COMMAND_REQUEST_PERMISSION);
            return pack.context.getString(R.string.output_waitingpermission);
        }

        TorchManager.getInstance().toggle(pack.context);

//        final MainPack info = (MainPack) pack;
//        if (!info.canUseFlash) {
//            return info.res.getString(R.string.output_flashlightnotavailable);
//        }
//
//        final boolean flashOn = info.isFlashOn;
//        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
//            info.initCamera();
//
//            if(info.camera == null) {
//                return info.res.getString(R.string.output_problemcamera);
//            }
//
//            new Thread() {
//                @Override
//                public void run() {
//                    super.run();
//
//                    if (!flashOn) {
//                        info.parameters.setFlashMode(Parameters.FLASH_MODE_TORCH);
//                        info.camera.setParameters(info.parameters);
//
//                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
//                            setSurfaceTexture(info.camera);
//                        }
//
//                        try {
//                            info.camera.startPreview();
//                        } catch (Exception e) {
//                            info.camera.release();
//                        }
//                    } else {
//                        info.parameters.setFlashMode(Parameters.FLASH_MODE_OFF);
//                        info.camera.setParameters(info.parameters);
//
//                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
//                            detachSurfaceTexture(info.camera);
//                        }
//
//                        try {
//                            info.camera.stopPreview();
//                        } catch (Exception e) {
//                            info.camera.release();
//                        }
//                    }
//                }
//            }.start();
//        } else {
//            if(!flashOn) {
//                flashOnMarshy(info.context);
//            } else {
//                flashOffMarshy(info.context);
//            }
//        }
//
//        info.isFlashOn = !flashOn;
//        if (info.isFlashOn) {
//            return info.res.getString(R.string.output_flashon);
//        } else {
//            return info.res.getString(R.string.output_flashoff);
//        }
        return null;
    }

//    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
//    public static void setSurfaceTexture(Camera camera) {
//        try {
//            camera.setPreviewTexture(new SurfaceTexture(0));
//        } catch (Exception e) {}
//    }
//
//    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
//    public static void detachSurfaceTexture(Camera camera) {
//        try {
//            camera.setPreviewTexture(null);
//        } catch (Exception e) {}
//    }
//
//    @TargetApi(Build.VERSION_CODES.M)
//    private boolean flashOnMarshy(Context context) {
//        try {
//            CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
//            manager.setTorchMode(manager.getCameraIdList()[0], true);
//            return true;
//        } catch (Exception e) {
//            return false;
//        }
//    }
//
//    @TargetApi(Build.VERSION_CODES.M)
//    private boolean flashOffMarshy(Context context) {
//        try {
//            CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
//            manager.setTorchMode(manager.getCameraIdList()[0], false);
//            return true;
//        } catch (Exception e) {
//            return false;
//        }
//    }

    @Override
    public int helpRes() {
        return R.string.help_flash;
    }

    @Override
    public int[] argType() {
        return new int[0];
    }

    @Override
    public int priority() {
        return 4;
    }

    @Override
    public String onNotArgEnough(ExecutePack info, int nArgs) {
        return null;
    }

    @Override
    public String onArgNotFound(ExecutePack info, int index) {
        return null;
    }

}
