package com.smarley.worktimer;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Calendar;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

final class WorkTimerState {
    static final String PREFS = "work_timer_state";
    static final String CHANNEL = "daily_goal";
    private final Context context;
    private final SharedPreferences prefs;

    WorkTimerState(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = this.context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    static void initialize(Context context) {
        WorkTimerState state = new WorkTimerState(context);
        state.createChannel();
        state.rollover();
        state.scheduleMidnight();
        state.scheduleGoal();
        GeofenceController.sync(context);
    }

    synchronized JSONObject snapshot() throws Exception {
        rollover();
        JSONArray sessions = sessions(today());
        long completed = completed(sessions);
        JSONObject out = new JSONObject();
        out.put("day", today());
        out.put("sessions", sessions);
        out.put("completedMs", completed);
        out.put("activeStart", prefs.contains("active_start") ? prefs.getLong("active_start", 0) : JSONObject.NULL);
        out.put("activeSource", prefs.getString("active_source", "manual"));
        out.put("goalMinutes", prefs.getInt("goal_minutes", 480));
        out.put("geoEnabled", prefs.getBoolean("geo_enabled", false));
        out.put("latitude", prefs.contains("latitude") ? Double.longBitsToDouble(prefs.getLong("latitude", 0)) : 0);
        out.put("longitude", prefs.contains("longitude") ? Double.longBitsToDouble(prefs.getLong("longitude", 0)) : 0);
        out.put("radiusMeters", prefs.getFloat("radius", 150));
        out.put("insideGeofence", prefs.getBoolean("inside", false));
        out.put("manualSuppressed", prefs.getBoolean("manual_suppressed", false));
        out.put("fineLocationGranted", GeofenceController.hasFinePermission(context));
        out.put("backgroundLocationGranted", GeofenceController.hasBackgroundPermission(context));
        out.put("geofenceMonitoringActive", prefs.getBoolean("geofence_registered", false));
        out.put("geofenceStateKnown", prefs.getBoolean("geofence_state_known", false));
        return out;
    }

    synchronized void configure(int goalMinutes, boolean geoEnabled, double lat, double lon, float radius) {
        if (goalMinutes < 15 || goalMinutes > 1440) throw new IllegalArgumentException("The goal must be between 15 minutes and 24 hours.");
        if (geoEnabled && (lat < -90 || lat > 90 || lon < -180 || lon > 180 || (lat == 0 && lon == 0))) throw new IllegalArgumentException("Enter a valid workplace location.");
        prefs.edit().putInt("goal_minutes", goalMinutes).putBoolean("geo_enabled", geoEnabled)
            .putLong("latitude", Double.doubleToRawLongBits(lat)).putLong("longitude", Double.doubleToRawLongBits(lon))
            .putFloat("radius", Math.max(100, Math.min(radius, 1000))).remove("goal_notified_day").apply();
        if (!geoEnabled) prefs.edit().putBoolean("inside", false).putBoolean("manual_suppressed", false)
            .putBoolean("geofence_registered", false).putBoolean("geofence_state_known", false).apply();
        GeofenceController.sync(context);
        scheduleGoal();
    }

    synchronized void start(String source) {
        rollover();
        if (prefs.contains("active_start")) return;
        prefs.edit().putLong("active_start", System.currentTimeMillis()).putString("active_source", source).apply();
        scheduleGoal();
    }

    synchronized void stop(boolean manual) {
        rollover();
        if (prefs.contains("active_start")) {
            long start = prefs.getLong("active_start", 0), end = System.currentTimeMillis();
            if (end > start) append(today(), start, end, prefs.getString("active_source", "manual"));
            prefs.edit().remove("active_start").remove("active_source").apply();
        }
        if (manual && prefs.getBoolean("geo_enabled", false) && prefs.getBoolean("inside", false)) {
            prefs.edit().putBoolean("manual_suppressed", true).apply();
        }
        cancelGoal();
    }

    synchronized void onEnter() {
        prefs.edit().putBoolean("inside", true).putBoolean("geofence_state_known", true).apply();
        if (prefs.getBoolean("geo_enabled", false) && !prefs.getBoolean("manual_suppressed", false)) start("geofence");
    }

    synchronized void onExit() {
        prefs.edit().putBoolean("inside", false).putBoolean("manual_suppressed", false).putBoolean("geofence_state_known", true).apply();
        if (prefs.getBoolean("geo_enabled", false)) stop(false);
    }

    synchronized void resetToday() {
        cancelGoal();
        prefs.edit().remove("sessions_" + today()).remove("active_start").remove("active_source").remove("goal_notified_day").apply();
    }

    synchronized void rollover() {
        if (!prefs.contains("active_start")) return;
        long start = prefs.getLong("active_start", 0);
        String startDay = dayOf(start), currentDay = today();
        if (startDay.equals(currentDay)) return;
        String source = prefs.getString("active_source", "manual");
        long cursor = start;
        while (!dayOf(cursor).equals(currentDay)) {
            long boundary = startOfNextDay(cursor);
            append(dayOf(cursor), cursor, boundary, source);
            cursor = boundary;
        }
        prefs.edit().putLong("active_start", cursor).remove("goal_notified_day").apply();
    }

    synchronized void fireGoalIfReached() {
        rollover();
        if (!prefs.contains("active_start")) return;
        long total = completed(sessions(today())) + System.currentTimeMillis() - prefs.getLong("active_start", 0);
        if (total < prefs.getInt("goal_minutes", 480) * 60000L) { scheduleGoal(); return; }
        if (today().equals(prefs.getString("goal_notified_day", ""))) return;
        prefs.edit().putString("goal_notified_day", today()).apply();
        createChannel();
        Intent open = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
        PendingIntent content = PendingIntent.getActivity(context, 20, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder n = new NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm).setContentTitle("Daily goal reached")
            .setContentText("You worked " + (prefs.getInt("goal_minutes",480) / 60.0) + " hours today.")
            .setPriority(NotificationCompat.PRIORITY_HIGH).setAutoCancel(true).setContentIntent(content)
            .setDefaults(NotificationCompat.DEFAULT_ALL);
        context.getSystemService(NotificationManager.class).notify(1001, n.build());
    }

    synchronized void scheduleGoal() {
        cancelGoal();
        rollover();
        if (!prefs.contains("active_start") || today().equals(prefs.getString("goal_notified_day", ""))) return;
        long total = completed(sessions(today())) + System.currentTimeMillis() - prefs.getLong("active_start", 0);
        long remaining = prefs.getInt("goal_minutes", 480) * 60000L - total;
        if (remaining <= 0) { fireGoalIfReached(); return; }
        AlarmManager am = context.getSystemService(AlarmManager.class);
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + remaining, goalIntent());
    }

