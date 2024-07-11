package com.zst.xposed.screenoffanimation.helpers.screenshot;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.view.Display;

import de.robv.android.xposed.XposedHelpers;

public class ScreenshotAndroid9 {
    public static Bitmap screenshotAndroid9(Display display, float[] dims) {
        Bitmap screenBitmap;
        //the method sourfacecontrol.screenshot(int, int) isn't present on 9.x+ source code

        Class<?> surface_class = XposedHelpers.findClass("android.view.SurfaceControl", null);
        //IBinder displaybinder = (IBinder)  XposedHelpers.callStaticMethod(surface_class, "getBuiltInDisplay", 0);
			/*screenshot(Rect sourceCrop, int width, int height,
			int minLayer, int maxLayer, boolean useIdentityTransform,
			int rotation)*/
        int rotate = display.getRotation();


        //not sure why but it's inverting for me so...
        if (rotate == 1) rotate = 3;
        else if(rotate == 3) rotate = 1;


        screenBitmap = (Bitmap) XposedHelpers.callStaticMethod(surface_class, "screenshot", new Rect(),
                (int) dims[0], (int) dims[1], 0, 9, false, rotate);

        //XposedBridge.log(display.getRotation() + "  -  " + rotate);
        return screenBitmap;
    }
}
