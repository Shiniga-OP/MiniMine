package com.minimine.engine;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class Logs {
    public ByteArrayOutputStream saida;
    public boolean ativo = true;

    public void capturar() {
        saida = new ByteArrayOutputStream();
        PrintStream console = new PrintStream(saida);

        System.setOut(console);
        System.setErr(console);
    }

    public String exibir() {
		return saida == null ? "você não chamou Logs.capturar" : saida.toString();
    }
}
