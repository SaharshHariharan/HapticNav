package com.rg185.HapticNav.app;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.CountDownTimer;
import android.provider.Settings;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.rg185.HapticNav.DirectionCallback;
import com.rg185.HapticNav.GoogleDirection;
import com.rg185.HapticNav.constant.TransportMode;
import com.rg185.HapticNav.model.Direction;
import com.rg185.HapticNav.model.Step;
import com.rg185.HapticNav.constant.AvoidType;
import com.rg185.HapticNav.util.DirectionConverter;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderApi;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import android.os.Vibrator;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

// Saharsh's imports
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import java.util.UUID;


public class DirectionActivity extends AppCompatActivity implements OnMapReadyCallback, View.OnClickListener, DirectionCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener, SensorEventListener {
    private Button btnCreateRoute;
    private Button btnGetDirections;
    private GoogleMap googleMap;
    private String serverKey = "AIzaSyCc9CSnaLTUslyqXELcUk4bQ6qLVSqamjg";
    private LatLng camera = new LatLng(0, 0);
    private LatLng origin;
    private Marker marker;
    Vibrator vibe;
    private FusedLocationProviderApi locationProvider = LocationServices.FusedLocationApi;
    String[] myCoordinates;
    private Double myLatitude;
    private Double myLongitude;
    LatLng myLocation;
    private GoogleApiClient googleApiClient;
    private LocationRequest locationRequest;
    String myDestinationString;
    private LatLng destination;
    String destinationLatitude;
    String destinationLongitude;
    ArrayList<LatLng> sectionPositionList;
    LatLng positionSecond;
    List<Step> stepList;
    private BroadcastReceiver broadcastReceiver;
    String address;
    private RequestQueue requestQueue;
    boolean checker = false;
    private Toast toaster;
    private Sensor accelerometer;
    private SensorManager sm;
    private float x,y,z,last_x,last_y,last_z;
    private long currentTime, lastUpdate;
    private final static long UPDATE_PERIOD = 100;
    private static final int SHAKE_THRESHOLD = 3300;
    ArrayList<PolylineOptions> polylineOptionList;
    LocationManager lm;
    Button button;
    TextToSpeech tt;
    boolean recalcChecker;
    boolean offRoute;
    // Recalculation

    ArrayList<Object> allPolylinePoints = new ArrayList<Object>();

    //Bluetooth

    private static final String TAG = "bluetooth1";

    Button btn1On, btn1Off, btn2On, btn2Off, btn3On, btn3Off, btn4On, btn4Off;
    String adress;

    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private OutputStream outStream = null;

    // SPP UUID service
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");



