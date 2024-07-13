package com.zst.xposed.screenoffanimation;

import static android.content.Context.RECEIVER_EXPORTED;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.XModuleResources;
import android.content.res.XResources;
import android.os.Build;
import android.view.WindowManager;

import com.zst.xposed.screenoffanimation.Common.Pref;
import com.zst.xposed.screenoffanimation.anim.AnimImplementation;
import com.zst.xposed.screenoffanimation.anim.Bounce;
import com.zst.xposed.screenoffanimation.anim.CRT;
import com.zst.xposed.screenoffanimation.anim.CRTVertical;
import com.zst.xposed.screenoffanimation.anim.FadeOut;
import com.zst.xposed.screenoffanimation.anim.FadeTiles;
import com.zst.xposed.screenoffanimation.anim.Flip;
import com.zst.xposed.screenoffanimation.anim.FlipTiles;
import com.zst.xposed.screenoffanimation.anim.LGOptimusG;
import com.zst.xposed.screenoffanimation.anim.LollipopFadeOut;
import com.zst.xposed.screenoffanimation.anim.ScaleDown;
import com.zst.xposed.screenoffanimation.anim.ScaleDownBottom;
import com.zst.xposed.screenoffanimation.anim.SideFill;
import com.zst.xposed.screenoffanimation.anim.TVBurnIn;
import com.zst.xposed.screenoffanimation.anim.VertuSigTouch;
import com.zst.xposed.screenoffanimation.anim.WP8;
import com.zst.xposed.screenoffanimation.helpers.ScreenshotUtil;
import com.zst.xposed.screenoffanimation.helpers.Utils;

