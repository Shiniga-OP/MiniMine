package com.minimine.utils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Executors;
import com.minimine.cenas.Jogo;
import java.util.concurrent.ThreadFactory;

public class TarefasUtil {
    public static ExecutorService mundo, entidades;
	public static ThreadFactory fabrica = new ThreadFactory() {
		@Override
		public Thread newThread(Runnable r) {
			final Thread t = new Thread(r);
			t.setDaemon(true);
			return t;
		}
	};
	
    public static void iniciar() {
		mundo = Executors.newFixedThreadPool(1, fabrica);
		entidades = Executors.newFixedThreadPool(1, fabrica);
    }

    public static void liberar() {
		mundo.shutdown();
		entidades.shutdown();
    }
}
