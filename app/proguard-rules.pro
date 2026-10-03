# kotlinx.serialization: keep generated serializers for the script/backup model.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

-keepclassmembers @kotlinx.serialization.Serializable class io.github.syntaxghost404.tappilot.** {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclasseswithmembers class io.github.syntaxghost404.tappilot.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# The accessibility service and tile service are referenced from the manifest only.
-keep class io.github.syntaxghost404.tappilot.service.TapPilotAccessibilityService
-keep class io.github.syntaxghost404.tappilot.service.ControlsTileService
