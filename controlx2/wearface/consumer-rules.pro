# Watch face services and complication providers are instantiated reflectively by the system
# from their manifest entries, never referenced from Kotlin, so they must survive shrinking.
# :wear currently builds with minifyEnabled false, but this keeps the module correct if that
# ever changes.
-keep class it.mattia.controlx2face.** { *; }
