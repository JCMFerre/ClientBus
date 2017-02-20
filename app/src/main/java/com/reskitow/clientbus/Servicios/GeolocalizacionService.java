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

/**
 * Servició que gestiona la localizaciones en segundo plano, y son insertadas con la clase AnadirRutas asíncrona.
 */
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

    /**
     * Aquí empieza el servició (Después del onCreate), inicializamos todo lo que nos interesa,
     * GoogleApiClient, Request de localizaciones y conectamos googleApiClient.
     *
     * @param intent
     * @param flags
     * @param startId
     * @return
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        idSesion = obtenerIdSesionPrefs();
        matricula = idSesion.substring(0, 7);
        inicializarGoogleApiClient();
        inicializarLocationRequest();
        conectarGoogleApiClient();
        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * Comprobamos si está conectado nuestro objeto apiClient y si no lo está, llamamos al método connect.
     * Al conectar debería llamar al callback onConnect.
     */
    private void conectarGoogleApiClient() {
        if (!apiClient.isConnected()) {
            apiClient.connect();
        }
    }

    /**
     * Si nuestro objeto localizacionesRequest está a null, creamos una instancia (new LocationRequest) y añadimos un intervalo de
     * 12000 milis, un intervalo máximo de 7500 milis y con prioridad de máxima precisión.
     */
    private void inicializarLocationRequest() {
        if (localizacionesRequest == null) {
            localizacionesRequest = new LocationRequest();
            localizacionesRequest.setInterval(12000);
            localizacionesRequest.setFastestInterval(7500);
            localizacionesRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        }
    }

    /**
     * Comprobamos que nuestro objeto GoogleApiClient está a null, si lo está, lo inicializamos, añadiéndole
     * la api LocationServices.API, callbacks de conexión y conexión fallida.
     */
    private void inicializarGoogleApiClient() {
        if (apiClient == null) {
            apiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
    }

    /**
     * Sobrescribimos el método onDestroy para desconectar el GoogleApiClient.
     */
    @Override
    public void onDestroy() {
        apiClient.disconnect();
        super.onDestroy();
    }

    /**
     * Obtiene el ID de sesión guardado en las preferencias.
     *
     * @return ID de sesión guardado.
     */
    private String obtenerIdSesionPrefs() {
        return PreferenceManager.getDefaultSharedPreferences(this).getString(getString(R.string.key_id_sesion), null);
    }

    /**
     * Obtiene la IP guardada en las preferencias.
     *
     * @return IP guardada.
     */
    private String obtenerIPPrefs() {
        return PreferenceManager.getDefaultSharedPreferences(this).getString(getString(R.string.key_ip_servidor), null);
    }

    /**
     * Listener de localizaciones, aquí cada localización que nos llegue crearemos un objeto Ruta y llamaremos a la Clase
     * AnadirRutas y le pasaremos la ruta en el execute.
     *
     * @param location Localización a añadir.
     */
    @Override
    public void onLocationChanged(Location location) {
        String fecha = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date().getTime());
        new AnadirRutas().execute(new Ruta(idSesion, matricula, fecha, location.getLatitude(), location.getLongitude()));
    }

    /**
     * Callback cuando está conectado, llamamos al método anadirActualizacionesLocalizaciones, para que
     * pida (Request) localizaciones a través de los objetos apiClient y localizacionesRequest, y this como
     * listener de onLocationChanged.
     *
     * @param bundle
     */
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        anadirActualizacionesLocalizaciones();
    }

    /**
     * Este método y el de onConnectionFailed (Debajo de este), habría que mirar que ha pasado con la conexión
     * y hacer algo (Atacar a la BD local por ejemplo).
     *
     * @param i
     */
    @Override
    public void onConnectionSuspended(int i) {
        //
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        //
    }

    /**
     * Método que hace el request de localizaciones según hemos establecido en el objeto localizacionesRequest.
     * Los permisos los hemos pedido en la Activity anterior y si estamos aquí es porque tenemos permisos.
     */
    @SuppressWarnings("MissingPermission")
    private void anadirActualizacionesLocalizaciones() {
        LocationServices.FusedLocationApi.requestLocationUpdates(apiClient, localizacionesRequest, this);
    }

    /**
     * Obtenemos el puerto del servidor de las preferencias, si no está seteado por defecto es 8080.
     *
     * @return Puerto del servidor.
     */
    private String obtenerPuertoPrefs() {
        return PreferenceManager.getDefaultSharedPreferences(this).getString(getString(R.string.key_puerto_servidor), "8080");
    }

    /**
     * Al finalizar el servicio se debería quitar el request de actualizaciones, comente porque petaba, solo desconecto
     * el GoogleApiClient en el onDestroy.
     */
    /*private void quitarActualizacionesLocalizaciones() {
        LocationServices.FusedLocationApi.removeLocationUpdates(apiClient, this);
    }*/

    /**
     * Clase que gestiona las inserciones con el WebService en segundo plano.
     * No se ha implementado la BD local.
     */
    private class AnadirRutas extends AsyncTask<Ruta, Void, Boolean> {

        private String ipServidor;
        private String puerto;

        /**
         * Método que se ejecuta en el hilo principal, aquí seteamos los atributos a nuestro gusto.
         */
        @Override
        protected void onPreExecute() {
            ipServidor = GeolocalizacionService.this.obtenerIPPrefs();
            puerto = GeolocalizacionService.this.obtenerPuertoPrefs();
        }

        /**
         * Método el cual se ejecuta en segundo plano, si queremos mostrar algo por el hilo principal
         * habría que llamar a publishProgress que este llama al onProgresUpdate. Aquí insertamos la
         * ruta que nos llega por parámetro.
         *
         * @param params Ruta(s) a añadir.
         * @return boolean si se ha ejecutado correctamente el bloque de código.
         */
        @Override
        protected Boolean doInBackground(Ruta... params) {
            boolean correcto = false;
            StringBuffer url = new StringBuffer().append("http://").append(ipServidor).append(":")
                    .append(puerto).append("/ServicioRestAutobus/webresources/rutas");
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
