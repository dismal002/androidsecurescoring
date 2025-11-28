# Keep Gson classes
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Keep model classes
-keep class com.security.scoringengine.models.** { *; }

# Keep security classes
-keep class com.security.scoringengine.security.** { *; }

# Keep scoring engine
-keep class com.security.scoringengine.scoring.** { *; }

# Obfuscate everything else
-repackageclasses 'o'
-allowaccessmodification
-optimizationpasses 5
