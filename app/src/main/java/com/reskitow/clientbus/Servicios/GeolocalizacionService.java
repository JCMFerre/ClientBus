package com.reskitow.clientbus.Servicios;

import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.reskitow.clientbus.Model.Ruta;
import com.reskitow.clientbus.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

public class GeolocalizacionService extends Service implements LocationListener,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private LocationRequest localizacionesRequest;
    private String idSesion;
    private String matricula;
    private GoogleApiClient apiClient;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        idSesion = obtenerIdSesionPrefs();
        matricula = idSesion.substring(0, 7);
        inicializarGoogleApiClient();
        inicializarLocationRequest();
        conectarGoogleApiClient();
        return super.onStartCommand(intent, flags, startId);
    }

    private void conectarGoogleApiClient() {
        if (!apiClient.isConnected()) {
            apiClient.connect();
        }
    }

    private void inicializarLocationRequest() {
        if (localizacionesRequest == null) {
            localizacionesRequest = new LocationRequest();
            localizacionesRequest.setInterval(12000);
            localizacionesRequest.setFastestInterval(7500);
            localizacionesRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        }
    }

    private void inicializarGoogleApiClient() {
        if (apiClient == null) {
            apiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
    }

    @Override
    public void onDestroy() {
        apiClient.disconnect();
        super.onDestroy();
    }

    private String obtenerIdSesionPrefs() {
        return PreferenceManager.getDefaultSharedPreferences(this).getString(getString(R.string.key_id_sesion), null);
    }

    private String obtenerIPPrefs() {
        return PreferenceManager.getDefaultSharedPreferences(this).getString(getString(R.string.key_ip_servidor), null);
    }

    @Override
    public void onLocationChanged(Location location) {
        String fecha = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date().getTime());
        new AnadirRutas().execute(new Ruta(idSesion, matricula, fecha, location.getLatitude(), location.getLongitude()));
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        anadirActualizacionesLocalizaciones();
    }

    @Override
    public void onConnectionSuspended(int i) {
        //
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        //
    }

    @SuppressWarnings("MissingPermission")
    private void anadirActualizacionesLocalizaciones() {
        LocationServices.FusedLocationApi.requestLocationUpdates(apiClient, localizacionesRequest, this);
    }

    /*private void quitarActualizacionesLocalizaciones() {
        LocationServices.FusedLocationApi.removeLocationUpdates(apiClient, this);
    }*/

    private class AnadirRutas extends AsyncTask<Ruta, Void, Boolean> {

        private String ipServidor;

        @Override
        protected void onPreExecute() {
            ipServidor = GeolocalizacionService.this.obtenerIPPrefs();
        }

        @Override
        protected Boolean doInBackground(Ruta... params) {
            boolean correcto = false;
            StringBuffer url = new StringBuffer().append("http://")
                    .append(ipServidor)
                    .append(":8080/ServicioRestAutobus/webresources/rutas");
            Ruta rutaActual = params[0];
            try {
                JSONObject jsonObject = new JSONObject()
                        .put("idSesion", rutaActual.getIdSesion())
                        .put("matricula", rutaActual.getMatricula())
                        .put("fecha", rutaActual.getFecha())
                        .put("latitud", rutaActual.getLatitud())
                        .put("longitud", rutaActual.getLongitud());
                HttpURLConnection urlConnection = (HttpURLConnection) new URL(url.toString()).openConnection();
                urlConnection.setRequestMethod("POST");
                urlConnection.setRequestProperty("Content-type", "application/json");
                urlConnection.setDoOutput(true);
                OutputStream os = urlConnection.getOutputStream();
                os.write(jsonObject.toString().getBytes());
                os.flush();
                os.close();
                int codigo = urlConnection.getResponseCode();
                if (codigo == HttpURLConnection.HTTP_NO_CONTENT) {
                    correcto = true;
                }
            } catch (IOException e) {
                correcto = false;
            } catch (JSONException e) {
                correcto = false;
            }
            return correcto;
        }
    }
}