import java.lang.reflect.Field;
import java.util.List;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class MainXposed implements IXposedHookZygoteInit, IXposedHookLoadPackage {
	
	public static boolean mDontAnimate;
	public static boolean mAnimationRunning;
	public static boolean mOnAnimationRunning;

	static XModuleResources sModRes;
	static XSharedPreferences sPref;
	
	static Context mContext;
	static WindowManager mWm;
	
	static boolean mEnabled = Common.Pref.Def.ENABLED;
	static int mAnimationIndex = Common.Pref.Def.EFFECT;
	static int mAnimationSpeed = Common.Pref.Def.SPEED;
	static List<Integer> mRandomAnimList;
	
	static boolean mOnEnabled = Common.Pref.Def.ENABLED;
	static int mOnAnimationIndex = Common.Pref.Def.EFFECT;
	static int mOnAnimationSpeed = Common.Pref.Def.SPEED;
	static List<Integer> mOnRandomAnimList;

	public static boolean mShouldOverrideStockDelay = Common.Pref.Def.ENABLED;
	public static int mOverrideDelayValue = Common.Pref.Def.STOCK_DELAY;

	@Override
	public void initZygote(StartupParam startupParam) throws Throwable {
		sModRes = XModuleResources.createInstance(startupParam.modulePath, null);
		sPref = new XSharedPreferences(Common.PACKAGE_THIS, Common.Pref.PREF_MAIN);
		//sPref = getPref();

		if (Build.VERSION.SDK_INT == 26) {
			refreshSettings();
		}

	}

	//https://github.com/ElderDrivers/EdXposed/wiki/New-XSharedPreferences
	private static XSharedPreferences getPref() {
		XSharedPreferences pref = new XSharedPreferences(Common.PACKAGE_THIS, Pref.PREF_MAIN);
		return pref.getFile().canRead() ? pref : null;
	}
	
	@Override
	public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
		if (lpparam.packageName.equals(Common.PACKAGE_THIS)) {
			hookMainActivity(lpparam);
			//refreshSettings();
			return;
		}

		if (!lpparam.packageName.equals("android")) {
			return;
		}

		refreshSettings();

		ScreenshotUtil.setupCoreClasses(lpparam.classLoader);

		setupHooksAndroid4(lpparam);

		setupHooksAndroid5(lpparam);

	}

	private static void setupHooksAndroid5(LoadPackageParam lpparam) {
		try{
			// Disable android 5.0+ ColorFade  // by NUI
			XposedHelpers.findAndHookMethod(XposedHelpers.findClass("com.android.server.display.DisplayPowerState", lpparam.classLoader),
					"prepareColorFade", Context.class, int.class,
					XC_MethodReplacement.returnConstant(false));
			XposedHelpers.findAndHookMethod(XposedHelpers.findClass("com.android.server.display.ColorFade", lpparam.classLoader),
					"draw", float.class,
					XC_MethodReplacement.returnConstant(true));
			XposedHelpers.findAndHookMethod(XposedHelpers.findClass("com.android.server.display.ColorFade", lpparam.classLoader),
					"drawFaded", float.class, float.class, float.class, float.class,
					XC_MethodReplacement.returnConstant(null));
			XposedHelpers.findAndHookMethod(XposedHelpers.findClass("com.android.server.display.DisplayPowerState", lpparam.classLoader),
					"scheduleColorFadeDraw",
					XC_MethodReplacement.returnConstant(null));
			XposedHelpers.findAndHookMethod(XposedHelpers.findClass("com.android.server.display.DisplayPowerController", lpparam.classLoader),
					"animateScreenStateChange", int.class, boolean.class,
					new XC_MethodReplacement() {
						@Override
						protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
							int target = (Integer) param.args[0];
							switch(target)
							{
								case 2:
								case 3:
								case 4:
									break;
								default:
									param.args[1] = false;
									break;
							}

							return Utils.callOriginal(param);
						}
					}
			);
		} catch (Throwable e2) {
			Utils.log("Attempt to remove native screen off animation failed - " + e2.toString());
		}
	}

	private void setupHooksAndroid4(LoadPackageParam lpparam) {
		try { // late Android 4.2.1 onwards (built after Aug 15, 2012)
			final Class<?> hookClass = XposedHelpers.findClass(
					"com.android.server.power.PowerManagerService", lpparam.classLoader);
			XposedBridge.hookAllMethods(hookClass, "goToSleepInternal", sScreenOffHook);
			XposedBridge.hookAllMethods(hookClass, "goToSleepNoUpdateLocked", sScreenOffHook);
			if(Build.VERSION.SDK_INT <= 30) {
				//method removed on Android 12 or newer
				XposedBridge.hookAllMethods(hookClass, "wakeUpNoUpdateLocked", sScreenWakeHook);
			} else {
				XposedBridge.hookAllMethods(hookClass, "wakePowerGroupLocked", sScreenWakeHookNew);
			}
			XposedBridge.hookAllMethods(hookClass, "systemReady", sInitHookNew);
			hookDisableNativeScreenOffAnim(lpparam);
			Utils.log("Done hooks for PowerManagerService (New Package)");

			XResources.setSystemWideReplacement("android", "bool", "config_animateScreenLights", true);
		} catch (Throwable e) {
			setupAndroid4Fallback(lpparam);
		}
	}

	private void setupAndroid4Fallback(LoadPackageParam lpparam) {
		// Android 4.0 to Android 4.2.1 (built before Aug 15, 2012)
		// https://github.com/android/platform_frameworks_base/commit/9630704ed3b265f008a8f64ec60a33cf9dcd3345
		try {
			final Class<?> hookClass = XposedHelpers.findClass(
					"com.android.server.PowerManagerService", lpparam.classLoader);
			XposedBridge.hookAllMethods(hookClass, "setPowerState", sScreenOffHook);
			XposedBridge.hookAllMethods(hookClass, "sendNotificationLocked", sScreenWakeHook);
			XposedBridge.hookAllMethods(hookClass, "init", sInitHook);
			Utils.log("Done hooks for PowerManagerService (Old Package)");

			// Disable native screen off anim.
			XResources.setSystemWideReplacement("android", "bool", "config_animateScreenLights", true);
		} catch (Throwable e1) {}
	}

	private final XC_MethodHook sInitHook = new XC_MethodHook() {
		@Override
		protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
			XposedBridge.log("Initializing init hook");
			mContext = (Context) param.args[0];
			mWm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
			installBroadcast();
		}
	};

	private final XC_MethodHook sInitHookNew = new XC_MethodHook() {
		@Override
		protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
			XposedBridge.log("Initializing new init hook");

			Field field = param.thisObject.getClass().getDeclaredField("mContext");
			field.setAccessible(true);

			mContext = (Context) field.get(param.thisObject);
			mWm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
			installBroadcast();
		}
	};
	
	private final XC_MethodReplacement sScreenOffHook = new XC_MethodReplacement() {
		@Override
		protected Object replaceHookedMethod(final MethodHookParam param) throws Throwable {
			if (param.method.getName().equals("setPowerState")) {
				// Android 4.0 to Android 4.2.1 (built before Aug 15, 2012)
				if ((Integer) param.args[0] != 0) {
					return Utils.callOriginal(param);
				}
			} else if (param.method.getName().equals("goToSleepNoUpdateLocked")) {
				// reason != GO_TO_SLEEP_REASON_TIMEOUT
				if ((Integer) param.args[1] != 2) {
					return Utils.callOriginal(param);
				}

				if (!Utils.isValidSleepEvent(param.thisObject, (Long) param.args[0])) {
					return false;
				}
			}
			
			if (!mEnabled || mDontAnimate) {
				return Utils.callOriginal(param);
			}

			if (mContext == null) {
				// If the context cannot be retrieved from the init method,
				try {
					mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
					mWm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
					installBroadcast();
				} catch (Exception e){
					Utils.log("Context cannot be retrieved (backup method failed) - " + e.toString());
					e.printStackTrace();
				}
			}
			
			AnimImplementation anim = findAnimation(mAnimationIndex, false);
			if (anim != null && anim.supportsScreenOff() && !mAnimationRunning) {
				try {
					anim.anim_speed = mAnimationSpeed;
					anim.animateScreenOffWithHandler(mContext, mWm, param, sModRes);
				} catch (Exception e) {
					// So we don't crash system.
					Utils.toast(mContext, sModRes.getString(R.string.error_animating));
					Utils.log("Error with animateOnHandler", e);
				}
			} else {
				if (!mAnimationRunning) {
					return Utils.callOriginal(param);
				}
			}
			
			if (param.method.getName().equals("goToSleepNoUpdateLocked")) {
				return true;
			} else {
				return null;
			}
		}
	};
	
	private final XC_MethodHook sScreenWakeHook = new XC_MethodHook() {
		@Override
		protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
			if (!mOnEnabled) {
				return;
			}

			if (param.method.getName().equals("sendNotificationLocked")) {
				if ((Boolean) param.args[0] == false) {
					return;
				}
			} else if ((Boolean) param.getResult() == false) {
				// not updating state
				return;
			}

			if (mContext == null) {
				// If the context cannot be retrieved from the init method,
				try {
					mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
					mWm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
					installBroadcast();
				} catch (Exception e) {
					Utils.log("Context cannot be retrieved in wake (backup method failed) - "
							+ e.toString());
					e.printStackTrace();
				}
			}
			
			final AnimImplementation anim = findAnimation(mOnAnimationIndex, true);
			if (anim != null && anim.supportsScreenOn()) {
				try {
					anim.anim_speed = mOnAnimationSpeed;
					anim.animateScreenOnWithHandler(mContext, mWm, sModRes);
				} catch (Exception e) {
					// So we don't crash system.
					Utils.toast(mContext, sModRes.getString(R.string.error_animating));
				}
			}
		}
	};

	private final XC_MethodHook sScreenWakeHookNew = new XC_MethodHook() {
		@Override
		protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
			if (!mOnEnabled) {
				return;
			}

//			probably I should hook only this power group
//			final PowerGroup powerGroup = mPowerGroups.get(Display.DEFAULT_DISPLAY_GROUP);
//
//			final PowerGroup powerGroup, long eventTime,
//			            @WakeReason int reason, String details, int uid, String opPackageName, int opUid

//			Object powerGroup = (Object) param.args[0];
//
//			Field mWakefulnessField = powerGroup.getClass().getDeclaredField("mWakefulness");
//			mWakefulnessField.setAccessible(true);
//			int currentWakefulness = (int) mWakefulnessField.get(powerGroup);
//

//			I should probably enable this check to avoid unneeded animations,
//			but it's not working with... probably because of the after hook,
//			the screen will already be awake

//			if(currentWakefulness == 1) { // WAKEFULNESS_AWAKE = 1
//				return;
//			}


			final AnimImplementation anim = findAnimation(mOnAnimationIndex, true);
			if (anim != null && anim.supportsScreenOn()) {
				try {
					anim.anim_speed = mOnAnimationSpeed;
					anim.animateScreenOnWithHandler(mContext, mWm, sModRes);
				} catch (Exception e) {
					// So we don't crash system.
					Utils.toast(mContext, sModRes.getString(R.string.error_animating));
				}
			}
		}
	};

	
	private void hookMainActivity(LoadPackageParam lpp) {
		final Class<?> cls = XposedHelpers.findClass(MainActivity.class.getName(), lpp.classLoader);
		XposedBridge.hookAllMethods(cls, "isXposedRunning",
				XC_MethodReplacement.returnConstant(true));
	}
	
	private void hookDisableNativeScreenOffAnim(LoadPackageParam lpp) {
		try {
			Class.forName("com.android.server.power.ElectronBeam", false, lpp.classLoader);
			final Class<?> cls = XposedHelpers.findClass("com.android.server.power.ElectronBeam",
					lpp.classLoader);
			XposedHelpers.findAndHookMethod(cls, "prepare", int.class,
					XC_MethodReplacement.returnConstant(false));
		} catch (ClassNotFoundException e) {
			//it's no surprise if this don't work because Electron Beam is ancient, but I'm moving the catch to show only
			//if both fails, because people think that this is the reason the app is not working for them.
			try {
				Class.forName("com.android.server.display.ColorFade", false, lpp.classLoader);
				final Class<?> cls = XposedHelpers.findClass("com.android.server.display.ColorFade",
						lpp.classLoader);
				XposedHelpers.findAndHookMethod(cls, "prepare", int.class,
						XC_MethodReplacement.returnConstant(false));
			} catch (ClassNotFoundException e1) {
				Utils.log("Attempt to remove native screen off animation failed - " + e1.toString());
				Utils.log("Attempt to remove native screen off animation failed - " + e.toString());
				// MethodNotFoundException
			}
			// MethodNotFoundException
		}
	}
	
	/**
	 * Registers the broadcast for refreshing and testing the settings
	 */
	private void installBroadcast() {
		IntentFilter filter = new IntentFilter();
		filter.addAction(Common.BROADCAST_REFRESH_SETTINGS);
		filter.addAction(Common.BROADCAST_TEST_OFF_ANIMATION);
		filter.addAction(Common.BROADCAST_TEST_ON_ANIMATION);


		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			mContext.registerReceiver(broadcastReceiver, filter, RECEIVER_EXPORTED);
		}else {
			mContext.registerReceiver(broadcastReceiver, filter);
		}

	}

	BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context c, Intent i) {
			if (i.getAction().equals(Common.BROADCAST_TEST_OFF_ANIMATION)) {
				final int anim_id = i.getIntExtra(Common.EXTRA_TEST_ANIMATION,
						Common.Pref.Def.EFFECT);
				AnimImplementation anim = findAnimation(anim_id, false);
				if (anim != null) {
					try {
						anim.anim_speed = mAnimationSpeed;
						anim.animateScreenOffWithHandler(mContext, mWm, null, sModRes);
					} catch (Exception e) {
						// So we don't crash system.
						Utils.toast(mContext, sModRes.getString(R.string.error_animating));
					}
				}

			}else if (i.getAction().equals(Common.BROADCAST_TEST_ON_ANIMATION)) {
				final int anim_id = i.getIntExtra(Common.EXTRA_TEST_ANIMATION,
						Common.Pref.Def.EFFECT);
				AnimImplementation anim = findAnimation(anim_id, true);
				if (anim != null) {
					try {
						anim.anim_speed = mOnAnimationSpeed;
						anim.animateScreenOnWithHandler(mContext, mWm, sModRes);
					} catch (Exception e) {
						// So we don't crash system.
						Utils.toast(mContext, sModRes.getString(R.string.error_animating));
					}
				}

			} else if (i.getAction().equals(Common.BROADCAST_REFRESH_SETTINGS)) {
				refreshSettings();
			}
		}
	};
	
	private AnimImplementation findAnimation(int id, boolean on) {
		switch (id) {
		case Common.Anim.UNKNOWN:
		case Common.Anim.FADE:
			return new FadeOut();
		case Common.Anim.CRT:
			return new CRT();
		case Common.Anim.CRT_VERTICAL:
			return new CRTVertical();
		case Common.Anim.SCALE:
			return new ScaleDown();
		case Common.Anim.TV_BURN:
			return new TVBurnIn();
		case Common.Anim.LG_OPTIMUS_G:
			return new LGOptimusG();
		case Common.Anim.FADE_TILES:
			return new FadeTiles();
		case Common.Anim.VERTU_SIG_TOUCH:
			return new VertuSigTouch();
		case Common.Anim.LOLLIPOP_FADE_OUT:
			return new LollipopFadeOut();
		case Common.Anim.SCALE_BOTTOM:
			return new ScaleDownBottom();
		case Common.Anim.BOUNCE:
			return new Bounce();
		case Common.Anim.FLIP:
			return new Flip();
		case Common.Anim.WP8:
			return new WP8();
		case Common.Anim.FLIP_TILES:
			return new FlipTiles();
		case Common.Anim.SIDE_FILL:
				return new SideFill();
		case Common.Anim.RANDOM:
			try {
				if (on) {
					return findAnimation(Utils.getRandomIntFromList(mOnRandomAnimList), on);
				} else {
					return findAnimation(Utils.getRandomIntFromList(mRandomAnimList), on);
				}
			} catch (Throwable t) {
				// RuntimeException if user selects no animation.
				return null;
			}
		default:
			return null;
		}
	}
	
	private void refreshSettings() {

		if(sPref == null) {
			XposedBridge.log("Attempt to reload null sharedPreferences...");
			return;
		}

		sPref.reload();

		mEnabled = sPref.getBoolean(Common.Pref.Key.ENABLED, Common.Pref.Def.ENABLED);
		mAnimationIndex = sPref.getInt(Common.Pref.Key.EFFECT, Common.Pref.Def.EFFECT);
		mAnimationSpeed = sPref.getInt(Common.Pref.Key.SPEED, Common.Pref.Def.SPEED);
		mRandomAnimList = Utils.integerSplitByCommaToArrayList(
				sPref.getString(Pref.Key.RANDOM_LIST, Pref.Def.RANDOM_LIST));
		
		mOnEnabled = sPref.getBoolean(Common.Pref.Key.ON_ENABLED, Common.Pref.Def.ENABLED);
		mOnAnimationIndex = sPref.getInt(Common.Pref.Key.ON_EFFECT, Common.Pref.Def.EFFECT);
		mOnAnimationSpeed = sPref.getInt(Common.Pref.Key.ON_SPEED, Common.Pref.Def.SPEED);
		mOnRandomAnimList = Utils.integerSplitByCommaToArrayList(
				sPref.getString(Pref.Key.ON_RANDOM_LIST, Pref.Def.RANDOM_LIST));

		mShouldOverrideStockDelay = sPref.getBoolean(Pref.Key.ENABLE_DEFAULT_DELAY_OVERRIDE, Common.Pref.Def.ENABLED);
		mOverrideDelayValue = sPref.getInt(Pref.Key.DELAY_OVERRIDE_SPEED, Pref.Def.STOCK_DELAY);

		mAnimationRunning = false;

	}
}
