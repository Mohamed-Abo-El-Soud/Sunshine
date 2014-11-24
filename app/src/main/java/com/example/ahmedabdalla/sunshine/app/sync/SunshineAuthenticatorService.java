package com.example.ahmedabdalla.sunshine.app.sync;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * Created by ahmedabdalla on 2014-11-20.
 */
public class SunshineAuthenticatorService extends Service{
    // Instance field that stores the authenticator object
    private SunshineAuthenticator mAuthenticator;

    @Override
    public void onCreate() {
        // Create a new authenticator object
        mAuthenticator = new SunshineAuthenticator(this);
    }

    /*
     * When the system binds to this Service to make the RPC call
     * return the authenticator's IBinder.
     */
    @Override
    public IBinder onBind(Intent intent) {
        return mAuthenticator.getIBinder();
    }
}
