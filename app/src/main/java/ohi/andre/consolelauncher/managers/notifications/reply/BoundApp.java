package ohi.andre.consolelauncher.managers.notifications.reply;

import ohi.andre.consolelauncher.tuils.Compare;

/**
 * Created by francescoandreuzzi on 24/01/2018.
 */

public class BoundApp implements Compare.Stringable {

    public int applicationId;
    public String label, packageName;

    public BoundApp(int applicationId, String packageName, String label) {
        this.applicationId = applicationId;
        this.packageName = packageName;
        this.label = label;
    }

    @Override
    public boolean equals(Object obj) {
        return obj != null && obj instanceof BoundApp && applicationId == ((BoundApp) obj).applicationId;
    }

    @Override
    public String getString() {
        return label;
    }
}
