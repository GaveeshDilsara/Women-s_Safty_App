package com.example.home;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.telephony.SmsManager;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

public class BatterySMSService extends Service {

    private static final String CHANNEL_ID = "BatterySMSServiceChannel";
    private FusedLocationProviderClient fusedLocationClient;

    @Override
    public void onCreate() {
        super.onCreate();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        createNotificationChannel();

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Battery SMS Service")
                .setContentText("Monitoring battery level...")
                .setSmallIcon(R.drawable.ic_notification)
                .build();
        startForeground(1, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && "SEND_SMS".equals(intent.getStringExtra("ACTION"))) {
            sendLocationAndSms();
        }
        return START_NOT_STICKY;
    }

    private void sendLocationAndSms() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permissions not granted", Toast.LENGTH_SHORT).show();
            return;
        }

        fusedLocationClient.getCurrentLocation(LocationRequest.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            String message = "Battery is low! My Location is: " + "https://maps.google.com/?q=" + location.getLatitude() + "," + location.getLongitude();
                            sendSmsToEmergencyContacts(message);
                        } else {
                            Toast.makeText(BatterySMSService.this, "Location not available", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(BatterySMSService.this, "Failed to get location: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void sendSmsToEmergencyContacts(String message) {
        SharedPreferences sharedPreferences = getSharedPreferences("EmergencyContacts", MODE_PRIVATE);

        String contact1 = sharedPreferences.getString("contact1", "");
        String contact2 = sharedPreferences.getString("contact2", "");
        String contact3 = sharedPreferences.getString("contact3", "");

        SmsManager smsManager = SmsManager.getDefault();
        try {
            if (!contact1.isEmpty()) {
                smsManager.sendTextMessage(contact1, null, message, null, null);
            }
            if (!contact2.isEmpty()) {
                smsManager.sendTextMessage(contact2, null, message, null, null);
            }
            if (!contact3.isEmpty()) {
                smsManager.sendTextMessage(contact3, null, message, null, null);
            }
            Toast.makeText(this, "Battery level is low. SMS sent to emergency contacts!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "SMS failed to send: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Battery SMS Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopForeground(true);
    }
}
