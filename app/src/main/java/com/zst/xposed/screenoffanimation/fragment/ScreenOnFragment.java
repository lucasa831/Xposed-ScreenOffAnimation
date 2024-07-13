package com.zst.xposed.screenoffanimation.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.zst.xposed.screenoffanimation.Common;
import com.zst.xposed.screenoffanimation.Common.Pref;
import com.zst.xposed.screenoffanimation.R;
import com.zst.xposed.screenoffanimation.widgets.EffectsCheckList;
import com.zst.xposed.screenoffanimation.widgets.IntervalSeekBar;
import com.zst.xposed.screenoffanimation.widgets.OnEffectsListView;

import java.util.ArrayList;
import java.util.List;

public class ScreenOnFragment extends Fragment {
	

	Switch mShouldOverrideStockDelay;
    TextView mDelaySpeedText;
    IntervalSeekBar mSeekDelaySpeed;

	private Activity parentActivity;
	private boolean isXposedRunning;
	private View view;
	private boolean contextInitialized = false;

	SharedPreferences mPref;
	int mCurrentAnim;
	List<Integer> mRandomAnimList;

	Switch mSwitchEnabled;
	ViewGroup mSettingsLayout;
	TextView mTextSpeed;
	IntervalSeekBar mSeekSpeed;

	public ScreenOnFragment() {
		super();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle bundle) {
		ScrollView sv = (ScrollView) inflater.inflate(R.layout.view_scroller, container, false);
		inflater.inflate(R.layout.view_screen_on, sv, true);
		return sv;
	}

	@Override
	public void onViewCreated(View v, Bundle savedInstanceState) {
		this.view = v;
		initializeFragment();
	}

	public void provideContext(Activity activity, boolean isXposedRunning) {
		this.parentActivity = activity;
		this.isXposedRunning = isXposedRunning;
		this.contextInitialized = true;

		initializeFragment();
	}

