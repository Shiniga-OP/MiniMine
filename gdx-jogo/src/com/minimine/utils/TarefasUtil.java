package com.minimine.utils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Executors;
import com.minimine.cenas.Jogo;
import java.util.concurrent.ThreadFactory;

public class TarefasUtil {
    public static ExecutorService exec;
	public static ThreadFactory fabrica = new ThreadFactory() {
		@Override
		public Thread newThread(Runnable r) {
			final Thread t = new Thread(r);
			t.setDaemon(true);
			return t;
		}
	};
	public static int modo = 0;

    public static void iniciar() {
		if(modo == 0) exec = Executors.newFixedThreadPool(1, fabrica);
		else if(modo == 1) exec = new ForkJoinPool(1);
    }

    public static void add(Runnable tarefa) {
        if(modo == 2) Jogo.servidor.addTarefa(tarefa);
		else exec.execute(tarefa);
    }

    public static void liberar() {
		if(modo != 2) exec.shutdown();
    }
}
