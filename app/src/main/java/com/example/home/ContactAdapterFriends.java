package com.example.home;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import java.util.List;

public class ContactAdapterFriends extends ArrayAdapter<String> {

    private SharedPreferences emergencyContactsPrefs;

    public ContactAdapterFriends(Context context, List<String> contacts) {
        super(context, 0, contacts);
        emergencyContactsPrefs = context.getSharedPreferences("EmergencyContacts", Context.MODE_PRIVATE);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        String contactName = getItem(position);

        if (convertView == null || convertView.findViewById(R.id.contactTextView) == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_contact, parent, false);
        }

        TextView contactTextView = convertView.findViewById(R.id.contactTextView);
        Button editButton = convertView.findViewById(R.id.editButton);
        Button callButton = convertView.findViewById(R.id.callButton);

        contactTextView.setText(contactName);

        editButton.setOnClickListener(v -> {
            String contactNumber = findContactNumber(contactName);
            if (contactNumber != null) {
                Intent intent = new Intent(getContext(), OpeningAddNumbers.class);
                intent.putExtra("name", contactName);
                intent.putExtra("contact", contactNumber);
                intent.putExtra("position", position);
                getContext().startActivity(intent);
            } else {
                Toast.makeText(getContext(), "Contact number not found", Toast.LENGTH_SHORT).show();
            }
        });

        callButton.setOnClickListener(v -> {
            String contactNumber = findContactNumber(contactName);
            if (contactNumber != null) {
                if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                    Intent callIntent = new Intent(Intent.ACTION_CALL);
                    callIntent.setData(Uri.parse("tel:" + contactNumber));
                    getContext().startActivity(callIntent);
                } else {
                    Toast.makeText(getContext(), "Call permission not granted", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(getContext(), "Contact number not found", Toast.LENGTH_SHORT).show();
            }
        });

        return convertView;
    }

    private String findContactNumber(String contactName) {
        for (int i = 1; i <= 10; i++) {
            String name = emergencyContactsPrefs.getString("name" + i, "");
            if (contactName.equals(name)) {
                return emergencyContactsPrefs.getString("contact" + i, "");
            }
        }
        return null;
    }
}
