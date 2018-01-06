package com.example.wangsl.locationprovider;

import android.location.Location;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {
Button button;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        button=(Button)findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Location location = LocationUtils.getInstance( MainActivity.this ).showLocation();
                if (location != null) {
                    String address = "纬度：" + location.getLatitude() + "经度：" + location.getLongitude();
                    Log.d( "FLY.LocationUtils", address );

                }else {
                    Log.d( "FLY.LocationUtils", "Null" );
                }
            }
        });
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocationUtils.getInstance( this ).removeLocationUpdatesListener();
    }
}
