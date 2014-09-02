package com.ktind.cgm.bgscout;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v13.app.FragmentPagerAdapter;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.ktind.cgm.bgscout.model.Battery;
import com.ktind.cgm.bgscout.model.DownloadDataSource;
import com.ktind.cgm.bgscout.model.EGV;


public class DeviceActivity extends Activity {
    private static final String TAG = DeviceActivity.class.getSimpleName();
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private String[] mDrawerMenuItems;
    private ActionBarDrawerToggle mDrawerToggle;
    private int numItemsInMenu;
    private boolean mBounded=false;
    private DeviceDownloadService mServer;
    private UIReceiver uiReceiver;
    // FIXME might need to make this hashmap otherwise we won't be able retrieve the lastdown for the different devices - it would only be a single lastDownload
    static private DownloadObject ld;
    static private HashMap<String,UIDevice> ui=new HashMap<String, UIDevice>();


    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v13.app.FragmentStatePagerAdapter}.
     */
    SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    ViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);
        mDrawerLayout= (DrawerLayout) findViewById(R.id.drawer_layout2);
        mDrawerList=(ListView) findViewById(R.id.left_drawer2);
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
//        String[] devices={"device_1","device_2","device_3","device_4"};
        ArrayList<String> mDrawerMenuItemsArrList=new ArrayList<String>();
        for (String device:Constants.DEVICES) {
            if (sharedPref.getBoolean(device+"_enable",false)){
                Log.d(TAG,device+" is enabled");
                mDrawerMenuItemsArrList.add(sharedPref.getString(device+"_name",device));
            }
        }
        mDrawerMenuItemsArrList.add("Start");
        mDrawerMenuItemsArrList.add("Stop");
        mDrawerMenuItemsArrList.add("Dump stats to log");
        mDrawerMenuItemsArrList.add("Settings");
        mDrawerMenuItemsArrList.add("Dump EGV");
        numItemsInMenu=mDrawerMenuItemsArrList.size();

        mDrawerMenuItems=mDrawerMenuItemsArrList.toArray(new String[mDrawerMenuItemsArrList.size()]);
        mDrawerList.setAdapter(new ArrayAdapter<String>(this,R.layout.drawer_list_item,mDrawerMenuItems));
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);

        mDrawerToggle = new ActionBarDrawerToggle(
                this,                  /* host Activity */
                mDrawerLayout,         /* DrawerLayout object */
                R.drawable.ic_drawer,  /* nav drawer icon to replace 'Up' caret */
                R.string.drawer_open,  /* "open drawer" description */
                R.string.drawer_close  /* "close drawer" description */
        ) {

            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                invalidateOptionsMenu();
            }

            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                invalidateOptionsMenu();
            }
        };
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        mSectionsPagerAdapter = new SectionsPagerAdapter(getFragmentManager());


        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.setPageTransformer(true, new ZoomOutPageTransformer());

        uiReceiver=new UIReceiver();
        IntentFilter intentFilter=new IntentFilter(Constants.UI_UPDATE);
        getBaseContext().registerReceiver(uiReceiver,intentFilter);
        if (savedInstanceState!=null){
            ld=savedInstanceState.getParcelable("lastDownload");
            if (ld==null){
                Log.d(TAG,"Its null...");
            } else {
                if (ui.containsKey(ld.getDeviceID())){
                    ui.get(ld.getDeviceID()).update(ld);
                } else {
                    Log.w("XXX","UI does not contain "+ld.getDeviceID());
                }
            }
//            UIDeviceList.get(0).update((DownloadObject) savedInstanceState.getSerializable("lastDownload"));
        } else {
            Log.w("XXX", "Saved instance does not contain anything");
        }

    }

    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView parent, View view, int position, long id) {
            selectItem(position);
            mDrawerLayout.closeDrawers();
        }
    }

    public void buildUI(){
        // Build the UI objects
        Log.d("XXX","Number of fragments here: "+PlaceholderFragment.fragments.size());
        for (String key:PlaceholderFragment.fragments.keySet()) {
            TextView egvValue = (TextView) PlaceholderFragment.fragments.get(key).getView().findViewById(R.id.reading_text);
            TextView uploaderBatteryLabel = (TextView) PlaceholderFragment.fragments.get(key).getView().findViewById(R.id.uploader_battery_label);
            TextView deviceBatteryLabel = (TextView) PlaceholderFragment.fragments.get(key).getView().findViewById(R.id.device_battery_label);
            TextView name = (TextView) PlaceholderFragment.fragments.get(key).getView().findViewById(R.id.app_name);
            ImageView main = (ImageView) PlaceholderFragment.fragments.get(key).getView().findViewById(R.id.main_display);
            ImageView direction = (ImageView) PlaceholderFragment.fragments.get(key).getView().findViewById(R.id.direction_image);
            ImageView uploaderBat = (ImageView) PlaceholderFragment.fragments.get(key).getView().findViewById(R.id.uploader_battery_indicator);
            ImageView deviceBat = (ImageView) PlaceholderFragment.fragments.get(key).getView().findViewById(R.id.device_battery_indicator);
            UIDevice uid=new UIDevice(main,direction,egvValue,name,uploaderBat,uploaderBatteryLabel,deviceBat,deviceBatteryLabel);
            Log.d("XXX", "Adding "+key+" to ui elements map");
            ui.put(key, uid);
        }

    }

    // FIXME breaks the rules - order here is must match the order the items were put into the string array(list)
    private void selectItem(int position){
        Log.d(TAG,"Position: "+position+ " number of items in menu: "+numItemsInMenu);
        if (position==(numItemsInMenu-2)) {
            Log.d(TAG,"Starting settings");
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        }
        if (position==(numItemsInMenu-3)) {
            Log.d(TAG,"Dumping stats");
            BGScout.statsMgr.logStats();
        }
        if (position==(numItemsInMenu-4)) {
            Log.d(TAG,"Stopping service");
            Intent intent=new Intent(Constants.STOP_DOWNLOAD_SVC);
            getBaseContext().sendBroadcast(intent);
//            Intent mIntent = new Intent(MainActivity.this, DeviceDownloadService.class);
//            bindSvc();
//            stopService(mIntent);
        }
        if (position==(numItemsInMenu-5)) {
            Log.d(TAG,"Starting service");
            Intent mIntent = new Intent(DeviceActivity.this, DeviceDownloadService.class);
            startService(mIntent);
            bindSvc();
            buildUI();
        }
        if (position==(numItemsInMenu-1)) {
            DownloadDataSource downloadDataSource=new DownloadDataSource(this);
            try {
                downloadDataSource.open();
                for (EGV egv:downloadDataSource.getEGVHistory("device_1"))
                    Log.d(TAG,"Date: "+new Date(egv.getEpoch())+" EGV: "+egv.getEgv()+" Trend: "+Trend.values()[egv.getTrend()].toString()+" Unit: "+GlucoseUnit.values()[egv.getUnit()]);
                for (Battery battery:downloadDataSource.getBatteryHistory("device_1"))
                    Log.d(TAG,"Date: "+new Date(battery.getEpoch())+" Battery Level:"+battery.getBatterylevel()+" Device: "+downloadDataSource.getDevice(battery.getDeviceid()).getName()+" Role: "+downloadDataSource.getRole(battery.getRoleid()).getRole());
                downloadDataSource.close();
            } catch (SQLException e) {
                Log.e(TAG,"Caught exception: ",e);
            }

            Intent mIntent = new Intent(DeviceActivity.this, DeviceDownloadService.class);
            startService(mIntent);
            bindSvc();
        }
    }

    public void bindSvc(){
        Intent mIntent = new Intent(DeviceActivity.this, DeviceDownloadService.class);
        bindService(mIntent, mConnection, BIND_AUTO_CREATE);
    }

    ServiceConnection mConnection = new ServiceConnection() {

        public void onServiceDisconnected(ComponentName name) {
            Toast.makeText(DeviceActivity.this, "Service is disconnected", Toast.LENGTH_LONG).show();
            mBounded = false;
            mServer = null;
        }

        public void onServiceConnected(ComponentName name, IBinder service) {
            Toast.makeText(DeviceActivity.this, "Service is connected", Toast.LENGTH_LONG).show();
            mBounded = true;
            DeviceDownloadService.LocalBinder mLocalBinder = (DeviceDownloadService.LocalBinder)service;
            mServer = mLocalBinder.getServerInstance();
        }
    };



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.device, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            int devNum=position+1;
            String devID="device_"+devNum;
            Log.d("Main","adding "+devID);
            return PlaceholderFragment.newInstance(position + 1,devID);
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
//            Log.d("main","getCount called");
//            return numItemsInMenu-5;
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            Locale l = Locale.getDefault();
            switch (position) {
                case 0:
                    return getString(R.string.title_section1).toUpperCase(l);
                case 1:
                    return getString(R.string.title_section2).toUpperCase(l);
                case 2:
                    return getString(R.string.title_section3).toUpperCase(l);
            }
            return null;
        }
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";
//        static public ArrayList <PlaceholderFragment> fragments= new ArrayList<PlaceholderFragment>();
        static public HashMap<String,PlaceholderFragment> fragments=new HashMap<String, PlaceholderFragment>();

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber,String deviceID) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);


            fragments.put(deviceID,fragment);
