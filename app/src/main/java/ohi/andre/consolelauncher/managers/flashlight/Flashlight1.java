package ohi.andre.consolelauncher.managers.flashlight;

import android.content.Context;
import android.content.Intent;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Build;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.List;

import ohi.andre.consolelauncher.tuils.PrivateIOReceiver;

/**
 * Created by francescoandreuzzi on 20/08/2017.
 */

public class Flashlight1 extends Flashlight {

    private Camera mCamera;
    private boolean flashSupported;

    public Flashlight1(Context context) {
        super(context);
        this.flashSupported = false;
    }

    @Override
    protected void turnOn() {
        if (this.ready() && !this.getStatus()) {
            try {

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                    this.mCamera.setPreviewTexture(new SurfaceTexture(0));
                    this.mCamera.startPreview();
                    this.updateStatus(true);
                } else {
                    Camera.Parameters parameters = mCamera.getParameters();
                    parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                    mCamera.setParameters(parameters);
                }
            } catch (Exception e) {
                if (this.mCamera != null) {
                    try {
                        this.mCamera.release();
                        this.mCamera = null;
                    } catch (Exception ex) {}
                }

                Intent intent = new Intent(PrivateIOReceiver.ACTION_OUTPUT);
                intent.putExtra(PrivateIOReceiver.TEXT, e.toString());
                LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
            }
        }
    }

    @Override
    protected void turnOff() {
        if (this.getStatus() && this.mCamera != null) {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                this.mCamera.stopPreview();
                this.mCamera.release();
                this.mCamera = null;
            } else {
                Camera.Parameters parameters = mCamera.getParameters();
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                mCamera.setParameters(parameters);
            }
            this.updateStatus(false);
        }
    }

    private boolean ready() {
        if (this.mCamera == null) {
            try {
                this.mCamera = Camera.open();
            } catch (Exception e) {
                Intent intent = new Intent(PrivateIOReceiver.ACTION_OUTPUT);
                intent.putExtra(PrivateIOReceiver.TEXT, e.toString());
                LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
                return false;
            }
        }
        Camera.Parameters mCameraParameters = this.mCamera.getParameters();
        List<String> supportedFlashModes = mCameraParameters.getSupportedFlashModes();
        if (supportedFlashModes != null) {
            if (supportedFlashModes.contains(Camera.Parameters.FLASH_MODE_TORCH)) {
                this.flashSupported = true;
                mCameraParameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
            } else if (supportedFlashModes.contains(Camera.Parameters.FLASH_MODE_ON)) {
                this.flashSupported = true;
                mCameraParameters.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
            }
        }
        if (this.flashSupported) {
            try {
                mCamera.setParameters(mCameraParameters);
            } catch (RuntimeException e) {
                Intent intent = new Intent(PrivateIOReceiver.ACTION_OUTPUT);
                intent.putExtra(PrivateIOReceiver.TEXT, e.toString());
                LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
                return false;
            }
        }
        return true;
    }

}