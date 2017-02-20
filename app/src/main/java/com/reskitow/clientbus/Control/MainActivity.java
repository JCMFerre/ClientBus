package com.reskitow.clientbus.Control;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.reskitow.clientbus.Model.Autobus;
import com.reskitow.clientbus.Persistencia.BDSQLite;
import com.reskitow.clientbus.R;
import com.reskitow.clientbus.Servicios.GeolocalizacionService;

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
    private static final int PETICION_PERMISO_LOCALIZACION = 33;

    private TextView txtInfoPermisos;
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

    /**
     * Comprueba el ID de sesión, si no está a null, significa que hay una sesión en curso,
     * y lanza la activity sesión con el método lanzarSesion().
     */
    private void comprobarSesion() {
        String idSesion = obtenerIdSesion();
        if ((idSesion != null) && (!idSesion.equals("null"))) {
            lanzarSesion();
        }
    }

    /**
     * Comprueba la IP, si está a null o mal formada, si cumple alguna de las dos condiciones
     * lanza la activity que gestiona el fragment de preferencias (lanzarPrefs()).
     */
    private void comprobarIp() {
        String ip = obtenerIpPrefs();
        if (ip == null || !isIPCorrecta(ip)) {
            lanzarPrefs();
        }
    }

    /**
     * Crea una instancia de BDSQLite, no he implementado la BD interna en esta app, en la
     * ServerBus sí. En otra versión mejorada de la app habría que tener en cuenta si no tenemos
     * conexión alternativas al webServices...
     */
    private void inicializarGestorBD() {
        bdsqLite = new BDSQLite(this);
    }

    /**
     * Lanza la activity SesionActivity y finaliza esta activity.
     */
    private void lanzarSesion() {
        startActivity(new Intent(this, SesionActivity.class));
        finish();
    }

    /**
     * Lanza la activity PreferenciasActivity.
     */
    private void lanzarPrefs() {
        startActivity(new Intent(this, PreferenciasActivity.class));
    }

    /**
     * Inicializa los eventos onClickListener de botonInicioSesion y txtInfoPermisos.
     */
    private void inicializarEventos() {
        botonInicioSesion.setOnClickListener(this);
        txtInfoPermisos.setOnClickListener(this);
    }

    /**
     * Inicializa las variables con las vistas del layout.
     */
    private void findViews() {
        etMatricula = (EditText) findViewById(R.id.et_login);
        etContrasena = (EditText) findViewById(R.id.et_password);
        botonInicioSesion = (Button) findViewById(R.id.btn_login);
        txtInfoPermisos = (TextView) findViewById(R.id.info_permisos);
    }

    /**
     * Infla el menu.
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
     * Listener de los botones del menú, lanza preferencias.
     *
     * @param item Item pulsado.
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

    /**
     * Listener de las vistas, R.id.info_permisos, pide los permisos en tiempo de ejecución y el R.id.btn_login
     * llama a comprobarCampos().
     *
     * @param view vista pulsada.
     */
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_login:
                comprobarCampos();
                break;
            case R.id.info_permisos:
                solicitarPermisos();
                break;
            default:
                break;
        }
    }

    /**
     * Comprueba si los campos no están vacíos, si no están vacíos llama al método iniciarSesion().
     */
    private void comprobarCampos() {
        if (validarCampo(R.id.login_error, etMatricula) &
                validarCampo(R.id.contrasena_error, etContrasena)) {
            iniciarSesion();
        }
    }

    /**
     * Valida el campo en caso de estar vacío, si es incorrecto pone la vista con el id que se le
     * pasa por parámetro a visible, si no la oculta.
     *
     * @param id       ID de la vista con el mensaje de error.
     * @param editText EditText a comprobar.
     * @return true si el campo no esta vacío o falso en caso contrario.
     */
    private boolean validarCampo(int id, EditText editText) {
        boolean campoVacio = editText.getText().toString().isEmpty();
        (findViewById(id)).setVisibility(campoVacio ? View.VISIBLE : View.GONE);
        return !campoVacio;
    }

    /**
     * Lanza la tarea asíncrona con el autobús a comprobar.
     */
    private void iniciarSesion() {
        new InicioSesion().execute(obtenerAutobusPorCampos());
    }

    /**
     * Crea un objeto de tipo Autobus, con los campos de los EditText.
     *
     * @return objeto creado.
     */
    private Autobus obtenerAutobusPorCampos() {
        return new Autobus(etMatricula.getText().toString(), etContrasena.getText().toString(), false);
    }

    /**
     * Comprueba si la ip es correcta (IP v4).
     *
     * @param ip Ip a comprobar.
     * @return Devuelve true si está bien formada y false al revés.
     */
    private boolean isIPCorrecta(String ip) {
        return PATTERN_IP.matcher(ip).matches();
    }

    /**
     * Método llamado desde la tarea asíncrona, que comprueba los permisos de la APP, si no están aceptados,
     * llama al método solicitarPermisos(), si no llama a permisosCorrectoLanzarTodo().
     * <p>
     * Si el inicio de sesión no ha sido correcto muestra un Toast informando de la situación.
     *
     * @param correcto Si el inicio a sido correcto.
     */
    private void validarSesion(Boolean correcto) {
        if (correcto) {
            if (ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                solicitarPermisos();
            } else {
                permisosCorrectoLanzarTodo();
            }
        } else {
            Toast.makeText(this, getString(R.string.error_al_iniciar_sesion), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Solicita los permisos de localización, el método onRequestPermissionResult, será llamado cuando
     * el usuario acepte o decline con los resultados..
     */
    private void solicitarPermisos() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                PETICION_PERMISO_LOCALIZACION);
    }

    /**
     * Método al cual es llamado cuando el usuario acepta o declina los permisos, si son aceptados por
     * el usuario, llama a permisosCorrectoLanzarTodo y oculta la vista con el texto informando de los
     * permisos, si no son aceptados, el TextView sigue ahí para que el usuario pueda volver a ver el Dialog
     * que pide los permisos en tiempo de ejecución.
     *
     * @param requestCode  Este parámetro deberá ser como PETICION_PERMISO_LOCALIZACION en este caso..
     * @param permissions  permisos solicitados.
     * @param grantResults resultado de los permisos solicitados, PackageManager.PERMISSION_GRANTED == Aceptados.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PETICION_PERMISO_LOCALIZACION) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                permisosCorrectoLanzarTodo();
                txtInfoPermisos.setVisibility(View.GONE);
            } else {
                txtInfoPermisos.setVisibility(View.VISIBLE);
            }
        }
    }

    /**
     * Guarda en las preferencias el ID de sesión que calcula el método calcularIdSesion,
     * lanza el servicio (lanzarServicio()) y la activity SesionActivity (lanzarSesion()).
     */
    private void permisosCorrectoLanzarTodo() {
        SharedPreferences preferencias = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = preferencias.edit();
        editor.putString(getString(R.string.key_id_sesion), calcularIdSesion(obtenerAutobusPorCampos().getMatricula().toUpperCase()));
        editor.commit();
        lanzarServicio();
        lanzarSesion();
    }

    /**
     * Lanza el servicio GeolocalizacionService.
     */
    private void lanzarServicio() {
        startService(new Intent(this, GeolocalizacionService.class));
    }

    /**
     * Obtener el ID de sesión, devuelve null si no existe.
     *
     * @return ID de sesión o null.
     */
    private String obtenerIdSesion() {
        return PreferenceManager.getDefaultSharedPreferences(MainActivity.this)
                .getString(MainActivity.this.getString(R.string.key_id_sesion), null);
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
     * Obtiene la IP guardada en las preferencias.
     *
     * @return IP guardada.
     */
    public String obtenerIpPrefs() {
        return PreferenceManager.getDefaultSharedPreferences(MainActivity.this)
                .getString(MainActivity.this.getString(R.string.key_ip_servidor), null);
    }

    /**
     * Calcula el ID de sesión con el formato siguiente: diaMesAñoHoraMinutoSegundosMilisegundosMatricula,
     * ej. 3292CSP1902201715112322.
     * <p>
     * La fecha se calcula en el momento de ejecutar este método y la matrícula es la que le pasan por parámetro.
     *
     * @param matricula
     * @return ID de sesión calculado.
     */
    private String calcularIdSesion(String matricula) {
        return new StringBuilder(23)
                .append(matricula)
                .append(new SimpleDateFormat("ddMMyyyyHHmmssSS").format(new Date().getTime()))
                .toString();
    }

    /**
     * Clase que gestiona la tarea asíncrona y obtiene la validación de sesión.
     */
    private class InicioSesion extends AsyncTask<Autobus, Integer, Boolean> {

        private ProgressDialog progressDialog;
        private String[] informacionProgressBar;
        private String ipServidor;
        private String puerto;

        /**
         * Método que se ejecuta en el hilo principal, aquí seteamos los atributos a nuestro gusto
         * y se aprovecha para mostrar una vista mientras realiza la tarea.
         */
        @Override
        protected void onPreExecute() {
            informacionProgressBar = MainActivity.this.getResources().getStringArray(R.array.info_tarea_asincrona);
            progressDialog = ProgressDialog.show(MainActivity.this,
                    MainActivity.this.getString(R.string.titulo_tarea_asincrona),
                    informacionProgressBar[0]);
            botonInicioSesion.setEnabled(false);
            etMatricula.setEnabled(false);
            etContrasena.setEnabled(false);
            ipServidor = MainActivity.this.obtenerIpPrefs();
            puerto = MainActivity.this.obtenerPuertoPrefs();
        }

        /**
         * Método el cual se ejecuta en segundo plano, si queremos mostrar algo por el hilo principal
         * habría que llamar a publishProgress que este llama al onProgresUpdate. Aquí recuperamos la
         * validación de sesión.
         * <p>
         * No se utilizan métodos ni clases @Deprecated, se utiliza HttpURLConnection y para formar
         * la URL SringBuffer porque al estar manejando varios hilos que sea sincronizado.
         *
         * @param params Autobús, con matrícula y contraseña para validar.
         * @return true si ha sido posible iniciar sesión (Cambia el estado de la BD externa a activo 'S') o false si no
         * ha sido posible o a ocurrido algún error.
         */
        @Override
        protected Boolean doInBackground(Autobus... params) {
            boolean correcto = false;
            try {
                publishProgress(2);
                StringBuffer json = new StringBuffer().append("http://").append(ipServidor).append(":")
                        .append(puerto).append("/ServicioRestAutobus/webresources/sesion/validarSesion/")
                        .append("%7B\"matricula\":\"").append(params[0].getMatricula()).append("\",\"contrasena\":\"")
                        .append(params[0].getContrasena()).append("\",\"activo\":false%7D");
                publishProgress(1);
                publishProgress(3);
                HttpURLConnection urlConnection = (HttpURLConnection) new URL(json.toString()).openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.setRequestProperty("Content-type", "application/json");
                publishProgress(4);
                int responseCode = urlConnection.getResponseCode();
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

        /**
         * Método que se ejecuta en el hilo principal, y si le llega un -33 significa que ha entrado en un catch,
         * así que mostraremos un Toast de error, si no, vamos mostrando el proceso de la tarea.
         *
         * @param values Índice del array de String's o -33 en caso de error.
         */
        @Override
        protected void onProgressUpdate(Integer... values) {
            if (values[0] == -33) {
                Toast.makeText(MainActivity.this, MainActivity.this.getString(R.string.error_rest), Toast.LENGTH_LONG).show();
            } else {
                progressDialog.setMessage(informacionProgressBar[values[0]]);
            }
        }

        /**
         * Método que se ejecuta después del doInBackground, le llega el resultado en forma de boolean que devuelve
         * el método anterior mencionado, ocultamos las vistas que salían mientras cargaba.
         * <p>
         * Llama al método validarSesion enviándole el resultado de inicio de sesión.
         *
         * @param result Resultado obtenido del inicio de sesión, true si es correcto, false en caso contrario.
         */
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
