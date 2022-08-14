package com.tanvir.training.googlemapbatch1;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.ArrayList;
import java.util.List;

public class MapsFragment extends Fragment implements GoogleMap.OnMapLongClickListener {
    private GeofencingClient geofencingClient;
    private List<Geofence> geofenceList = new ArrayList<>();
    private SupportMapFragment mapFragment;
    private PendingIntent geofencePendingIntent;
    private GoogleMap map;
    private double latitude, longitude;
    private FusedLocationProviderClient providerClient;
    private ActivityResultLauncher<String> launcher =
            registerForActivityResult(new ActivityResultContracts
                            .RequestPermission(),
                    result -> {
                        if (result) {
                            detectUserLocation();
                        }else {
                            //show dialog and explain why you need this permission
                        }
                    });
    private OnMapReadyCallback callback = new OnMapReadyCallback() {

        @SuppressLint("MissingPermission")
        @Override
        public void onMapReady(GoogleMap googleMap) {
            map = googleMap;
            LatLng myPosition = new LatLng(latitude, longitude);
            map.addMarker(new MarkerOptions().position(myPosition).title("I am here!"));
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(myPosition, 16f));
            map.setMyLocationEnabled(true);
            map.getUiSettings().setMyLocationButtonEnabled(true);
            map.getUiSettings().setZoomControlsEnabled(true);
            map.setOnMapLongClickListener(MapsFragment.this::onMapLongClick);
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_maps, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        providerClient = LocationServices
                .getFusedLocationProviderClient(getActivity());
        geofencingClient = LocationServices
                .getGeofencingClient(getActivity());

        mapFragment =
                (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        checkLocationPermission();

    }

    @SuppressLint("MissingPermission")
    private void detectUserLocation() {
        providerClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location == null) return;

                    if (mapFragment != null) {
                        mapFragment.getMapAsync(callback);
                    }

                    latitude = location.getLatitude();
                    longitude = location.getLongitude();
                    //double lat = location.getLatitude();
                    //double lng = location.getLongitude();

                    //Log.e("WeatherApp", "Lat:"+lat+",lon:"+lng);
                });
    }

    private void checkLocationPermission() {
        if (LocationPermissionService.isLocationPermissionGranted(getActivity())) {
            detectUserLocation();
        } else {
            LocationPermissionService.requestLocationPermission(launcher);
        }
    }

    @Override
    public void onMapLongClick(@NonNull LatLng latLng) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Destination");
        builder.setMessage("Add a name for your destination");
        final EditText editText = new EditText(getActivity());
        builder.setView(editText);
        builder.setPositiveButton("ADD", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                final String destination = editText.getText().toString();
                createGeofence(destination, latLng);
            }
        });
        builder.setNegativeButton("CANCEL", null);
        final AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void createGeofence(String destination, LatLng latLng) {
        final Geofence geofence = new Geofence.Builder()
                .setRequestId(destination)
                .setCircularRegion(latLng.latitude, latLng.longitude, 150f)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                .setExpirationDuration(6 * 60 * 60 * 1000)
                .build();
        geofenceList.add(geofence);

        final GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
                builder.addGeofences(geofenceList);
                builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);

        if (LocationPermissionService.isLocationPermissionGranted(getActivity())) {
            geofencingClient.addGeofences(builder.build(), getGeofencePendingIntent())
                    .addOnSuccessListener(unused ->
                            Toast.makeText(getActivity(), "Added", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> {
                        Toast.makeText(getActivity(), e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                    });
        }


    }

    private PendingIntent getGeofencePendingIntent() {
        // Reuse the PendingIntent if we already have it.
        if (geofencePendingIntent != null) {
            return geofencePendingIntent;
        }
        Intent intent = new Intent(getActivity(), GeofenceBroadcastReceiver.class);
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when
        // calling addGeofences() and removeGeofences().
        geofencePendingIntent = PendingIntent.getBroadcast(getActivity(), 0, intent, PendingIntent.
                FLAG_UPDATE_CURRENT);
        return geofencePendingIntent;
    }

}