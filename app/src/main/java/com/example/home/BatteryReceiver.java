package com.example.home;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;

public class BatteryReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        float batteryPct = level * 100 / (float) scale;

        if (batteryPct <= 1) {
            Intent serviceIntent = new Intent(context, BatterySMSService.class);
            serviceIntent.putExtra("ACTION", "SEND_SMS");
            context.startService(serviceIntent);
        }
    }
}
