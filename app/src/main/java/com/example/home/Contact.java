package com.example.home;

public class Contact {
    private String name; // The name of the contact
    private String phoneNumber; // The phone number of the contact
    private int imageResourceId; // The drawable resource ID for the contact's image

    public Contact(String name, String phoneNumber, int imageResourceId) {
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.imageResourceId = imageResourceId;
    }

    public String getName() {
        return name;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public int getImageResourceId() {
        return imageResourceId;
    }
}
