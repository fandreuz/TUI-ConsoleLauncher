package ohi.andre.consolelauncher.managers.notifications.reply;

import it.andreuzzi.comparestring2.StringableObject;

/**
 * Created by francescoandreuzzi on 24/01/2018.
 */

public class BoundApp implements StringableObject {

    public int applicationId;
    public String label, packageName;

    String lowercaseLabel;

    public BoundApp(int applicationId, String packageName, String label) {
        this.applicationId = applicationId;
        this.packageName = packageName;
        this.label = label;

        this.lowercaseLabel = label.toLowerCase();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof BoundApp && applicationId == ((BoundApp) obj).applicationId;
    }

    @Override
    public String getLowercaseString() {
        return lowercaseLabel;
    }

    @Override
    public String getString() {
        return label;
    }
}
