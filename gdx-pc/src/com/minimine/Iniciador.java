package com.minimine;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.badlogic.gdx.Gdx;

public class Iniciador {
    public static void main (String[] arg) {
        LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
        config.title = "MiniMine";
        config.addIcon("minimine.png", com.badlogic.gdx.Files.FileType.Internal);
        config.width = 1280;
        config.height = 720;

        Debugador debug = new Debugador() {
            @Override public boolean ehArm64() { return false; }
            @Override public long obterHeapLivre() { return Runtime.getRuntime().freeMemory(); }
            @Override public long obterHeapTotal() { return Runtime.getRuntime().totalMemory(); }
        };

        Instalador instalador = new Instalador() {
            public void instalar(String caminho) {}
        };
        Gdx.files = new com.badlogic.gdx.backends.lwjgl.LwjglFiles();

        try {
            new LwjglApplication(new Inicio(Gdx.files.getExternalStoragePath(), debug, instalador), config);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
