package com.zst.xposed.screenoffanimation.anim;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.Transformation;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

import com.zst.xposed.screenoffanimation.R;
import com.zst.xposed.screenoffanimation.helpers.ScreenshotUtil;
import com.zst.xposed.screenoffanimation.helpers.Utils;
import com.zst.xposed.screenoffanimation.widgets.AnimationEndListener;

import de.robv.android.xposed.XC_MethodHook.MethodHookParam;
import de.robv.android.xposed.XposedBridge;

public class SideFill extends AnimImplementation {
	/**
	 * Side fill Animation, Center rectangle extends to sides fading to black
	 */

	@Override
	public void animateScreenOff(final Context ctx, WindowManager wm, MethodHookParam param, Resources res) {
		final ImageView view = new ImageView(ctx);
		final Bitmap b = ScreenshotUtil.takeScreenshot(ctx);
		view.setImageBitmap(b);
		view.setColorFilter(Color.BLACK);
		view.setScaleType(ScaleType.FIT_XY);
		view.setBackgroundColor(Color.TRANSPARENT);
		final AlphaAnimation alpha = new AlphaAnimation(1.25f, 0) {
			@Override
			@SuppressWarnings("deprecation")
			protected void applyTransformation(float interpolatedTime, Transformation t) {
				final float fromAlpha = 1;
				float newAlpha = fromAlpha + ((0 - fromAlpha) * interpolatedTime);
				if (newAlpha > 1)
					newAlpha = 1;
				if (Build.VERSION.SDK_INT >= 16) {
					view.setImageAlpha((int) (255 - (newAlpha * 255)));
				} else {
					view.setAlpha((int) (255 - (newAlpha * 255)));
				}
			}
		};
		final AnimationSet anim = new AnimationSet(false);
		anim.addAnimation(loadSFAnimation(ctx, res));
		anim.addAnimation(alpha);
		anim.setDuration(anim_speed);
		final float scale = (anim_speed) / 200;
		if (scale >= 1) {
			anim.scaleCurrentDuration(scale);
		}
		anim.setFillAfter(true);
		anim.setStartOffset(100);

		final ScreenOffAnim holder = new ScreenOffAnim(ctx, wm, param) {
			@Override
			public void animateScreenOffView() {
				view.startAnimation(anim);
			}
		};
		anim.setAnimationListener(new AnimationEndListener() {
			@Override
			public void onAnimationEnd(Animation animation) {
				finish(ctx, holder, 100);
			}
		});
		BitmapDrawable ob = new BitmapDrawable(ctx.getResources(), b);
		holder.mFrame.setBackground(ob);
		holder.showScreenOffView(view);

	}

	public Animation loadSFAnimation(Context ctx, Resources res) {
		return Utils.loadAnimation(ctx, res, R.anim.sidefill);
	}

	@Override
	public boolean supportsScreenOn() {
		return false;
	}

	@Override
	public void animateScreenOn(final Context c, WindowManager wm, Resources res) throws Exception
	{
		throw new Exception("This class doesn't support screen on animation");
	}


}