	private void initializeFragment() {

		if(view == null || !contextInitialized) {
			return;
		}
		
		mSettingsLayout = view.findViewById(R.id.layout_on_anim);

		mSwitchEnabled = view.findViewById(R.id.switch_wake_enable);

		if(isXposedRunning) {
			mPref = parentActivity.getSharedPreferences(Pref.PREF_MAIN, Context.MODE_PRIVATE);

			mSwitchEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
                mPref.edit().putBoolean(Pref.Key.ON_ENABLED, isChecked).commit();
                updateSettings();
            });

		} else {
			mSwitchEnabled.setOnTouchListener((v, event) -> {
                Toast.makeText(parentActivity, "Module must be activated", Toast.LENGTH_SHORT).show();
                mSwitchEnabled.setChecked(true); //is off for some reason
                return false;
            });
		}

		mTextSpeed = view.findViewById(R.id.tV_wake_speed_value);
		mSeekSpeed = view.findViewById(R.id.seekBar_wake_speed);
		mSeekSpeed.setAttr(2000, 100, 10);
		mSeekSpeed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			boolean mFromUser;
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				if (mFromUser) {
					mPref.edit().putInt(Pref.Key.ON_SPEED, mSeekSpeed.getRealProgress()).commit();
					updateSettings();
				}
			}
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {}
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				mFromUser = fromUser;
				mTextSpeed.setText(mSeekSpeed.getRealProgress() + " ms");
			}
		});

		view.findViewById(R.id.select_wake_anim_button).setOnClickListener(v -> {
            final AlertDialog dialog = new
                    AlertDialog.Builder(parentActivity).create();
            dialog.setView(new OnEffectsListView(parentActivity, mCurrentAnim) {
                @Override
                public void onSelectEffect(int animId) {
                    mPref.edit().putInt(Pref.Key.ON_EFFECT, animId).commit();
                    dialog.dismiss();
                    updateSettings();

                    if (animId == Common.Anim.RANDOM) {
                        ViewGroup vg = (ViewGroup) parentActivity.getLayoutInflater()
                                .inflate(R.layout.dialog_random_list, null);
                        vg.addView(new EffectsCheckList(parentActivity, mRandomAnimList, true) {
                            @Override
                            public void onChangeCheck(List<Integer> list) {
                                mRandomAnimList = list;
                                if (list.isEmpty()) {
                                    mPref.edit().putString(Pref.Key.ON_RANDOM_LIST, "").commit();
                                } else {
                                    StringBuilder str_list = new StringBuilder();
                                    for (int i : list) {
                                        str_list.append(i + ",");
                                    }
                                    mPref.edit().putString(Pref.Key.ON_RANDOM_LIST,
                                            str_list.toString()).commit();
                                    parentActivity.sendBroadcast(new Intent(Common.BROADCAST_REFRESH_SETTINGS));
                                }
                            }
                        });
                        new AlertDialog.Builder(parentActivity)
                            .setView(vg)
                            .show();
                    }
                }
            });
            dialog.show();
        });

		view.findViewById(R.id.preview_wake_button).setOnClickListener(v -> previewEffect());


		mShouldOverrideStockDelay = view.findViewById(R.id.switch_enable_custom_delay);
		mShouldOverrideStockDelay.setOnCheckedChangeListener((buttonView, isChecked) -> {
            mPref.edit().putBoolean(Pref.Key.ENABLE_DEFAULT_DELAY_OVERRIDE, isChecked).commit();
            updateSettings();
        });

		mDelaySpeedText = view.findViewById(R.id.tV_custom_delay_speed_value);
		mSeekDelaySpeed = view.findViewById(R.id.seekBar_custom_delay);

		mSeekDelaySpeed.setAttr(2000, 100, 10);
		mSeekDelaySpeed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			boolean mFromUser;
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				if (mFromUser) {
					mPref.edit().putInt(Pref.Key.DELAY_OVERRIDE_SPEED, mSeekDelaySpeed.getRealProgress()).commit();
					updateSettings();
				}
			}
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {}
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				mFromUser = fromUser;
				mDelaySpeedText.setText(mSeekDelaySpeed.getRealProgress() + " ms");
			}
		});

		loadPref();
	}

	void updateSettings() {
		parentActivity.sendBroadcast(new Intent(Common.BROADCAST_REFRESH_SETTINGS));
		loadPref();
	}

	void previewEffect() {
		Intent i = new Intent(Common.BROADCAST_TEST_ON_ANIMATION);
		i.putExtra(Common.EXTRA_TEST_ANIMATION, mCurrentAnim);
		parentActivity.sendBroadcast(i);
	}
	
	public void loadPref() {
		if(mPref == null) {
			mSettingsLayout.setVisibility(View.GONE);
			return;
		}

		final boolean enabled = mPref.getBoolean(Pref.Key.ON_ENABLED, Pref.Def.ENABLED);
		final int speed = mPref.getInt(Pref.Key.ON_SPEED, Pref.Def.SPEED);
		
		mCurrentAnim = mPref.getInt(Pref.Key.ON_EFFECT, Pref.Def.EFFECT);

		mRandomAnimList = new ArrayList<>();
		String randomAnimString =  mPref.getString(Pref.Key.ON_RANDOM_LIST, Pref.Def.RANDOM_LIST);
		if (!TextUtils.isEmpty(randomAnimString)) {
			for (String item : randomAnimString.split(",")) {
				if (!TextUtils.isEmpty(item))
					mRandomAnimList.add(Integer.parseInt(item));
			}
		}
		
		mSwitchEnabled.setChecked(enabled);
		mSettingsLayout.setVisibility(enabled ? View.VISIBLE : View.GONE);
		mSeekSpeed.setRealProgress(speed);
		mTextSpeed.setText(speed + " ms");


		final boolean shouldOverrideStockDelay = mPref.getBoolean(Pref.Key.ENABLE_DEFAULT_DELAY_OVERRIDE, Pref.Def.ENABLED);
        final int delaySpeedOverride = mPref.getInt(Pref.Key.DELAY_OVERRIDE_SPEED, Pref.Def.STOCK_DELAY);

        mShouldOverrideStockDelay.setChecked(shouldOverrideStockDelay);
        mSeekDelaySpeed.setRealProgress(delaySpeedOverride);
        mDelaySpeedText.setText(delaySpeedOverride + " ms");
	}
}
