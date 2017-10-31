package com.example.jurek.btrobot;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.RunnableFuture;

import static com.example.jurek.btrobot.R.id.textView;

public class MainActivity extends AppCompatActivity {

    private BluetoothAdapter BA;
    private Set<BluetoothDevice>pairedDevices;
    private static final UUID MY_UUID =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private OutputStream os;
    private InputStream is;
    private boolean result;
    boolean stopThread, stopSendThread;
    byte buffer[];
    private int speedLeft = 0 , speedRight = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        seekBarsListeners();

        BA = BluetoothAdapter.getDefaultAdapter();

    }


    public void seekBarsListeners(){
        SeekBar seekBarL = (SeekBar) findViewById(R.id.mySeekBar1);
        SeekBar seekBarR = (SeekBar) findViewById(R.id.mySeekBar2);
        final TextView t1 = (TextView) findViewById(textView);
        final TextView t2 = (TextView) findViewById(R.id.textView2);

        seekBarL.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener(){

                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress,  boolean fromUser){
                        speedLeft = progress - 50;
                        t1.setText("V Left : " + speedLeft);
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                        seekBar.setProgress(50);

                    }

                }
        );

        seekBarR.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener(){

                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress,  boolean fromUser){
                        speedRight = progress - 50;
                        t2.setText("V Right : " + speedRight);
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                        seekBar.setProgress(50);

                    }

                }
        );
    }



    public void onClickButton(View view) throws InterruptedException {
        if(!BA.isEnabled()){
            Intent turnOn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(turnOn, 0);
            Toast.makeText(getApplicationContext(), "Turned on",Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getApplicationContext(), "Already on", Toast.LENGTH_SHORT).show();
        }

        pairedDevices = BA.getBondedDevices();

        System.out.println("bt wlaczony");
        startTransmission(((EditText) findViewById(R.id.editText1)).getText().toString());
    }

    public boolean startTransmission(String name) throws InterruptedException {
        BluetoothDevice device = null;
        for(BluetoothDevice bd : pairedDevices){
            if(bd.getName().equals(name)){

                device = bd;
                break;
            }
        }
        BluetoothSocket socket = null;
        boolean connected = false;
        int i = 0;
        while (!connected){
            try {
                connected = true;
                socket = device.createRfcommSocketToServiceRecord(MY_UUID);
                socket.connect();


            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Nie polaczono");
                connected = false;
                Thread.sleep(1000);
            }
            finally {
                if (i > 5){
                    i=0;
                    System.out.println("5 nieudanych prob");
                    break;
                }
            }
        }
        if(connected){
            System.out.println("Polaczono - otwieram strumienie");
            try {
                os = socket.getOutputStream();

            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                is = socket.getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }


            System.out.println("connection opened");
        }
        beginListenForData();
        beginSendData();
        return connected;
    }

    void beginListenForData()
    {
        final Handler handler = new Handler();
        stopThread = false;
        buffer = new byte[1024];
        Thread thread  = new Thread(new Runnable()
        {
            public void run()
            {
                while(!Thread.currentThread().isInterrupted() && !stopThread)
                {
                    try
                    {
                        int byteCount = is.available();
                        if(byteCount > 0)
                        {
                            byte[] rawBytes = new byte[byteCount];
                            is.read(rawBytes);
                            final String string=new String(rawBytes,"UTF-8");
                            handler.post(new Runnable() {
                                public void run()
                                {
                                    TextView textView = (TextView) findViewById(R.id.textView5);
                                    textView.setText(string);
                                }
                            });

                        }
                    }
                    catch (IOException ex)
                    {
                        stopThread = true;
                    }
                }
            }
        });

        thread.start();
    }

    void beginSendData(){
        //final Handler handler = new Handler();
        stopSendThread = false;
        final Thread thread = new Thread(new Runnable() {

            public void run() {
                while(!Thread.currentThread().isInterrupted() && !stopSendThread){
                    try{
                        try {
                            Thread.currentThread().sleep(100);

                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        String string = ":1" + (char)speedLeft + (char)speedRight + "@";
                        try {
                            os.write(string.getBytes());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                    } catch (Exception e){
                        e.printStackTrace();
                        stopSendThread = true;
                    }


                }

            }
        });
        thread.start();
        System.out.println("koniec funkcji startsend");
    }

    public void onClickSend(View view) {
        EditText editText = (EditText) findViewById(R.id.editText1) ;
        String string = editText.getText().toString();
        string.concat("\n");
        try {
            os.write(string.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
        TextView tv = (TextView) findViewById(R.id.textView4);
        tv.setText("\nSent:"+string+"\n");
    }
}