    // MAC-address of Bluetooth module (you must edit this line)
    private static String MACaddress = "20:16:02:18:43:56";

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        if(Build.VERSION.SDK_INT >= 10){
            try {
                final Method  m = device.getClass().getMethod("createInsecureRfcommSocketToServiceRecord", new Class[] { UUID.class });
                return (BluetoothSocket) m.invoke(device, MY_UUID);
            } catch (Exception e) {
                Log.e(TAG, "Could not create Insecure RFComm Connection",e);
            }
        }
        return  device.createRfcommSocketToServiceRecord(MY_UUID);
    }

    @Override
    public void onResume() {
        super.onResume();

        //Bluetooth
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        checkBTState();
        Log.d(TAG, "...onResume - try connect...");

        // Set up a pointer to the remote node using it's address.
        BluetoothDevice device = btAdapter.getRemoteDevice(MACaddress);

        // Two things are needed to make a connection:
        //   A MAC address, which we got above.
        //   A Service ID or UUID.  In this case we are using the
        //     UUID for SPP.

        try {
            btSocket = createBluetoothSocket(device);
        } catch (IOException e1) {
            errorExit("Fatal Error", "In onResume() and socket create failed: " + e1.getMessage() + ".");
        }

        // Discovery is resource intensive.  Make sure it isn't going on
        // when you attempt to connect and pass your message.
        btAdapter.cancelDiscovery();

        // Establish the connection.  This will block until it connects.
        Log.d(TAG, "...Connecting...");
        try {
            btSocket.connect();
            Log.d(TAG, "...Connection ok...");
        } catch (IOException e) {
            try {
                btSocket.close();
            } catch (IOException e2) {
                errorExit("Fatal Error", "In onResume() and unable to close socket during connection failure" + e2.getMessage() + ".");
            }
        }

        // Create a data stream so we can talk to server.
        Log.d(TAG, "...Create Socket...");

        try {
            outStream = btSocket.getOutputStream();
        } catch (IOException e) {
            errorExit("Fatal Error", "In onResume() and output stream creation failed:" + e.getMessage() + ".");
        }

        //Bluetooth End

        if (googleApiClient.isConnected()){
            requestLocationUpdates();
        }



        if (broadcastReceiver == null) {
            broadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {



                    myCoordinates = intent.getExtras().get("coordinates").toString().split(",");


                }
            };

        }
        registerReceiver(broadcastReceiver, new IntentFilter("location_update"));


    }

    //Bluetooth

   private void checkBTState() {
        // Check for Bluetooth support and then check to make sure it is turned on
        // Emulator doesn't support Bluetooth and will return null
        if(btAdapter==null) {
            errorExit("Fatal Error", "Bluetooth not support");
        } else {
            if (btAdapter.isEnabled()) {
                Log.d(TAG, "...Bluetooth ON...");
            } else {
                //Prompt user to turn on Bluetooth
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 1);
            }
        }
    }

    private void errorExit(String title, String message){
        Toast.makeText(getBaseContext(), title + " - " + message, Toast.LENGTH_LONG).show();
        //finish();
    }

    private void sendData(String message) {
        byte[] msgBuffer = message.getBytes();

        Log.d(TAG, "...Send data: " + message + "...");

        try {
            outStream.write(msgBuffer);
        } catch (IOException e) {
            String msg = "In onResume() and an exception occurred during write: " + e.getMessage();
            if (MACaddress.equals("00:00:00:00:00:00"))
                msg = msg + ".\n\nUpdate your server MACaddress from 00:00:00:00:00:00 to the correct MACaddress on line 35 in the java code";
            msg = msg +  ".\n\nCheck that the SPP UUID: " + MY_UUID.toString() + " exists on server.\n\n";

            errorExit("Fatal Error", msg);
        }
    }

    //Bluetooth end
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        this.setTitle("Navigation for the Visually Impaired");
        setContentView(R.layout.activity_transit_direction);
        vibe = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        btnCreateRoute = (Button) findViewById(R.id.btn_request_direction);
        btnCreateRoute.setOnClickListener(this);
        btnGetDirections = (Button) findViewById(R.id.button2);
        requestQueue = Volley.newRequestQueue(this);
        //btn = (Button) findViewById(R.id.button3);
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        checkBTState();

        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        locationRequest = new LocationRequest();
        locationRequest.setInterval(0);
        locationRequest.setFastestInterval(0);
        locationRequest.setMaxWaitTime(0);
        locationRequest.setSmallestDisplacement(0);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        tt=new TextToSpeech(getApplicationContext(),new TextToSpeech.OnInitListener()
        {
            public void onInit(int status)
            {
                if(status!=TextToSpeech.ERROR)
                    tt.setLanguage(Locale.CANADA);
                   }
        });

        sm = (SensorManager)getSystemService(SENSOR_SERVICE);
        accelerometer = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sm.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        currentTime = lastUpdate = (long)0.0;
        x = y = z = last_x = last_y = last_z = (float)0.0;



        if (!runtime_permissions()) {

            enable_location_service();
        }
        lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        if(!lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                !lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);
        }
        /*else if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
        }*/


        ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMapAsync(this);




    }

    private void enable_location_service() {

        Intent i = new Intent(getApplicationContext(), GPS_Service.class);
        startService(i);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                enable_location_service();
            } else {

                runtime_permissions();
            }
        }


    }

    private boolean runtime_permissions() {
        if (Build.VERSION.SDK_INT >= 23 && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 100);

            return true;
        }
        return false;
    }


    public void promptSpeechInput() {

        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        i.putExtra(RecognizerIntent.EXTRA_PROMPT, "What is your destination?");

        try {
            startActivityForResult(i, 100);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(DirectionActivity.this, "Sorry your device doesn't support speech language", Toast.LENGTH_LONG).show();
        }

    }


    public void onActivityResult(int request_code, int result_code, Intent i) {

        super.onActivityResult(request_code, result_code, i);

        switch (request_code) {

            case 100:
                if (result_code == RESULT_OK && i != null) {

                    ArrayList<String> result = i.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    myDestinationString = result.get(0);
                    btnCreateRoute.setHint("Create Route to: " + myDestinationString);

                        geocode();


                }
                break;

        }

    }

    @Override
    public void onMapReady(final GoogleMap googleMap) {
        this.googleMap = googleMap;
         googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(camera, 0));
        new CountDownTimer(2000, 1000) {
            public void onFinish() {
                // When timer is finished
                // Execute your code here
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myLocation, 20));
            }

            public void onTick(long millisUntilFinished) {
                // millisUntilFinished    The amount of time until finished.
            }
        }.start();

                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            return;
        }
        googleMap.setMyLocationEnabled(true);


    }

    public void SpeechToText(View v) {

        promptSpeechInput();

    }


    public void geocode() {

                 if (!(myDestinationString.equals(""))) {

                    JsonObjectRequest request = new JsonObjectRequest("https://maps.googleapis.com/maps/api/geocode/json?address=" + myDestinationString + "&key=AIzaSyCc9CSnaLTUslyqXELcUk4bQ6qLVSqamjg", new Response.Listener<JSONObject>() {

                        public void onResponse(JSONObject response) {

                            try {
                                address = response.getJSONArray("results").getJSONObject(0).getString("formatted_address");
                                destinationLatitude = response.getJSONArray("results").getJSONObject(0)
                                        .getJSONObject("geometry").getJSONObject("location")
                                        .getString("lat");
                                destinationLongitude = response.getJSONArray("results").getJSONObject(0)
                                        .getJSONObject("geometry").getJSONObject("location")
                                        .getString("lng");
                                destination = new LatLng(Double.parseDouble(destinationLatitude), Double.parseDouble(destinationLongitude));

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {

                        }
                    }
                    );
                    requestQueue.add(request);
                    new CountDownTimer(2000, 1000) {
                        public void onFinish() {
                            // When timer is finished
                            // Execute your code here

                            requestDirection();
                            googleMap.clear();
                        }

                        public void onTick(long millisUntilFinished) {
                            // millisUntilFinished    The amount of time until finished.
                        }
                    }.start();

                }
                else{

                    toaster = Toast.makeText(DirectionActivity.this, "Please input the destination.", Toast.LENGTH_LONG);
                    toaster.show();
                }

            }





    public void requestDirection() {

        if (destination!=null) {

            Snackbar.make(btnCreateRoute, "Direction Requesting...", Snackbar.LENGTH_SHORT).show();
            GoogleDirection.withServerKey(serverKey)
                    .from(origin)
                    .to(destination)
                    .avoid(AvoidType.FERRIES)
                    .avoid(AvoidType.HIGHWAYS)
                    .avoid(AvoidType.TOLLS)
                    .transportMode(TransportMode.WALKING)
                    .execute(this);
                    googleMap.clear();
        }
        else{

            toaster = Toast.makeText(DirectionActivity.this, "Try Again", Toast.LENGTH_LONG);
            toaster.show();

        }
    }

    @Override
    public void onDirectionSuccess(Direction direction, String rawBody) {
        googleMap.clear();
        Snackbar.make(btnCreateRoute, "Success with status : " + direction.getStatus(), Snackbar.LENGTH_LONG).show();
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myLocation, 16));


        if (direction.isOK()) {
            vibe.vibrate(50);
            sectionPositionList = direction.getRouteList().get(0).getLegList().get(0).getSectionPoint();
            stepList = direction.getRouteList().get(0).getLegList().get(0).getStepList();
            //for ( LatLng position : sectionPositionList) {
            recalcChecker = false;

            googleMap.addMarker(new MarkerOptions().title("Origin").position(stepList.get(0).getStartLocation().getCoordination()));


            if (stepList.get(sectionPositionList.size() - 2).getHtmlInstruction().toLowerCase().contains("destination will be on the right")) {
                googleMap.addMarker(new MarkerOptions().title("Destination will be on the right").position(stepList.get(sectionPositionList.size() - 2).getEndLocation().getCoordination()));
            } else if (stepList.get(sectionPositionList.size() - 2).getHtmlInstruction().toLowerCase().contains("destination will be on the left")) {
                googleMap.addMarker(new MarkerOptions().title("Destination will be on the left").position(stepList.get(sectionPositionList.size() - 2).getEndLocation().getCoordination()));
            } else {
                googleMap.addMarker(new MarkerOptions().title("Destination").position(stepList.get(sectionPositionList.size() - 2).getEndLocation().getCoordination()));

            }
            for (int x = 0; x <= sectionPositionList.size() - 3; x++) {

                if ((stepList.get(x + 1).getHtmlInstruction().toLowerCase().contains("right")) && (stepList.get(x + 1).getHtmlInstruction().toLowerCase().contains("left"))) {

                    if (stepList.get(x + 1).getHtmlInstruction().toLowerCase().indexOf("right") < stepList.get(x + 1).getHtmlInstruction().toLowerCase().indexOf("left")) {
                        googleMap.addMarker(new MarkerOptions().title("Turn Right").position(stepList.get(x).getEndLocation().getCoordination()));
                    } else if (stepList.get(x + 1).getHtmlInstruction().toLowerCase().indexOf("left") < stepList.get(x + 1).getHtmlInstruction().toLowerCase().indexOf("right")) {
                        googleMap.addMarker(new MarkerOptions().title("Turn Left").position(stepList.get(x).getEndLocation().getCoordination()));

                    }
                } else if (stepList.get(x + 1).getHtmlInstruction().toLowerCase().contains("right")) {
                    googleMap.addMarker(new MarkerOptions().title("Turn Right").position(stepList.get(x).getEndLocation().getCoordination()));
                } else if (stepList.get(x + 1).getHtmlInstruction().toLowerCase().contains("left")) {
                    googleMap.addMarker(new MarkerOptions().title("Turn Left").position(stepList.get(x).getEndLocation().getCoordination()));
                } else if (stepList.get(x + 1).getHtmlInstruction().toLowerCase().contains("continue")) {
                    googleMap.addMarker(new MarkerOptions().title("Continue Past Intersection").position(stepList.get(x).getEndLocation().getCoordination()));
                }

            }


            polylineOptionList = DirectionConverter.createTransitPolyline(this, stepList, 5, Color.RED, 3, Color.BLUE);

            for (int x = 0; x < polylineOptionList.size(); x++){

                Object[] temporaryArray = polylineOptionList.get(x).getPoints().toArray();

                for (int i = 0; i < temporaryArray.length; i++){

                 allPolylinePoints.add(temporaryArray[i]);

                }
            }



            for (PolylineOptions polylineOption : polylineOptionList) {
                googleMap.addPolyline(polylineOption);

            }



            tt.speak("Your destination is " + address + ". The time it will take to get there is " + direction.getRouteList().get(0).getLegList().get(0).getDuration().getText().toString() + ". The distance to the destination is " + direction.getRouteList().get(0).getLegList().get(0).getDistance().getText().toString(), TextToSpeech.QUEUE_FLUSH, null, null);

            /*if (((myLatitude <= stepList.get(0).getStartLocation().getLatitude() + 0.00009) && (myLatitude >= stepList.get(0).getStartLocation().getLatitude() - 0.00009)) && ((myLongitude <= stepList.get(0).getStartLocation().getLongitude() + 0.00009) && (myLongitude >= stepList.get(0).getStartLocation().getLongitude() - 0.00009))) {
            for (int i = 0; i < stepList.size(); i++) {

                for (double x = stepList.get(i).getStartLocation().getLongitude(); x <= stepList.get(i).getEndLocation().getLongitude(); x = x + 0.0000001) {

                    routeListX.add(x);
                }
                for (double y = stepList.get(i).getStartLocation().getLatitude(); y <= stepList.get(i).getEndLocation().getLatitude(); y = y + 0.0000001) {

                    routeListY.add(y);
                }

            }
        }*/

        }
        if (direction.isOK()) {
            checker = true;
        }
        else {
            checker=false;
        }
        //startActivity(iintent);
    }

    @Override
    public void onDirectionFailure(Throwable t) {
        Snackbar.make(btnCreateRoute, t.getMessage(), Snackbar.LENGTH_SHORT).show();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

        requestLocationUpdates();


    }

    private void requestLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
