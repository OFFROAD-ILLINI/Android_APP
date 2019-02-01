package illini.offroad.bajatimer;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class TimerTalker {
    private boolean activated = false;


    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private OutputStream outStream = null;
    private InputStream inStream = null;

    // Well known SPP UUID
    private static final UUID MY_UUID =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private static String address = null;
    private Context context;

    public TimerTalker(Context context, String address) {
        this.context = context;

        btAdapter = BluetoothAdapter.getDefaultAdapter();
        checkBTState();


        BluetoothDevice device = btAdapter.getRemoteDevice(address);

        // Two things are needed to make a connection:
        //   A MAC address, which we got above.
        //   A Service ID or UUID.  In this case we are using the
        //     UUID for SPP.
        try {
            btSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
        } catch (IOException e) {
            errorExit("Fatal Error", "In onResume() and socket create failed: " + e.getMessage() + ".");
        }

        // Discovery is resource intensive.  Make sure it isn't going on
        // when you attempt to connect and pass your message.
        btAdapter.cancelDiscovery();

        // Establish the connection.  This will block until it connects.
        try {
            btSocket.connect();
        } catch (IOException e) {
            try {
                btSocket.close();
            } catch (IOException e2) {
                errorExit("Fatal Error", "In onResume() and unable to close socket during connection failure" + e2.getMessage() + ".");
            }
        }

        // Create a data stream so we can talk to server.
        try {
            outStream = btSocket.getOutputStream();
        } catch (IOException e) {
            errorExit("Fatal Error", "In onResume() and output stream creation failed:" + e.getMessage() + ".");
        }

        try {
            inStream = btSocket.getInputStream();
        } catch (IOException e) {
            errorExit("Fatal Error", "In onResume() and input stream creation failed:" + e.getMessage() + ".");
        }
    }

    public void activate() {
        try {
            inStream.reset();
            sendData(0);
            while (inStream.available() == 0) {
                //arduino should send a message back as some form of an ack, possible just zero again but content isnt checked, so we wait for it
            }
            activated = true;
        } catch (IOException e) {
            errorExit("Fatal Error", "Activate failed to clear instream: " + e.getMessage() + ".");
        }
    }

    public boolean isActivated() {
        return activated;
    }

    public int getTime() {
        //TODO read first bit of bluetooth emssage which encodes timed or not, if times, read rest of message for time else return 0
        if (activated) {
            return getData();
        }
        return 0;
    }

    public void pause() {

        if (outStream != null) {
            try {
                outStream.flush();
            } catch (IOException e) {
                errorExit("Fatal Error", "In onPause() and failed to flush output stream: " + e.getMessage() + ".");
            }
        }


        try {
            btSocket.close();
        } catch (IOException e2) {
            errorExit("Fatal Error", "In onPause() and failed to close socket." + e2.getMessage() + ".");
        }
    }


    private void checkBTState() {
        // Check for Bluetooth support and then check to make sure it is turned on

        // Emulator doesn't support Bluetooth and will return null
        if (btAdapter == null) {
            errorExit("Fatal Error", "Bluetooth Not supported. Aborting.");
        } else {
            if (!btAdapter.isEnabled()) {
                //Prompt user to turn on Bluetooth
                Intent enableBtIntent = new Intent(btAdapter.ACTION_REQUEST_ENABLE);
                context.startActivity(enableBtIntent);
            }
        }
    }

    private void errorExit(String title, String message) {
/*        ((Activity) context).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast msg = Toast.makeText(context,
                title + " - " + message, Toast.LENGTH_SHORT);
                msg.show();
            }
        });*/
        ((Activity) context).runOnUiThread(() -> {
            Toast msg = Toast.makeText(context,
                    title + " - " + message, Toast.LENGTH_SHORT);
            msg.show();
        });
        ((Activity) context).finish();

    }

    private void sendData(int message) {

        try {
            outStream.write(message);
        } catch (IOException e) {
            String msg = "In onResume() and an exception occurred during write: " + e.getMessage();
            if (address.equals("00:00:00:00:00:00"))
                msg = msg + ".\n\nUpdate your server address from 00:00:00:00:00:00";
            msg = msg + ".\n\nCheck that the SPP UUID: " + MY_UUID.toString() + " exists on server.\n\n";

            errorExit("Fatal Error", msg);
        }
    }
    private int getData() {
        try {
            return inStream.read();
        } catch (IOException e) {
            String msg = "In onResume() and an exception occurred during write: " + e.getMessage();
            if (address.equals("00:00:00:00:00:00"))
                msg = msg + ".\n\nUpdate your server address from 00:00:00:00:00:00";
            msg = msg + ".\n\nCheck that the SPP UUID: " + MY_UUID.toString() + " exists on server.\n\n";

            errorExit("Fatal Error", msg);
            return -1;
        }
    }
}
