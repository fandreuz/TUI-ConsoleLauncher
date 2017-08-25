package ohi.andre.consolelauncher.managers.flashlight;

import android.content.Context;
import android.os.Build;

/**
 * Created by francescoandreuzzi on 20/08/2017.
 */

public class TorchManager {

    private static TorchManager mInstance;
    private final String flashType;
    private Torch mTorch;
    private String torchType;

    private TorchManager() {
        this.flashType = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) ? Flashlight2.TYPE : Flashlight1.TYPE;
        this.setTorchType(Constants.ID_DEVICE_OUTPUT_TORCH_FLASH);
    }

    public static TorchManager getInstance() {
        if (mInstance == null) {
            mInstance = new TorchManager();
        }
        return mInstance;
    }

    public boolean isOn() {
        return this.mTorch != null;
    }

    public void setTorchType(String torchType) {
        this.torchType = torchType;
    }

    public void turnOn(Context context) {
        if (this.mTorch == null) {
            if (this.torchType.equals(Flashlight.TYPE)) {
                if (this.flashType.equals(Flashlight1.TYPE)) {
                    this.mTorch = new Flashlight1(context);
                } else if (this.flashType.equals(Flashlight2.TYPE)) {
                    this.mTorch = new Flashlight2(context);
                }
            }
        }
        this.mTorch.setEnabled(true);
        this.mTorch.start(true);
    }

    public void turnOff() {
        if (this.mTorch != null) {
            this.mTorch.start(false);
            this.mTorch = null;
        }
    }

    public void toggle(Context context) {
        if (this.mTorch == null) {
            this.turnOn(context);
        } else {
            if (this.mTorch.getStatus()) {
                this.turnOff();
            } else {
                this.turnOn(context);
            }
        }
    }
}