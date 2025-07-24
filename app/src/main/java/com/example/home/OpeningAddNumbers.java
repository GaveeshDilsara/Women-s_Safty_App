package com.example.home;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class OpeningAddNumbers extends AppCompatActivity {

    private int position = -1;
    private SharedPreferences sharedPreferences;

    private static final String PREF_NAME = "MyAppPreferences";
    private static final String CONTACTS_SAVED_KEY = "ContactsSaved";
    private static final int SMS_PERMISSION_REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.SEND_SMS}, SMS_PERMISSION_REQUEST_CODE);
        }

        Intent intent = getIntent();
        boolean isEditMode = intent != null && intent.hasExtra("name") && intent.hasExtra("contact");

        if (!isEditMode) {
            SharedPreferences preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
            boolean contactsSaved = preferences.getBoolean(CONTACTS_SAVED_KEY, false);

            if (contactsSaved) {
                Intent mainActivityIntent = new Intent(OpeningAddNumbers.this, MainActivity.class);
                startActivity(mainActivityIntent);
                finish();
                return;
            }
        }

        setContentView(R.layout.activity_opening_add_numbers);

        sharedPreferences = getSharedPreferences("EmergencyContacts", MODE_PRIVATE);

        Button saveButton = findViewById(R.id.contactsave);
        EditText name1 = findViewById(R.id.name1);
        EditText contact1 = findViewById(R.id.contact1);
        EditText name2 = findViewById(R.id.name2);
        EditText contact2 = findViewById(R.id.contact2);
        EditText name3 = findViewById(R.id.name3);
        EditText contact3 = findViewById(R.id.contact3);

        if (isEditMode) {
            name1.setText(intent.getStringExtra("name"));
            contact1.setText(intent.getStringExtra("contact"));
            position = intent.getIntExtra("position", -1);

            name2.setVisibility(View.GONE);
            contact2.setVisibility(View.GONE);
            name3.setVisibility(View.GONE);
            contact3.setVisibility(View.GONE);
        }

        saveButton.setOnClickListener(v -> {
            if (isValidContact(contact1) && (position != -1 || (isValidContact(contact2) && isValidContact(contact3)))) {
                SharedPreferences.Editor editor = sharedPreferences.edit();

                if (position == -1) {
                    editor.putString("name1", name1.getText().toString());
                    editor.putString("contact1", contact1.getText().toString());
                    editor.putString("name2", name2.getText().toString());
                    editor.putString("contact2", contact2.getText().toString());
                    editor.putString("name3", name3.getText().toString());
                    editor.putString("contact3", contact3.getText().toString());
                } else {
                    editor.putString("name" + (position + 1), name1.getText().toString());
                    editor.putString("contact" + (position + 1), contact1.getText().toString());
                }

                editor.apply();

                Toast.makeText(OpeningAddNumbers.this, "Contact Saved", Toast.LENGTH_SHORT).show();

                sendSmsMessage(contact1.getText().toString(), name1.getText().toString());
                if (position == -1) {
                    sendSmsMessage(contact2.getText().toString(), name2.getText().toString());
                    sendSmsMessage(contact3.getText().toString(), name3.getText().toString());
                }

                SharedPreferences.Editor prefEditor = getSharedPreferences(PREF_NAME, MODE_PRIVATE).edit();
                prefEditor.putBoolean(CONTACTS_SAVED_KEY, true);
                prefEditor.apply();

                Intent mainActivityIntent = new Intent(OpeningAddNumbers.this, MainActivity.class);
                startActivity(mainActivityIntent);
                finish();
            } else {
                Toast.makeText(OpeningAddNumbers.this, "Each contact number must be exactly 10 digits", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (position == -1) {
            super.onBackPressed();
        } else {
            Intent intent = new Intent(OpeningAddNumbers.this, MainActivity.class);
            startActivity(intent);
            finish();
        }
    }

    private boolean isValidContact(EditText contact) {
        String contactStr = contact.getText().toString().trim();
        return !TextUtils.isEmpty(contactStr) && contactStr.length() == 10;
    }

    private void sendSmsMessage(String phoneNumber, String name) {
        String message = "Hi " + name + ", you have been added to my emergency contacts.";
        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNumber, null, message, null, null);
            Toast.makeText(this, "Welcom massage was sent", Toast.LENGTH_SHORT).show();
        } catch (IllegalArgumentException e) {
            Toast.makeText(this, "Invalid phone number: " + phoneNumber, Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        } catch (SecurityException e) {
            Toast.makeText(this, "SMS permission not granted", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        } catch (Exception e) {
            Toast.makeText(this, "Failed to send SMS to " + phoneNumber, Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == SMS_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "SMS permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "SMS permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
