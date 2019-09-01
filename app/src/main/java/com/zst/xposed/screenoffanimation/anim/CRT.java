package com.zst.xposed.screenoffanimation.anim;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.Transformation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

import com.zst.xposed.screenoffanimation.R;
import com.zst.xposed.screenoffanimation.helpers.ScreenshotUtil;
import com.zst.xposed.screenoffanimation.helpers.Utils;
import com.zst.xposed.screenoffanimation.widgets.AnimationEndListener;

import de.robv.android.xposed.XC_MethodHook.MethodHookParam;


public class CRT extends AnimImplementation {
	/**
	 * Electron Beam (CRT) Animation
	 */

	@Override
	public void animateScreenOff(final Context ctx, WindowManager wm, MethodHookParam param, Resources res) {
		final ImageView view = new ImageView(ctx);
		view.setScaleType(ScaleType.FIT_XY);
		view.setImageBitmap(ScreenshotUtil.takeScreenshot(ctx));
		view.setBackgroundColor(Color.WHITE);
		//view.setBackground(res.getDrawable(R.drawable.crtback)); //waiting feedback
		final AlphaAnimation alpha = new AlphaAnimation(1.25f, 0) {
			@Override
			@SuppressWarnings("deprecation")
			protected void applyTransformation(float interpolatedTime, Transformation t) {
				final float fromAlpha = 1;
				float newAlpha = fromAlpha + ((0 - fromAlpha) * interpolatedTime);
				if (newAlpha > 1)
					newAlpha = 1;
				if (Build.VERSION.SDK_INT >= 16) {
					view.setImageAlpha((int) (newAlpha * 255));
				} else {
					view.setAlpha((int) (newAlpha * 255));
				}
			}
		};
		final AnimationSet anim = new AnimationSet(false);
		anim.addAnimation(loadCRTAnimation(ctx, res));
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

		holder.mFrame.setBackgroundColor(Color.BLACK);
		holder.showScreenOffView(view);
	}

	public Animation loadCRTAnimation(Context ctx, Resources res) {
		return Utils.loadAnimation(ctx, res, R.anim.crt_tv);
	}

	@Override
	public boolean supportsScreenOn() {
		return true;
	}

	@Override
	public void animateScreenOn(final Context c, WindowManager wm, Resources res) throws Exception
	{
		anim_speed = anim_speed + 100;

		final DisplayMetrics display = new DisplayMetrics();
		wm.getDefaultDisplay().getRealMetrics(display);

		final PortionView upperPortion = new PortionView(c, display.widthPixels, display.heightPixels, true);
		final PortionView lowerPortion = new PortionView(c, display.widthPixels, display.heightPixels, false);

		final SidesView rightPortion = new SidesView(c, display.widthPixels, display.heightPixels, true);
		final SidesView leftPortion = new SidesView(c, display.widthPixels, display.heightPixels, false);

		final FrameLayout layoutOn = new FrameLayout(c);
		layoutOn.addView(upperPortion);
		layoutOn.addView(lowerPortion);

		layoutOn.addView(rightPortion);
		layoutOn.addView(leftPortion);

		final Animation anim1 = new Animation() {
			int adjusted_height = (display.heightPixels / 2) + (display.widthPixels / 16) + upperPortion.mGap;
			int adjusted_width =  display.widthPixels / 4;
			int anim = 1;


			@Override
			protected void applyTransformation(float interpolatedTime, Transformation t)
			{
				float newWidth = adjusted_width * interpolatedTime;

				if(newWidth*4>=(display.widthPixels*0.667))
				{
					float newHeight = ((adjusted_height  * interpolatedTime)*anim)/4;
					anim++;
					upperPortion.setY(-newHeight);
					lowerPortion.setY(newHeight);
				}
				else
				{
					upperPortion.setY(-1); //high res devices shoud have thoose values as 1 and -1
					lowerPortion.setY(1);
				}

				newWidth = adjusted_width * interpolatedTime * 3;

				rightPortion.setX(newWidth);
				leftPortion.setX(-newWidth);
			}
		};
		anim1.setDuration(anim_speed);
		anim1.setInterpolator(new AccelerateDecelerateInterpolator());

		final ScreenOnAnim holder = new ScreenOnAnim(c, wm) {
			@Override
			public void animateScreenOnView() {

				layoutOn.startAnimation(anim1);
			}
		};
		anim1.setAnimationListener(new AnimationEndListener() {
			@Override
			public void onAnimationEnd(Animation animation) {
				holder.finishScreenOnAnim();
			}
		});

		holder.showScreenOnView(layoutOn);
	}

