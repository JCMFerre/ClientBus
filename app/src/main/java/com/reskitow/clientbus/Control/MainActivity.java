package com.reskitow.clientbus.Control;

import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.reskitow.clientbus.Model.Autobus;
import com.reskitow.clientbus.Persistencia.BDSQLite;
import com.reskitow.clientbus.R;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private EditText etMatricula;
    private EditText etContrasena;
    private BDSQLite bdsqLite;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        inicializarGestorBD();
        findViews();
        inicializarEventos();
    }

    private void inicializarEventos() {
        findViewById(R.id.btn_login).setOnClickListener(this);
    }

    private void inicializarGestorBD() {
        bdsqLite = new BDSQLite(this);
    }

    private void findViews() {
        etMatricula = (EditText) findViewById(R.id.et_login);
        etContrasena = (EditText) findViewById(R.id.et_password);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_login:
                comprobarCampos();
                break;
            default:
                mostrarToast(getString(R.string.error_pulsando_vista), Toast.LENGTH_SHORT);
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
        if (bdsqLite.getValidacionSesion(obtenerAutobusPorCampos())) {
            mostrarToast(getString(R.string.msj_inicio_correcto), Toast.LENGTH_SHORT);
        } else {
            mostrarToast(getString(R.string.msj_inicio_incorrecto), Toast.LENGTH_SHORT);
        }
        bdsqLite.close();
    }

    private Autobus obtenerAutobusPorCampos() {
        return new Autobus(etMatricula.getText().toString(), etContrasena.getText().toString());
    }

    private void mostrarToast(String mensaje, int duracion) {
        Toast.makeText(this, mensaje, duracion).show();
    }
}
