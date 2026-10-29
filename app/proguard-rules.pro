# kotlinx.serialization: keep generated serializers for DTOs.
-keepclassmembers @kotlinx.serialization.Serializable class com.iptvtv.data.remote.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
