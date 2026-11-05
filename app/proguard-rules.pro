# kotlinx.serialization: keep generated serializers for persisted config models.
-keepattributes *Annotation*, InnerClasses
-keepclassmembers @kotlinx.serialization.Serializable class com.dots.app.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.dots.app.**$$serializer { *; }
