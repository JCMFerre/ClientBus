package com.reskitow.clientbus.Model;

/**
 * Created by ALUMNEDAM on 03/02/2017.
 */

public class Autobus {

    private String matricula;
    private String contrasena;

    public Autobus(String matricula, String contrasena) {
        this.matricula = matricula;
        this.contrasena = contrasena;
    }

    public String getMatricula() {
        return matricula;
    }

    public void setMatricula(String matricula) {
        this.matricula = matricula;
    }

    public String getContrasena() {
        return contrasena;
    }

    public void setContrasena(String contrasena) {
        this.contrasena = contrasena;
    }

    @Override
    public String toString() {
        return "Autobus{" +
                "matricula='" + matricula + '\'' +
                ", contrasena='" + contrasena + '\'' +
                '}';
    }
}
