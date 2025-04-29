package com.example.diplomaappmodeltflite;

import android.app.Activity;
import android.util.Log;

import com.google.android.libraries.navigation.NavigationApi;
import com.google.android.libraries.navigation.Navigator;

public class NavigationSessionManager {

    private static NavigationSessionManager instance;
    private Navigator navigator;
    private boolean navigationStarted = false;

    private NavigationSessionManager() {
        // Private constructor
    }

    public static synchronized NavigationSessionManager getInstance() {
        if (instance == null) {
            instance = new NavigationSessionManager();
        }
        return instance;
    }

    public void startNavigation(Activity activity, NavigationApi.NavigatorListener listener) {
        if (navigator != null && navigationStarted) {
            Log.d("NavigationSessionManager", "Navigation already running");
            listener.onNavigatorReady(navigator);
            return;
        }

        NavigationApi.getNavigator(activity, new NavigationApi.NavigatorListener() {
            @Override
            public void onNavigatorReady(Navigator nav) {
                navigator = nav;
                navigationStarted = true;
                listener.onNavigatorReady(nav);
            }

            @Override
            public void onError(@NavigationApi.ErrorCode int errorCode) {
                Log.e("NavigationSessionManager", "Navigation error: " + errorCode);
                listener.onError(errorCode);
            }
        });
    }

    public Navigator getNavigator() {
        return navigator;
    }

    public boolean isNavigationRunning() {
        return navigationStarted;
    }

    public void stopNavigation() {
        if (navigator != null) {
            navigator.stopGuidance();
            navigator.cleanup();
            navigator = null;
            navigationStarted = false;
        }
    }
}