//            fragments.add(fragment);
            return fragment;
        }

        public PlaceholderFragment() {
        }

        public void updateView(DownloadObject dl){
            TextView egvValue=(TextView) this.getView().findViewById(R.id.reading_text);
            TextView uploaderBattery=(TextView) this.getView().findViewById(R.id.uploader_battery_label);
            TextView deviceBattery=(TextView) this.getView().findViewById(R.id.device_battery_label);
            TextView name=(TextView) this.getView().findViewById(R.id.app_name);

            try {
                egvValue.setText(String.valueOf(dl.getLastReading()));
                uploaderBattery.setText(String.valueOf(dl.getUploaderBattery()));
                deviceBattery.setText(String.valueOf(dl.getDeviceBattery()));
                name.setText(dl.getDeviceName());
            } catch (NoDataException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onDestroyView() {
            Log.d("XXX","onDestoryView called");
            super.onDestroyView();
        }

        @Override
        public void onDestroy() {
            Log.d("XXX","onDestory called");
            super.onDestroy();
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            Log.d("XXX","saving instance state");
            outState.putParcelable("lastDownload", ld);
            super.onSaveInstanceState(outState);
        }

        @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
        @Override
        public void onViewStateRestored(Bundle savedInstanceState) {
            super.onViewStateRestored(savedInstanceState);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            Log.d("XXX","onCreateView called");
            if (savedInstanceState!=null){
                ld=savedInstanceState.getParcelable("lastDownload");
                if (ld==null){
                    Log.d(TAG,"Its null...");
                } else {
                    if (ui.containsKey(ld.getDeviceID())){
                        ui.get(ld.getDeviceID()).update(ld);
                    } else {
                        Log.w("XXX","UI does not contain "+ld.getDeviceID());
                    }
                }
//            UIDeviceList.get(0).update((DownloadObject) savedInstanceState.getSerializable("lastDownload"));
            } else {
                Log.w("XXX", "Saved instance does not contain anything");
            }
            View rootView = inflater.inflate(R.layout.fragment_device, container, false);
            return rootView;
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // If the nav drawer is open, hide action items related to the content view
        boolean drawerOpen = mDrawerLayout.isDrawerOpen(mDrawerList);
//        menu.findItem(R.id.action_websearch).setVisible(!drawerOpen);
        return super.onPrepareOptionsMenu(menu);
    }

    public class DepthPageTransformer implements ViewPager.PageTransformer {
        private static final float MIN_SCALE = 0.75f;

        public void transformPage(View view, float position) {
            int pageWidth = view.getWidth();

            if (position < -1) { // [-Infinity,-1)
                // This page is way off-screen to the left.
                view.setAlpha(0);

            } else if (position <= 0) { // [-1,0]
                // Use the default slide transition when moving to the left page
                view.setAlpha(1);
                view.setTranslationX(0);
                view.setScaleX(1);
                view.setScaleY(1);

            } else if (position <= 1) { // (0,1]
                // Fade the page out.
                view.setAlpha(1 - position);

                // Counteract the default slide transition
                view.setTranslationX(pageWidth * -position);

                // Scale the page down (between MIN_SCALE and 1)
                float scaleFactor = MIN_SCALE
                        + (1 - MIN_SCALE) * (1 - Math.abs(position));
                view.setScaleX(scaleFactor);
                view.setScaleY(scaleFactor);

            } else { // (1,+Infinity]
                // This page is way off-screen to the right.
                view.setAlpha(0);
            }
        }
    }

    public class ZoomOutPageTransformer implements ViewPager.PageTransformer {
        private static final float MIN_SCALE = 0.85f;
        private static final float MIN_ALPHA = 0.5f;

        public void transformPage(View view, float position) {
            int pageWidth = view.getWidth();
            int pageHeight = view.getHeight();

            if (position < -1) { // [-Infinity,-1)
                // This page is way off-screen to the left.
                view.setAlpha(0);

            } else if (position <= 1) { // [-1,1]
                // Modify the default slide transition to shrink the page as well
                float scaleFactor = Math.max(MIN_SCALE, 1 - Math.abs(position));
                float vertMargin = pageHeight * (1 - scaleFactor) / 2;
                float horzMargin = pageWidth * (1 - scaleFactor) / 2;
                if (position < 0) {
                    view.setTranslationX(horzMargin - vertMargin / 2);
                } else {
                    view.setTranslationX(-horzMargin + vertMargin / 2);
                }

                // Scale the page down (between MIN_SCALE and 1)
                view.setScaleX(scaleFactor);
                view.setScaleY(scaleFactor);

                // Fade the page relative to its size.
                view.setAlpha(MIN_ALPHA +
                        (scaleFactor - MIN_SCALE) /
                                (1 - MIN_SCALE) * (1 - MIN_ALPHA));

            } else { // (1,+Infinity]
                // This page is way off-screen to the right.
                view.setAlpha(0);
            }
        }
    }

    protected class UIDevice{
        protected TextView name;
        protected ImageView main_display;
        protected ImageView direction;
        protected TextView bg;
        protected DownloadObject lastDownload;
        protected ImageView uploaderBattery;
        protected TextView uploaderBatteryLabel;
        protected ImageView deviceBattery;
        protected TextView deviceBatteryLabel;
        int mainBGColor;
        int currentBGColor;

        public UIDevice(ImageView main, ImageView dir, TextView reading, TextView n, ImageView ubat, TextView ubatl, ImageView dbat, TextView dbatl){
            setMain_display(main);
//            main_display.setBackgroundColor(mainBGColor);
            setDirection(dir);
            setBg(reading);
            setName(n);
            setUploaderBattery(ubat);
            setDeviceBattery(dbat);
            setUploaderBatteryLabel(ubatl);
            setDeviceBatteryLabel(dbatl);
            deviceBattery.setImageResource(R.drawable.battery);
            uploaderBattery.setImageResource(R.drawable.battery);
        }

        public TextView getUploaderBatteryLabel() {
            return uploaderBatteryLabel;
        }

        public void setUploaderBatteryLabel(TextView uploaderBatteryLabel) {
            this.uploaderBatteryLabel = uploaderBatteryLabel;
        }

        public TextView getDeviceBatteryLabel() {
            return deviceBatteryLabel;
        }

        public void setDeviceBatteryLabel(TextView deviceBatteryLabel) {
            this.deviceBatteryLabel = deviceBatteryLabel;
        }

        public ImageView getUploaderBattery() {
            return uploaderBattery;
        }

        public void setUploaderBattery(ImageView uploaderBattery) {
            this.uploaderBattery = uploaderBattery;
        }

        public ImageView getDeviceBattery() {
            return deviceBattery;
        }

        public void setDeviceBattery(ImageView deviceBattery) {
            this.deviceBattery = deviceBattery;
        }

        public void update(DownloadObject dl){
            lastDownload=dl;
            name.setText(dl.getDeviceName());
            direction.setImageResource(R.drawable.trendarrows);

            try {
                direction.setImageLevel(dl.getLastTrend().getVal());
                int r=dl.getLastReading();
                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
                Resources res=getBaseContext().getResources();
                int lowThreshold=Integer.valueOf(sharedPref.getString(dl.getDeviceID() + "_low_threshold", String.valueOf(res.getInteger(R.integer.pref_default_device_low))));
                int highThreshold=Integer.valueOf(sharedPref.getString(dl.getDeviceID() + "_high_threshold", String.valueOf(res.getInteger(R.integer.pref_default_device_high))));

//                int newColor=Color.WHITE;
                if (r>highThreshold) {
                    currentBGColor= Color.rgb(255, 199, 0);
                }else if (r<lowThreshold){
                    currentBGColor=Color.RED;
                } else {
                    currentBGColor=Color.rgb(0,170,0);
                }
                mainBGColor=currentBGColor;
                main_display.setBackgroundColor(mainBGColor);
                bg.setText(String.valueOf(r));

                int dbat=dl.getDeviceBattery();
                deviceBatteryLabel.setText(String.valueOf(dbat));

                deviceBattery.setImageLevel(dbat);

                float ubat=dl.getUploaderBattery();
                uploaderBatteryLabel.setText(String.valueOf((int) ubat));
                uploaderBattery.setImageLevel((int) ubat);


            } catch (NoDataException e) {
                Log.d(TAG,"No data in previous download",e);
            }
        }

        public TextView getName() {
            return name;
        }

        public void setName(TextView name) {
            this.name = name;
        }

        public ImageView getMain_display() {
            return main_display;
        }

        public void setMain_display(ImageView main_display) {
            this.main_display = main_display;
        }

        public ImageView getDirection() {
            return direction;
        }

        public void setDirection(ImageView direction) {
            this.direction = direction;
        }

        public TextView getBg() {
            return bg;
        }

        public void setBg(TextView bg) {
            this.bg = bg;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBounded)
            unbindService(mConnection);
        if (uiReceiver!=null)
            getBaseContext().unregisterReceiver(uiReceiver);
    }

    public class UIReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Constants.UI_UPDATE)){
                Log.d(TAG,"Received a UI update");
                DownloadObject downloadObject=new DownloadObject();
                downloadObject=intent.getParcelableExtra("download");
                String bgReading= null;
                bgReading = String.valueOf(downloadObject.getLastReadingString());
                float uploaderBat=downloadObject.getUploaderBattery();
                int devBattery=downloadObject.getDeviceBattery();
                String devID=downloadObject.getDeviceID();
                Log.i(TAG,"Uploader battery: "+uploaderBat);
                Log.i(TAG,"Device battery: "+devBattery);
                Log.i(TAG,"deviceID: "+devID);
                Log.i(TAG,"Reading: "+bgReading);
                Log.i(TAG,"Name: "+downloadObject.getDeviceName());
                ui.get(devID).update(downloadObject);
                ld=downloadObject;
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putParcelable("lastDownload", ld);
        super.onSaveInstanceState(savedInstanceState);
    }
}
