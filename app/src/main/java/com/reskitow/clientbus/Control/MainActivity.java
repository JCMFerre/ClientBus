package com.reskitow.clientbus.Control;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
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
                iniciarSesion();
                break;
            default:
                Toast.makeText(this, getString(R.string.error_pulsando_vista), Toast.LENGTH_SHORT).show();
        }
    }

    private void iniciarSesion() {
        if (bdsqLite.getValidacionSesion(obtenerAutobusPorCampos())) {
            // TODO INICIO DE SESSIÓN CORRECTO.
        } else {
            // TODO INICIO DE SESSIÓN INCORRECTO.
        }
    }

    private Autobus obtenerAutobusPorCampos() {
        return new Autobus(etMatricula.getText().toString(), etContrasena.getText().toString());
    }
}
