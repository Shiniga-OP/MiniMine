package com.minimine.engine;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.FileOutputStream;
import java.io.FileDescriptor;

public class Logs {
    private static ByteArrayOutputStream saida = null;
    private static boolean ativo = false;

    public static void capturar() {
        if(ativo) return;

        saida = new ByteArrayOutputStream();
        PrintStream console = new PrintStream(saida);
        System.setOut(console);
        System.setErr(console);
        ativo = true;
    }

    public static String exibir() {
        if(!ativo || saida == null) return "Logs.capturar() não foi chamado.";
        return saida.toString();
    }

    public static void parar() {
        if(!ativo) return;
        System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out)));
        System.setErr(new PrintStream(new FileOutputStream(FileDescriptor.err)));
        ativo = false;
    }
}
