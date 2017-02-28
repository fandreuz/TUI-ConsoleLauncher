-keep public class ohi.andre.consolelauncher.commands.main.raw.* { public *; }
-keep public abstract class ohi.andre.consolelauncher.commands.main.generals.* { public *; }
-keep public class ohi.andre.consolelauncher.commands.tuixt.raw.* { public *; }

-dontwarn ohi.andre.consolelauncher.commands.main.raw.**

-keepclassmembers class * extends com.stephentuso.welcome.WelcomeActivity {
    public static java.lang.String welcomeKey();
}