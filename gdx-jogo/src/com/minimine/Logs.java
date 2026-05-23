package com.minimine;

import com.badlogic.gdx.ApplicationLogger;
import com.minimine.utils.ArquivosUtil;

public class Logs implements ApplicationLogger {
	public static String logs = "";
	
	

	@Override
	public void debug(String string, String string1) {
		logs += string + ": " + string1 + "\n";
		System.out.println(string + ": " + string1 + "\n");
		ArquivosUtil.escrever(Inicio.externo+"/MiniMine/debug/logs.txt", logs);
	}

	@Override
	public void debug(String string, String string1, Throwable throwable) {
		logs += string + ": " + string1 + throwable.getMessage() + "\n";
		System.out.println(string + ": " + string1 + throwable.getMessage() + "\n");
		ArquivosUtil.escrever(Inicio.externo+"/MiniMine/debug/logs.txt", logs);
	}

	@Override
	public void error(String string, String string1) {
		logs += string + ": " + string1 + "\n";
		System.err.println(string + ": " + string1 + "\n");
		ArquivosUtil.escrever(Inicio.externo+"/MiniMine/debug/logs.txt", logs);
	}

	@Override
	public void error(String string, String string1, Throwable throwable) {
		logs += string + ": " + string1 + throwable.getMessage() + "\n";
		System.err.println(string + ": " + string1 + throwable.getMessage() + "\n");
		ArquivosUtil.escrever(Inicio.externo+"/MiniMine/debug/logs.txt", logs);
	}

	public static void log(Object msg) {
		logs += msg.toString() + "\n";
		System.out.println(msg.toString() + "\n");
		ArquivosUtil.escrever(Inicio.externo+"/MiniMine/debug/logs.txt", logs);
	}

	@Override
	public void log(String string, String string1) {
		logs += string + ": " + string1 + "\n";
		System.out.println(string + ": " + string1 + "\n");
		ArquivosUtil.escrever(Inicio.externo+"/MiniMine/debug/logs.txt", logs);
	}

	@Override
	public void log(String string, String string1, Throwable throwable) {
		logs += string + ": " + string1 + throwable.getMessage() + "\n";
		System.out.println(string + ": " + string1 + throwable.getMessage() + "\n");
		ArquivosUtil.escrever(Inicio.externo+"/MiniMine/debug/logs.txt", logs);
	}
}
