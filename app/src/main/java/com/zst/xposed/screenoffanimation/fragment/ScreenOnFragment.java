package com.zst.xposed.screenoffanimation.fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import com.zst.xposed.screenoffanimation.Common;
import com.zst.xposed.screenoffanimation.Common.Pref;
import com.zst.xposed.screenoffanimation.R;
import com.zst.xposed.screenoffanimation.widgets.EffectsCheckList;
import com.zst.xposed.screenoffanimation.widgets.IntervalSeekBar;
import com.zst.xposed.screenoffanimation.widgets.OnEffectsListView;

import java.util.ArrayList;
import java.util.List;

public class ScreenOnFragment extends ScreenOffFragment {
	
	private static ScreenOnFragment sScreenOnFragmentInstance;

	Switch mShouldOverrideStockDelay;
    TextView mDelaySpeedText;
    IntervalSeekBar mSeekDelaySpeed;

	private Activity parentActivity;

	public ScreenOnFragment(Activity activity) {
		super(activity);
		parentActivity = activity;
	}


	public static ScreenOnFragment getInstance() {
		if (sScreenOnFragmentInstance == null) {
			sScreenOnFragmentInstance = new ScreenOnFragment(null);
		}
		return sScreenOnFragmentInstance;
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle bundle) {
		ScrollView sv = (ScrollView) inflater.inflate(R.layout.view_scroller, container, false);
		inflater.inflate(R.layout.view_screen_on, sv, true);
		return sv;
	}
	
	@SuppressLint("WorldReadableFiles")
	@SuppressWarnings("deprecation")
	@Override
	public void onViewCreated(View v, Bundle savedInstanceState) {
		mPref = getActivity().getSharedPreferences(Pref.PREF_MAIN, Context.MODE_PRIVATE);
		
		mSettingsLayout = (ViewGroup) v.findViewById(R.id.layout_on_anim);
		
		mSwitchEnabled = (Switch) v.findViewById(R.id.switch_wake_enable);
		mSwitchEnabled.setOnCheckedChangeListener(new OnCheckedChangeListener(){
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				mPref.edit().putBoolean(Pref.Key.ON_ENABLED, isChecked).commit();
				updateSettings();
			}
		});
		
		mTextSpeed = (TextView) v.findViewById(R.id.tV_wake_speed_value);
		mSeekSpeed = (IntervalSeekBar) v.findViewById(R.id.seekBar_wake_speed);
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
		
		v.findViewById(R.id.select_wake_anim_button).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				final AlertDialog dialog = new
						AlertDialog.Builder(getActivity()).create();
				dialog.setView(new OnEffectsListView(getActivity(), mCurrentAnim) {
					@Override
					public void onSelectEffect(int animId) {
						mPref.edit().putInt(Pref.Key.ON_EFFECT, animId).commit();
						dialog.dismiss();
						updateSettings();
						
						if (animId == Common.Anim.RANDOM) {
							ViewGroup vg = (ViewGroup) getActivity().getLayoutInflater()
									.inflate(R.layout.dialog_random_list, null);
							vg.addView(new EffectsCheckList(getActivity(), mRandomAnimList, true) {
								@Override
								public void onChangeCheck(List<Integer> list) {
									mRandomAnimList = list;
									if (list.size() == 0) {
										mPref.edit().putString(Pref.Key.ON_RANDOM_LIST, "").commit();
									} else {
										StringBuilder str_list = new StringBuilder();
										for (int i : list) {
											str_list.append(i + ",");
										}
										mPref.edit().putString(Pref.Key.ON_RANDOM_LIST,
												str_list.toString()).commit();
										getActivity().sendBroadcast(new Intent(Common.BROADCAST_REFRESH_SETTINGS));
									}
								}
							});
							new AlertDialog.Builder(getActivity())
								.setView(vg)
								.show();
						}
					}
				});
				dialog.show();
			}
		});
		
		v.findViewById(R.id.preview_wake_button).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				previewEffect(false);
			}
		});


        mShouldOverrideStockDelay = v.findViewById(R.id.switch_enable_custom_delay);
        mShouldOverrideStockDelay.setOnCheckedChangeListener(new OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mPref.edit().putBoolean(Pref.Key.ENABLE_DEFAULT_DELAY_OVERRIDE, isChecked).commit();
                updateSettings();
            }
        });

        mDelaySpeedText = v.findViewById(R.id.tV_custom_delay_speed_value);
        mSeekDelaySpeed = v.findViewById(R.id.seekBar_custom_delay);

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
	
	@Override
	void previewEffect(boolean on) {
		Intent i = new Intent(Common.BROADCAST_TEST_ON_ANIMATION);
		i.putExtra(Common.EXTRA_TEST_ANIMATION, mCurrentAnim);
		parentActivity.sendBroadcast(i);
	}
	
	@Override
	public void loadPref() {
		final boolean enabled = mPref.getBoolean(Pref.Key.ON_ENABLED, Pref.Def.ENABLED);
		final int speed = mPref.getInt(Pref.Key.ON_SPEED, Pref.Def.SPEED);
		
		mCurrentAnim = mPref.getInt(Pref.Key.ON_EFFECT, Pref.Def.EFFECT);

		mRandomAnimList = new ArrayList<Integer>();
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
