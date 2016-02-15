package oscilloscope.mijaz.com.bluescope;

/**
 * Created by eee on 3/5/2015.
 */

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.Button;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class BluetoothActivity extends Activity implements View.OnClickListener, SeekBar.OnSeekBarChangeListener {

    String prescaler_string = "p128",threshold_string = "t50";

    int seekbar_value;

    BluetoothAdapter mBluetoothAdapter;
    BluetoothSocket mmSocket;
    BluetoothDevice mmDevice;
    OutputStream mmOutputStream;
    InputStream mmInputStream;
    Thread workerThread;
    public int[] readBuffer;
    int readBufferPosition;
    int counter;
    volatile boolean stopWorker;
    protected PowerManager.WakeLock mWakeLock;

    private TextView prescaler_value,threshold_value,frequency_value;
    private Button mConnectButton;
    private Button run_buton,pause_button;
    private SeekBar seekbar_p,seekbar_t,seekbar_f;

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;

    private boolean bready = false;
    private boolean pause = false;

    public WaveformView mWaveform = null;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);

        prescaler_value = (TextView) findViewById(R.id.prescaler);
        threshold_value = (TextView) findViewById(R.id.threshold);
        frequency_value = (TextView) findViewById(R.id.frequency);

        seekbar_f = (SeekBar) findViewById(R.id.seekBar_frequency);
        seekbar_t = (SeekBar) findViewById(R.id.seekBar_threshold);
        seekbar_p = (SeekBar) findViewById(R.id.seekBar_prescaler);

        seekbar_f.setOnSeekBarChangeListener(this);
        seekbar_t.setOnSeekBarChangeListener(this);
        seekbar_p.setOnSeekBarChangeListener(this);

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        this.mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "My Tag");
        this.mWakeLock.acquire();


        run_buton = (Button) findViewById(R.id.run_button);
        pause_button = (Button) findViewById(R.id.pause_button);
        run_buton.setOnClickListener(this);
        pause_button.setOnClickListener(this);

        mConnectButton = (Button) findViewById(R.id.button_connect);
        mConnectButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View arg0) {
                findBT();
                openBT();
            }
        });

        mWaveform = (WaveformView) findViewById(R.id.WaveformArea);

    }

    @Override
    public void onClick(View v)
    {
        int buttonID;
        buttonID = v.getId();
        switch (buttonID) {


            case R.id.run_button:
                    if (bready == true) {
                        pause = false;
                        sendMessage(prescaler_string);
                        sendMessage(threshold_string);
                        Toast.makeText(this, "Started....", Toast.LENGTH_SHORT).show();
                        beginListenForData();
                    } else {
                        Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
                    }
                    break;
            case R.id.pause_button:
                    pause = true;
                    Toast.makeText(this, "Paused....", Toast.LENGTH_SHORT).show();
                break;

        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
    {
        seekbar_value = progress;

        switch (seekBar.getId())
        {
            case R.id.seekBar_prescaler:
                int s = seekbar_value/10;
                switch(s){
                    case 0:
                    case 1: prescaler_string = "p8";
                        prescaler_value.setText ("008");
                        break;
                    case 3:
                    case 2: prescaler_string = "p16";
                        prescaler_value.setText ("016");
                        break;
                    case 4:
                    case 5: prescaler_string = "p32";
                        prescaler_value.setText ("032");
                        break;
                    case 6:
                    case 7: prescaler_string = "p64";
                        prescaler_value.setText ("064");
                        break;
                    default:
                        prescaler_value.setText ("128");
                        break;
                        }

                break;
            case R.id.seekBar_threshold:
               s = seekbar_value;
               threshold_string = "t" + Integer.toString(s);
                threshold_value.setText(Integer.toString(s));

                    break;
            case R.id.seekBar_frequency:
                s = seekbar_value/10;
                switch(s){
                    case 0:
                    case 1:
                        mWaveform.setTimeBase(1);
                        frequency_value.setText ("100 Hz");
                        break;
                    case 3:
                    case 2:
                        mWaveform.setTimeBase(2);
                        frequency_value.setText ("1 kHz");
                        break;
                    case 4:
                    case 5:
                        mWaveform.setTimeBase(3);
                        frequency_value.setText ("10 kHz");
                        break;
                    case 6:
                    case 7:
                        mWaveform.setTimeBase(4);
                        frequency_value.setText ("100 kHz");
                        break;
                    case 8:
                    case 9:
                    case 10:
                        mWaveform.setTimeBase(5);
                        frequency_value.setText ("1 MHz");
                        break;
                    default:
                        mWaveform.setTimeBase(1);
                        frequency_value.setText ("100 Hz");
                        break;

                }
                break;

        }
    }
    @Override
    public void onStartTrackingTouch(SeekBar seekBar)
    {
        // TODO Auto-generated method stub
    }
    @Override
    public void onStopTrackingTouch(SeekBar seekBar)
    {
        // TODO Auto-generated method stub
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        // Stop the Bluetooth RFCOMM services
        if (bready==true) closeBT();
        // release screen being on
        if (mWakeLock.isHeld()) {
            mWakeLock.release();
        }
    }


    void findBT()
    {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(mBluetoothAdapter == null)
        {
            Toast.makeText(this, "No adapter found", Toast.LENGTH_SHORT).show();
        }

        if(!mBluetoothAdapter.isEnabled())
        {
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, 0);
        }

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if(pairedDevices.size() > 0)
        {
            for(BluetoothDevice device : pairedDevices)
            {
                if(device.getName().equals("HC-05"))
                {
                    mmDevice = device;
                    break;
                }
            }
        }
        Toast.makeText(this, "Connected to HC-05 Bluetooth Module", Toast.LENGTH_SHORT).show();
    }

    void openBT()
    {
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
        try {//Standard SerialPortService ID
            mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
        }catch (IOException e) { }

        try {
            if(mmSocket != null)
            mmSocket.connect();
        }catch (IOException e) {}

        try {
            if(mmSocket != null) {
                mmOutputStream = mmSocket.getOutputStream();
                mmInputStream = mmSocket.getInputStream();
                Toast.makeText(this, "Please Turn on Bluetooth", Toast.LENGTH_SHORT).show();
            }
            else{

            }
            }catch (IOException e) { }

        bready = true;
    }

    void beginListenForData()
    {
        final Handler handler = new Handler();
        final byte data_start = 36;//This is ascii code for $
         final byte data_end = 35;//This is the ASCII code #
        stopWorker = false;
        readBufferPosition = 0;
        readBuffer = new int[1024];
        workerThread = new Thread(new Runnable()
        {
            public void run()
            {
                while(!Thread.currentThread().isInterrupted() && !stopWorker)
                {
                    try
                    {
                        sendMessage(new String("s"));
                        int bytesAvailable = mmInputStream.available();
                        if(bytesAvailable > 0)
                        {
                            byte[] packetBytes = new byte[bytesAvailable];
                            mmInputStream.read(packetBytes);
                            for(int i=0;i<bytesAvailable;i++)
                            {   int x = 0;
                                byte b = packetBytes[i];
                                if (b < 0) // if negative
                                {
                                    x = (int) ((b & 0x7F) + 128);
                                }
                                else
                                {
                                    x = (int) b;
                                }
                              /*  if (b == data_start) {
                                    readBufferPosition = 0;
                                }*/
                                if(b == data_end)
                                {
                                    mWaveform.set_data(readBuffer);
                                    readBufferPosition = 0;
                                }
                                else
                                {
                                    readBuffer[readBufferPosition++] = x;
                                }
                            }
                        }
                    }
                    catch (IOException ex)
                    {
                        stopWorker = true;
                    }
                }
            }
        });
        if(pause = false){
            workerThread.start();
        }

        else {
            sendMessage(new String("S"));
        }
    }

    void sendMessage(String msg)
    {
        try {
            mmOutputStream.write(msg.getBytes());
        } catch (IOException e){}
    }

    void closeBT()
    {
        stopWorker = true;
        try {
            mmOutputStream.close();
            mmInputStream.close();
            mmSocket.close();
        }catch(IOException e){}
    }


}
