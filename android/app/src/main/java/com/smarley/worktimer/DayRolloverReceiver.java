package com.smarley.worktimer;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
public class DayRolloverReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        WorkTimerState state = new WorkTimerState(context);
        state.rollover(); state.scheduleMidnight(); state.scheduleGoal();
    }
}
