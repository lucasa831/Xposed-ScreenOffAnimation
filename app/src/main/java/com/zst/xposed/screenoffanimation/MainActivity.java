package com.zst.xposed.screenoffanimation;

import com.zst.xposed.screenoffanimation.Common.Pref;
import com.zst.xposed.screenoffanimation.fragment.ScreenOffFragment;
import com.zst.xposed.screenoffanimation.fragment.ScreenOnFragment;

import android.content.Context;
import android.os.Bundle;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.PagerTabStrip;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

public class MainActivity extends Activity {
	
	final static int MENU_RESET_SETTINGS = 0x100;
	
	SharedPreferences mPref;
	FragmentPagerAdapter mAdapter;

	private ScreenOffFragment sScreenOffFragmentInstance;
	private ScreenOnFragment sScreenOnFragmentInstance;

	@SuppressLint("WorldReadableFiles")
	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_viewpager);

		if(isXposedRunning()) {
			//will fail if not...
			mPref = getSharedPreferences(Pref.PREF_MAIN, Context.MODE_WORLD_READABLE);
		}

		setup();
	}
	
	/**
	 * This will be overriden in MainXposed.java when the module is active
	 */
	private boolean isXposedRunning() {
		return false;
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if(!isXposedRunning()) {
			return true;
		}

		menu.add(Menu.NONE, MENU_RESET_SETTINGS, 0, R.string.menu_reset_settings_title);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_RESET_SETTINGS:
			DialogInterface.OnClickListener click = (dialog, which) -> {
                mPref.edit().clear().commit();
                dialog.dismiss();
                sScreenOffFragmentInstance.loadPref();
                sScreenOnFragmentInstance.loadPref();
            };
			new AlertDialog.Builder(this)
					.setMessage(getResources().getString(R.string.menu_reset_settings_dialog))
					.setPositiveButton(android.R.string.yes, click)
					.setNegativeButton(android.R.string.no, null)
					.show();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void setup() {
		findViewById(R.id.xposed_inactive).setVisibility(isXposedRunning() ? View.GONE : View.VISIBLE);

		final Activity activity = this;
		sScreenOffFragmentInstance = new ScreenOffFragment();
		sScreenOffFragmentInstance.provideContext(activity, isXposedRunning());

		sScreenOnFragmentInstance = new ScreenOnFragment();
		sScreenOnFragmentInstance.provideContext(activity, isXposedRunning());

		mAdapter = new FragmentPagerAdapter(getFragmentManager()) {
			@Override
			@SuppressLint("ValidFragment")
			public Fragment getItem(int position) {
				switch (position) {
				case 0:
					return sScreenOffFragmentInstance;
				case 1:
					return sScreenOnFragmentInstance;
				}
				return new Fragment();
			}
			
			@Override
			public String getPageTitle(int pos) {
				switch (pos) {
				case 0:
					return getResources().getString(R.string.setting_anim_off);
				case 1:
					return getResources().getString(R.string.setting_anim_on);
				}
				return "";
			}
			
			@Override
			public int getCount() {
				return 2;
			}
		};
		ViewPager viewPager = findViewById(R.id.view_pager);
		viewPager.setAdapter(mAdapter);
		
		PagerTabStrip pts = findViewById(R.id.pager_title_strip);
		pts.setTabIndicatorColor(getResources().getColor(R.color.theme_color));
		pts.setDrawFullUnderline(false);
		pts.setTextColor(getResources().getColor(R.color.theme_color));
		pts.setBackgroundColor(Color.TRANSPARENT);
	}
}