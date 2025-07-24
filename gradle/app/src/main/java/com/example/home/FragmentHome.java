package com.example.home;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.telephony.SmsManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.example.home.network.PlacesService;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class FragmentHome extends Fragment {

    private SupportMapFragment supportMapFragment;
    private Button btnMyLocation, btnPanic;

    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationCallback locationCallback;
    private GoogleMap googleMap;
    private Location currentLocation;

    private PlacesClient placesClient;
    private PlacesService placesService;

    private boolean isPanicModeOn = false;
    private Handler handler;
    private Runnable sendLocationRunnable;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_home, container, false);

        SharedPreferences emergencyContactsPreferences = ((MainActivity) getActivity()).getEmergencyContactsPreferences();

        supportMapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.google_map);
        Button btnHospitals = rootView.findViewById(R.id.btn_hospitals);
        Button btnPolice = rootView.findViewById(R.id.btn_police);
        Button btnPharmacies = rootView.findViewById(R.id.btn_other_places);
        btnMyLocation = rootView.findViewById(R.id.btn_my_location);
        btnPanic = rootView.findViewById(R.id.panic_button);

        if (supportMapFragment == null) {
            Toast.makeText(getActivity(), "Error initializing map fragment", Toast.LENGTH_SHORT).show();
            return rootView;
        }

        Places.initialize(getActivity().getApplicationContext(), "AIzaSyDJ1_q3v8_kgWPZtci0BGZSpUmprdss6F4");
        placesClient = Places.createClient(getActivity());

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://maps.googleapis.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        placesService = retrofit.create(PlacesService.class);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(getActivity());

        handler = new Handler(Looper.getMainLooper());

        Dexter.withContext(getActivity()).withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse response) {
                        getCurrentLocation();
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse response) {
                        Toast.makeText(getActivity(), "Permission Denied", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest request, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                }).check();

        btnHospitals.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentLocation != null) {
                    searchNearbyPlaces("hospital");
                }
            }
        });

        btnPolice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentLocation != null) {
                    searchNearbyPlaces("police");
                }
            }
        });

        btnPharmacies.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentLocation != null) {
                    searchNearbyPlaces("pharmacy");
                }
            }
        });

        btnMyLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentLocation != null) {
                    LatLng latLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
                } else {
                    Toast.makeText(getActivity(), "Current location not available", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnPanic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isPanicModeOn) {
                    turnOffPanicMode();
                } else {
                    turnOnPanicMode();
                }
            }
        });

        supportMapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(@NonNull GoogleMap map) {
                googleMap = map;

                googleMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                    @Override
                    public boolean onMarkerClick(Marker marker) {
                        LatLng destinationLatLng = marker.getPosition();
                        String uri = "http://maps.google.com/maps?saddr=" + currentLocation.getLatitude() + "," + currentLocation.getLongitude() + "&daddr=" + destinationLatLng.latitude + "," + destinationLatLng.longitude;
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                        intent.setPackage("com.google.android.apps.maps");
                        startActivity(intent);
                        return true;
                    }
                });
            }
        });

        return rootView;
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
            return;
        }

        supportMapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(@NonNull GoogleMap map) {
                googleMap = map;

                LocationRequest locationRequest = LocationRequest.create()
                        .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                        .setInterval(5000)
                        .setFastestInterval(20000);

                locationCallback = new LocationCallback() {
                    @Override
                    public void onLocationResult(LocationResult locationResult) {
                        if (locationResult == null) {
                            return;
                        }
                        for (Location location : locationResult.getLocations()) {
                            currentLocation = location; // Update current location
                            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                            googleMap.clear();
                            googleMap.addMarker(new MarkerOptions().position(latLng).title("Current Location").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
                            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
                        }
                    }
                };

                fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null);
            }
        });
    }

    private void searchNearbyPlaces(String placeType) {

        LatLng currentLatLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
        googleMap.clear();
        googleMap.addMarker(new MarkerOptions().position(currentLatLng).title("Current Location").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));

        String location = currentLocation.getLatitude() + "," + currentLocation.getLongitude();

        placesService.getNearbyPlaces(location, 5000, placeType, "AIzaSyDJ1_q3v8_kgWPZtci0BGZSpUmprdss6F4").enqueue(new Callback<PlacesResponse>() {
            @Override
            public void onResponse(Call<PlacesResponse> call, Response<PlacesResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<PlaceResult> places = response.body().results;
                    for (PlaceResult place : places) {
                        LatLng latLng = new LatLng(place.geometry.location.lat, place.geometry.location.lng);
                        googleMap.addMarker(new MarkerOptions().position(latLng).title(place.name));
                    }
                } else {
                    Toast.makeText(getActivity(), "No places found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<PlacesResponse> call, Throwable t) {
                Toast.makeText(getActivity(), "Error fetching places", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void turnOnPanicMode() {
        isPanicModeOn = true;
        btnPanic.setText("Panic Button: ON");
        btnPanic.setBackgroundTintList(getResources().getColorStateList(android.R.color.holo_red_dark));

        Toast.makeText(getActivity(), "Panic mode activated", Toast.LENGTH_SHORT).show();

        sendLocationToEmergencyContacts();

        sendLocationRunnable = new Runnable() {
            @Override
            public void run() {
                sendLocationToEmergencyContacts();
                if (isPanicModeOn) {
                    handler.postDelayed(this, 60000);
                }
            }
        };

        handler.postDelayed(sendLocationRunnable, 60000);
    }

    private void turnOffPanicMode() {
        isPanicModeOn = false;
        btnPanic.setText("Panic Button: OFF");
        btnPanic.setBackgroundTintList(getResources().getColorStateList(android.R.color.holo_red_light));

        Toast.makeText(getActivity(), "Panic mode deactivated", Toast.LENGTH_SHORT).show();

        if (sendLocationRunnable != null) {
            handler.removeCallbacks(sendLocationRunnable);
        }
    }

    private void sendLocationToEmergencyContacts() {
        if (currentLocation != null) {
            double latitude = currentLocation.getLatitude();
            double longitude = currentLocation.getLongitude();
            String locationMessage = "Emergency! My current location is: " +
                    "https://maps.google.com/?q=" + latitude + "," + longitude;

            Toast.makeText(getActivity(), "Sending location to emergency contacts: ", Toast.LENGTH_SHORT).show();

            SharedPreferences emergencyContactsPreferences = getActivity().getSharedPreferences("EmergencyContacts", getActivity().MODE_PRIVATE);
            String contact1 = emergencyContactsPreferences.getString("contact1", null);
            String contact2 = emergencyContactsPreferences.getString("contact2", null);
            String contact3 = emergencyContactsPreferences.getString("contact3", null);

            if (contact1 != null) {
                sendSms(contact1, locationMessage);
            }
            if (contact2 != null) {
                sendSms(contact2, locationMessage);
            }
            if (contact3 != null) {
                sendSms(contact3, locationMessage);
            }
        } else {
            Toast.makeText(getActivity(), "Current location not available", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendSms(String phoneNumber, String message) {
        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNumber, null, message, null, null);
        } else {
            Toast.makeText(getActivity(), "SMS permission not granted", Toast.LENGTH_SHORT).show();
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.SEND_SMS}, 1);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    getCurrentLocation();
                }
            } else {
                Toast.makeText(getActivity(), "Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (fusedLocationProviderClient != null && locationCallback != null) {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        }

        if (sendLocationRunnable != null) {
            handler.removeCallbacks(sendLocationRunnable);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocation();
        }
    }
}
