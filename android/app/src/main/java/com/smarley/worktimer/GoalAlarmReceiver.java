package com.smarley.worktimer;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
public class GoalAlarmReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) { new WorkTimerState(context).fireGoalIfReached(); }
}