// All of the point finder
    @Override
    public void onLocationChanged(Location location) {

        btAdapter = BluetoothAdapter.getDefaultAdapter();
        checkBTState();

        myLatitude = location.getLatitude();
        myLongitude = location.getLongitude();
        myLocation = new LatLng(myLatitude,myLongitude);;
        origin = myLocation;
        camera = myLocation;

        if(marker!=null){
            marker.remove();
        }
        marker = googleMap.addMarker(new MarkerOptions().title("You").position(myLocation));

        if(toaster!=null){
             toaster.cancel();
        }
        if (checker==true){


            if (((myLatitude <= stepList.get(0).getStartLocation().getLatitude() + 0.00009) && (myLatitude >= stepList.get(0).getStartLocation().getLatitude() - 0.00009)) && ((myLongitude <= stepList.get(0).getStartLocation().getLongitude() + 0.00009) && (myLongitude >= stepList.get(0).getStartLocation().getLongitude() - 0.00009))) {

                btAdapter = BluetoothAdapter.getDefaultAdapter();
                checkBTState();

                sendData("1");

                toaster = Toast.makeText(DirectionActivity.this, "Origin", Toast.LENGTH_LONG);
                toaster.show();
                vibe.vibrate(1500);
                tt.speak("You are at the origin.",TextToSpeech.QUEUE_FLUSH,null,null);
                recalcChecker = true;

            }
            else if (((myLatitude <= stepList.get(sectionPositionList.size() - 2).getEndLocation().getLatitude() + 0.00009) && (myLatitude >= stepList.get(sectionPositionList.size() - 2).getEndLocation().getLatitude() - 0.00009)) && ((myLongitude <= stepList.get(sectionPositionList.size() - 2).getEndLocation().getLongitude() + 0.00009) && (myLongitude >= stepList.get(sectionPositionList.size() - 2).getEndLocation().getLongitude() - 0.00009))) {

                if (stepList.get(sectionPositionList.size() - 2).getHtmlInstruction().toLowerCase().contains("destination will be on the right")) {
                    btAdapter = BluetoothAdapter.getDefaultAdapter();
                    checkBTState();

                    sendData("2");

                    toaster = Toast.makeText(DirectionActivity.this, "Destination will be on the right", Toast.LENGTH_LONG);
                    toaster.show();
                    vibe.vibrate(3000);
                    tt.speak("The destination will be on the right.",TextToSpeech.QUEUE_FLUSH,null,null);

                } else if (stepList.get(sectionPositionList.size() - 2).getHtmlInstruction().toLowerCase().contains("destination will be on the left")) {

                    btAdapter = BluetoothAdapter.getDefaultAdapter();
                    checkBTState();

                    sendData("3");

                    toaster = Toast.makeText(DirectionActivity.this, "Destination will be on the left", Toast.LENGTH_LONG);
                    toaster.show();
                    vibe.vibrate(3000);
                    tt.speak("The destination will be on the left.",TextToSpeech.QUEUE_FLUSH,null,null);

                } else if (stepList.get(sectionPositionList.size() - 2).getHtmlInstruction().toLowerCase().contains("destination")&&(!(stepList.get(sectionPositionList.size() - 2).getHtmlInstruction().toLowerCase().contains("right")))&&(!(stepList.get(sectionPositionList.size() - 2).getHtmlInstruction().toLowerCase().contains("left")))){

                    btAdapter = BluetoothAdapter.getDefaultAdapter();
                    checkBTState();

                    sendData("4");

                    toaster = Toast.makeText(DirectionActivity.this, "Destination", Toast.LENGTH_LONG);
                    toaster.show();
                    vibe.vibrate(3000);
                    tt.speak("You have arrived at the destination.",TextToSpeech.QUEUE_FLUSH,null,null);
                }

            }
                        for (int x = 0; x <= sectionPositionList.size() - 3; x++) {

                if ((((myLatitude <= stepList.get(x).getEndLocation().getLatitude() + 0.00009) && (myLatitude >= stepList.get(x).getEndLocation().getLatitude() - 0.00009)) && ((myLongitude <= stepList.get(x).getEndLocation().getLongitude() + 0.00009) && (myLongitude >= stepList.get(x).getEndLocation().getLongitude() - 0.00009)))) {

                    if ((stepList.get(x + 1).getHtmlInstruction().toLowerCase().contains("right")) && (stepList.get(x + 1).getHtmlInstruction().toLowerCase().contains("left"))) {

                        if (stepList.get(x + 1).getHtmlInstruction().toLowerCase().indexOf("right") < stepList.get(x + 1).getHtmlInstruction().toLowerCase().indexOf("left")) {
                            btAdapter = BluetoothAdapter.getDefaultAdapter();
                            checkBTState();

                            sendData("5");

                            toaster = Toast.makeText(DirectionActivity.this, "Turn Right", Toast.LENGTH_LONG);
                            toaster.show();
                            vibe.vibrate(50);
                            tt.speak("Turn right.",TextToSpeech.QUEUE_FLUSH,null,null);

                        } else if (stepList.get(x + 1).getHtmlInstruction().toLowerCase().indexOf("left") < stepList.get(x + 1).getHtmlInstruction().toLowerCase().indexOf("right")) {
                            btAdapter = BluetoothAdapter.getDefaultAdapter();
                            checkBTState();

                            sendData("6");

                            toaster = Toast.makeText(DirectionActivity.this, "Turn Left", Toast.LENGTH_LONG);
                            toaster.show();
                            vibe.vibrate(250);
                            tt.speak("Turn left.",TextToSpeech.QUEUE_FLUSH,null,null);

                        }

                    } else if (stepList.get(x + 1).getHtmlInstruction().toLowerCase().contains("right")) {
                        btAdapter = BluetoothAdapter.getDefaultAdapter();
                        checkBTState();

                        sendData("5");

                        toaster = Toast.makeText(DirectionActivity.this, "Turn Right", Toast.LENGTH_LONG);
                        toaster.show();
                        vibe.vibrate(25);
                        tt.speak("Turn right.",TextToSpeech.QUEUE_FLUSH,null,null);

                    } else if (stepList.get(x + 1).getHtmlInstruction().toLowerCase().contains("left")) {
                        btAdapter = BluetoothAdapter.getDefaultAdapter();
                        checkBTState();

                        sendData("6");
                        toaster = Toast.makeText(DirectionActivity.this, "Turn Left", Toast.LENGTH_LONG);
                        toaster.show();
                        vibe.vibrate(250);
                        tt.speak("Turn left.",TextToSpeech.QUEUE_FLUSH,null,null);

                    } else if (stepList.get(x + 1).getHtmlInstruction().toLowerCase().contains("continue")) {

                        btAdapter = BluetoothAdapter.getDefaultAdapter();
                        checkBTState();

                        sendData("7");

                        toaster = Toast.makeText(DirectionActivity.this, "Continue Past Intersection", Toast.LENGTH_LONG);
                        toaster.show();
                        vibe.vibrate(750);
                        tt.speak("Please continue past the intersection.",TextToSpeech.QUEUE_FLUSH,null,null);
                    }

                }


            }



            /*if (recalcChecker == true){
           for (int x = 0; x < allPolylinePoints.size(); x++) {


               String polylineString = allPolylinePoints.get(x).toString();
               String polylineStringLat = polylineString.substring(polylineString.indexOf('(') + 1, polylineString.indexOf(','));
               double polyLineLat = Double.parseDouble(polylineStringLat);
               String polylineStringLong = polylineString.substring(polylineString.indexOf(',') + 1, polylineString.indexOf(')'));
               double polyLineLong = Double.parseDouble(polylineStringLong);
               //btnCreateRoute.setHint(polylineStringLat + " " + polylineStringLong);



                   if (((myLatitude <= polyLineLat + 0.00030) && (myLatitude >= polyLineLat - 0.00030)) && (((myLongitude <= polyLineLong + 0.00030) && (myLongitude >= polyLineLong - 0.00030)))){

                       offRoute = false;
                       x = allPolylinePoints.size();
                   }
                else {

                       offRoute = true;
                   }
                }
            }

            if(offRoute == true){

                tt.speak("Recalculating!",TextToSpeech.QUEUE_FLUSH,null,null);
                requestDirection();
                offRoute = false;
            }*/

        }

    }

    @Override
    protected void onStart() {
        super.onStart();
        googleApiClient.connect();
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "...In onPause()...");

        if (outStream != null) {
            try {
                outStream.flush();
            } catch (IOException e) {
                errorExit("Fatal Error", "In onPause() and failed to flush output stream: " + e.getMessage() + ".");
            }
        }

        try     {
            btSocket.close();
        } catch (IOException e2) {
            errorExit("Fatal Error", "In onPause() and failed to close socket." + e2.getMessage() + ".");
        }



    }

    @Override
    protected void onStop() {
        super.onStop();
        googleApiClient.disconnect();

    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        long currentTime = System.currentTimeMillis();

        if((currentTime-lastUpdate) > UPDATE_PERIOD){
            long diffTime = (currentTime - lastUpdate);
            lastUpdate = currentTime;

            x = sensorEvent.values[0];
            y = sensorEvent.values[1];
            z = sensorEvent.values[2];

            float speed  = Math.abs(x + y + z - last_x - last_y - last_z)/diffTime * 10000;

            if (speed > SHAKE_THRESHOLD){
                //aCounter++;
                promptSpeechInput();
                vibe.vibrate(50);
            }
            last_z = z;
            last_y = y;
            last_x = x;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    @Override
    public void onClick(View view) {

    }


}