    void scheduleMidnight() {
        Calendar c = Calendar.getInstance(); c.add(Calendar.DAY_OF_YEAR,1); c.set(Calendar.HOUR_OF_DAY,0);c.set(Calendar.MINUTE,0);c.set(Calendar.SECOND,2);c.set(Calendar.MILLISECOND,0);
        context.getSystemService(AlarmManager.class).setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,c.getTimeInMillis(),midnightIntent());
    }

    private void cancelGoal() { context.getSystemService(AlarmManager.class).cancel(goalIntent()); }
    private PendingIntent goalIntent(){return PendingIntent.getBroadcast(context,10,new Intent(context,GoalAlarmReceiver.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);}
    private PendingIntent midnightIntent(){return PendingIntent.getBroadcast(context,11,new Intent(context,DayRolloverReceiver.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);}
    private JSONArray sessions(String day){try{return new JSONArray(prefs.getString("sessions_"+day,"[]"));}catch(Exception e){return new JSONArray();}}
    private long completed(JSONArray a){long total=0;for(int i=0;i<a.length();i++){JSONObject s=a.optJSONObject(i);if(s!=null)total+=Math.max(0,s.optLong("end")-s.optLong("start"));}return total;}
    private void append(String day,long start,long end,String source){try{JSONArray a=sessions(day);a.put(new JSONObject().put("start",start).put("end",end).put("source",source));prefs.edit().putString("sessions_"+day,a.toString()).apply();}catch(Exception ignored){}}
    private static String today(){return dayOf(System.currentTimeMillis());}
    private static String dayOf(long ms){return new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(new Date(ms));}
    private static long startOfNextDay(long ms){Calendar c=Calendar.getInstance();c.setTimeInMillis(ms);c.add(Calendar.DAY_OF_YEAR,1);c.set(Calendar.HOUR_OF_DAY,0);c.set(Calendar.MINUTE,0);c.set(Calendar.SECOND,0);c.set(Calendar.MILLISECOND,0);return c.getTimeInMillis();}
    private void createChannel(){if(Build.VERSION.SDK_INT>=26){NotificationChannel c=new NotificationChannel(CHANNEL,"Daily goals",NotificationManager.IMPORTANCE_HIGH);c.setDescription("Alerts when the daily work goal is reached");c.enableVibration(true);context.getSystemService(NotificationManager.class).createNotificationChannel(c);}}
}
