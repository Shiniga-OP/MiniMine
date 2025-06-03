package com.minimine.engine;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.FileReader;
import android.os.Environment;
import java.io.FileOutputStream;

public class ArmUtils {
	public static String lerArquivo(File arquivo) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(arquivo));
		StringBuilder sb = new StringBuilder();
		String linha;
		while((linha = br.readLine()) != null) {
			sb.append(linha);
		}
		br.close();
		return sb.toString();
	}
	
	public static File criarArquivo(String caminho, String nome) {
		File pasta = new File(caminho);
		if(!pasta.exists()) pasta.mkdirs();

		return new File(pasta, nome);
	}
}
