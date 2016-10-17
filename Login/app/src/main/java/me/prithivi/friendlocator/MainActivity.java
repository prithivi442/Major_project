package me.prithivi.friendlocator;

import android.app.ActionBar;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.TabActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.parse.ParseACL;
import com.parse.ParseException;
import com.parse.ParseInstallation;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

/**
 * Main Activity - user comes here after login
 */
public class MainActivity extends TabActivity implements
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener,
        LocationListener {

    private static String ACTIVITY = "MainActivity";
    private static final int MILLISECONDS_PER_SECOND = 1000;
    private static final int UPDATE_INTERVAL_IN_SECONDS = 5;
    private static final long UPDATE_INTERVAL = MILLISECONDS_PER_SECOND * UPDATE_INTERVAL_IN_SECONDS;
    private static final int FASTEST_INTERVAL_IN_SECONDS = 5;
    private static final long FASTEST_INTERVAL = MILLISECONDS_PER_SECOND * FASTEST_INTERVAL_IN_SECONDS;
    private static final float SMALLEST_DISPLACEMENT_IN_METERS = 2f;
    private static final double LOG_DISTANCE_CHANGE_TO_DB = 1.0;

    private static double VIBRATE_DISTANCE = 10.0;

    private static Context context;
    private ActionBar actionBar;
    private int defaultTab = -1;
    private String friendEmail = null;
    private String friendName = null;
    private String invitor = null;
    private Boolean pushReceived = null;
    private Boolean declined = null;
    public LocationManager locationManager;
    public Location lastKnownLocation;
    private Location currentLocation;
    private double distanceChanged;

    LocationRequest locationRequest;
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor sharedPreferencesEditor;
    LocationClient locationClient;
    boolean updatesRequested;

    /**
     * onCreate()
     * @param savedInstanceState
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(ACTIVITY, "Main Activity");
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);

        // Save the current Installation to Parse.
        ParseInstallation installation = ParseInstallation.getCurrentInstallation();
        installation.put("user", ParseUser.getCurrentUser());
        installation.put("userEmail", ParseUser.getCurrentUser().getEmail());

        ParseACL acl = new ParseACL();
        acl.setPublicReadAccess(true);
        acl.setPublicWriteAccess(true);
        installation.setACL(acl);

        installation.saveInBackground();

        this.locationRequest = LocationRequest.create();
        this.locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        this.locationRequest.setInterval(UPDATE_INTERVAL);
        this.locationRequest.setFastestInterval(FASTEST_INTERVAL);
        this.locationRequest.setSmallestDisplacement(SMALLEST_DISPLACEMENT_IN_METERS);
        this.sharedPreferences = getSharedPreferences("SharedPreferences", Context.MODE_PRIVATE);
        this.sharedPreferencesEditor = this.sharedPreferences.edit();
        this.locationClient = new LocationClient(this, this, this);
        this.updatesRequested = false;
        this.locationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        this.lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

        this.updateDatabase(this.lastKnownLocation);

        try {
            Log.d(ACTIVITY, "LOCATION LATITUDE: " + this.lastKnownLocation.getLatitude());
        } catch(Exception e) {
            Log.d(ACTIVITY, "NO LOCATION LATITUDE YET");
        }
    }

    /**
     * onPause()
     */
    @Override
    protected void onPause() {
        super.onPause();
        Log.d(ACTIVITY, "onPause");
        this.sharedPreferencesEditor.putBoolean("KEY_UPDATES_ON", this.updatesRequested);
        this.sharedPreferencesEditor.commit();
    }

    /**
     * onStart()
     */
    @Override
    protected void onStart() {
        super.onStart();
        Log.d(ACTIVITY, "onStart");
        this.locationClient.connect();
    }

    /**
     * onResume()
     */
    @Override
    protected void onResume() {
        super.onResume();
        Log.d(ACTIVITY, "onResume");
        if(isNetworkAvailable()) {
            this.getExtras();
        }
        else {
            Log.d(ACTIVITY, "No network connection!");
            this.goToActivity("Login", null, null);
        }
    }

    /**
     * onStop()
     */
    @Override
    protected void onStop() {
        super.onStop();
        Log.d(ACTIVITY, "onStop");
    }

    /**
     * onDestroy()
     */
    protected void onDestroy() {
        super.onDestroy();
        Log.d(ACTIVITY, "onDestroy");
        this.stopPeriodicUpdates();
        if (ParseUser.getCurrentUser() != null) {
            ParseUser user = ParseUser.getCurrentUser();
            user.put("isOnline", false);
            user.saveInBackground();
            this.locationClient.disconnect();
        }
    }

    /**
     * getExtras()
     */
    private void getExtras() {
        Bundle extras = this.getIntent().getExtras();
        if(extras != null) {
            this.defaultTab = extras.getInt("defaultTab");
            Log.d(ACTIVITY, "Default tab: " + this.defaultTab);

            this.friendEmail = extras.getString("FriendEmail");
            Log.d(ACTIVITY, "Friend Email: " + this.friendEmail);

            this.friendName = extras.getString("FriendName");
            Log.d(ACTIVITY, "Friend Name: " + this.friendName);

            this.invitor = extras.getString("invitor");
            Log.d(ACTIVITY, "Invitor Name: " + this.invitor);

            this.pushReceived = extras.getBoolean("pushReceived");
            Log.d(ACTIVITY, "Push Received: " + this.pushReceived);

            this.declined = extras.getBoolean("declined");
            Log.d(ACTIVITY, "Push Received: " + this.pushReceived);
        }

        this.goToActivity("Friends", null, null);

        Log.d(ACTIVITY, "DECLINED VALUE: "+this.declined);

        if(this.invitor!=null){
            goToActivity("Map", null, null);
        }
        else if(this.friendEmail!=null && this.friendName!=null){
            this.goToActivity("Map", this.friendEmail, this.friendName);
        }
        else if(this.friendEmail!=null ){
            this.goToActivity("Map", this.friendEmail, null);
        }
        else{
            this.goToActivity("Map", null, null);
        }
        this.goToActivity("Settings", null, null);
    }

    /**
     * gotToActivity()
     * @param activityName
     * @param friendEmail
     */
    public void goToActivity(String activityName, String friendEmail, String friendName) {

        TabHost tabHost = (TabHost)findViewById(android.R.id.tabhost);

        if(activityName.equals("Login")) {
            Intent loginIntent = new Intent(this, LoginActivity.class);
            startActivity(loginIntent);
        }

        if(activityName.equals("Friends")) {
            // Friends Tab
            TabSpec friendsTab = tabHost.newTabSpec("Friends");
            // Friends Tab Indicator View
            View friendsTabIndicator = LayoutInflater.from(this).inflate(R.layout.bottom_tab, getTabWidget(), false);
            ((TextView) friendsTabIndicator.findViewById(R.id.title)).setText("Friends");
            ((ImageView) friendsTabIndicator.findViewById(R.id.image)).setImageResource(R.drawable.icon_friends_tab);
            // Add view to friends tab
            friendsTab.setIndicator(friendsTabIndicator);
            Intent friendsIntent = new Intent(this, FriendsActivity.class);
            friendsTab.setContent(friendsIntent);
            tabHost.addTab(friendsTab);
        }
        else if(activityName.equals("Map")) {

            // Map Tab
            TabSpec mapTab = tabHost.newTabSpec("Map");
            // Map Tab Indicator View
            View mapTabIndicator = LayoutInflater.from(this).inflate(R.layout.bottom_tab, getTabWidget(), false);
            ((TextView) mapTabIndicator.findViewById(R.id.title)).setText("Map");
            ((ImageView) mapTabIndicator.findViewById(R.id.image)).setImageResource(R.drawable.icon_map_tab);
            // Add view to map tab
            mapTab.setIndicator(mapTabIndicator);
            Intent mapIntent = new Intent(this, MapActivity.class);
            if (this.declined != null && this.declined == true) {
                Log.d(ACTIVITY, "Not adding anything to extras");
            } else {
                if (this.invitor != null) {
                    mapIntent.putExtra("invitor", this.invitor);
                } else if (friendEmail != null && friendName != null) {
                    mapIntent.putExtra("FriendEmail", friendEmail);
                    mapIntent.putExtra("FriendName", friendName);
                } else if (friendEmail != null) {
                    mapIntent.putExtra("FriendEmail", friendEmail);
                }
                if (this.pushReceived != null) {
                    mapIntent.putExtra("pushReceived", this.pushReceived);
                }
            }
            mapTab.setContent(mapIntent);
            tabHost.addTab(mapTab);
        }
        else if(activityName.equals("Settings")) {

            // Settings Tab
            TabSpec settingsTab = tabHost.newTabSpec("Settings");
            // Settings Tab Indicator View
            View settingsTabIndicator = LayoutInflater.from(this).inflate(R.layout.bottom_tab, getTabWidget(), false);
            ((TextView) settingsTabIndicator.findViewById(R.id.title)).setText("Settings");
            ((ImageView) settingsTabIndicator.findViewById(R.id.image)).setImageResource(R.drawable.icon_settings_tab);
            // Add view to settings tab
            settingsTab.setIndicator(settingsTabIndicator);
            Intent settingsIntent = new Intent(this, SettingsActivity.class);
            settingsTab.setContent(settingsIntent);
            tabHost.addTab(settingsTab);
        }

        if(this.declined!=null && this.declined == true){
            tabHost.setCurrentTab(0);
        }
        else if(this.invitor!=null){
            tabHost.setCurrentTab(1);
        }
        else if(friendEmail!=null){
            tabHost.setCurrentTab(1);
        }
    }

    /**
     * onConnected()
     * @param dataBundle
     */
    @Override
    public void onConnected(Bundle dataBundle) {
        Log.d(ACTIVITY, "onConnected");
        if(this.updatesRequested) {
            this.locationClient.requestLocationUpdates(this.locationRequest, this);
        }
        this.currentLocation = getLocation();
        this.startPeriodicUpdates();
    }

    /**
     * onDisconnected()
     */
    @Override
    public void onDisconnected() {
        Log.d(ACTIVITY, "onDisconnected");
    }

    /**
     * onConnectionFailed
     * @param connectionResult
     */
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(ACTIVITY, "onConnectionFailed");
        showErrorDialog(connectionResult.getErrorCode());
    }

    /**
     * ErrorDialogFragment Class inner
     */
    public static class ErrorDialogFragment extends DialogFragment {

        private Dialog errorDialog;

        /**
         * Initializer
         */
        public ErrorDialogFragment() {
            super();
            this.errorDialog = null;
        }

        /**
         * setDialog()
         * @param dialog
         */
        public void setDialog(Dialog dialog) {
            this.errorDialog = dialog;
        }

        /**
         * onCreateDialog()
         * @param savedInstanceState
         * @return Dialog
         */
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return this.errorDialog;
        }
    }

    /**
     * servicesConnected()
     * @return boolean
     */
    private boolean servicesConnected() {
        Log.d(ACTIVITY, "servicesConnected");
        int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getApplicationContext());

        if (status == ConnectionResult.SUCCESS) {
            return true;
        }
        else if (status == ConnectionResult.SERVICE_MISSING ||
                status == ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED ||
                status == ConnectionResult.SERVICE_DISABLED) {
            Dialog dialog = GooglePlayServicesUtil.getErrorDialog(status, this, 1);
            dialog.show();
        }
        return false;
    }

    /**
     * onLocationChanged() listener - listens for location changes
     * @param location
     */
    public void onLocationChanged(Location location) {
        Log.d(ACTIVITY, "MainActivity onLocationChanged()");
        if(isNetworkAvailable()) {
            this.currentLocation = location;
            String locationString = "Updated Location: " + this.currentLocation.getLatitude() + ", " + this.currentLocation.getLongitude();
            Log.d(ACTIVITY, locationString);
            if (this.lastKnownLocation != null) {
                this.distanceChanged = this.roundDistance(this.currentLocation.distanceTo(this.lastKnownLocation));
                Log.d(ACTIVITY, "Distance Changed: " + this.distanceChanged);
                if (this.distanceChanged >= LOG_DISTANCE_CHANGE_TO_DB) {
                    this.updateDatabase(this.currentLocation);
                }
                if(this.friendEmail!=null){
                    Location friendLocation = this.getUserLocation(this.friendEmail);
                    double distance;
                    try {
                        distance = this.currentLocation.distanceTo(friendLocation);
                    } catch (Exception e) {
                        Log.d(ACTIVITY, "Error getting location: " + e);
                        distance = -1.0;
                    }
                }
            }
            this.lastKnownLocation = location;
        }
    }

    /**
     * updateDatabase() - update DB with new user location
     * @param location
     */
    private void updateDatabase(Location location) {
        if(isNetworkAvailable()) {
            ParseUser user = ParseUser.getCurrentUser();
            try {
                user.put("latitude", location.getLatitude());
                user.put("longitude", location.getLongitude());
                user.saveInBackground(new SaveCallback() {
                    public void done(ParseException e) {
                        if (e != null) {
                            Log.d(ACTIVITY, "Error saving location! " + e.getLocalizedMessage());
                        } else {
                            Log.d(ACTIVITY, "Location saved successfully");
                        }
                    }
                });
            } catch (Exception e) {
                Log.d(ACTIVITY, "Error: " + e);
            }
        }
    }

    /**
     * getLocation()
     * @return Location
     */
    private Location getLocation() {
        if (this.servicesConnected()) {
            return this.locationClient.getLastLocation();
        } else {
            return null;
        }
    }

    /**
     * startPeriodicUpdates() - start location updates
     */
    private void startPeriodicUpdates() {
        this.locationClient.requestLocationUpdates(locationRequest, this);
    }

    /**
     * stopPeriodicUpdates() - stop location updates
     */
    private void stopPeriodicUpdates() {
        this.locationClient.removeLocationUpdates(this);
    }

    /**
     * showErrorDialog()
     * @param code
     */
    public void showErrorDialog(int code) {
        Toast.makeText(this, "Error code: " + code, Toast.LENGTH_SHORT).show();
    }

    /**
     * roundDistance() - rounds distance to ##.##
     * @param number
     * @return
     */
    public double roundDistance(double number) {
        double n = Math.round(number * 100);
        n = n/100;

        return n;
    }

    /**
     * getActivityContext() - gets activity context, good to use in inner classes
     * @return Context
     */
    public static Context getActivityContext() {
        return MainActivity.context;
    }

    /**
     * isNetworkAvailable()
     * @return boolean
     */
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivitymanager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivitymanager.getActiveNetworkInfo();
        return networkInfo !=null && networkInfo.isConnected();
    }

    /**
     * vibrate
     * @param milliseconds
     */
    private void vibrate(int milliseconds) {
        Vibrator v = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
        v.vibrate(milliseconds);
    }

    /**
     * getUserByEmail()
     * @param emailAddress
     * @return ParseUser
     */
    private ParseUser getUserByEmail(String emailAddress) {
        ParseUser user = null;
        ParseQuery<ParseUser> query = ParseUser.getQuery();
        query.whereEqualTo("username", emailAddress);
        try {
            user = query.getFirst().fetchIfNeeded();
        } catch (ParseException e) {
            Log.d(ACTIVITY, e.getLocalizedMessage());
        }
        return user;
    }

    /**
     * getUserLocation() - gets location of a friend
     * @return
     */
    private Location getUserLocation(String email) {
        Log.d(ACTIVITY, "getUserLocation() for " + email);
        Location location = new Location(LocationManager.GPS_PROVIDER);

        ParseUser user = this.getUserByEmail(email);

        location.setLatitude((Double)user.get("latitude"));
        location.setLongitude((Double)user.get("longitude"));

        Log.d(ACTIVITY, "USER's EMAIL   : " + email);
        Log.d(ACTIVITY, "USER's LOCATION: " + location);

        return location;
    }

    /**
     * capitalizeString()
     * @param str
     * @return String
     */
    public String capitalizeString(String str) {
        String newString = str.substring(0, 1).toUpperCase() + str.substring(1);
        return newString;
    }

    /**
     * toastIt() - toast used for form verification
     * @param message
     */
    public void toastIt(String message) {
        Log.d(ACTIVITY, message);
        Toast t = Toast.makeText(getApplicationContext(), this.capitalizeString(message), Toast.LENGTH_LONG);
        t.setGravity(Gravity.CENTER, 0, 0);
        t.show();
    }

}