package com.example.sankalp.bluetoothforled;

        import android.app.Activity;
        import android.bluetooth.BluetoothAdapter;
        import android.bluetooth.BluetoothDevice;
        import android.bluetooth.BluetoothSocket;
        import android.content.Intent;
        import android.os.Build;
        import android.os.Bundle;
        import android.speech.tts.TextToSpeech;
        import android.util.Log;
        import android.view.View;
        import android.view.View.OnClickListener;
        import android.widget.Button;
        import android.widget.Toast;

        import java.io.IOException;
        import java.io.OutputStream;
        import java.lang.reflect.Method;
        import java.util.Locale;
        import java.util.UUID;

public class MainActivity extends Activity {
    private static final String TAG = "bluetooth1";

    Button btn1On, btn1Off, btn2On, btn2Off, btn3On, btn3Off, btn4On, btn4Off;
    String adress;

    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private OutputStream outStream = null;

    TextToSpeech tt;

    // SPP UUID service
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");



    // MAC-address of Bluetooth module (you must edit this line)
    private static String address = "20:16:02:18:43:56";


    /** Called when the activity is first created. */

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        btn1On = (Button) findViewById(R.id.btn1On);
        btn1Off = (Button) findViewById(R.id.btn1Off);
        btn2On = (Button) findViewById(R.id.btn2On) ;
        btn2Off = (Button) findViewById(R.id.btn2Off) ;
        btn3On = (Button) findViewById(R.id.btn3On) ;
        btn3Off = (Button) findViewById(R.id.btn3Off) ;
        btn4On = (Button) findViewById(R.id.btn4On) ;
       //btn4Off = (Button) findViewById(R.id.btn4Off) ;

        btAdapter = BluetoothAdapter.getDefaultAdapter();
        checkBTState();

        tt = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR)
                    tt.setLanguage(Locale.CANADA);
            }
        });





        btn1On.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                sendData("1");
                Toast.makeText(getBaseContext(), "You are at the origin.", Toast.LENGTH_SHORT).show();
                tt.speak("You are at the origin.", TextToSpeech.QUEUE_FLUSH,null,null);
            }
        });

        btn1Off.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                sendData("2");
                Toast.makeText(getBaseContext(), "The destination will be on the right.", Toast.LENGTH_SHORT).show();
                tt.speak("The destination will be on the right.", TextToSpeech.QUEUE_FLUSH,null,null);
            }
        });

        btn2On.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                sendData("3");
                Toast.makeText(getBaseContext(), "The destination will be on the left.", Toast.LENGTH_SHORT).show();
                tt.speak("The destination will be on the left.", TextToSpeech.QUEUE_FLUSH,null,null);
            }
        });

        btn2Off.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                sendData("4");
                Toast.makeText(getBaseContext(), "You are at the destination.", Toast.LENGTH_SHORT).show();
                tt.speak("You are at the destination.", TextToSpeech.QUEUE_FLUSH,null,null);
            }
        });

        btn3On.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                sendData("5");
                Toast.makeText(getBaseContext(), "Turn right.", Toast.LENGTH_SHORT).show();
                tt.speak("Turn right.", TextToSpeech.QUEUE_FLUSH,null,null);
            }
        });

        btn3Off.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                sendData("6");
                Toast.makeText(getBaseContext(), "Turn left.", Toast.LENGTH_SHORT).show();
                tt.speak("Turn left.", TextToSpeech.QUEUE_FLUSH,null,null);
            }
        });

        btn4On.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                sendData("7");
                Toast.makeText(getBaseContext(), "Continue past the intersection.", Toast.LENGTH_SHORT).show();
                tt.speak("Continue past the intersection.", TextToSpeech.QUEUE_FLUSH,null,null);
            }
        });


    }

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

        Log.d(TAG, "...onResume - try connect...");

        // Set up a pointer to the remote node using it's address.
        BluetoothDevice device = btAdapter.getRemoteDevice(address);

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
        finish();
    }

    private void sendData(String message) {
        byte[] msgBuffer = message.getBytes();

        Log.d(TAG, "...Send data: " + message + "...");

        try {
            outStream.write(msgBuffer);
        } catch (IOException e) {
            String msg = "In onResume() and an exception occurred during write: " + e.getMessage();
            if (address.equals("00:00:00:00:00:00"))
                msg = msg + ".\n\nUpdate your server address from 00:00:00:00:00:00 to the correct address on line 35 in the java code";
            msg = msg +  ".\n\nCheck that the SPP UUID: " + MY_UUID.toString() + " exists on server.\n\n";

            errorExit("Fatal Error", msg);
        }
    }
}