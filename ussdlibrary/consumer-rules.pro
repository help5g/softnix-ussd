# Keep the public API of the USSD library so apps using it (with minify on) don't break.
-keep public class com.softnix.ussd.UssdDialer { public *; }
-keep public class com.softnix.ussd.UssdConfig { public *; }
-keep public class com.softnix.ussd.UssdPermissionHelper { public *; }
-keep public interface com.softnix.ussd.UssdCallback { *; }
# The accessibility service is referenced from the merged manifest; keep it.
-keep class com.softnix.ussd.UssdAccessibilityService { *; }