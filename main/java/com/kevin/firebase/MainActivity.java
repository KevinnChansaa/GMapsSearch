package com.kevin.firebase;

import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import android.widget.SearchView;
import android.widget.Toast;import java.io.IOException;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMapLongClickListener {
    private int currentMarkerColorIndex = 0;
    private List<BitmapDescriptor> markerColors = new ArrayList<>();
    private static final String TAG = MainActivity.class.getSimpleName();
    private TextView txtDetails;
    private GoogleMap mMap;
    private EditText inputLatitude, inputLongitude;
    private Button btnSave;
    private DatabaseReference mFirebaseDatabase;
    private boolean mapClicked = false;
    private String userId;

    private SearchView searchView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        inputLatitude = findViewById(R.id.Latitude);
        inputLongitude = findViewById(R.id.Longitude);
        btnSave = findViewById(R.id.btn_save);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        searchView = findViewById(R.id.searchView);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // Perform search when the user submits the query
                searchLocation(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // Handle changes in the query text if needed
                return false;
            }
        });
        // If it doesn't exist, create a new one
        if (mapFragment == null) {
            mapFragment = SupportMapFragment.newInstance();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.mapContainer, mapFragment)
                    .commit();
        }
        mapFragment.getMapAsync(this);
        FirebaseDatabase mFirebaseInstance = FirebaseDatabase.getInstance();
        mFirebaseDatabase = mFirebaseInstance.getReference("coordinate");
        mFirebaseInstance.getReference("app_title").setValue("Maps Database");

        mFirebaseInstance.getReference("app_title").addValueEventListener(new ValueEventListener() {

            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.e(TAG, "App title updated");

                String appTitle = dataSnapshot.getValue(String.class);

                // update toolbar title
                getSupportActionBar().setTitle(appTitle);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.e(TAG, "Failed to read app title value.", error.toException());
            }



        });

        // Save / update the user
        // Save / update the user
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String latitude = inputLatitude.getText().toString();
                String longitude = inputLongitude.getText().toString();

                // Check for valid latitude and longitude
                if (!TextUtils.isEmpty(latitude) && !TextUtils.isEmpty(longitude)) {
                    LatLng locationLatLng = new LatLng(Double.parseDouble(latitude), Double.parseDouble(longitude));

                    // Add a marker at the specified location
                    addMarkerAtLocation(locationLatLng);

                    // Center the map camera on the specified location
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(locationLatLng, 15));

                    // Display latitude and longitude in EditText components
                    inputLatitude.setText(String.valueOf(locationLatLng.latitude));
                    inputLongitude.setText(String.valueOf(locationLatLng.longitude));

                    // Save the location to the database
                    saveLocationToDatabase(String.valueOf(locationLatLng.latitude), String.valueOf(locationLatLng.longitude));

                    // Reset the mapClicked flag to allow map interaction again
                    mapClicked = false;
                }

                // Perform the search using the search bar
                String query = searchView.getQuery().toString();
                if (!TextUtils.isEmpty(query)) {
                    searchLocation(query);
                }
            }
        });


        toggleButton();
    }

    private void searchLocation(String locationName) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());

        try {
            List<Address> addresses = geocoder.getFromLocationName(locationName, 1);

            if (!addresses.isEmpty()) {
                Address address = addresses.get(0);
                double latitude = address.getLatitude();
                double longitude = address.getLongitude();

                LatLng locationLatLng = new LatLng(latitude, longitude);

                // Clear existing markers on the map
                mMap.clear();

                // Add a marker at the searched location
                addMarkerAtLocation(locationLatLng);

                // Center the map camera on the searched location
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(locationLatLng, 15));

                // Display latitude and longitude in EditText components
                if (inputLatitude != null && inputLongitude != null) {
                    inputLatitude.setText(String.valueOf(latitude));
                    inputLongitude.setText(String.valueOf(longitude));
                }

                // Save the location to the database
                saveLocationToDatabase(String.valueOf(latitude), String.valueOf(longitude));
            } else {
                // Handle case when no results are found
                Toast.makeText(this, "Location not found", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
            // Handle IOException, for example, show an error message to the user
            Toast.makeText(this, "Error while searching location: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            // Handle other exceptions, if any
            Toast.makeText(this, "An unexpected error occurred", Toast.LENGTH_SHORT).show();
        }
    }


    private void enableMapLongClick() {
        // Set the flag to false, indicating that the map has not been clicked
        mapClicked = false;

        // Enable long clicks on the map
        mMap.setOnMapLongClickListener(MainActivity.this);
    }

    private void saveLocationToDatabase(String latitude, String longitude) {
        // Check for already existed userId
        if (TextUtils.isEmpty(userId)) {
            userId = mFirebaseDatabase.push().getKey();
        }

        // Create a User object with latitude and longitude
        User user = new User(latitude, longitude);

        // Save the user to the database
        mFirebaseDatabase.child(userId).setValue(user);

        // Add a ValueEventListener to listen for changes in the database
        addUserChangeListener();
    }


    private void addMarkerAtLocation(LatLng location) {
        BitmapDescriptor markerColor = getNextMarkerColor();

        mMap.addMarker(new MarkerOptions()
                .position(location)
                .title("Searched Location")
                .icon(markerColor));

        String address = getCompleteAddress(location.latitude, location.longitude);
        Log.d("Search Location", "Latitude: " + location.latitude + ", Longitude: " + location.longitude + ", Address: " + address);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Sydney with blue color and move the camera
        LatLng sydney = new LatLng(-34, 151);
        BitmapDescriptor blueMarkerIcon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE);

        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney").icon(blueMarkerIcon));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
        mMap.setOnMapLongClickListener(this);
    }


    @Override
    public void onMapLongClick(@NonNull LatLng latLng) {
        // Get the next marker color
        BitmapDescriptor markerColor = getNextMarkerColor();

        MarkerOptions markerOptions = new MarkerOptions()
                .position(latLng)
                .title(latLng.toString())
                .icon(markerColor);

        mMap.addMarker(new MarkerOptions().position(latLng).title("Marker Title").icon(markerColor));

        String address = getCompleteAddress(latLng.latitude, latLng.longitude);
        Log.d("Map Long Click", "Latitude: " + latLng.latitude + ", Longitude: " + latLng.longitude + ", Address: " + address);

        // Display latitude and longitude in EditText components
        if (inputLatitude != null && inputLongitude != null) {
            inputLatitude.setText(String.valueOf(latLng.latitude));
            inputLongitude.setText(String.valueOf(latLng.longitude));
        }

        // Save the location to the database
        saveLocationToDatabase(String.valueOf(latLng.latitude), String.valueOf(latLng.longitude));

        // Set the flag to true, indicating that the map has been clicked
        mapClicked = true;

        // Disable further map interaction
    }


    private void toggleButton() {
        btnSave.setText("Save");
    }

    private void createUser(String Latitude, String Longitude) {
        if (TextUtils.isEmpty(userId)) {
            userId = mFirebaseDatabase.push().getKey();
        }

        User user = new User(Latitude, Longitude);

        mFirebaseDatabase.child(userId).setValue(user);

        addUserChangeListener();
    }

    private void addUserChangeListener() {
        mFirebaseDatabase.child(userId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                User user = dataSnapshot.getValue(User.class);

                if (user == null) {
                    Log.e(TAG, "User data is null!");
                    return;
                }

                Log.e(TAG, "User data is changed!" + user.getLatitude() + ", " + user.getLongitude());

                inputLatitude.setText("");
                inputLongitude.setText("");

                toggleButton();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Failed to read user", error.toException());
            }
        });
    }

    private void updateUser(String Latitude, String Longitude) {
        if (!TextUtils.isEmpty(Latitude))
            mFirebaseDatabase.child(userId).child("Latitude").setValue(Latitude);

        if (!TextUtils.isEmpty(Longitude))
            mFirebaseDatabase.child(userId).child("Longitude").setValue(Longitude);
    }

    private String getCompleteAddress(double LATITUDE, double LONGITUDE) {
        String ret = " ";
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(LATITUDE, LONGITUDE, 1);
            if (addresses != null) {
                Address returnedAddress = addresses.get(0);
                StringBuilder strReturnedAddress = new StringBuilder();
                for (int i = 0; i <= returnedAddress.getMaxAddressLineIndex(); i++) {
                    strReturnedAddress.append(returnedAddress.getAddressLine(i)).append("/n");
                }
                ret = strReturnedAddress.toString();
                Log.d("Current Location Address", ret);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            Log.e("Current location address", "cannot get address");
        }
        return ret;
    }

    private BitmapDescriptor getNextMarkerColor() {
        if (markerColors.isEmpty()) {
            // Add your desired marker colors
            markerColors.add(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
            markerColors.add(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
            markerColors.add(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN));
            markerColors.add(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA));
            markerColors.add(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW));
            markerColors.add(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET));
            markerColors.add(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ROSE));
            markerColors.add(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE));
            markerColors.add(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));

            // Add more colors as needed

            // Set the current marker color index back to the beginning
            currentMarkerColorIndex = 0;
        }

        // Get the current marker color
        BitmapDescriptor markerColor = markerColors.get(currentMarkerColorIndex);

        // Move to the next marker color
        currentMarkerColorIndex = (currentMarkerColorIndex + 1) % markerColors.size();

        return markerColor;
    }
}
