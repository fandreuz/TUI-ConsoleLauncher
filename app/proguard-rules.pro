-keep public class ohi.andre.consolelauncher.commands.main.raw.** { *; }
-keep public class ohi.andre.consolelauncher.commands.main.specific.** { *; }
-keep public class ohi.andre.consolelauncher.commands.tuixt.raw.** { *; }
-keep public class ohi.andre.consolelauncher.tuils.GenericFileProvider { *; }
-keep public class ohi.andre.consolelauncher.tuils.PrivateIOReceiver { *; }
-keep public class ohi.andre.consolelauncher.tuils.PublicIOReceiver { *; }
-keep class ohi.andre.consolelauncher.managers.** { *; }
-keep class ohi.andre.consolelauncher.tuils.libsuperuser.**
-keep class ohi.andre.consolelauncher.managers.suggestions.HideSuggestionViewValues
-keep public class it.andreuzzi.comparestring2.**

-dontwarn ohi.andre.consolelauncher.commands.main.raw.**

-dontwarn javax.annotation.**
-dontwarn javax.inject.**
-dontwarn sun.misc.Unsafe

-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

-dontwarn org.htmlcleaner.**
-dontwarn com.jayway.jsonpath.**
-dontwarn org.slf4j.**

-dontwarn org.jdom2.**