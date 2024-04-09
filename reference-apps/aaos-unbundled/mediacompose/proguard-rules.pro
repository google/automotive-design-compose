# This is a test app so don't obfuscate anything even when testing with Proguard
-dontobfuscate

# Keep the stubs of the car library
-keep class android.car.** {*;}
