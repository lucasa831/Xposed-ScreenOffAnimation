package com.zst.xposed.screenoffanimation.helpers.screenshot;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.IBinder;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import de.robv.android.xposed.XposedBridge;

public class ScreenshotAndroid14 extends ScreenshotBase {

    static Class<?> initializeClassSafe(String className) {
        if(Build.VERSION.SDK_INT < 34) {
            return null;
        }

        return ScreenshotBase.baseInitializeClassSafe(className);
    }

    private static final Class<?> SCREENCAPTURE_CLASS = initializeClassSafe("android.window.ScreenCapture");
    private static final Class<?> SCREENCAPTURE_DISPLAY_CAPTURE_ARGS_BUILDER_CLASS = initializeClassSafe("android.window.ScreenCapture$DisplayCaptureArgs$Builder");
    private static final Class<?> SCREENCAPTURE_DISPLAY_CAPTURE_ARGS_CLASS = initializeClassSafe("android.window.ScreenCapture$DisplayCaptureArgs");
    private static final Class<?> SCREENCAPTURE_SCREENSHOT_HW_BUFFER_CLASS = initializeClassSafe("android.window.ScreenCapture$ScreenshotHardwareBuffer");

    public static Class<?> DISPLAYCONTROL_CLASS;

    @SuppressLint("PrivateApi")
    public static Bitmap screenshotAndroid14(IBinder displayToken, float[] dims) {
        try {
            int width = (int) dims[0];
            int height = (int) dims[1];


            Constructor<?> builderConstructor = SCREENCAPTURE_DISPLAY_CAPTURE_ARGS_BUILDER_CLASS.getDeclaredConstructor(IBinder.class);
            builderConstructor.setAccessible(true);
            Object builder = builderConstructor.newInstance(displayToken);


            Method sizeMethod = SCREENCAPTURE_DISPLAY_CAPTURE_ARGS_BUILDER_CLASS.getMethod("setSize", Integer.TYPE, Integer.TYPE);
            sizeMethod.setAccessible(true);
            sizeMethod.invoke(builder, width, height);

            Method build = SCREENCAPTURE_DISPLAY_CAPTURE_ARGS_BUILDER_CLASS.getMethod("build");
            build.setAccessible(true);
            Object captureArgs  = build.invoke(builder);


            Method argsMethod = SCREENCAPTURE_CLASS.getMethod("captureDisplay", SCREENCAPTURE_DISPLAY_CAPTURE_ARGS_CLASS);
            argsMethod.setAccessible(true);
            Object cap = argsMethod.invoke(null, captureArgs);
            if (cap == null) {
                throw new Exception("Inject SurfaceControl captureDisplay return null");
            }

            return (Bitmap)SCREENCAPTURE_SCREENSHOT_HW_BUFFER_CLASS.getMethod("asBitmap").invoke(cap);
        } catch (Exception e) {
            XposedBridge.log("Screenshot failed: " + e.getMessage());
            return null;
        }
    }

}
