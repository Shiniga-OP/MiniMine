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
	public boolean solido, culling, modeloX;

	public Bloco(CharSequence nome, String topo) {this(nome, topo, topo);}
	public Bloco(CharSequence nome, String topo, String lados) {this(nome, topo, lados, topo);}
	public Bloco(CharSequence nome, String topo, String lados, String baixo) {this(nome,topo, lados, baixo, false);}
	public Bloco(CharSequence nome, String topo, boolean transparente, boolean solido, boolean culling, int luz) {this(nome, topo, topo, topo, transparente, solido, culling, luz, false);}
	public Bloco(CharSequence nome, String topo, boolean transparente) {this(nome, topo, topo, topo, transparente);}
	public Bloco(CharSequence nome, String topo, boolean transparente, boolean solido) {this(nome, topo, topo, topo, transparente, solido);}
	public Bloco(CharSequence nome, String topo, String lados, String baixo, boolean transparente) {this(nome, topo, lados, baixo, transparente, true);}
	public Bloco(CharSequence nome, String topo, String lados, String baixo, boolean transparente, boolean solido) {this(nome, topo, lados, baixo, transparente, solido, true, 0, false);}
	public Bloco(CharSequence nome, String topo, boolean transparente, boolean solido, boolean culling) {this(nome, topo, topo, topo, transparente, solido, culling, 0, false);}
	public Bloco(CharSequence nome, String topo, boolean transparente, boolean solido, boolean culling, int luz, boolean formaX) {this(nome, topo, topo, topo, transparente, solido, culling, luz, formaX);}
	
	public Bloco(CharSequence nome, String topo, String lados, String baixo, boolean transparente, boolean solido, boolean culling, int luz, boolean formaX) {
		this.nome = nome;
		this.tipo = blocos.size();
		this.topo = topo; this.lados = lados; this.baixo = baixo;
		this.transparente = transparente;
		this.solido = solido;
		this.culling = culling;
		this.luz = luz;
		this.modeloX = formaX;
		numIds.put(this.tipo, this);
		texIds.put(this.nome, this);
	}

	public Bloco() {}
	
	public static void iniciar() {
		Bloco.blocos.add(null);
        Bloco.blocos.add(new Bloco("grama", "grama_topo", "grama_lado", "terra"));
        Bloco.blocos.add(new Bloco("terra", "terra"));
        Bloco.blocos.add(new Bloco("pedra", "pedra"));
        Bloco.blocos.add(new Bloco("agua", "agua", true, false, false));
        Bloco.blocos.add(new Bloco("areia", "areia"));
        Bloco.blocos.add(new Bloco("tronco", "tronco_topo", "tronco_lado"));
        Bloco.blocos.add(new Bloco("folha", "folha", true, true, false));
        Bloco.blocos.add(new Bloco("tabua_madeira", "tabua_madeira"));
        Bloco.blocos.add(new Bloco("cacto", "cacto_topo", "cacto_lado"));
        Bloco.blocos.add(new Bloco("vidro", "vidro", true, true, false));
        Bloco.blocos.add(new Bloco("tocha", "tocha", false, true, true, 13));
		Bloco.blocos.add(new Bloco("pedregulho", "pedregulho"));
		Bloco.blocos.add(new Bloco("cascalho", "cascalho"));
		Bloco.blocos.add(new Bloco("gelo", "gelo"));
		Bloco.blocos.add(new Bloco("neve", "neve"));
		Bloco.blocos.add(new Bloco("coral_rosa", "coral_rosa"));
		Bloco.blocos.add(new Bloco("coral_azul", "coral_azul"));
		Bloco.blocos.add(new Bloco("coral_amarelo", "coral_amarelo"));
		Bloco.blocos.add(new Bloco("capim", "capim", true, false, false, 0, true));
		Bloco.blocos.add(new Bloco("tulipa", "tulipa", true, false, false, 3, true));
		Bloco.blocos.add(new Bloco("iris_azul", "iris_azul", true, false, false, 1, true));

		Bloco.addSom("grama", "grama_1", "terra_1", "terra_2", "terra_3");
		Bloco.addSom("terra", "terra_1", "terra_2", "terra_3");
		Bloco.addSom("areia", "terra_1", "terra_2", "terra_3");
		Bloco.addSom("cascalho", "terra_1", "terra_2", "terra_3");
		Bloco.addSom("pedra", "pedra_1", "pedra_2");
		Bloco.addSom("folha", "terra_1", "terra_2", "terra_3");
		Bloco.addSom("tabua_madeira", "madeira_1", "madeira_2", "madeira_3");
		Bloco.addSom("tocha", "madeira_1", "madeira_2", "madeira_3");
	}

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
	
	public static void liberar() {
		Bloco.blocos.clear();
		Bloco.numIds.clear();
		Bloco.texIds.clear();
		Bloco.sons.clear();
	}
}

