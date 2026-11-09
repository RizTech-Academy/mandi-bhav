# Mandi Bhav's own R8 rules.
#
# There are none, on purpose. Everything the app uses by reflection is covered
# by rules its libraries ship inside their AARs and JARs, which R8 reads by
# itself: kotlinx.serialization (the JSON classes and the Navigation 3 keys),
# Retrofit, Room, Hilt and hilt-work (the worker's class name), WorkManager,
# coroutines, OkHttp and Coil. The merged list is written to
# app/build/outputs/mapping/release/configuration.txt on every release build.
#
# Add a rule here only when a minified build has been shown to need it, with
# a comment saying which class, why, and how it failed without it. A broad
# rule such as -keep class com.riztechacademy.** { *; } makes the crash go
# away by switching R8 off for the whole app.
