package com.minimine.mundo.blocos;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import com.minimine.audio.Audio;
import com.minimine.graficos.TipoRender;
import com.minimine.inventario.ItemRegistro;
import com.minimine.graficos.Texturas;
import com.badlogic.gdx.audio.Sound;
import com.micro.janelas.PainelFatiado;

public class Bloco {
	public static List<Bloco> blocos = new ArrayList<>();
	public static HashMap<String, Bloco> texIds = new HashMap<>();
	public static HashMap<Integer, Bloco> numIds = new HashMap<>();
	public static HashMap<String, String[]> sons = new HashMap<>();
	public static int AGUA;

	public final String nome;
	public final int tipo;
	public final String topo, lados, baixo;
	public final int luz;
	public final TipoRender render;
	public boolean solido, culling, modeloX, colisao = true;
	public int durabilidade = 3; // ticks para quebrar (0 = instantaneo)
	public static boolean ABERTO = false;
	/*
	 * interface de UI associada a este bloco
	 * null = bloco sem interface(comportamento padrão: colocar/quebrar)
	 * atribuida em Bloco.iniciar() para os blocos que precisarem
	 */
	public InterfaceBloco ui = null;
	public EventoBloco evento = null;

	public Bloco(String nome, String topo) {this(nome, topo, topo);}
	public Bloco(String nome, String topo, String lados) {this(nome, topo, lados, topo);}
	public Bloco(String nome, String topo, String lados, String baixo) {this(nome, topo, lados, baixo, TipoRender.OPACO, true, true, 0, false);}
	public Bloco(String nome, String topo, TipoRender render) {this(nome, topo, topo, topo, render, true, true, 0, false);}
	public Bloco(String nome, String topo, TipoRender render, boolean solido) {this(nome, topo, topo, topo, render, solido, true, 0, false);}
	public Bloco(String nome, String topo, TipoRender render, boolean solido, boolean culling) {this(nome, topo, topo, topo, render, solido, culling, 0, false);}
	public Bloco(String nome, String topo, TipoRender render, boolean solido, boolean culling, int luz) {this(nome, topo, topo, topo, render, solido, culling, luz, false);}
	public Bloco(String nome, String topo, TipoRender render, boolean solido, boolean culling, int luz, boolean formaX) {this(nome, topo, topo, topo, render, solido, culling, luz, formaX);}

	public Bloco(String nome, String topo, String lados, String baixo, TipoRender render, boolean solido, boolean culling, int luz, boolean formaX) {
		this.nome = nome;
		this.tipo = blocos.size();
		this.topo = topo; this.lados = lados; this.baixo = baixo;
		this.render = render;
		this.solido = solido;
		this.culling = culling;
		this.luz = luz;
		this.modeloX = formaX;
		numIds.put(this.tipo, this);
		texIds.put(this.nome, this);
		ItemRegistro.registrar(this.nome, this.lados);
	}

