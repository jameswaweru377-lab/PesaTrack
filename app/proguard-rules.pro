# R3: keep reflective metadata where library consumer rules need it.
#
# PesaTrack currently uses Gson's JSON tree API, not reflective app-model
# serialization. Chaquopy is called via its public Kotlin/Java API and bundles
# its own Android integration. Compose dependencies provide consumer rules.
# Add targeted -keep entries only for a reproduced release-only failure.
-keepattributes Signature,InnerClasses,EnclosingMethod,*Annotation*
