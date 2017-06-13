package ohi.andre.consolelauncher.tuils;

/**
 * Created by francescoandreuzzi on 27/04/2017.
 */

public class StoppableThread extends Thread {

    private volatile boolean stopped = false;

    @Override
    public void interrupt() {
        super.interrupt();

        stopped = true;
    }

    @Override
    public boolean isInterrupted() {
        return stopped || super.isInterrupted();
    }
}