	public static void iniciar() {
		Bloco.add(null);
        Bloco.add(new Bloco("grama", "grama_topo", "grama_lado", "terra"));
        Bloco.add(new Bloco("terra", "terra"));
        Bloco.add(new Bloco("pedra", "pedra"));
        Bloco.add(new Bloco("agua", "agua", TipoRender.LIQUIDO, false, false)).solido = false;
        Bloco.add(new Bloco("areia", "areia"));
        Bloco.add(new Bloco("tronco", "tronco_topo", "tronco_lado"));
        Bloco.add(new Bloco("folha", "folha", TipoRender.RECORTE));
        Bloco.add(new Bloco("tabua_madeira", "tabua_madeira"));
        Bloco.add(new Bloco("cacto", "cacto_topo", "cacto_lado"));
        Bloco.add(new Bloco("vidro", "vidro", TipoRender.TRANSLUCIDO, true, false));
        Bloco.add(new Bloco("tocha", "tocha", TipoRender.RECORTE, true, true, 13));
		Bloco.add(new Bloco("pedregulho", "pedregulho"));
		Bloco.add(new Bloco("cascalho", "cascalho"));
		Bloco.add(new Bloco("gelo", "gelo"));
		Bloco.add(new Bloco("neve", "neve"));
		Bloco.add(new Bloco("coral_rosa", "coral_rosa"));
		Bloco.add(new Bloco("coral_azul", "coral_azul"));
		Bloco.add(new Bloco("coral_amarelo", "coral_amarelo"));
		Bloco.add(new Bloco("capim", "capim", TipoRender.RECORTE, false, false, 0, true)).colisao = false;
		Bloco.add(new Bloco("tulipa", "tulipa", TipoRender.RECORTE, false, false, 0, true)).colisao = false;
		Bloco.add(new Bloco("tulipa_luminosa", "tulipa", TipoRender.RECORTE, false, false, 5, true)).colisao = false;
		Bloco.add(new Bloco("iris_azul", "iris_azul", TipoRender.RECORTE, false, false, 1, true)).colisao = false;
		Bloco.add(new Bloco("arenito", "arenito"));
		Bloco.add(new Bloco("pilar_arenito", "pilar_arenito_topo", "pilar_arenito_lado"));
		Bloco.add(new Bloco("bloco_nulo", "nulo", TipoRender.AR, false, false)).colisao = false;
		Bloco.add(new Bloco("bloco_estrutura", "bloco_estrutura"));

		// durabilidade: segundos para quebrar
		texIds.get("grama").durabilidade = 2;
		texIds.get("terra").durabilidade = 2;
		texIds.get("pedra").durabilidade = 7;
		texIds.get("agua").durabilidade = 0;
		texIds.get("areia").durabilidade = 2;
		texIds.get("tronco").durabilidade = 5;
		texIds.get("folha").durabilidade = 1;
		texIds.get("tabua_madeira").durabilidade = 4;
		texIds.get("cacto").durabilidade = 1;
		texIds.get("vidro").durabilidade = 1;
		texIds.get("tocha").durabilidade = 1;
		texIds.get("pedregulho").durabilidade = 6;
		texIds.get("cascalho").durabilidade = 2;
		texIds.get("gelo").durabilidade = 2;
		texIds.get("neve").durabilidade = 1;
		texIds.get("coral_rosa").durabilidade = 2;
		texIds.get("coral_azul").durabilidade = 2;
		texIds.get("coral_amarelo").durabilidade = 2;
		texIds.get("capim").durabilidade = 0;
		texIds.get("tulipa").durabilidade = 0;
		texIds.get("tulipa_luminosa").durabilidade = 0;
		texIds.get("iris_azul").durabilidade = 0;
		texIds.get("arenito").durabilidade = 7;
		texIds.get("pilar_arenito").durabilidade = 4;
		texIds.get("bloco_nulo").durabilidade = 0;
		texIds.get("bloco_estrutura").durabilidade = 0;

		Bloco.addSom("grama", "grama_1", "terra_1", "terra_2", "terra_3");
		Bloco.addSom("terra", "terra_1", "terra_2", "terra_3");
		Bloco.addSom("areia", "terra_1", "terra_2", "terra_3");
		Bloco.addSom("cascalho", "terra_1", "terra_2", "terra_3");
		Bloco.addSom("pedra", "pedra_1", "pedra_2");
		Bloco.addSom("folha", "terra_1", "terra_2", "terra_3");
		Bloco.addSom("tabua_madeira", "madeira_1", "madeira_2", "madeira_3");
		Bloco.addSom("tocha", "madeira_1", "madeira_2", "madeira_3");

		AGUA = texIds.get("agua").tipo;
	}
	/*
	 * chamado dentro do construtor de UI, apos visualBase e fonte estarem prontos
	 * cria as instâncias de InterfaceBloco e as injeta nos blocos correspondentes
	 */
	public static void iniciarInterfaces(final PainelFatiado base, final com.badlogic.gdx.graphics.g2d.BitmapFont fonte) {
		// bloco_estrutura
		BlocoEstrutura.iniciar(texIds.get("bloco_estrutura"), base, fonte);
		BlocoEstrutura.iniciarEventos(texIds.get("bloco_estrutura"));
	}

	public static void addSom(String bloco, String... sonoros) {
		sons.put(bloco, sonoros);
	}

	public static Bloco add(Bloco b) {
		blocos.add(b);
		return b;
	}

	public static void tocarSom(Object bloco) {
		if(sons.containsKey(bloco)) {
			String[] sonoros = sons.get(bloco);
			for(int i = 0; i < sonoros.length; i++) {
				if(Math.random() > 0.6) {
					Sound m = Audio.sons.get(sonoros[i]);
					m.play();
					return;
				}
			}
			Audio.sons.get(sonoros[0]).play();
		} else {
			tocarSom("pedra");
		}
	}

	public static void liberar() {
		// libera interfaces antes de limpar os mapas
		for(Bloco b : blocos) {
			if(b != null && b.ui != null) {
				b.ui.liberar();
				b.ui = null;
			}
		}
		Bloco.blocos.clear();
		Bloco.numIds.clear();
		Bloco.texIds.clear();
		Bloco.sons.clear();
	}

	public final String texturaId(int faceId) {
        switch(faceId) {
            case 0: return topo;
            case 1: return baixo;
            default: return lados;
        }
    }
}
