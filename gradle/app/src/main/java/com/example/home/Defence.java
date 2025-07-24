package com.example.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

public class Defence extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_defence);

        findViewById(R.id.Laws).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Defence.this, Laws.class);
                startActivity(intent);
            }
        });

        findViewById(R.id.self_defence).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Defence.this, SelfDefence.class);
                startActivity(intent);
            }
        });

        findViewById(R.id.tips).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Defence.this, SafetyTips.class);
                startActivity(intent);
            }
        });

        findViewById(R.id.procedures).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Defence.this, EmergencyProcegers.class);
                startActivity(intent);
            }
        });
    }
}
