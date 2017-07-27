-keep public class ohi.andre.consolelauncher.commands.main.raw.* { public *; }
-keep public abstract class ohi.andre.consolelauncher.commands.main.generals.* { public *; }
-keep public class ohi.andre.consolelauncher.commands.tuixt.raw.* { public *; }

-keep public class ohi.andre.consolelauncher.managers.notifications.NotificationService
-keep public class ohi.andre.consolelauncher.tuils.KeeperService

-dontwarn ohi.andre.consolelauncher.commands.main.raw.**

-dontwarn javax.annotation.**
-dontwarn javax.inject.**
-dontwarn sun.misc.Unsafe