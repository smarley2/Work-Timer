package com.smarley.worktimer;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.content.ContextCompat;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;

final class GeofenceController {
    static final String ID = "workplace";
    static boolean hasFinePermission(Context c){return ContextCompat.checkSelfPermission(c,Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED;}
    static boolean hasBackgroundPermission(Context c){return Build.VERSION.SDK_INT<29||ContextCompat.checkSelfPermission(c,Manifest.permission.ACCESS_BACKGROUND_LOCATION)==PackageManager.PERMISSION_GRANTED;}
    static PendingIntent intent(Context c){return PendingIntent.getBroadcast(c,30,new Intent(c,GeofenceReceiver.class),PendingIntent.FLAG_UPDATE_CURRENT|(Build.VERSION.SDK_INT>=31?PendingIntent.FLAG_MUTABLE:0));}
    static void sync(Context c){
        Context app=c.getApplicationContext(); SharedPreferences p=app.getSharedPreferences(WorkTimerState.PREFS,Context.MODE_PRIVATE);
        if(!p.getBoolean("geo_enabled",false)||!hasFinePermission(app)||!hasBackgroundPermission(app)){
            p.edit().putBoolean("geofence_registered",false).apply();
            LocationServices.getGeofencingClient(app).removeGeofences(intent(app));
            return;
        }
        double lat=Double.longBitsToDouble(p.getLong("latitude",0)),lon=Double.longBitsToDouble(p.getLong("longitude",0));
        if(lat==0&&lon==0){p.edit().putBoolean("geofence_registered",false).apply();return;}
        Geofence fence=new Geofence.Builder().setRequestId(ID).setCircularRegion(lat,lon,p.getFloat("radius",150))
            .setExpirationDuration(Geofence.NEVER_EXPIRE).setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER|Geofence.GEOFENCE_TRANSITION_EXIT)
            .setNotificationResponsiveness(60000).build();
        GeofencingRequest req=new GeofencingRequest.Builder().setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER).addGeofence(fence).build();
        p.edit().putBoolean("geofence_registered",false).apply();
        try{
            LocationServices.getGeofencingClient(app).addGeofences(req,intent(app))
                .addOnSuccessListener(unused->p.edit().putBoolean("geofence_registered",true).apply())
                .addOnFailureListener(error->p.edit().putBoolean("geofence_registered",false).apply());
        }catch(SecurityException ignored){p.edit().putBoolean("geofence_registered",false).apply();}
    }
}
