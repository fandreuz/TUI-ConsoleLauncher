package ohi.andre.consolelauncher.tuils;

/**
 * Created by andre on 10/11/15.
 */
public class AppInfo {

    public String packageName;
    public String publicLabel;

    public AppInfo(String packageName, String publicLabel) {
        this.packageName = packageName;
        this.publicLabel = publicLabel;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof AppInfo))
            return false;

        AppInfo i = (AppInfo) o;
        return this.packageName.equals(i.packageName);
    }

    @Override
    public int hashCode() {
        return packageName.hashCode();
    }
}
