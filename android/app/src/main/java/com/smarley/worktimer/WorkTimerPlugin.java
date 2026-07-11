package com.smarley.worktimer;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.google.android.gms.location.LocationServices;

@CapacitorPlugin(name="WorkTimer")
public class WorkTimerPlugin extends Plugin {
    @PluginMethod public void getState(PluginCall call){try{call.resolve(JSObject.fromJSONObject(new WorkTimerState(getContext()).snapshot()));}catch(Exception e){call.reject(e.getMessage());}}
    @PluginMethod public void startManual(PluginCall call){new WorkTimerState(getContext()).start("manual");call.resolve();}
    @PluginMethod public void stopManual(PluginCall call){new WorkTimerState(getContext()).stop(true);call.resolve();}
    @PluginMethod public void resetDay(PluginCall call){new WorkTimerState(getContext()).resetToday();call.resolve();}
    @PluginMethod public void configure(PluginCall call){try{new WorkTimerState(getContext()).configure(call.getInt("goalMinutes",480),Boolean.TRUE.equals(call.getBoolean("geoEnabled",false)),call.getDouble("latitude",0.0),call.getDouble("longitude",0.0),call.getFloat("radiusMeters",150f));call.resolve();}catch(Exception e){call.reject(e.getMessage());}}
    @PluginMethod public void requestPermissions(PluginCall call){
        java.util.ArrayList<String> needed=new java.util.ArrayList<>();
        if(ContextCompat.checkSelfPermission(getContext(),Manifest.permission.ACCESS_COARSE_LOCATION)!=PackageManager.PERMISSION_GRANTED)needed.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        if(ContextCompat.checkSelfPermission(getContext(),Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED)needed.add(Manifest.permission.ACCESS_FINE_LOCATION);
        if(Build.VERSION.SDK_INT>=33&&ContextCompat.checkSelfPermission(getContext(),Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)needed.add(Manifest.permission.POST_NOTIFICATIONS);
        if(!needed.isEmpty()){ActivityCompat.requestPermissions(getActivity(),needed.toArray(new String[0]),909);JSObject result=new JSObject();result.put("next","Select precise location. Then tap again to allow location all the time.");call.resolve(result);return;}
        if(Build.VERSION.SDK_INT>=29&&!GeofenceController.hasBackgroundPermission(getContext())){Intent i=new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:"+getContext().getPackageName()));getActivity().startActivity(i);}
        else GeofenceController.sync(getContext());
        call.resolve();
    }
    @PluginMethod public void getCurrentLocation(PluginCall call){
        if(!GeofenceController.hasFinePermission(getContext())){call.reject("Allow precise location first.");return;}
        try{LocationServices.getFusedLocationProviderClient(getContext()).getCurrentLocation(com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,null).addOnSuccessListener(location->{if(location==null)call.reject("Current location is unavailable.");else{JSObject result=new JSObject();result.put("latitude",location.getLatitude());result.put("longitude",location.getLongitude());call.resolve(result);}}).addOnFailureListener(e->call.reject(e.getMessage()));}catch(SecurityException e){call.reject("Location permission was not granted.");}
    }
}