	public class SidesView extends View {
		final Paint mPaint;
		final boolean mTop;
		final int mGap;

		Path mPath;

		public SidesView(Context context, int screenWidth, int screenHeight, boolean right)
		{
			super(context);
			mPaint = new Paint();
			mPaint.setAntiAlias(true);
			mPaint.setStyle(Paint.Style.FILL);
			mPaint.setColor(Color.BLACK);

			mTop = right;
			mGap = Utils.dp(8, getContext());

			mPath = makePortion(right, screenWidth, screenHeight, mGap);
		}

		@Override
		public void setY(float y) {
			super.setY(mTop ? (y - mGap) : (y + mGap));
			// undo the offset so what we see is correct.
		}

		@Override
		protected void onDraw(Canvas canvas) {
			canvas.drawPath(mPath, mPaint);
		}

		//
		private Path makePortion(boolean facingRight, int screenWidth, int screenHeight, int gap) {

			final Path path = new Path();

			// add gap to the mid_height to offset the final bit.

			if (facingRight)
			{
				path.moveTo(screenWidth/2, -gap);
				path.lineTo(screenWidth, -gap);
				path.lineTo(screenWidth, screenHeight);
				path.lineTo(screenWidth/2, screenHeight);

			} else {
				path.moveTo(0, -gap);
				path.lineTo(screenWidth/2, -gap);
				path.lineTo(screenWidth/2, screenHeight);
				path.lineTo(0, screenHeight);

			}
			return path;
		}
	}

	public class PortionView extends View {
		final Paint mPaint;
		final boolean mTop;
		final int mGap;

		Path mPath;

		public PortionView(Context context, int screenWidth, int screenHeight, boolean top)
		{
			super(context);
			mPaint = new Paint();
			mPaint.setAntiAlias(true);
			mPaint.setStyle(Paint.Style.FILL);
			mPaint.setColor(Color.BLACK);

			mTop = top;
			mGap = Utils.dp(8, getContext());
			mPath = makePortion(top, screenWidth, screenHeight,
					(screenWidth / 8), mGap);
		}

		@Override
		public void setY(float y) {
			super.setY(mTop ? (y - mGap) : (y + mGap));
			// undo the offset so what we see is correct.
		}

		@Override
		protected void onDraw(Canvas canvas) {
			canvas.drawPath(mPath, mPaint);
		}

		// http://stackoverflow.com/questions/15150701/
		private Path makePortion(boolean facingDown, int screenWidth, int screenHeight,
								 int triangleHeight, int gap) {

			final Path path = new Path();
			final int mid_height_top = (screenHeight - triangleHeight) / 2 + gap;
			final int mid_height_bottom = (screenHeight - triangleHeight) / 2 - gap;
			// add gap to the mid_height to offset the final bit.

			if (facingDown) {
				path.moveTo(0, -gap); // top-left screen
				path.lineTo(screenWidth, -gap); // top-right screen
				path.lineTo(screenWidth, mid_height_top); // corner-right (where triangle starts)
				path.lineTo(0, mid_height_top);// corner-left (where triangle starts)

			} else {
				path.moveTo(0, screenHeight + gap); // bottom-left screen
				path.lineTo(screenWidth, screenHeight + gap); // bottom-right screen
				path.lineTo(screenWidth, mid_height_bottom); // corner-right (where triangle starts)
				path.lineTo(0, mid_height_bottom);// corner-left (where triangle starts)
			}
			return path;
		}
	}

}
