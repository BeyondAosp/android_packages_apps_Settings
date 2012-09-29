/*
 * Copyright (C) 2012 The CyanogenMod Project
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

package com.android.settings.cyanogenmod;

import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;
import android.view.IWindowManager;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

public class StatusBar extends SettingsPreferenceFragment implements OnPreferenceChangeListener {

    private static final String STATUS_BAR_AM_PM = "status_bar_am_pm";
    private static final String STATUS_BAR_BATTERY = "status_bar_battery";
    private static final String STATUS_BAR_CLOCK = "status_bar_show_clock";
    private static final String STATUS_BAR_BRIGHTNESS_CONTROL = "status_bar_brightness_control";
    private static final String STATUS_BAR_SIGNAL = "status_bar_signal";
    private static final String COMBINED_BAR_AUTO_HIDE = "combined_bar_auto_hide";
    private static final String COMBINED_BAR_AUTO_HIDE_TIMEOUT = "combined_bar_auto_hide_timeout";
    private static final String STATUS_BAR_NOTIF_COUNT = "status_bar_notif_count";
    private static final String STATUS_BAR_CATEGORY_GENERAL = "status_bar_general";

    private static final String KEY_TABLET_MODE = "tablet_mode";
    private static final String KEY_TABLET_UI = "tablet_ui";
    private static final String KEY_TABLET_FLIPPED = "tablet_flipped";
    private static final String KEY_NAVIGATION_CONTROLS = "navigation_controls";
    private static final String COMBINED_BAR_NAVIGATION_FORCE_MENU =
            "combined_bar_navigation_force_menu";
    private static final String STATUS_BAR_CLOCK_COLOR = "status_bar_clock_color";
    private static final String COMBINED_BAR_NAVIGATION_COLOR = "combined_bar_navigation_color";
    private static final String COMBINED_BAR_NAVIGATION_GLOW = "combined_bar_navigation_glow";
    private static final String COMBINED_BAR_NAVIGATION_GLOW_COLOR =
            "combined_bar_navigation_glow_color";
    private static final String COMBINED_BAR_NAVIGATION_QUICK_GLOW =
            "combined_bar_navigation_quick_glow";

    private ListPreference mStatusBarAmPm;
    private ListPreference mStatusBarBattery;
    private ListPreference mStatusBarCmSignal;
    private CheckBoxPreference mStatusBarClock;
    private CheckBoxPreference mStatusBarBrightnessControl;
    private CheckBoxPreference mCombinedBarAutoHide;
    private CheckBoxPreference mStatusBarNotifCount;
    private PreferenceCategory mPrefCategoryGeneral;

    private CheckBoxPreference mTabletMode;
    private CheckBoxPreference mTabletUI;
    private CheckBoxPreference mTabletFlipped;
    private CheckBoxPreference mNavigationControls;
    private CheckBoxPreference mCombinedBarNavigationForceMenu;
    private Preference mStatusBarClockColor;
    private CheckBoxPreference mCombinedBarNavigationGlow;
    private CheckBoxPreference mCombinedBarNavigationQuickGlow;
    private Preference mCombinedBarNavigationGlowColor;
    private Preference mCombinedBarNavigationColor;
    private SeekBarPreference mCombinedBarTimeout;

    private ContentResolver mContentResolver;
    private Context mContext;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.status_bar);

        PreferenceScreen prefSet = getPreferenceScreen();

        mContext = getActivity().getApplicationContext();
        mContentResolver = mContext.getContentResolver();

        mStatusBarClock = (CheckBoxPreference) prefSet.findPreference(STATUS_BAR_CLOCK);
        mStatusBarBrightnessControl = (CheckBoxPreference) prefSet.findPreference(STATUS_BAR_BRIGHTNESS_CONTROL);
        mStatusBarAmPm = (ListPreference) prefSet.findPreference(STATUS_BAR_AM_PM);
        mStatusBarBattery = (ListPreference) prefSet.findPreference(STATUS_BAR_BATTERY);
        mCombinedBarAutoHide = (CheckBoxPreference) prefSet.findPreference(COMBINED_BAR_AUTO_HIDE);
        mStatusBarCmSignal = (ListPreference) prefSet.findPreference(STATUS_BAR_SIGNAL);
        mCombinedBarNavigationForceMenu =
                (CheckBoxPreference) prefSet.findPreference(COMBINED_BAR_NAVIGATION_FORCE_MENU);
        mStatusBarClockColor = (Preference) prefSet.findPreference(STATUS_BAR_CLOCK_COLOR);
        mCombinedBarNavigationGlow =
                (CheckBoxPreference) prefSet.findPreference(COMBINED_BAR_NAVIGATION_GLOW);
        mCombinedBarNavigationQuickGlow =
                (CheckBoxPreference) prefSet.findPreference(COMBINED_BAR_NAVIGATION_QUICK_GLOW);
        mCombinedBarNavigationGlowColor =
                (Preference) prefSet.findPreference(COMBINED_BAR_NAVIGATION_GLOW_COLOR);
        mCombinedBarNavigationColor =
                (Preference) prefSet.findPreference(COMBINED_BAR_NAVIGATION_COLOR);
        mStatusBarNotifCount = (CheckBoxPreference) prefSet.findPreference(STATUS_BAR_NOTIF_COUNT);
        mTabletMode = (CheckBoxPreference) findPreference(KEY_TABLET_MODE);
        mTabletUI = (CheckBoxPreference) findPreference(KEY_TABLET_UI);
        mTabletFlipped = (CheckBoxPreference) findPreference(KEY_TABLET_FLIPPED);
        mNavigationControls = (CheckBoxPreference) findPreference(KEY_NAVIGATION_CONTROLS);
        mPrefCategoryGeneral = (PreferenceCategory) findPreference(STATUS_BAR_CATEGORY_GENERAL);

        mStatusBarAmPm.setOnPreferenceChangeListener(this);
        mStatusBarBattery.setOnPreferenceChangeListener(this);
        mStatusBarCmSignal.setOnPreferenceChangeListener(this);

        mCombinedBarTimeout = (SeekBarPreference) prefSet.findPreference(COMBINED_BAR_AUTO_HIDE_TIMEOUT);
        mCombinedBarTimeout.setDefault(Settings.System.getInt(getActivity().getApplicationContext()
                .getContentResolver(), Settings.System.FULLSCREEN_TIMEOUT, 2));
        mCombinedBarTimeout.setOnPreferenceChangeListener(this);

    }

    public void onResume() {
        super.onResume();

        PreferenceScreen prefSet = getPreferenceScreen();

        mStatusBarClock.setChecked((Settings.System.getInt(mContentResolver,
                Settings.System.STATUS_BAR_CLOCK, 1) == 1));
        mStatusBarBrightnessControl.setChecked((Settings.System.getInt(mContentResolver,
                Settings.System.STATUS_BAR_BRIGHTNESS_CONTROL, 0) == 1));

        try {
            if (Settings.System.getInt(mContentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE)
                    == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
                mStatusBarBrightnessControl.setEnabled(false);
                mStatusBarBrightnessControl.setSummary(R.string.status_bar_toggle_info);
            }
        } catch (SettingNotFoundException e) {
        }

        try {
            if (Settings.System.getInt(mContentResolver,
                    Settings.System.TIME_12_24) == 24) {
                mStatusBarAmPm.setEnabled(false);
                mStatusBarAmPm.setSummary(R.string.status_bar_am_pm_info);
            }
        } catch (SettingNotFoundException e ) {
        }

        int statusBarAmPm = Settings.System.getInt(mContentResolver,
                Settings.System.STATUS_BAR_AM_PM, 2);
        mStatusBarAmPm.setValue(String.valueOf(statusBarAmPm));
        mStatusBarAmPm.setSummary(mStatusBarAmPm.getEntry());

        int statusBarBattery = Settings.System.getInt(mContentResolver,
                Settings.System.STATUS_BAR_BATTERY, 0);
        mStatusBarBattery.setValue(String.valueOf(statusBarBattery));
        mStatusBarBattery.setSummary(mStatusBarBattery.getEntry());

        int signalStyle = Settings.System.getInt(mContentResolver,
                Settings.System.STATUS_BAR_SIGNAL_TEXT, 0);
        mStatusBarCmSignal.setValue(String.valueOf(signalStyle));
        mStatusBarCmSignal.setSummary(mStatusBarCmSignal.getEntry());

        mCombinedBarAutoHide.setChecked((Settings.System.getInt(mContentResolver,
                Settings.System.FULLSCREEN_MODE, 0) == 1));

        mStatusBarNotifCount.setChecked((Settings.System.getInt(mContentResolver,
                Settings.System.STATUS_BAR_NOTIF_COUNT, 0) == 1));

        mTabletMode.setChecked(Settings.System.getInt(mContentResolver,
                        Settings.System.TABLET_MODE, 0) > 0);

        mTabletUI.setChecked(Settings.System.getInt(mContentResolver,
                        Settings.System.TABLET_MODE, 0) == 2);

        mTabletFlipped.setChecked(Settings.System.getInt(mContentResolver,
                        Settings.System.TABLET_FLIPPED, 0) == 1);

        mNavigationControls.setChecked(Settings.System.getInt(mContentResolver,
                        Settings.System.NAVIGATION_CONTROLS, 1) == 1);

        mCombinedBarNavigationForceMenu.setChecked((Settings.System.getInt(mContentResolver,
                Settings.System.FORCE_SOFT_MENU_BUTTON, 0) == 1));

        mCombinedBarNavigationGlow.setChecked((Settings.System.getInt(mContentResolver,
                Settings.System.COMBINED_BAR_NAVIGATION_GLOW, 1) == 1));
        mCombinedBarNavigationQuickGlow.setChecked((Settings.System.getInt(mContentResolver,
                Settings.System.COMBINED_BAR_NAVIGATION_GLOW_TIME, 0) == 1));

        if (Utils.isHybrid(mContext)) {
            mTabletUI.setEnabled(mTabletMode.isChecked());
            mTabletFlipped.setEnabled(mTabletMode.isChecked());
            mStatusBarBrightnessControl.setEnabled(!mTabletMode.isChecked());
        }

        if (Utils.isTablet(mContext)) {
            mPrefCategoryGeneral.removePreference(mStatusBarBrightnessControl);
            mPrefCategoryGeneral.removePreference(mTabletMode);
            mPrefCategoryGeneral.removePreference(mTabletUI);
            mTabletFlipped.setEnabled(true);
        }

        IWindowManager windowManager = IWindowManager.Stub.asInterface(
                ServiceManager.getService(Context.WINDOW_SERVICE));
        try {
            if (windowManager.hasNavigationBar()) {
                mCombinedBarNavigationForceMenu.setEnabled(false);
            }
        } catch (RemoteException e) {
        }

        mStatusBarCmSignal.setEnabled(!mTabletMode.isChecked());
        mCombinedBarTimeout.setSummary(String.valueOf(mCombinedBarTimeout.getDefault()));
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mStatusBarAmPm) {
            int statusBarAmPm = Integer.valueOf((String) newValue);
            int index = mStatusBarAmPm.findIndexOfValue((String) newValue);
            Settings.System.putInt(mContentResolver,
                    Settings.System.STATUS_BAR_AM_PM, statusBarAmPm);
            mStatusBarAmPm.setSummary(mStatusBarAmPm.getEntries()[index]);
            return true;
        } else if (preference == mStatusBarBattery) {
            int statusBarBattery = Integer.valueOf((String) newValue);
            int index = mStatusBarBattery.findIndexOfValue((String) newValue);
            Settings.System.putInt(mContentResolver,
                    Settings.System.STATUS_BAR_BATTERY, statusBarBattery);
            mStatusBarBattery.setSummary(mStatusBarBattery.getEntries()[index]);
            return true;
        } else if (preference == mStatusBarCmSignal) {
            int signalStyle = Integer.valueOf((String) newValue);
            int index = mStatusBarCmSignal.findIndexOfValue((String) newValue);
            Settings.System.putInt(mContentResolver,
                    Settings.System.STATUS_BAR_SIGNAL_TEXT, signalStyle);
            mStatusBarCmSignal.setSummary(mStatusBarCmSignal.getEntries()[index]);
            return true;
        } else if (preference == mCombinedBarTimeout) {
            int value = (Integer) newValue;
            Settings.System.putInt(mContentResolver, Settings.System.FULLSCREEN_TIMEOUT, value);
            mCombinedBarTimeout.setSummary(String.valueOf(value));
        }
        return false;
    }

    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        boolean value;

        if (preference == mStatusBarClock) {
            value = mStatusBarClock.isChecked();
            Settings.System.putInt(mContentResolver,
                    Settings.System.STATUS_BAR_CLOCK, value ? 1 : 0);
            return true;
        } else if (preference == mStatusBarBrightnessControl) {
            value = mStatusBarBrightnessControl.isChecked();
            Settings.System.putInt(mContentResolver,
                    Settings.System.STATUS_BAR_BRIGHTNESS_CONTROL, value ? 1 : 0);
            return true;
        } else if (preference == mCombinedBarAutoHide) {
            value = mCombinedBarAutoHide.isChecked();
            Settings.System.putInt(mContentResolver,
                    Settings.System.FULLSCREEN_MODE, value ? 1 : 0);
            return true;
        } else if (preference == mStatusBarNotifCount) {
            value = mStatusBarNotifCount.isChecked();
            Settings.System.putInt(mContentResolver,
                    Settings.System.STATUS_BAR_NOTIF_COUNT, value ? 1 : 0);
            return true;
        } else if (preference == mTabletMode) {
            value = mTabletMode.isChecked();
            Settings.System.putInt(getContentResolver(), Settings.System.TABLET_MODE,
                    value ? (mTabletUI.isChecked() ? 2 : 1) : 0);
            mTabletUI.setEnabled(value);
            mTabletFlipped.setEnabled(value);
            mStatusBarBrightnessControl.setEnabled(!value);
            mStatusBarCmSignal.setEnabled(!value);
            IWindowManager windowManager = IWindowManager.Stub.asInterface(
                    ServiceManager.getService(Context.WINDOW_SERVICE));
            try {
                mCombinedBarNavigationForceMenu.setEnabled(!windowManager.hasNavigationBar());
            } catch (RemoteException e) {
            }
            return true;
        } else if (preference == mTabletUI) {
            value = mTabletUI.isChecked();
            Settings.System.putInt(getContentResolver(), Settings.System.TABLET_MODE,
                    value ? 2 : (mTabletMode.isChecked() ? 1 : 0));
            IWindowManager wm = IWindowManager.Stub.asInterface(ServiceManager.checkService(
                    Context.WINDOW_SERVICE));
            try {
                wm.clearForcedDisplaySize();
            } catch (Exception e) {
            }
            return true;
        } else if (preference == mTabletFlipped) {
            value = mTabletFlipped.isChecked();
            Settings.System.putInt(getContentResolver(), Settings.System.TABLET_FLIPPED,
                    value ? 1 : 0);
            return true;
        } else if (preference == mNavigationControls) {
            value = mNavigationControls.isChecked();
            Settings.System.putInt(getContentResolver(), Settings.System.NAVIGATION_CONTROLS,
                    value ? 1 : 0);
            return true;
        } else if (preference == mCombinedBarNavigationForceMenu) {
            value = mCombinedBarNavigationForceMenu.isChecked();
            Settings.System.putInt(getContentResolver(),
                    Settings.System.FORCE_SOFT_MENU_BUTTON, value ? 1 : 0);
            return true;
        } else if (preference == mStatusBarClockColor) {
            ColorPickerDialog cp = new ColorPickerDialog(getActivity(),
                    mColorListener, Settings.System.getInt(mContentResolver,
                    Settings.System.STATUS_BAR_CLOCK_COLOR, 0xFF33B5E5));
            cp.setDefaultColor(0xFF33B5E5);
            cp.show();
            return true;
        } else if (preference == mCombinedBarNavigationGlow) {
            value = mCombinedBarNavigationGlow.isChecked();
            Settings.System.putInt(mContentResolver,
                    Settings.System.COMBINED_BAR_NAVIGATION_GLOW, value ? 1 : 0);
            return true;
        } else if (preference == mCombinedBarNavigationQuickGlow) {
            value = mCombinedBarNavigationQuickGlow.isChecked();
            Settings.System.putInt(mContentResolver,
                    Settings.System.COMBINED_BAR_NAVIGATION_GLOW_TIME, value ? 1 : 0);
            return true;
        } else if (preference == mCombinedBarNavigationGlowColor) {
            ColorPickerDialog cp = new ColorPickerDialog(getActivity(),
                    mGlowColorListener, Settings.System.getInt(mContentResolver,
                    Settings.System.COMBINED_BAR_NAVIGATION_GLOW_COLOR,
                    getActivity().getApplicationContext().getResources().getColor(
                    com.android.internal.R.color.holo_blue_light)));
            cp.setDefaultColor(0x00000000);
            cp.show();
            return true;
        } else if (preference == mCombinedBarNavigationColor) {
            ColorPickerDialog cp = new ColorPickerDialog(getActivity(),
                    mButtonColorListener, Settings.System.getInt(mContentResolver,
                    Settings.System.COMBINED_BAR_NAVIGATION_COLOR,
                    getActivity().getApplicationContext().getResources().getColor(
                    com.android.internal.R.color.transparent)));
            cp.setDefaultColor(0x00000000);
            cp.show();
            return true;
        }
        return false;
    }

    ColorPickerDialog.OnColorChangedListener mColorListener =
        new ColorPickerDialog.OnColorChangedListener() {
            public void colorChanged(int color) {
                Settings.System.putInt(getContentResolver(),
                        Settings.System.STATUS_BAR_CLOCK_COLOR, color);
            }
            public void colorUpdate(int color) {
            }
    };

    ColorPickerDialog.OnColorChangedListener mButtonColorListener =
        new ColorPickerDialog.OnColorChangedListener() {
            public void colorChanged(int color) {
                Settings.System.putInt(getContentResolver(),
                        Settings.System.COMBINED_BAR_NAVIGATION_COLOR, color);
            }
            public void colorUpdate(int color) {
            }
    };

    ColorPickerDialog.OnColorChangedListener mGlowColorListener =
        new ColorPickerDialog.OnColorChangedListener() {
            public void colorChanged(int color) {
                Settings.System.putInt(getContentResolver(),
                        Settings.System.COMBINED_BAR_NAVIGATION_GLOW_COLOR, color);
            }
            public void colorUpdate(int color) {
            }
    };
}
