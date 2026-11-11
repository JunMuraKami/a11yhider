package jp.a11yhider;

import java.util.Collections;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class HideAccessibilityServices implements IXposedHookLoadPackage {

    private static final String TAG = "A11yHider";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        XposedBridge.log(TAG + ": Hooking " + lpparam.packageName);

        hookReturnEmptyList("android.view.accessibility.AccessibilityManager",
                "getEnabledAccessibilityServiceList");
        hookReturnEmptyList("android.view.accessibility.AccessibilityManager",
                "getInstalledAccessibilityServiceList");
        hookSettingsString("getString");
        hookSettingsString("getStringForUser");
        hookReturnFalse("android.view.accessibility.AccessibilityManager", "isEnabled");
        hookReturnFalse("android.view.accessibility.AccessibilityManager", "isTouchExplorationEnabled");
    }

    private void hookReturnEmptyList(String className, String methodName) {
        try {
            Class<?> clazz = Class.forName(className);
            XposedBridge.hookAllMethods(clazz, methodName, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    param.setResult(Collections.emptyList());
                }
            });
            XposedBridge.log(TAG + ": Hooked " + methodName + " -> empty list");
        } catch (Exception e) {
            XposedBridge.log(TAG + ": Failed to hook " + methodName + ": " + e);
        }
    }

    private void hookReturnFalse(String className, String methodName) {
        try {
            Class<?> clazz = Class.forName(className);
            XposedBridge.hookAllMethods(clazz, methodName, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    param.setResult(Boolean.FALSE);
                }
            });
            XposedBridge.log(TAG + ": Hooked " + methodName + " -> false");
        } catch (Exception e) {
            XposedBridge.log(TAG + ": Failed to hook " + methodName + ": " + e);
        }
    }

    private void hookSettingsString(String methodName) {
        try {
            Class<?> settingsSecure = Class.forName("android.provider.Settings$Secure");
            XposedBridge.hookAllMethods(settingsSecure, methodName, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (param.args.length >= 2
                            && "enabled_accessibility_services".equals(param.args[1])) {
                        param.setResult("");
                    }
                }
            });
            XposedBridge.log(TAG + ": Hooked Settings.Secure." + methodName);
        } catch (Exception e) {
            XposedBridge.log(TAG + ": Failed to hook " + methodName + ": " + e);
        }
    }
}
