package com.smarley.worktimer;

import com.getcapacitor.BridgeActivity;
import android.os.Bundle;

public class MainActivity extends BridgeActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        registerPlugin(WorkTimerPlugin.class);
        super.onCreate(savedInstanceState);
        WorkTimerState.initialize(this);
    }
}
