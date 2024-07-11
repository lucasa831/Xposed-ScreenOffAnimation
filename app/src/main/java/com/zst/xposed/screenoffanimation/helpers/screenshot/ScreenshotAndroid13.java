package com.zst.xposed.screenoffanimation.helpers.screenshot;

import android.graphics.Bitmap;
import android.os.Build;
import android.os.IBinder;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import de.robv.android.xposed.XposedBridge;

public class ScreenshotAndroid13 extends ScreenshotBase {

    static Class<?> initializeClassSafe(String className) {
        if(Build.VERSION.SDK_INT != 33) {
            //not android 13, don't load these...
            return null;
        }

        return ScreenshotBase.baseInitializeClassSafe(className);
    }

    public static final Class<?> SURFACECONTROL_CLASS = initializeClassSafe("android.view.SurfaceControl");
    private static final Class<?> SURFACECONTROL_DISPLAY_CAPTURE_ARGS_BUILDER_CLASS = initializeClassSafe("android.view.SurfaceControl$DisplayCaptureArgs$Builder");
    private static final Class<?> SURFACECONTROL_DISPLAY_CAPTURE_ARGS_CLASS = initializeClassSafe("android.view.SurfaceControl$DisplayCaptureArgs");
    private static final Class<?> SURFACECONTROL_SCREENSHOT_HW_BUFFER_CLASS = initializeClassSafe("android.view.SurfaceControl$ScreenshotHardwareBuffer");


    //https://github.com/Genymobile/scrcpy/issues/2727
    public static Bitmap screenshotAndroid13(IBinder displayToken, float[] dims) {
        try {
            int width = (int) dims[0];
            int height = (int) dims[1];

            Constructor<?> builderConstructor = SURFACECONTROL_DISPLAY_CAPTURE_ARGS_BUILDER_CLASS.getDeclaredConstructor(IBinder.class);
            builderConstructor.setAccessible(true);
            Object builder = builderConstructor.newInstance(displayToken);

//			Method sourceCropField = builderClass.getMethod("setSourceCrop", Rect.class);
//			Rect crop = new Rect();
//			crop.set(0, 0, width, height);
//			sourceCropField.invoke(builder, crop);

            Method sizeMethod = SURFACECONTROL_DISPLAY_CAPTURE_ARGS_BUILDER_CLASS.getMethod("setSize", Integer.TYPE, Integer.TYPE);
            sizeMethod.setAccessible(true);
            sizeMethod.invoke(builder, width, height);

            Method build = SURFACECONTROL_DISPLAY_CAPTURE_ARGS_BUILDER_CLASS.getMethod("build");
            build.setAccessible(true);
            Object captureArgs  = build.invoke(builder);


            Method argsMethod = SURFACECONTROL_CLASS.getMethod("captureDisplay", SURFACECONTROL_DISPLAY_CAPTURE_ARGS_CLASS);
            argsMethod.setAccessible(true);
            Object cap = argsMethod.invoke(SURFACECONTROL_CLASS, captureArgs);
            if (cap == null) {
                throw new Exception("Inject SurfaceControl captureDisplay return null");
            }

            return (Bitmap)SURFACECONTROL_SCREENSHOT_HW_BUFFER_CLASS.getMethod("asBitmap").invoke(cap);
        } catch (Exception e) {
            // ignore exception
            XposedBridge.log("Screenshot failed: " + e.getMessage());
            return null;
        }
    }

}
