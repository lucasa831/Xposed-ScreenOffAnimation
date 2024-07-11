package com.zst.xposed.screenoffanimation.helpers.screenshot;

import de.robv.android.xposed.XposedBridge;

public class ScreenshotBase {
    static Class<?> baseInitializeClassSafe(String className) {
        try {
            Class<?> aClass = Class.forName(className);

            XposedBridge.log("Class initialized: " + className);

            return aClass;
        } catch (ClassNotFoundException e) {
            XposedBridge.log("Class init failed: " + className + ", error:" + e.getMessage());
        }
        return null;
    }

}
