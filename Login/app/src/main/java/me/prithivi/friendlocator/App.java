package me.prithivi.friendlocator;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParsePush;
import com.parse.SaveCallback;

/**
 * App - Called before any Activity
 */
public class App extends Application {

    private static Context context;
    private static String ACTIVITY = "App";

    /**
     * Initializer
     */
    public App() {
        context = this;
    }

    /**
     * onCreate()
     */
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(ACTIVITY, "onCreate()");

        ParseObject.registerSubclass(me.prithivi.friendlocator.FriendInvitation.class);
        ParseObject.registerSubclass(me.prithivi.friendlocator.Friends.class);
        ParseObject.registerSubclass(me.prithivi.friendlocator.ActiveConnection.class);
        Parse.initialize(this, "FntDYUygeghtqQGonaEeM48fwTiRXlY9yoNkylf8", "g3yqj7iyZlu6sJgVG72IKXO3sIC4MJW2XN5IYb8I");

        ParsePush.subscribeInBackground("", new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if (e == null) {
                    Log.d(ACTIVITY, "successfully subscribed to the broadcast channel.");
                } else {
                    Log.e(ACTIVITY, "failed to subscribe for push: " + e.getLocalizedMessage());
                }
            }
        });

    }

    /**
     * getAppContext()
     * @return Context
     */
    public static Context getAppContext() {
        return App.context;
    }
}