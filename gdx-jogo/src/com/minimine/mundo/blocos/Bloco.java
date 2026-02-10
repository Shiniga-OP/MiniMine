package com.minimine.mundo.blocos;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import com.minimine.audio.Audio;
import com.badlogic.gdx.audio.Music;

public class Bloco {
	public static List<Bloco> blocos = new ArrayList<>();
	public static HashMap<CharSequence, Bloco> texIds = new HashMap<>();
	public static HashMap<Integer, Bloco> numIds = new HashMap<>();
	public static HashMap<String, String[]> sons = new HashMap<>();

	public CharSequence nome;
	public int tipo;
	public String topo, lados, baixo;
	public int luz;
	public boolean transparente;
	public boolean solido;
	public boolean culling;

	public Bloco(CharSequence nome, String topo) {this(nome, topo, topo, topo, false, true, true, 0);}
	public Bloco(CharSequence nome, String topo, String lados) {this(nome, topo, lados, topo, false, true, true, 0);}
	public Bloco(CharSequence nome, String topo, String lados, String baixo) {this(nome,topo, lados, baixo, false, true, true, 0);}
	public Bloco(CharSequence nome, String topo, boolean transparente) {this(nome, topo, topo, topo, transparente, true, true, 0);}
	public Bloco(CharSequence nome, String topo, boolean transparente, boolean solido) {this(nome, topo, topo, topo, transparente, solido, true, 0);}
	public Bloco(CharSequence nome, String topo, String lados, String baixo, boolean transparente) {this(nome, topo, lados, baixo, transparente, true, true, 0);}
	public Bloco(CharSequence nome, String topo, String lados, String baixo, boolean transparente, boolean solido) {this(nome, topo, lados, baixo, transparente, solido, true, 0);}
	public Bloco(CharSequence nome, String topo, boolean transparente, boolean solido, boolean culling) {this(nome, topo, topo, topo, transparente, solido, culling, 0);}

	public Bloco(CharSequence nome, String topo, String lados, String baixo, boolean transparente, boolean solido, boolean culling, int luz) {
		this.nome = nome;
		this.tipo = blocos.size();
		this.topo = topo; this.lados = lados; this.baixo = baixo;
		this.transparente = transparente;
		this.solido = solido;
		this.culling = culling;
		this.luz = luz;
		numIds.put(this.tipo, this);
		texIds.put(this.nome, this);
	}

	public Bloco() {}

	public String texturaId(int faceId) {
        switch(faceId) {
            case 0: return topo;
            case 1: return baixo;
            default: return lados;
        }
    }

	public static void addSom(String bloco, String... sonoros) {
		sons.put(bloco, sonoros);
	}

	public static void tocarSom(Object bloco) {
		if(sons.containsKey(bloco)) {
			String[] sonoros = sons.get(bloco);
			for(int i = 0; i < sonoros.length; i++) {
				if(Math.random() > 0.6) {
					Music m = Audio.sons.get(sonoros[i]);
					m.play();
					return;
				}
			}
			Music m = Audio.sons.get(sonoros[0]);
			m.play();
		} else {
			tocarSom("pedra");
		}
	}
}

