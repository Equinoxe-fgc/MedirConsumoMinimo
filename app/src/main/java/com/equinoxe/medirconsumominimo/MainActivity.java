package com.equinoxe.medirconsumominimo;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;

import java.io.FileOutputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    final static long lPeriodoComprobacion = 120*1000;

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

    float fBrilloAnterior;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        btnStart = findViewById(R.id.btnStart);
        chkGPS = findViewById(R.id.chkGPS);
        chkSendServer = findViewById(R.id.chkEnvioServidor);

        df = new DecimalFormat("###.##");
        sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");

        checkForPermissions();

        final TimerTask timerTaskComprobarBateria = new TimerTask() {
            public void run() {
                grabarMedidas();
            }
        };

        timerGrabarDatos = new Timer();
        timerGrabarDatos.scheduleAtFixedRate(timerTaskComprobarBateria, lPeriodoComprobacion, lPeriodoComprobacion);
    }

    public void onStart (View v) {
        if (bStarted) {
            btnStart.setText(R.string.start);

            Window window = getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

            WindowManager.LayoutParams layoutParams = window.getAttributes();
            layoutParams.screenBrightness = fBrilloAnterior / 255f;
            window.setAttributes(layoutParams);

            try {
                fOut.close();
            } catch (Exception e) {
                Toast.makeText(this, "Error al cerrar archivo.", Toast.LENGTH_LONG).show();
            }
        } else {
            btnStart.setText(R.string.stop);

            bLocation = chkGPS.isChecked();
            bSendServer = chkSendServer.isChecked();

            String sMensaje = "Enciende: ";
            if (bLocation)
                sMensaje += "GPS ";
            if (bSendServer)
                sMensaje += "Wifi";

            if (bLocation || bSendServer)
                Toast.makeText(this, sMensaje, Toast.LENGTH_LONG).show();


            Window window = getWindow();
            WindowManager.LayoutParams layoutParams = window.getAttributes();
            fBrilloAnterior = layoutParams.screenBrightness;
            if (bSendServer) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

                layoutParams.screenBrightness = 1f / 255f;
                window.setAttributes(layoutParams);
            }

            String sFichero = Environment.getExternalStorageDirectory() + "/" + android.os.Build.MODEL + "_Descarga.txt";
            String currentDateandTime = sdf.format(new Date());
            try {
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

    public void grabarMedidas() {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = registerReceiver(null, ifilter);

        BatteryManager mBatteryManager = (BatteryManager) this.getSystemService(Context.BATTERY_SERVICE);

        try {
            String sCadena = sdf.format(new Date()) + ":" +
                    batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) + ":" +
                    batteryStatus.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1) + ":" +
                    batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) + ":" +
                    mBatteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE) + ":" +
                    mBatteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);
            fOut.write(sCadena.getBytes());

        } catch (Exception e) {
            Log.e("Fichero de resultados", e.getMessage(), e);
        }
    }

    @Override
    public void onDestroy() {
        timerGrabarDatos.cancel();
        grabarMedidas();
        try {
            //fLog.close();
            fOut.close();
        } catch (Exception e) {
            Log.e("Error - ", "Error cerrando fichero");
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }

        super.onDestroy();
    }

    private void checkForPermissions() {
        String[] PERMISSIONS_STORAGE = {
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {  // Only ask for these permissions on runtime when running Android 6.0 or higher
            int permission = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (permission != PackageManager.PERMISSION_GRANTED) {
                // We don't have permission so prompt the user
                ActivityCompat.requestPermissions(this, PERMISSIONS_STORAGE, 1);
            }
        }
    }
}
