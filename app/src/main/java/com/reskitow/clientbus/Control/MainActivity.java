package com.reskitow.clientbus.Control;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.reskitow.clientbus.Model.Autobus;
import com.reskitow.clientbus.Persistencia.BDSQLite;
import com.reskitow.clientbus.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final Pattern PATTERN_IP = Pattern.compile(
            "^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$");

    private EditText etMatricula;
    private EditText etContrasena;
    private Button botonInicioSesion;
    private BDSQLite bdsqLite;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        comprobarSesion();
        comprobarIp();
        inicializarGestorBD();
        findViews();
        inicializarEventos();
    }

    private void comprobarSesion() {
        String idSesion = obtenerIdSesion();
        if ((idSesion != null) && (!idSesion.equals("null"))) {
            lanzarSesion();
        }
    }

    private void lanzarSesion() {
        startActivity(new Intent(this, SesionActivity.class));
        finish();
    }

    private void comprobarIp() {
        String ip = obtenerIpPrefs();
        if (ip == null || !isIPCorrecta(ip)) {
            lanzarPrefs();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_principal, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean retorno = true;
        switch (item.getItemId()) {
            case R.id.btn_menu_ajustes:
                lanzarPrefs();
                break;
            default:
                retorno = super.onOptionsItemSelected(item);
                break;
        }
        return retorno;
    }

    private void lanzarPrefs() {
        startActivity(new Intent(this, PreferenciasActivity.class));
    }

    private void inicializarEventos() {
        botonInicioSesion.setOnClickListener(this);
    }

    private void inicializarGestorBD() {
        bdsqLite = new BDSQLite(this);
    }

    private void findViews() {
        etMatricula = (EditText) findViewById(R.id.et_login);
        etContrasena = (EditText) findViewById(R.id.et_password);
        botonInicioSesion = (Button) findViewById(R.id.btn_login);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_login:
                comprobarCampos();
                break;
            default:
                break;
        }
    }

    private void comprobarCampos() {
        if (validarCampo(R.id.et_login_encap, etMatricula, getString(R.string.campo_login_error)) &
                validarCampo(R.id.et_password_encap, etContrasena, getString(R.string.campo_contrasena_error))) {
            iniciarSesion();
        }
    }

    private boolean validarCampo(int id, EditText editText, String campoError) {
        boolean campoVacio = editText.getText().toString().isEmpty();
        ((TextInputLayout) findViewById(id)).setError(campoVacio ? campoError : null);
        return !campoVacio;
    }

    private void iniciarSesion() {
        new InicioSesion().execute(obtenerAutobusPorCampos());
    }

    private Autobus obtenerAutobusPorCampos() {
        return new Autobus(etMatricula.getText().toString(), etContrasena.getText().toString());
    }

    private boolean isIPCorrecta(String ip) {
        return PATTERN_IP.matcher(ip).matches();
    }

    private void validarSesion(Boolean correcto) {
        if (correcto) {
            SharedPreferences preferencias = PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor editor = preferencias.edit();
            editor.putString(getString(R.string.key_id_sesion), calcularIdSesion(obtenerAutobusPorCampos().getMatricula().toUpperCase()));
            // INICIAR SERVICIO CON GEOLOCALIZACION
            editor.commit();
            lanzarSesion();
        } else {
            Toast.makeText(this, getString(R.string.error_al_iniciar_sesion), Toast.LENGTH_SHORT).show();
        }
    }

    private String obtenerIdSesion() {
        return PreferenceManager.getDefaultSharedPreferences(MainActivity.this)
                .getString(MainActivity.this.getString(R.string.key_id_sesion), null);
    }

    public String obtenerIpPrefs() {
        return PreferenceManager.getDefaultSharedPreferences(MainActivity.this)
                .getString(MainActivity.this.getString(R.string.key_ip_servidor), null);
    }

    private String calcularIdSesion(String matricula) {
        return new StringBuilder(23)
                .append(matricula)
                .append(new SimpleDateFormat("ddMMyyyyHHmmssSS").format(new Date().getTime()))
                .toString();
    }

    private class InicioSesion extends AsyncTask<Autobus, Integer, Boolean> {

        private ProgressDialog progressDialog;
        private String[] informacionProgressBar;
        private String ipServidor;

        @Override
        protected void onPreExecute() {
            informacionProgressBar = MainActivity.this.getResources().getStringArray(R.array.info_tarea_asincrona);
            ipServidor = MainActivity.this.obtenerIpPrefs();
            progressDialog = ProgressDialog.show(MainActivity.this,
                    MainActivity.this.getString(R.string.titulo_tarea_asincrona),
                    informacionProgressBar[0]);
            botonInicioSesion.setEnabled(false);
            etMatricula.setEnabled(false);
            etContrasena.setEnabled(false);
        }

        @Override
        protected Boolean doInBackground(Autobus... params) {
            boolean correcto = false;
            try {
                publishProgress(2);
                StringBuffer json = new StringBuffer().append("http://").append(ipServidor)
                        .append(":8080/ServicioRestAutobus/webresources/sesion/validarSesion/")
                        .append("%7B\"matricula\":\"").append(params[0].getMatricula()).append("\",\"contrasena\":\"")
                        .append(params[0].getContrasena()).append("\",\"activo\":false%7D");
                publishProgress(1);
                publishProgress(3);
                HttpURLConnection urlConnection = (HttpURLConnection) new URL(json.toString()).openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.setRequestProperty("Content-type", "application/json");
                publishProgress(4);
                int responseCode = urlConnection.getResponseCode();
                Log.i("GET", "Response Code :: " + responseCode);
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                    String inputLine;
                    StringBuffer response = new StringBuffer();
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();
                    correcto = Boolean.parseBoolean(response.toString());
                }
            } catch (IOException e) {
                publishProgress(-33);
                correcto = false;
            }
            return correcto;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            if (values[0] == -33) {
                Toast.makeText(MainActivity.this, MainActivity.this.getString(R.string.error_rest), Toast.LENGTH_LONG).show();
            } else {
                progressDialog.setMessage(informacionProgressBar[values[0]]);
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            progressDialog.setMessage(informacionProgressBar[informacionProgressBar.length - 1]);
            botonInicioSesion.setEnabled(true);
            etMatricula.setEnabled(true);
            etContrasena.setEnabled(true);
            progressDialog.dismiss();
            validarSesion(result);
        }
    }
}
