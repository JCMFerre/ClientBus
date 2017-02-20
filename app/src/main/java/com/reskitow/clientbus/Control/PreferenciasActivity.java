package com.reskitow.clientbus.Control;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

/**
 * Activity que gestiona el fragment de preferencias.
 */
public class PreferenciasActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new PreferenciasFragment())
                .commit();
    }
}
