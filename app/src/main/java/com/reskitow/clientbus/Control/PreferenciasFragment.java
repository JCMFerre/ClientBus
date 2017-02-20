package com.reskitow.clientbus.Control;

import android.os.Bundle;
import android.preference.PreferenceFragment;

import com.reskitow.clientbus.R;

/**
 * Fragment que gestiona las preferencias editadas por el usuario, utiliza el xml preferencias.
 */
public class PreferenciasFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferencias);
    }
}
