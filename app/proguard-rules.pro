# R8 rules for the release build. Room, Hilt, Retrofit, OkHttp, kotlinx.serialization, WorkManager and
# Firebase ship their own consumer rules, so this file only holds what the app itself needs.

# Stack traces in Crashlytics: keep file names and line numbers, and let the mapping file restore the rest.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
