# A11yHider

A Xposed/LSPosed (Vector) module that hides the presence of accessibility
services from any app you choose. Some apps refuse to start, or show a
warning, when they detect an accessibility service is enabled on the
device — even an unrelated one such as a launcher tweak or theme helper.
A11yHider makes the hooked app see no accessibility services at all.

## What it hooks

For every app in its scope, A11yHider returns fixed values from these
APIs, regardless of what is actually enabled on the device:

| API | Hooked return value |
|---|---|
| `AccessibilityManager.getEnabledAccessibilityServiceList()` | empty list |
| `AccessibilityManager.getInstalledAccessibilityServiceList()` | empty list |
| `AccessibilityManager.isEnabled()` | `false` |
| `AccessibilityManager.isTouchExplorationEnabled()` | `false` |
| `Settings.Secure.getString("enabled_accessibility_services")` | `""` |
| `Settings.Secure.getStringForUser(..., "enabled_accessibility_services")` | `""` |

This is a blanket "no services at all" response rather than a per-service
filter, so it keeps working if you later enable a different accessibility
service that the target app would also object to.

## Requirements

- A rooted device with an Xposed-compatible framework: LSPosed, or a
  fork such as Vector (which also runs legacy Xposed modules through
  `VectorLegacyBridge`).
- Module support for legacy-style Xposed modules (`xposed_init`,
  `IXposedHookLoadPackage`). LSPosed/Vector support this directly.

## Installation

1. Build the APK (see **Building** below), or use a prebuilt release
   if one is attached to this repository.
2. Install it: `adb install a11yhider.apk`.
3. Open your LSPosed/Vector manager app, find **A11yHider** in the
   module list, and enable it.
4. In the module's scope screen, add the app(s) you want to hide
   accessibility services from. The module ships with **no apps in
   scope by default** — nothing is hooked until you add a target here.
5. Force-stop (or reboot) the target app so the hook takes effect on
   its next launch.

No rebuild is needed to add or remove target apps — scope is managed
entirely from the manager app's UI.

## Building

The project is deliberately dependency-free (no Gradle) so it can be
built with just the Android SDK command-line tools and a JDK.

Requirements:
- JDK 17
- Android SDK with `platforms/android-31` and `build-tools/31.0.0`
  (or similar versions — adjust paths below)

```powershell
$JAVAC = "<jdk17>\bin\javac.exe"
$JAR = "<jdk17>\bin\jar.exe"
$JARSIGNER = "<jdk17>\bin\jarsigner.exe"
$ANDROID_JAR = "<sdk>\platforms\android-31\android.jar"
$AAPT = "<sdk>\build-tools\31.0.0\aapt.exe"
$D8 = "<sdk>\build-tools\31.0.0\d8.bat"

# 1. Compile
& $JAVAC -source 8 -target 8 -encoding UTF-8 -cp $ANDROID_JAR -d build\classes (Get-ChildItem src -Recurse -Filter *.java | % FullName)

# 2. Dex only the module's own classes (jp/...), not the Xposed API stubs
& cmd /c $D8 --min-api 26 --output build (Get-ChildItem build\classes\jp -Recurse -Filter *.class | % FullName)

# 3. Package resources + manifest
& $AAPT package -f -M AndroidManifest.xml -S res -I $ANDROID_JAR -F build\a11yhider.apk

# 4. Add the dex
Push-Location build; & $AAPT add a11yhider.apk classes.dex; Pop-Location

# 5. Add assets/xposed_init (must use forward slashes, and no BOM)
& $JAR uf build\a11yhider.apk assets/xposed_init

# 6. Sign (a debug keystore is fine for local/sideloaded installs)
& $JARSIGNER -keystore debug.keystore -storepass android -keypass android build\a11yhider.apk debug
```

### Gotchas this project ran into

- **`assets/xposed_init` must use forward slashes** when added with
  `jar`. Adding it from a Windows-style path (`assets\xposed_init`)
  produces an entry Vector cannot find.
- **No BOM in `xposed_init`**: if the file is saved as UTF-8 with a
  byte-order mark, the class name read from it gets a stray character
  prefixed, and `ClassNotFoundException` is thrown when the module
  loads. Write it as UTF-8 **without** BOM.
- **Non-ASCII characters in `.java` sources** can fail to compile with
  `javac` on a non-UTF-8 system locale (e.g. Windows Shift-JIS/CP932),
  even with `-encoding UTF-8` passed — stick to ASCII in source files,
  or build from a UTF-8 locale.
- The module needs a `LAUNCHER` activity (see `DummyActivity`) or the
  manager app will report "module APK not found" even though it is
  installed correctly.

## Project layout

```
src/jp/a11yhider/HideAccessibilityServices.java   module logic
src/jp/a11yhider/DummyActivity.java               launcher activity (shows usage info)
src/de/robv/android/xposed/...                    Xposed API stubs (compile-time only)
assets/xposed_init                                 entry point class name for the framework
res/values/arrays.xml                              xposed_scope (left empty; configure via manager UI)
AndroidManifest.xml
```

## Disclaimer

This module changes what a hooked app can observe about the device's
accessibility state. Use it only on your own device, for apps you are
authorized to modify the behavior of. Some apps use accessibility
checks as part of anti-tampering or fraud-prevention logic; hiding
that signal may violate those apps' terms of service.

## License

MIT — see [LICENSE](LICENSE).
