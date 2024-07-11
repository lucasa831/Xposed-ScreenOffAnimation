/*
 * Copyright (C) 2011 The Android Open Source Project
 * Contains modifications by zst123, Copyright (C) 2014
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zst.xposed.screenoffanimation.helpers;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.os.Build;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

import com.zst.xposed.screenoffanimation.helpers.screenshot.ScreenshotAndroid13;
import com.zst.xposed.screenoffanimation.helpers.screenshot.ScreenshotAndroid14;
import com.zst.xposed.screenoffanimation.helpers.screenshot.ScreenshotAndroid9;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * These methods were taken from 
 * com.android.systemui.screenshot.GlobalScreenshot
 */
public class ScreenshotUtil {

	//https://github.com/Genymobile/scrcpy/issues/2888

	public static void setupCoreClasses(ClassLoader classLoader) {
		if(Build.VERSION.SDK_INT >= 34) {
			ScreenshotAndroid14.DISPLAYCONTROL_CLASS =
					initializeClassXposed("com.android.server.display.DisplayControl", classLoader);
		}
	}

	public static Class<?> initializeClassXposed(String className, ClassLoader classLoader) {
		try {

			Class<?> aClass = XposedHelpers.findClass(className, classLoader);

			XposedBridge.log("Class initialized: " + className);

			return aClass;
		} catch (XposedHelpers.ClassNotFoundError e) {
			XposedBridge.log("Class init failed: " + className + ", error:" + e.getMessage());
		}
		return null;
	}


	public static Bitmap takeScreenshot(Context context) {
		Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE))
				.getDefaultDisplay();
		// We need to orient the screenshot correctly (and the Surface api seems to take screenshots
		// only in the natural orientation of the device :!)
		
		Matrix displayMatrix = new Matrix();
		DisplayMetrics displayMetrics = new DisplayMetrics();
		display.getRealMetrics(displayMetrics);
		
		float[] dims = { displayMetrics.widthPixels, displayMetrics.heightPixels };
		float degrees = getDegreesForRotation(display.getRotation());
		boolean requiresRotation = (degrees > 0);
		if (requiresRotation) {
			// Get the dimensions of the device in its native orientation
			displayMatrix.reset();
			displayMatrix.preRotate(-degrees);
			displayMatrix.mapPoints(dims);
			dims[0] = Math.abs(dims[0]);
			dims[1] = Math.abs(dims[1]);
		}
		
		// Take the screenshot
		Bitmap screenBitmap;
		if (Build.VERSION.SDK_INT >= 34) {
			final IBinder displayToken = getBuiltInDisplay();
			screenBitmap = ScreenshotAndroid14.screenshotAndroid14(displayToken, dims);
		} else if (Build.VERSION.SDK_INT == 33) {
			final IBinder displayToken = getBuiltInDisplay();
			screenBitmap = ScreenshotAndroid13.screenshotAndroid13(displayToken, dims);
		} else if (Build.VERSION.SDK_INT >= 28) {
			screenBitmap = ScreenshotAndroid9.screenshotAndroid9(display, dims);
		}
		else if (Build.VERSION.SDK_INT >= 18) {
			Class<?> surface_class = XposedHelpers.findClass("android.view.SurfaceControl", null);
			screenBitmap = (Bitmap) XposedHelpers.callStaticMethod(surface_class, "screenshot",
					(int) dims[0], (int) dims[1]);
		} else {
			screenBitmap = (Bitmap) XposedHelpers.callStaticMethod(Surface.class, "screenshot",
					(int) dims[0], (int) dims[1]);
		}
		
		if (screenBitmap == null) {
			return null;
		}

		//the method from android 9.x+ has built in rotation... also this throws an exception
		if (requiresRotation && Build.VERSION.SDK_INT < 28) {
			// Rotate the screenshot to the current orientation
			Bitmap ss = Bitmap.createBitmap(displayMetrics.widthPixels,
					displayMetrics.heightPixels, Bitmap.Config.ARGB_8888);
			Canvas c = new Canvas(ss);
			c.translate(ss.getWidth() / 2, ss.getHeight() / 2);
			c.rotate(degrees);
			c.translate(-dims[0] / 2, -dims[1] / 2);
			c.drawBitmap(screenBitmap, 0, 0, null);
			c.setBitmap(null);
			screenBitmap = ss;
		}
		
		// Optimizations
		screenBitmap.setHasAlpha(false);
		screenBitmap.prepareToDraw();
		
		return screenBitmap;
	}


	//https://cs.android.com/android/platform/superproject/main/+/main:frameworks/base/core/java/android/window/ScreenCapture.java;drc=8aacf6b6a42b9b90939b8874980734dbef6308de;l=186

	private static Method getGetBuiltInDisplayMethod() throws NoSuchMethodException {
		Method getBuiltInDisplayMethod = null;
		// the method signature has changed in Android Q
		// <https://github.com/Genymobile/scrcpy/issues/586>
		if (Build.VERSION.SDK_INT < 29) {
			getBuiltInDisplayMethod = ScreenshotAndroid13.SURFACECONTROL_CLASS.getMethod("getBuiltInDisplay", int.class);
		} else if (Build.VERSION.SDK_INT < 34){
			getBuiltInDisplayMethod = ScreenshotAndroid13.SURFACECONTROL_CLASS.getMethod("getInternalDisplayToken");
		} else {
			getBuiltInDisplayMethod = ScreenshotAndroid14.DISPLAYCONTROL_CLASS.getMethod("getPhysicalDisplayToken", long.class);
		}

		return getBuiltInDisplayMethod;
	}

	public static IBinder getBuiltInDisplay() {
		try {
			Method method = getGetBuiltInDisplayMethod();
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || Build.VERSION.SDK_INT >= 34) {
				// call getBuiltInDisplay(0)
				return (IBinder) method.invoke(null, 0);
			}
			// call getInternalDisplayToken()
			return (IBinder) method.invoke(null);
		} catch (InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
			XposedBridge.log("getBuiltInDisplay failed: " + e.getMessage());
			return null;
		}
	}


	private static float getDegreesForRotation(int value) {
		switch (value) {
		case Surface.ROTATION_90:
			return 360f - 90f;
		case Surface.ROTATION_180:
			return 360f - 180f;
		case Surface.ROTATION_270:
			return 360f - 270f;
		}
		return 0f;
	}


}
