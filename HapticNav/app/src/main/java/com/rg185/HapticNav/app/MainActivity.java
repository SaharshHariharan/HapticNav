package com.rg185.HapticNav.app;

import android.content.Context;
import android.content.Intent;
import android.os.Vibrator;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;


public class MainActivity extends AppCompatActivity {

    Vibrator vibe;
    private Button btnGetDestination;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        vibe = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        btnGetDestination = (Button) findViewById(R.id.btn_transit);

    }

    public void getDestination (View v){

        vibe.vibrate(50);
        openActivity(DirectionActivity.class);

    }


    public void goToTransitDirection() {
        openActivity(DirectionActivity.class);
    }

    public void openActivity(Class<?> cs) {
        startActivity(new Intent(this, cs));
    }


}
