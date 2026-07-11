package com.smarley.worktimer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

public class GeofenceReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        GeofencingEvent event = GeofencingEvent.fromIntent(intent);
        if (event == null || event.hasError()) return;
        WorkTimerState state = new WorkTimerState(context);
        if (event.getGeofenceTransition() == Geofence.GEOFENCE_TRANSITION_ENTER) state.onEnter();
        else if (event.getGeofenceTransition() == Geofence.GEOFENCE_TRANSITION_EXIT) state.onExit();
    }
}
