package com.example.home;

import static com.example.home.AddFriendsFragment.REQUEST_CALL_PHONE;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

public class MainActivity extends AppCompatActivity {

    // Request codes for permissions and settings
    private static final int REQUEST_VIDEO_CAPTURE = 5;
    private static final int REQUEST_CAMERA_PERMISSION = 6;
    private Uri videoUri; // To store the video URI

    private static final int REQUEST_SMS_PERMISSION = 2;
    private static final int REQUEST_LOCATION_PERMISSION = 3;
    private static final int REQUEST_CHECK_SETTINGS = 4;

    // UI components
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ActionBarDrawerToggle drawerToggle;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private BroadcastReceiver batteryReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Start the Foreground Service
        Intent serviceIntent = new Intent(this, BatterySMSService.class);
        ContextCompat.startForegroundService(this, serviceIntent);

        // Register the battery receiver
        BatteryReceiver batteryReceiver = new BatteryReceiver();
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(batteryReceiver, filter);

        // Setup the Toolbar
        Toolbar toolbar = findViewById(R.id.tool_bar);
        setSupportActionBar(toolbar);

        // Setup the Drawer Layout and Navigation View
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.open, R.string.close);
        drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();

        // Setup Navigation Drawer Listener
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                handleNavigationItemSelected(item);
                drawerLayout.closeDrawer(GravityCompat.START); // Close the drawer after item click
                return true;
            }
        });

        // Setup Bottom Navigation View
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnNavigationItemSelectedListener(navListener);

        // Set the default fragment to FragmentHome if no saved state exists
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment, new FragmentHome()).commit();
        }

        // Initialize the location client for fetching user's location
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Initialize the Location Callback to handle location updates
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    sendEmergencyMessage("Location unavailable.");
                    return;
                }
                Location location = locationResult.getLastLocation();
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                String locationMessage = "Emergency! Please help me immediately. My location is: " +
                        "https://maps.google.com/?q=" + latitude + "," + longitude;
                sendEmergencyMessage(locationMessage);
                fusedLocationClient.removeLocationUpdates(locationCallback);
            }
        };
    }

    // Listener for Bottom Navigation items
    private BottomNavigationView.OnNavigationItemSelectedListener navListener =
            new BottomNavigationView.OnNavigationItemSelectedListener() {
                @Override
                public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                    Fragment selectedFragment = null;
                    int itemId = item.getItemId();

                    if (itemId == R.id.home) {
                        selectedFragment = new FragmentHome();
                    } else if (itemId == R.id.recording) {
                        selectedFragment = new RecordingFragment();
                    } else if (itemId == R.id.sos) {
                        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                            requestSmsPermission();
                        } else if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            requestLocationPermission();
                        } else {
                            sendEmergencyMessageWithLocation();
                        }
                        selectedFragment = new SOSFragment();
                    } else if (itemId == R.id.call24) {
                        selectedFragment = new Call247Fragment();
                    } else if (itemId == R.id.add_friends) {
                        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CALL_PHONE}, REQUEST_CALL_PHONE);
                        } else {
                            selectedFragment = new AddFriendsFragment();
                        }
                    }

                    if (selectedFragment != null) {
                        getSupportFragmentManager().beginTransaction()
                                .replace(R.id.fragment, selectedFragment)
                                .addToBackStack(null)
                                .commit();
                    }

                    return true;
                }
            };

    private void dispatchTakeVideoIntent() {
        Intent takeVideoIntent = new Intent(android.provider.MediaStore.ACTION_VIDEO_CAPTURE);
        if (takeVideoIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takeVideoIntent, REQUEST_VIDEO_CAPTURE);
        } else {
            // Log an error message if no activity can handle the intent
            Toast.makeText(this, "Unable to start video recording.", Toast.LENGTH_SHORT).show();
            Log.e("MainActivity", "No application can handle the ACTION_VIDEO_CAPTURE intent.");
        }
    }

    private void handleNavigationItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == R.id.home) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment, new FragmentHome())
                    .addToBackStack(null)
                    .commit();
        } else if (itemId == R.id.about) {
            Intent intent = new Intent(MainActivity.this, AboutUs.class);
            startActivity(intent);
        } else if (itemId == R.id.share) {
            shareApp();
        } else if (itemId == R.id.rate_us) {
            showRatingDialog();
        } else if (itemId == R.id.self_defence) {
            Intent intent = new Intent(MainActivity.this, Defence.class);
            startActivity(intent);
        }
    }

    private void showRatingDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.rateus, null);

        RatingBar ratingBar = view.findViewById(R.id.ratingBar);
        Button submitButton = view.findViewById(R.id.btn_submit_rating);
        TextView averageRatingTextView = view.findViewById(R.id.tv_average_rating);

        SharedPreferences prefs = getSharedPreferences("AppRatings", MODE_PRIVATE);
        final float[] averageRating = {prefs.getFloat("averageRating", 0)};
        final int[] ratingCount = {prefs.getInt("ratingCount", 0)};

        // Display the average rating
        averageRatingTextView.setText("Average Rating: " + (ratingCount[0] > 0 ? averageRating[0] : "No ratings yet"));

        submitButton.setOnClickListener(v -> {
            float userRating = ratingBar.getRating();
            if (userRating > 0) {
                // Update rating in SharedPreferences
                float totalRating = averageRating[0] * ratingCount[0] + userRating;
                ratingCount[0]++;
                averageRating[0] = totalRating / ratingCount[0];

                SharedPreferences.Editor editor = prefs.edit();
                editor.putFloat("averageRating", averageRating[0]);
                editor.putInt("ratingCount", ratingCount[0]);
                editor.apply();

                // Update UI
                averageRatingTextView.setText("Average Rating: " + averageRating[0]);
                Toast.makeText(MainActivity.this, "Thank you for your rating!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, "Please select a rating", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setView(view);
        builder.create().show();
    }

    private void shareApp() {
        String shareMessage = "Check out this amazing safety app, Her Guardian! You can download it from the following link:\n\n" +
                "https://play.google.com/store/apps/details?id=" + getPackageName() + "\n\n" +
                "Stay safe and protected!";

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Her Guardian Safety App");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage);

        startActivity(Intent.createChooser(shareIntent, "Share Her Guardian via"));
    }

    private void requestSmsPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, REQUEST_SMS_PERMISSION);
    }

    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_SMS_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    requestLocationPermission();
                } else {
                    sendEmergencyMessageWithLocation();
                }
            } else {
                Toast.makeText(this, "SMS permission denied", Toast.LENGTH_SHORT).show();
            }
        }
        else if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                sendEmergencyMessageWithLocation();
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
        else if (requestCode == REQUEST_CALL_PHONE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment, new AddFriendsFragment())
                        .addToBackStack(null)
                        .commit();
            } else {
                Toast.makeText(this, "Call permission denied", Toast.LENGTH_SHORT).show();
            }
        }
        else if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                dispatchTakeVideoIntent();
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void sendEmergencyMessageWithLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestLocationPermission();
            return;
        }

        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(10000); // 10 seconds
        locationRequest.setFastestInterval(5000); // 5 seconds

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);

        SettingsClient settingsClient = LocationServices.getSettingsClient(this);
        settingsClient.checkLocationSettings(builder.build())
                .addOnSuccessListener(new OnSuccessListener<LocationSettingsResponse>() {
                    @Override
                    public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        if (e instanceof com.google.android.gms.common.api.ResolvableApiException) {
                            try {
                                com.google.android.gms.common.api.ResolvableApiException resolvable = (com.google.android.gms.common.api.ResolvableApiException) e;
                                resolvable.startResolutionForResult(MainActivity.this, REQUEST_CHECK_SETTINGS);
                            } catch (IntentSender.SendIntentException sendEx) {
                                sendEmergencyMessage("Emergency! Please help me immediately. Location unavailable.");
                            }
                        } else {
                            sendEmergencyMessage("Emergency! Please help me immediately. Location unavailable.");
                        }
                    }
                });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CHECK_SETTINGS) {
            if (resultCode == RESULT_OK) {
                sendEmergencyMessageWithLocation();
            } else {
                sendEmergencyMessage("Emergency! Please help me immediately. Location unavailable.");
            }
        }
        else if (requestCode == REQUEST_VIDEO_CAPTURE && resultCode == RESULT_OK) {
            videoUri = data.getData();
            if (videoUri != null) {
                Toast.makeText(this, "Video recorded successfully: " + videoUri.toString(), Toast.LENGTH_SHORT).show();
                // Optionally, add code to handle the videoUri (e.g., play the video or save it)
            } else {
                Toast.makeText(this, "Video recording failed, no data returned.", Toast.LENGTH_SHORT).show();
                Log.e("MainActivity", "Video recording failed, no data returned.");
            }
        } else if (requestCode == REQUEST_VIDEO_CAPTURE) {
            Toast.makeText(this, "Video recording cancelled.", Toast.LENGTH_SHORT).show();
            Log.i("MainActivity", "Video recording was cancelled.");
        }
    }

    private void sendEmergencyMessage(String message) {
        SharedPreferences sharedPreferences = getSharedPreferences("EmergencyContacts", MODE_PRIVATE);

        String contact1 = sharedPreferences.getString("contact1", "");
        String contact2 = sharedPreferences.getString("contact2", "");
        String contact3 = sharedPreferences.getString("contact3", "");

        if (!contact1.isEmpty()) {
            sendSms(contact1, message);
        }
        if (!contact2.isEmpty()) {
            sendSms(contact2, message);
        }
        if (!contact3.isEmpty()) {
            sendSms(contact3, message);
        }

        Toast.makeText(this, "Emergency message sent to contacts", Toast.LENGTH_SHORT).show();
    }

    private void sendSms(String phoneNumber, String message) {
        SmsManager smsManager = SmsManager.getDefault();
        smsManager.sendTextMessage(phoneNumber, null, message, null, null);
    }

    // Public method to expose the SharedPreferences
    public SharedPreferences getEmergencyContactsPreferences() {
        return getSharedPreferences("EmergencyContacts", MODE_PRIVATE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(batteryReceiver);
    }

}
