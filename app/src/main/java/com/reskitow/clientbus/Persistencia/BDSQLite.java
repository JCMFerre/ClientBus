package com.reskitow.clientbus.Persistencia;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.widget.Toast;

import com.reskitow.clientbus.Model.Autobus;
import com.reskitow.clientbus.R;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;

public class BDSQLite extends SQLiteOpenHelper {

    // Nombre de la BD.
    private static final String DB_AUTOBUSES = "DB_AUTOBUSES";

    // Tabla donde se guardarán las matriculas y contraseñas para acceder.
    private static final String TABLA_USUARIOS = "USUARIOS_AUTOBUSES";

    // Campos de la tabla USUARIOS_AUTOBUSES.
    private static final String KEY_MATRICULA_USUARIOS = "MATRICULA";
    private static final String KEY_CONTRASENA_USUARIOS = "CONTRASENA";

    private Context context;

    public BDSQLite(Context context) {
        super(context, DB_AUTOBUSES, null, 1);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        String[] querys = {"CREATE TABLE " + TABLA_USUARIOS + " (" + KEY_MATRICULA_USUARIOS
                + " TEXT PRIMARY KEY, " + KEY_CONTRASENA_USUARIOS + " TEXT NOT NULL)", "INSERT INTO "
                + TABLA_USUARIOS + " VALUES ('1111BUS', 'CONTRASENA1'), ('0002BUS', 'bus2')"};
        for (String query : querys) {
            sqLiteDatabase.execSQL(query);
        }
        sqLiteDatabase.close();
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        String query = "DROP TABLE IF EXIST " + TABLA_USUARIOS;
        sqLiteDatabase.execSQL(query);
    }

    public boolean anadirAutobus(Autobus autobus) {
        SQLiteDatabase db = this.getWritableDatabase();
        long resultado = db.insert(TABLA_USUARIOS, null, crearContentValuesAutobus(autobus));
        cerrarRecursos(new Closeable[]{db});
        // Si ocurre algun error devuelve -1.
        return resultado != -1;
    }

    public Autobus[] getTotsElsAutobus() {
        ArrayList<Autobus> autobusos = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLA_USUARIOS, null);
        if (cursor.moveToNext()) {
            do {
                autobusos.add(obtenerAutobusCursor(cursor));
            } while (cursor.moveToNext());
        }
        return (Autobus[]) autobusos.toArray();
    }

    private Autobus obtenerAutobusCursor(Cursor cursor) {
        return new Autobus(cursor.getString(cursor.getColumnIndex(KEY_MATRICULA_USUARIOS)),
                cursor.getString(cursor.getColumnIndex(KEY_CONTRASENA_USUARIOS)));
    }

    private ContentValues crearContentValuesAutobus(Autobus autobus) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(KEY_MATRICULA_USUARIOS, autobus.getMatricula());
        contentValues.put(KEY_CONTRASENA_USUARIOS, autobus.getContrasena());
        return contentValues;
    }

    public boolean getValidacionSesion(Autobus autobus) {
        String query = "SELECT * FROM " + TABLA_USUARIOS + " WHERE " + KEY_MATRICULA_USUARIOS
                + " = ? AND " + KEY_CONTRASENA_USUARIOS + " = ?";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, new String[]{autobus.getMatricula(), autobus.getContrasena()});
        int numColumnas = cursor.getCount();
        cerrarRecursos(new Closeable[]{cursor, db});
        return numColumnas > 0;
    }

    private void cerrarRecursos(Closeable[] closeables) {
        for (Closeable closeable : closeables) {
            try {
                closeable.close();
            } catch (IOException e) {
                Toast.makeText(context, context.getString(R.string.error_cerrar_recurso), Toast.LENGTH_LONG).show();
                break;
            }
        }
    }

}
