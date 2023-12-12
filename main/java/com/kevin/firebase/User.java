package com.kevin.firebase;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
class User {

    private String Latitude;
    private String Longitude;

    public User() {
    }

    public User(String Latitude, String Longitude) {
        this.Latitude = Latitude;
        this.Longitude = Longitude;
    }

    public String getLatitude() {
        return Latitude;
    }

    public String getLongitude() {
        return Longitude;
    }
}