package com.equinoxe.medirconsumominimo;

import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    final static long lPrimeraComprobacion = 10*60*1000;
    final static long lPeriodoComprobacion = 60*1000;
    final static int iUmbralCarga = 5;

    Timer timerGrabarDatos;
    DecimalFormat df;
    SimpleDateFormat sdf;
    FileOutputStream fOut;

    boolean bLocation;
    boolean bSendServer;

    boolean bStarted = false;

    private Button btnStart;
    private CheckBox chkGPS;
    private CheckBox chkSendServer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnStart = findViewById(R.id.btnStart);
        chkGPS = findViewById(R.id.chkGPS);
        chkSendServer = findViewById(R.id.chkEnvioServidor);

        df = new DecimalFormat("###.##");
        sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");

        final TimerTask timerTaskComprobarBateria = new TimerTask() {
            public void run() {
                int iCarga = getBatteryCharge();
                if (iCarga < iUmbralCarga)
                    grabarCarga(iCarga);
            }
        };

        timerGrabarDatos = new Timer();
        timerGrabarDatos.scheduleAtFixedRate(timerTaskComprobarBateria, lPrimeraComprobacion, lPeriodoComprobacion);
    }

    public void onStart (View v) {
        if (bStarted) {
            btnStart.setText(R.string.start);

            try {
                fOut.close();
            } catch (Exception e) {
                Toast.makeText(this, "Error al cerrar archivo.", Toast.LENGTH_LONG).show();
            }
        } else {
            btnStart.setText(R.string.stop);

            bLocation = chkGPS.isChecked();
            bSendServer = chkSendServer.isChecked();

            String sFichero = Environment.getExternalStorageDirectory() + "/" + android.os.Build.MODEL + "_Descarga.txt";
            String currentDateandTime = sdf.format(new Date());
            try {
                FileInputStream fIn = new FileInputStream(sFichero);
                InputStreamReader sReader = new InputStreamReader(fIn);
                BufferedReader buffreader = new BufferedReader(sReader);

                fOut = new FileOutputStream(sFichero, false);
                String sCadena = android.os.Build.MODEL + " " + bLocation + " " + bSendServer + " " + currentDateandTime + "\n";
                fOut.write(sCadena.getBytes());
                fOut.flush();
            } catch (Exception e) {
                Toast.makeText(this, "Error al crear archivo: " + sFichero, Toast.LENGTH_LONG).show();
            }
        }

        bStarted = !bStarted;
    }

    private int getBatteryCharge() {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = registerReceiver(null, ifilter);
        return batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
    }

    private void grabarCarga(int iCarga) {
        String currentDateandTime = sdf.format(new Date());
        try {
            String sCadena = currentDateandTime + " - " + iCarga + "\n";
            fOut.write(sCadena.getBytes());
            fOut.flush();
        } catch (Exception e) {
            Toast.makeText(this, "Error grabar.", Toast.LENGTH_LONG).show();
        }
    }
}
