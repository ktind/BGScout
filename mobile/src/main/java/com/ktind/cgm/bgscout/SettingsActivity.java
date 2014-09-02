package com.ktind.cgm.bgscout;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Configuration;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.support.v4.app.NavUtils;

import java.util.List;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends PreferenceActivity {
    private static final String TAG = SettingsActivity.class.getSimpleName();
    /**
     * Determines whether to always show the simplified settings UI, where
     * settings are presented in a single list. When false, settings are shown
     * as a master/detail two-pane view on tablets. When true, a single pane is
     * shown on tablets.
     */
    private static final boolean ALWAYS_SIMPLE_PREFS = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupActionBar();
    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void setupActionBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            // Show the Up button in the action bar.
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            // This ID represents the Home or Up button. In the case of this
            // activity, the Up button is shown. Use NavUtils to allow users
            // to navigate up one level in the application structure. For
            // more details, see the Navigation pattern on Android Design:
            //
            // http://developer.android.com/design/patterns/navigation.html#up-vs-back
            //
            // TODO: If Settings has multiple levels, Up should navigate up
            // that hierarchy.
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        setupSimplePreferencesScreen();
    }

    /**
     * Shows the simplified settings UI if the device configuration if the
     * device configuration dictates that a simplified, single-pane UI should be
     * shown.
     */
    private void setupSimplePreferencesScreen() {
        if (!isSimplePreferences(this)) {
            return;
        }

        // In the simplified UI, fragments are not used at all and we instead
        // use the older PreferenceActivity APIs.

        // Add 'general' preferences.
        addPreferencesFromResource(R.xml.pref_general);

        // Bind the summaries of EditText/List/Dialog/Ringtone preferences to
        // their values. When their values change, their summaries are updated
        // to reflect the new value, per the Android Design guidelines.
//        String[] devices={"device_1","device_2","device_3","device_4"};
        for (String device:Constants.DEVICES){
            Log.d(TAG, "Trying to setup "+device+"_name");
            bindPreferenceSummaryToValue(findPreference(device + "_name"));
            Log.d(TAG, "Trying to setup "+device+"_high_threshold");
            bindPreferenceSummaryToValue(findPreference(device+"_high_threshold"));
            Log.d(TAG, "Trying to setup "+device+"_critical_high_threshold");
            bindPreferenceSummaryToValue(findPreference(device+"_critical_high_threshold"));
            Log.d(TAG, "Trying to setup "+device+"_low_threshold");
            bindPreferenceSummaryToValue(findPreference(device+"_low_threshold"));
            Log.d(TAG, "Trying to setup "+device+"_critical_low_threshold");
            bindPreferenceSummaryToValue(findPreference(device+"_critical_low_threshold"));
            Log.d(TAG, "Trying to setup "+device+"_high_ringtone");
            bindPreferenceSummaryToValue(findPreference(device+"_high_ringtone"));
            Log.d(TAG, "Trying to setup "+device+"_critical_high_ringtone");
            bindPreferenceSummaryToValue(findPreference(device+"_critical_high_ringtone"));
            Log.d(TAG, "Trying to setup "+device+"_low_ringtone");
            bindPreferenceSummaryToValue(findPreference(device+"_low_ringtone"));
            Log.d(TAG, "Trying to setup "+device+"_critical_ringtone");
            bindPreferenceSummaryToValue(findPreference(device+"_critical_low_ringtone"));
            Log.d(TAG, "Trying to setup "+device+"_mongo_uri");
            bindPreferenceSummaryToValue(findPreference(device+"_mongo_uri"));
            Log.d(TAG, "Trying to setup "+device+"_nsapi");
            bindPreferenceSummaryToValue(findPreference(device+"_nsapi"));
            Log.d(TAG, "Trying to setup "+device+"_mqtt_endpoint");
            bindPreferenceSummaryToValue(findPreference(device+"_mqtt_endpoint"));
            Log.d(TAG, "Trying to setup "+device+"_mqtt_user");
            bindPreferenceSummaryToValue(findPreference(device+"_mqtt_user"));
            Log.d(TAG, "Trying to setup button_"+device);
            Preference buttonPref=findPreference("button_"+device);
            sBindPreferenceSummaryToValueListener.onPreferenceChange(buttonPref,findPreference(device+"_name"));
        }
//        bindPreferenceSummaryToValue(findPreference("device_1_name"));
//        bindPreferenceSummaryToValue(findPreference("device_2_name"));
//        bindPreferenceSummaryToValue(findPreference("device_3_name"));
//        bindPreferenceSummaryToValue(findPreference("device_4_name"));
//        bindPreferenceSummaryToValue(findPreference("device_1_high_threshold"));
//        bindPreferenceSummaryToValue(findPreference("device_1_low_threshold"));
//        bindPreferenceSummaryToValue(findPreference("device_2_high_threshold"));
//        bindPreferenceSummaryToValue(findPreference("device_2_low_threshold"));
//        bindPreferenceSummaryToValue(findPreference("device_3_high_threshold"));
//        bindPreferenceSummaryToValue(findPreference("device_3_low_threshold"));
//        bindPreferenceSummaryToValue(findPreference("device_4_high_threshold"));
//        bindPreferenceSummaryToValue(findPreference("device_4_low_threshold"));
//        bindPreferenceSummaryToValue(findPreference("device_1_high_ringtone"));
//        bindPreferenceSummaryToValue(findPreference("device_1_low_ringtone"));
//        bindPreferenceSummaryToValue(findPreference("device_2_high_ringtone"));
//        bindPreferenceSummaryToValue(findPreference("device_2_low_ringtone"));
//        bindPreferenceSummaryToValue(findPreference("device_3_high_ringtone"));
//        bindPreferenceSummaryToValue(findPreference("device_3_low_ringtone"));
//        bindPreferenceSummaryToValue(findPreference("device_4_high_ringtone"));
//        bindPreferenceSummaryToValue(findPreference("device_4_low_ringtone"));
//        bindPreferenceSummaryToValue(findPreference("device_1_mongo_uri"));
//        bindPreferenceSummaryToValue(findPreference("device_2_mongo_uri"));
//        bindPreferenceSummaryToValue(findPreference("device_3_mongo_uri"));
//        bindPreferenceSummaryToValue(findPreference("device_4_mongo_uri"));

//        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);
//
//        // Trigger the listener immediately with the preference's
//        // current value.
//        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
//                PreferenceManager
//                        .getDefaultSharedPreferences(preference.getContext())
//                        .getString(preference.getKey(), ""));
    }

    /** {@inheritDoc} */
    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this) && !isSimplePreferences(this);
    }

    /**
     * Helper method to determine if the device has an extra-large screen. For
     * example, 10" tablets are extra-large.
     */
    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
        & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    /**
     * Determines whether the simplified settings UI should be shown. This is
     * true if this is forced via {@link #ALWAYS_SIMPLE_PREFS}, or the device
     * doesn't have newer APIs like {@link PreferenceFragment}, or the device
     * doesn't have an extra-large screen. In these cases, a single-pane
     * "simplified" settings UI should be shown.
     */
    private static boolean isSimplePreferences(Context context) {
        return ALWAYS_SIMPLE_PREFS
                || Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB
                || !isXLargeTablet(context);
    }

    /** {@inheritDoc} */
    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onBuildHeaders(List<Header> target) {
//        loadHeadersFromResource(R.xml.pref_headers,target);
//        if (!isSimplePreferences(this)) {
//            loadHeadersFromResource(R.xml.pref_headers, target);
//        }
    }

    /** TODO Implement something here
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();

            if (preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                // Set the summary to reflect the new value.
                preference.setSummary(
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : null);

            } else if (preference instanceof RingtonePreference) {
                // For ringtone preferences, look up the correct display value
                // using RingtoneManager.
                if (TextUtils.isEmpty(stringValue)) {
                    // Empty values correspond to 'silent' (no ringtone).
                    preference.setSummary(R.string.pref_ringtone_silent);

                } else {
                    Ringtone ringtone = RingtoneManager.getRingtone(
                            preference.getContext(), Uri.parse(stringValue));

                    if (ringtone == null) {
                        // Clear the summary if there was a lookup error.
                        preference.setSummary(null);
                    } else {
                        // Set the summary to reflect the new ringtone display
                        // name.
                        String name = ringtone.getTitle(preference.getContext());
                        preference.setSummary(name);
                    }
                }

            } else {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                preference.setSummary(stringValue);
            }
            return true;
        }
    };

    /**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference title) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the type of preference.
     *
     * @see #sBindPreferenceSummaryToValueListener
     */
    private static void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's
        // current value.
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }

//    /**
//     * This fragment shows general preferences only. It is used when the
//     * activity is showing a two-pane settings UI.
//     */
//    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
//    public static class GeneralPreferenceFragment extends PreferenceFragment {
//        @Override
//        public void onCreate(Bundle savedInstanceState) {
//            super.onCreate(savedInstanceState);
//            addPreferencesFromResource(R.xml.pref_general);
//        }
//    }
}
