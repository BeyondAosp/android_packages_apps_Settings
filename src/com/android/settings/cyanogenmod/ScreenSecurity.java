/*
 * Copyright (C) 2012 CyanogenMod
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

import static android.provider.Settings.System.SCREEN_OFF_TIMEOUT;

import java.util.ArrayList;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.util.Log;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.ChooseLockSettingsHelper;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

public class ScreenSecurity extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener, DialogInterface.OnClickListener {

    private static final String TAG = "ScreenSecurity";

    private static final String KEY_UNLOCK_SET_OR_CHANGE = "unlock_set_or_change";

    private static final String KEY_BIOMETRIC_WEAK_IMPROVE_MATCHING = "biometric_weak_improve_matching";

    private static final String KEY_LOCK_ENABLED = "lockenabled";

    private static final String KEY_VISIBLE_PATTERN = "visiblepattern";

    private static final String KEY_TACTILE_FEEDBACK_ENABLED = "unlock_tactile_feedback";

    private static final String KEY_SECURITY_CATEGORY = "security_category";

    private static final String KEY_LOCK_AFTER_TIMEOUT = "lock_after_timeout";

    private static final String KEY_POWER_INSTANTLY_LOCKS = "power_button_instantly_locks";

    private static final int SET_OR_CHANGE_LOCK_METHOD_REQUEST = 123;

    private static final int CONFIRM_EXISTING_FOR_BIOMETRIC_IMPROVE_REQUEST = 124;

    private static final String SLIDE_LOCK_DELAY_TOGGLE = "slide_lock_delay_toggle";

    private static final String SLIDE_LOCK_TIMEOUT_DELAY = "slide_lock_timeout_delay";

    private static final String SLIDE_LOCK_SCREENOFF_DELAY = "slide_lock_screenoff_delay";

    private static final String MENU_UNLOCK_PREF = "menu_unlock";

    private static final String LOCKSCREEN_QUICK_UNLOCK_CONTROL = "quick_unlock_control";

    private static final String KEY_LOCK_BEFORE_UNLOCK = "lock_before_unlock";

    private static final String KEY_ACCESS_RECENTS = "insecure_access_recents";

    private static final String KEY_LOCKSCREEN_TIMEOUT = "slide_lock_timeout";

    private static final String KEY_LOCKSCREEN_ALIGNMENT = "lockscreen_alignment";

    private LockPatternUtils mLockPatternUtils;

    private CheckBoxPreference mSlideLockDelayToggle;

    private ListPreference mSlideLockTimeoutDelay;

    private ListPreference mSlideLockScreenOffDelay;

    private CheckBoxPreference mPowerButtonInstantlyLocks;

    DevicePolicyManager mDPM;

    private ChooseLockSettingsHelper mChooseLockSettingsHelper;

    private ListPreference mLockAfter;

    private CheckBoxPreference mVisiblePattern;

    private CheckBoxPreference mTactileFeedback;

    private CheckBoxPreference mMenuUnlock;

    private CheckBoxPreference mQuickUnlockScreen;

    private CheckBoxPreference mAccessRecents;

    private ListPreference mLockscreenOnTimeout;

    private ListPreference mLockscreenAlignment;

    boolean mHasNavigationBar = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mLockPatternUtils = new LockPatternUtils(getActivity());

        mDPM = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);

        mChooseLockSettingsHelper = new ChooseLockSettingsHelper(getActivity());
    }

    private PreferenceScreen createPreferenceHierarchy() {
        PreferenceScreen root = getPreferenceScreen();

        if (root != null) {
            root.removeAll();
        }

        addPreferencesFromResource(R.xml.screen_security);
        root = getPreferenceScreen();

        // Add options for lock/unlock screen
        int resid = 0;

        if (!mLockPatternUtils.isSecure()) {
            if (mLockPatternUtils.isLockScreenDisabled()) {
                resid = R.xml.security_settings_lockscreen;
            } else {
                resid = R.xml.security_settings_chooser;
            }
        } else if (mLockPatternUtils.usingBiometricWeak() &&
                mLockPatternUtils.isBiometricWeakInstalled()) {
            resid = R.xml.security_settings_biometric_weak;
        } else {
            switch (mLockPatternUtils.getKeyguardStoredPasswordQuality()) {
                case DevicePolicyManager.PASSWORD_QUALITY_SOMETHING:
                    resid = R.xml.security_settings_pattern;
                    break;
                case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC:
                    resid = R.xml.security_settings_pin;
                    break;
                case DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC:
                case DevicePolicyManager.PASSWORD_QUALITY_ALPHANUMERIC:
                case DevicePolicyManager.PASSWORD_QUALITY_COMPLEX:
                    resid = R.xml.security_settings_password;
                    break;
            }
        }

        addPreferencesFromResource(resid);

        // lock after preference
        // For secure lock screen the default AOSP time delay implementation is used
        // For slide lock screen use the CyanogenMod slide lock delay and timeout settings
        mLockAfter = (ListPreference) root.findPreference(KEY_LOCK_AFTER_TIMEOUT);
        if (mLockAfter != null) {
            setupLockAfterPreference();
            updateLockAfterPreferenceSummary();
        } else if (!mLockPatternUtils.isLockScreenDisabled()) {
            addPreferencesFromResource(R.xml.security_settings_slide_delay_cyanogenmod);

            mSlideLockDelayToggle = (CheckBoxPreference) root
                    .findPreference(SLIDE_LOCK_DELAY_TOGGLE);
            mSlideLockDelayToggle.setChecked(Settings.System.getInt(getActivity()
                    .getApplicationContext().getContentResolver(),
                    Settings.System.SCREEN_LOCK_SLIDE_DELAY_TOGGLE, 0) == 1);

            mSlideLockTimeoutDelay = (ListPreference) root
                    .findPreference(SLIDE_LOCK_TIMEOUT_DELAY);
            int slideTimeoutDelay = Settings.System.getInt(getActivity().getApplicationContext()
                    .getContentResolver(),
                    Settings.System.SCREEN_LOCK_SLIDE_TIMEOUT_DELAY, 5000);
            mSlideLockTimeoutDelay.setValue(String.valueOf(slideTimeoutDelay));
            updateSlideAfterTimeoutSummary();
            mSlideLockTimeoutDelay.setOnPreferenceChangeListener(this);

            mSlideLockScreenOffDelay = (ListPreference) root
                    .findPreference(SLIDE_LOCK_SCREENOFF_DELAY);
            int slideScreenOffDelay = Settings.System.getInt(getActivity().getApplicationContext()
                    .getContentResolver(),
                    Settings.System.SCREEN_LOCK_SLIDE_SCREENOFF_DELAY, 0);
            mSlideLockScreenOffDelay.setValue(String.valueOf(slideScreenOffDelay));
            updateSlideAfterScreenOffSummary();
            mSlideLockScreenOffDelay.setOnPreferenceChangeListener(this);

            mLockscreenOnTimeout = (ListPreference) root
                    .findPreference(KEY_LOCKSCREEN_TIMEOUT);
            int lockscreenOnTimeout = Settings.System.getInt(getActivity().getApplicationContext()
                    .getContentResolver(),
                    Settings.System.LOCKSCREEN_TIMEOUT, 10000);
            mLockscreenOnTimeout.setValue(String.valueOf(lockscreenOnTimeout));
            updateLockscreenTimeoutSummary();
            mLockscreenOnTimeout.setOnPreferenceChangeListener(this);
        }

        // visible pattern
        mVisiblePattern = (CheckBoxPreference) root.findPreference(KEY_VISIBLE_PATTERN);

        // lock instantly on power key press
        mPowerButtonInstantlyLocks = (CheckBoxPreference) root.findPreference(
                KEY_POWER_INSTANTLY_LOCKS);
        checkPowerInstantLockDependency();

        // don't display visible pattern if biometric and backup is not pattern
        if (resid == R.xml.security_settings_biometric_weak &&
                mLockPatternUtils.getKeyguardStoredPasswordQuality() !=
                DevicePolicyManager.PASSWORD_QUALITY_SOMETHING) {
            PreferenceGroup securityCategory = (PreferenceGroup)
                    root.findPreference(KEY_SECURITY_CATEGORY);
            if (securityCategory != null && mVisiblePattern != null) {
                securityCategory.removePreference(root.findPreference(KEY_VISIBLE_PATTERN));
            }
        }

        // tactile feedback. Should be common to all unlock preference screens.
        mTactileFeedback = (CheckBoxPreference) root.findPreference(KEY_TACTILE_FEEDBACK_ENABLED);
        if (!((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).hasVibrator()) {
            PreferenceGroup securityCategory = (PreferenceGroup)
                    root.findPreference(KEY_SECURITY_CATEGORY);
            if (securityCategory != null && mTactileFeedback != null) {
                securityCategory.removePreference(mTactileFeedback);
            }
        }

        // Add the additional CyanogenMod settings
        addPreferencesFromResource(R.xml.security_settings_cyanogenmod);

        // Quick Unlock Screen Control
        mQuickUnlockScreen = (CheckBoxPreference) root
                .findPreference(LOCKSCREEN_QUICK_UNLOCK_CONTROL);
        mQuickUnlockScreen.setChecked(Settings.System.getInt(getActivity()
                .getApplicationContext().getContentResolver(),
                Settings.System.LOCKSCREEN_QUICK_UNLOCK_CONTROL, 0) == 1);

        // Menu Unlock
        mMenuUnlock = (CheckBoxPreference) root.findPreference(MENU_UNLOCK_PREF);
        mMenuUnlock.setChecked(Settings.System.getInt(getActivity().getApplicationContext()
                .getContentResolver(),
                Settings.System.MENU_UNLOCK_SCREEN, 0) == 1);

        // Access Recents
        mAccessRecents = (CheckBoxPreference) root
                .findPreference(KEY_ACCESS_RECENTS);
        mAccessRecents.setChecked(Settings.System.getInt(getActivity()
                .getApplicationContext().getContentResolver(),
                Settings.System.LOCKSCREEN_RECENTS, 0) == 1);

        mLockscreenAlignment = (ListPreference) findPreference(KEY_LOCKSCREEN_ALIGNMENT);
        if (mLockPatternUtils.isSecure()) {
            Settings.System.putInt(getActivity().getApplicationContext()
                    .getContentResolver(), Settings.System.LOCKSCREEN_ALIGNMENT, 0);
        }
        int lockscreenAlignment = Settings.System.getInt(getActivity().getApplicationContext()
                .getContentResolver(), Settings.System.LOCKSCREEN_ALIGNMENT, 0);
        mLockscreenAlignment.setValue(String.valueOf(lockscreenAlignment));
        updateLockscreenAlignmentSummary();
        mLockscreenAlignment.setOnPreferenceChangeListener(this);

        //Disable the MenuUnlock setting if no menu button is available
        if (getActivity().getApplicationContext().getResources()
                .getBoolean(com.android.internal.R.bool.config_showNavigationBar)) {
            mMenuUnlock.setEnabled(false);
        }

        // disable lock options if lock screen is secure
        if (mLockPatternUtils.isSecure() || !Utils.isScreenLarge(getResources())
                || mLockPatternUtils.isLockScreenDisabled()) {
            PreferenceCategory additional =
                    (PreferenceCategory) findPreference("additional_options");
            additional.removePreference(mAccessRecents);
            additional.removePreference(mLockscreenAlignment);
            // disable lock options if lock screen set to NONE
            if (!mLockPatternUtils.isSecure() && mLockPatternUtils.isLockScreenDisabled()) {
                additional.removePreference(mQuickUnlockScreen);
                additional.removePreference(mMenuUnlock);
            }
        }

        return root;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void updateSlideAfterTimeoutSummary() {
        // Update summary message with current value
        long currentTimeout = Settings.System.getInt(getActivity().getApplicationContext()
                .getContentResolver(),
                Settings.System.SCREEN_LOCK_SLIDE_TIMEOUT_DELAY, 5000);
        final CharSequence[] entries = mSlideLockTimeoutDelay.getEntries();
        final CharSequence[] values = mSlideLockTimeoutDelay.getEntryValues();
        int best = 0;
        for (int i = 0; i < values.length; i++) {
            long timeout = Long.valueOf(values[i].toString());
            if (currentTimeout >= timeout) {
                best = i;
            }
        }
        mSlideLockTimeoutDelay.setSummary(entries[best]);
    }

    private void updateSlideAfterScreenOffSummary() {
        // Update summary message with current value
        long currentTimeout = Settings.System.getInt(getActivity().getApplicationContext()
                .getContentResolver(),
                Settings.System.SCREEN_LOCK_SLIDE_SCREENOFF_DELAY, 0);
        final CharSequence[] entries = mSlideLockScreenOffDelay.getEntries();
        final CharSequence[] values = mSlideLockScreenOffDelay.getEntryValues();
        int best = 0;
        for (int i = 0; i < values.length; i++) {
            long timeout = Long.valueOf(values[i].toString());
            if (currentTimeout >= timeout) {
                best = i;
            }
        }
        mSlideLockScreenOffDelay.setSummary(entries[best]);
    }

    private void updateLockscreenAlignmentSummary() {
        // Update summary message with current value
        int currentAlignment = Settings.System.getInt(getActivity().getApplicationContext()
                .getContentResolver(), Settings.System.LOCKSCREEN_ALIGNMENT, 0);
        final CharSequence[] entries = mLockscreenAlignment.getEntries();
        final CharSequence[] values = mLockscreenAlignment.getEntryValues();
        int best = 0;
        for (int i = 0; i < values.length; i++) {
            int alignment = Integer.valueOf(values[i].toString());
            if (currentAlignment >= alignment) {
                best = i;
            }
        }
        mLockscreenAlignment.setSummary(entries[best]);
    }

    private void updateLockscreenTimeoutSummary() {
        // Update summary message with current value
        long currentTimeout = Settings.System.getInt(getActivity().getApplicationContext()
                .getContentResolver(),
                Settings.System.LOCKSCREEN_TIMEOUT, 10000);
        final CharSequence[] entries = mLockscreenOnTimeout.getEntries();
        final CharSequence[] values = mLockscreenOnTimeout.getEntryValues();
        int best = 0;
        for (int i = 0; i < values.length; i++) {
            long timeout = Long.valueOf(values[i].toString());
            if (currentTimeout >= timeout) {
                best = i;
            }
        }
        mLockscreenOnTimeout.setSummary(entries[best]);
    }

    private void setupLockAfterPreference() {
        // Compatible with pre-Froyo
        long currentTimeout = Settings.Secure.getLong(getContentResolver(),
                Settings.Secure.LOCK_SCREEN_LOCK_AFTER_TIMEOUT, 5000);
        mLockAfter.setValue(String.valueOf(currentTimeout));
        mLockAfter.setOnPreferenceChangeListener(this);
        final long adminTimeout = (mDPM != null ? mDPM.getMaximumTimeToLock(null) : 0);
        final long displayTimeout = Math.max(0,
                Settings.System.getInt(getContentResolver(), SCREEN_OFF_TIMEOUT, 0));
        if (adminTimeout > 0) {
            // This setting is a slave to display timeout when a device policy
            // is enforced.
            // As such, maxLockTimeout = adminTimeout - displayTimeout.
            // If there isn't enough time, shows "immediately" setting.
            disableUnusableTimeouts(Math.max(0, adminTimeout - displayTimeout));
        }
    }

    private void updateLockAfterPreferenceSummary() {
        // Update summary message with current value
        long currentTimeout = Settings.Secure.getLong(getContentResolver(),
                Settings.Secure.LOCK_SCREEN_LOCK_AFTER_TIMEOUT, 5000);
        final CharSequence[] entries = mLockAfter.getEntries();
        final CharSequence[] values = mLockAfter.getEntryValues();
        int best = 0;
        for (int i = 0; i < values.length; i++) {
            long timeout = Long.valueOf(values[i].toString());
            if (currentTimeout >= timeout) {
                best = i;
            }
        }
        mLockAfter.setSummary(getString(R.string.lock_after_timeout_summary, entries[best]));
    }

    private void checkPowerInstantLockDependency() {
        if (mPowerButtonInstantlyLocks != null) {
            long timeout = Settings.Secure.getLong(getContentResolver(),
                    Settings.Secure.LOCK_SCREEN_LOCK_AFTER_TIMEOUT, 5000);
            if (timeout == 0) {
                mPowerButtonInstantlyLocks.setEnabled(false);
            } else {
                mPowerButtonInstantlyLocks.setEnabled(true);
            }
        }
    }

    private void disableUnusableTimeouts(long maxTimeout) {
        final CharSequence[] entries = mLockAfter.getEntries();
        final CharSequence[] values = mLockAfter.getEntryValues();
        ArrayList<CharSequence> revisedEntries = new ArrayList<CharSequence>();
        ArrayList<CharSequence> revisedValues = new ArrayList<CharSequence>();
        for (int i = 0; i < values.length; i++) {
            long timeout = Long.valueOf(values[i].toString());
            if (timeout <= maxTimeout) {
                revisedEntries.add(entries[i]);
                revisedValues.add(values[i]);
            }
        }
        if (revisedEntries.size() != entries.length || revisedValues.size() != values.length) {
            mLockAfter.setEntries(
                    revisedEntries.toArray(new CharSequence[revisedEntries.size()]));
            mLockAfter.setEntryValues(
                    revisedValues.toArray(new CharSequence[revisedValues.size()]));
            final int userPreference = Integer.valueOf(mLockAfter.getValue());
            if (userPreference <= maxTimeout) {
                mLockAfter.setValue(String.valueOf(userPreference));
            } else {
                // There will be no highlighted selection since nothing in the
                // list matches
                // maxTimeout. The user can still select anything less than
                // maxTimeout.
                // TODO: maybe append maxTimeout to the list and mark selected.
            }
        }
        mLockAfter.setEnabled(revisedEntries.size() > 0);
    }

    @Override
    public void onResume() {
        super.onResume();

        // Make sure we reload the preference hierarchy since some of these
        // settings
        // depend on others...
        createPreferenceHierarchy();

        final LockPatternUtils lockPatternUtils = mChooseLockSettingsHelper.utils();
        if (mVisiblePattern != null) {
            mVisiblePattern.setChecked(lockPatternUtils.isVisiblePatternEnabled());
        }
        if (mTactileFeedback != null) {
            mTactileFeedback.setChecked(lockPatternUtils.isTactileFeedbackEnabled());
        }
        if (mPowerButtonInstantlyLocks != null) {
            mPowerButtonInstantlyLocks.setChecked(lockPatternUtils.getPowerButtonInstantlyLocks());
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        boolean value;
        final String key = preference.getKey();

        final LockPatternUtils lockPatternUtils = mChooseLockSettingsHelper.utils();
        if (KEY_UNLOCK_SET_OR_CHANGE.equals(key)) {
            startFragment(this, "com.android.settings.ChooseLockGeneric$ChooseLockGenericFragment",
                    SET_OR_CHANGE_LOCK_METHOD_REQUEST, null);
        } else if (KEY_BIOMETRIC_WEAK_IMPROVE_MATCHING.equals(key)) {
            ChooseLockSettingsHelper helper =
                    new ChooseLockSettingsHelper(this.getActivity(), this);
            if (!helper.launchConfirmationActivity(
                    CONFIRM_EXISTING_FOR_BIOMETRIC_IMPROVE_REQUEST, null, null)) {
                startBiometricWeakImprove(); // no password set, so no need to
                                             // confirm
            }
        } else if (KEY_LOCK_ENABLED.equals(key)) {
            lockPatternUtils.setLockPatternEnabled(isToggled(preference));
        } else if (KEY_VISIBLE_PATTERN.equals(key)) {
            lockPatternUtils.setVisiblePatternEnabled(isToggled(preference));
        } else if (KEY_TACTILE_FEEDBACK_ENABLED.equals(key)) {
            lockPatternUtils.setTactileFeedbackEnabled(isToggled(preference));
        } else if (KEY_LOCK_BEFORE_UNLOCK.equals(key)) {
            lockPatternUtils.setLockBeforeUnlock(isToggled(preference));
        } else if (KEY_POWER_INSTANTLY_LOCKS.equals(key)) {
            lockPatternUtils.setPowerButtonInstantlyLocks(isToggled(preference));
        } else if (preference == mSlideLockDelayToggle) {
            value = mSlideLockDelayToggle.isChecked();
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.SCREEN_LOCK_SLIDE_DELAY_TOGGLE, value ? 1 : 0);
        } if (preference == mQuickUnlockScreen) {
            value = mQuickUnlockScreen.isChecked();
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.LOCKSCREEN_QUICK_UNLOCK_CONTROL, value ? 1 : 0);
        } else if (preference == mMenuUnlock) {
            value = mMenuUnlock.isChecked();
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.MENU_UNLOCK_SCREEN, value ? 1 : 0);
        } else if (preference == mAccessRecents) {
            value = mAccessRecents.isChecked();
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.LOCKSCREEN_RECENTS, value ? 1 : 0);
        } else {
            // If we didn't handle it, let preferences handle it.
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }

        return true;
    }

    private boolean isToggled(Preference pref) {
        return ((CheckBoxPreference) pref).isChecked();
    }

    /**
     * see confirmPatternThenDisableAndClear
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CONFIRM_EXISTING_FOR_BIOMETRIC_IMPROVE_REQUEST &&
                resultCode == Activity.RESULT_OK) {
            startBiometricWeakImprove();
            return;
        }
        createPreferenceHierarchy();
    }

    public boolean onPreferenceChange(Preference preference, Object value) {
        if (preference == mLockAfter) {
            int timeout = Integer.parseInt((String) value);
            try {
                Settings.Secure.putInt(getContentResolver(),
                        Settings.Secure.LOCK_SCREEN_LOCK_AFTER_TIMEOUT, timeout);
            } catch (NumberFormatException e) {
                Log.e("SecuritySettings", "could not persist lockAfter timeout setting", e);
            }
            updateLockAfterPreferenceSummary();
            checkPowerInstantLockDependency();
        } else if (preference == mSlideLockTimeoutDelay) {
            int slideTimeoutDelay = Integer.valueOf((String) value);
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.SCREEN_LOCK_SLIDE_TIMEOUT_DELAY,
                    slideTimeoutDelay);
            updateSlideAfterTimeoutSummary();
        } else if (preference == mSlideLockScreenOffDelay) {
            int slideScreenOffDelay = Integer.valueOf((String) value);
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.SCREEN_LOCK_SLIDE_SCREENOFF_DELAY, slideScreenOffDelay);
            updateSlideAfterScreenOffSummary();
        } else if (preference == mLockscreenOnTimeout) {
            int lockTimeout = Integer.valueOf((String) value);
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.LOCKSCREEN_TIMEOUT, lockTimeout);
            updateLockscreenTimeoutSummary();
        } else if (preference == mLockscreenAlignment) {
            int alignment = Integer.valueOf((String) value);
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.LOCKSCREEN_ALIGNMENT, alignment);
            updateLockscreenAlignmentSummary();
        }

        return true;
    }

    public void startBiometricWeakImprove() {
        Intent intent = new Intent();
        intent.setClassName("com.android.facelock", "com.android.facelock.AddToSetup");
        startActivity(intent);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {

    }
}
