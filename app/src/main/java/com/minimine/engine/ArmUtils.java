package com.minimine.engine;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.FileReader;

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
}
