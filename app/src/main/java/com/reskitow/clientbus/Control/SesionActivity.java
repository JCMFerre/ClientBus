package com.reskitow.clientbus.Control;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.reskitow.clientbus.R;
import com.reskitow.clientbus.Servicios.GeolocalizacionService;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class SesionActivity extends AppCompatActivity implements View.OnClickListener {

    private Button btnCerrarSesion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sesion);
        mostrarInfoTextViews();
        findViews();
        inicializarEventos();
    }

    /**
     * Inicializa el evento OnClickListener del btnCerrarSesion.
     */
    private void inicializarEventos() {
        btnCerrarSesion.setOnClickListener(this);
    }

    /**
     * Muestra la info al usuario a través de TextView, obtiene el id de sesión de las preferencias,
     * muestra el id de sesión y la matrícula haciendo idSesion.subString(0,7).
     */
    private void mostrarInfoTextViews() {
        String idSesion = obtenerIdSesionPrefs();
        ((TextView) findViewById(R.id.txt_id_sesion)).setText(getString(R.string.info_sesion_actual) + " " + idSesion);
        ((TextView) findViewById(R.id.txt_matricula_sesion)).setText(getString(R.string.et_login) + ": " + idSesion.substring(0, 7));
    }

    /**
     * finViewById del botón de cerrar sesión.
     */
    private void findViews() {
        btnCerrarSesion = (Button) findViewById(R.id.btn_cerrar_sesion);
    }

    /**
     * Infla el menú.
     *
     * @param menu
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_principal, menu);
        return true;
    }

    /**
     * Listener menú, el único botón lanza las preferencias.
     *
     * @param item
     * @return
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.btn_menu_ajustes:
                lanzarPrefs();
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void lanzarPrefs() {
        startActivity(new Intent(this, PreferenciasActivity.class));
    }

    /**
     * Obtiene la IP guardada en las preferencias.
     *
     * @return IP guardada.
     */
    public String obtenerIpPrefs() {
        return PreferenceManager.getDefaultSharedPreferences(this)
                .getString(this.getString(R.string.key_ip_servidor), null);
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
     * Obtiene el ID de sesión guardado en las preferencias.
     *
     * @return ID de sesión guardado.
     */
    public String obtenerIdSesionPrefs() {
        return PreferenceManager.getDefaultSharedPreferences(this)
                .getString(this.getString(R.string.key_id_sesion), null);
    }

    /**
     * Si ha sido posible cerrar sesión para el servicio GeolocalizacionService, quita el ID de sesión
     * de las preferencias, inicia la activity principal MainActivity y finaliza esta.
     * <p>
     * Si no se ha cerrado sesión correctamente muestra un error en forma de Toast.
     *
     * @param resultado true si se ha cerrado sesión correctamente o false en caso contrario.
     */
    private void cerrarSesion(Boolean resultado) {
        if (resultado) {
            stopService(new Intent(this, GeolocalizacionService.class));
            quitarIdSesionPreferencias();
            startActivity(new Intent(this, MainActivity.class));
            finish();
        } else {
            Toast.makeText(this, getString(R.string.error_rest), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Quita el ID de sesión de las preferencias.
     */
    private void quitarIdSesionPreferencias() {
        SharedPreferences preferencias = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = preferencias.edit();
        editor.putString(getString(R.string.key_id_sesion), null);
        editor.commit();
    }

    /**
     * Listener de los botones.
     * El único botón llama al iniciarCierreSesion.
     *
     * @param v vista pulsada.
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_cerrar_sesion:
                iniciarCierreSesion();
                break;
        }
    }

    /**
     * Ejecuta la tarea asíncrona CerrarSesion.
     */
    private void iniciarCierreSesion() {
        new CerrarSesion().execute();
    }

    private class CerrarSesion extends AsyncTask<Void, Integer, Boolean> {

        private String ipServidor;
        private String idSesion;
        private String puerto;
        private ProgressDialog progressDialog;

        /**
         * Método que se ejecuta en el hilo principal, aquí seteamos los atributos a nuestro gusto
         * y se aprovecha para mostrar una vista mientras realiza la tarea.
         */
        @Override
        protected void onPreExecute() {
            progressDialog = ProgressDialog.show(SesionActivity.this,
                    SesionActivity.this.getString(R.string.sesion_cerrar_titulo),
                    SesionActivity.this.getString(R.string.mensaje_sesion_dialog));
            ipServidor = SesionActivity.this.obtenerIpPrefs();
            idSesion = SesionActivity.this.obtenerIdSesionPrefs();
            puerto = SesionActivity.this.obtenerPuertoPrefs();
        }

        /**
         * Método el cual se ejecuta en segundo plano, si queremos mostrar algo por el hilo principal
         * habría que llamar a publishProgress que este llama al onProgresUpdate. Aquí recuperamos la
         * validación de sesión.
         * <p>
         * No se utilizan métodos ni clases @Deprecated, se utiliza HttpURLConnection y para formar
         * la URL SringBuffer porque al estar manejando varios hilos que sea sincronizado.
         *
         * @param params
         * @return true si ha sido posible cerrar sesión (Cambia el estado de la BD externa a activo 'N') o false si no
         * ha sido posible o a ocurrido algún error.
         */
        @Override
        protected Boolean doInBackground(Void... params) {
            boolean correcto = false;
            StringBuffer stringBuffer = new StringBuffer().append("http://").append(ipServidor)
                    .append(":").append(puerto).append("/ServicioRestAutobus/webresources/sesion");
            try {
                JSONObject json = new JSONObject().put("matricula", idSesion.substring(0, 7)).put("contrasena", "noimportaesto").put("activo", false);
                HttpURLConnection urlConnection = (HttpURLConnection) new URL(stringBuffer.toString()).openConnection();
                urlConnection.setRequestMethod("POST");
                urlConnection.setRequestProperty("Content-type", "application/json");
                urlConnection.setDoOutput(true);
                OutputStream os = urlConnection.getOutputStream();
                os.write(json.toString().getBytes());
                os.flush();
                os.close();
                int codigo = urlConnection.getResponseCode();
                if (codigo == HttpURLConnection.HTTP_NO_CONTENT) {
                    correcto = true;
                }
            } catch (IOException io) {
                correcto = false;
                publishProgress(-33);
            } catch (JSONException e) {
                correcto = false;
                e.printStackTrace();
            }
            return correcto;
        }

        /**
         * Método que se ejecuta en el hilo principal, y si le llega un -33 significa que ha entrado en un catch,
         * así que mostraremos un Toast de error.
         *
         * @param values -33 en caso de error.
         */
        @Override
        protected void onProgressUpdate(Integer... values) {
            if (values[0] == -33) {
                Toast.makeText(SesionActivity.this, SesionActivity.this.getString(R.string.error_rest), Toast.LENGTH_LONG);
            }
        }

        /**
         * Quitamos el ProgressDialog con el método dismiss y llamamos al método cerrarSesion enviándole el resultado.
         *
         * @param resultado true si se ha cerrado sesión correctamente o false en caso contrario.
         */
        @Override
        protected void onPostExecute(Boolean resultado) {
            progressDialog.dismiss();
            SesionActivity.this.cerrarSesion(resultado);
        }

    }

}
