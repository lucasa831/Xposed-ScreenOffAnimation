package com.zst.xposed.screenoffanimation.fragment;

import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
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
import android.widget.CompoundButton;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.zst.xposed.screenoffanimation.Common;
import com.zst.xposed.screenoffanimation.R;
import com.zst.xposed.screenoffanimation.Common.Pref;
import com.zst.xposed.screenoffanimation.widgets.EffectsListView;
import com.zst.xposed.screenoffanimation.widgets.EffectsCheckList;
import com.zst.xposed.screenoffanimation.widgets.IntervalSeekBar;

public class ScreenOffFragment extends Fragment {
	
	private static ScreenOffFragment sScreenOffFragmentInstance;

	private Activity parentActivity;

	public ScreenOffFragment(Activity activity) {
		super();
		parentActivity = activity;
	}

	public static ScreenOffFragment getInstance() {
		if (sScreenOffFragmentInstance == null) {
			sScreenOffFragmentInstance = new ScreenOffFragment(null);
		}
		return sScreenOffFragmentInstance;
	}
	
	SharedPreferences mPref;
	int mCurrentAnim;
	List<Integer> mRandomAnimList;
	
	Switch mSwitchEnabled;
	ViewGroup mSettingsLayout;
	TextView mTextSpeed;
	IntervalSeekBar mSeekSpeed;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle bundle) {
		ScrollView sv = (ScrollView) inflater.inflate(R.layout.view_scroller, container, false);
		inflater.inflate(R.layout.view_screen_off, sv, true);
		return sv;
	}
	
	@SuppressLint("WorldReadableFiles")
	@SuppressWarnings("deprecation")
	@Override
	public void onViewCreated(View v, Bundle savedInstanceState) {
		mPref = getActivity().getSharedPreferences(Pref.PREF_MAIN, Context.MODE_PRIVATE);

		mSettingsLayout = (ViewGroup) v.findViewById(R.id.layout_off_anim);
		
		mSwitchEnabled = (Switch) v.findViewById(R.id.switch_enable);
		mSwitchEnabled.setOnCheckedChangeListener(new OnCheckedChangeListener(){
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				mPref.edit().putBoolean(Pref.Key.ENABLED, isChecked).commit();
				updateSettings();
			}
		});
		
		mTextSpeed = (TextView) v.findViewById(R.id.tV_speed_value);
		mSeekSpeed = (IntervalSeekBar) v.findViewById(R.id.seekBar_speed);
		mSeekSpeed.setAttr(2000, 100, 10);
		mSeekSpeed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			boolean mFromUser;
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				if (mFromUser) {
					mPref.edit().putInt(Pref.Key.SPEED, mSeekSpeed.getRealProgress()).commit();
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
		
		v.findViewById(R.id.select_anim_button).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				final AlertDialog dialog = new
						AlertDialog.Builder(getActivity()).create();
				dialog.setView(new EffectsListView(getActivity(), mCurrentAnim) {
					@Override
					public void onSelectEffect(int animId) {
						mPref.edit().putInt(Pref.Key.EFFECT, animId).commit();
						dialog.dismiss();
						updateSettings();
						
						if (animId == Common.Anim.RANDOM) {
							ViewGroup vg = (ViewGroup) getActivity().getLayoutInflater()
									.inflate(R.layout.dialog_random_list, null);
							vg.addView(new EffectsCheckList(getActivity(), mRandomAnimList, false) {
								@Override
								public void onChangeCheck(List<Integer> list) {
									mRandomAnimList = list;
									if (list.size() == 0) {
										mPref.edit().putString(Pref.Key.RANDOM_LIST, "").commit();
									} else {
										StringBuilder str_list = new StringBuilder();
										for (int i : list) {
											str_list.append(i + ",");
										}
										mPref.edit().putString(Pref.Key.RANDOM_LIST,
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
		
		v.findViewById(R.id.preview_button).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				previewEffect(false);
			}
		});
		
		loadPref();
	}
	
	void updateSettings() {
		parentActivity.sendBroadcast(new Intent(Common.BROADCAST_REFRESH_SETTINGS));
		loadPref();
	}
	
	void previewEffect(boolean on) {
		Intent i = new Intent(Common.BROADCAST_TEST_OFF_ANIMATION);
		i.putExtra(Common.EXTRA_TEST_ANIMATION, mCurrentAnim);
		parentActivity.sendBroadcast(i);
	}
	
	public void loadPref() {
		final boolean enabled = mPref.getBoolean(Pref.Key.ENABLED, Pref.Def.ENABLED);
		final int speed = mPref.getInt(Pref.Key.SPEED, Pref.Def.SPEED);
		
		mCurrentAnim = mPref.getInt(Pref.Key.EFFECT, Pref.Def.EFFECT);
		
		mRandomAnimList = new ArrayList<Integer>();
		String randomAnimString =  mPref.getString(Pref.Key.RANDOM_LIST, Pref.Def.RANDOM_LIST);
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
	}
}
