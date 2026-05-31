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
	public final Textura9 textura;
	public final int luz;
	public final TipoRender render;
	public boolean colisao = true;
	public BlocoModelo modelo;
	public float dureza = 3f; // ticks para quebrar (0 = instantaneo)
	public int viscosidade = 1;  // ticks entre propagações
	public float densidade = 1f; // densidade do fluido
	public static boolean ABERTO = false;
	/*
	 * interface de UI associada a este bloco
	 * null = bloco sem interface(comportamento padrão: colocar/quebrar)
	 * atribuida em iniciar() para os blocos que precisarem
	*/
	public InterfaceBloco ui = null;
	public EventoBloco evento = null;

	public Bloco(String nome, Textura9 textura, Propriedade pro) {
		this.nome = nome;
		this.tipo = blocos.size();
		this.textura = textura;
		this.render = pro.render;
		this.colisao = pro.colide;
		this.luz = pro.luz;
		this.modelo = pro.modelo;
		this.dureza = pro.dureza;
		numIds.put(this.tipo, this);
		texIds.put(this.nome, this);
		ItemRegistro.registrar(this.nome, textura.sul);
	}

	public static void iniciar() {
		add(null);
        add(new Bloco("grama",
					  new Textura9().topo("grama_topo")
					  .lados("grama_lado").base("terra"),
					  new Propriedade().dureza(2)
					  ));
        add(new Bloco(
				"terra", new Textura9().def("terra"),
				new Propriedade().dureza(2)
			));
        add(new Bloco(
				"pedra", new Textura9().def("pedra"),
				new Propriedade().dureza(7)
			));
        add(new Bloco(
				"agua", new Textura9().def("agua"),
				new Propriedade().render(TipoRender.LIQUIDO)
				.colisao(false)
			));
        add(new Bloco(
				"areia", new Textura9().def("areia"),
				new Propriedade().dureza(2)
			));
        add(new Bloco(
				"tronco",
				new Textura9().topo("tronco_topo").lados("tronco_lado"),
				new Propriedade().dureza(5)
			));
        add(new Bloco(
				"folha", new Textura9().def("folha"),
				new Propriedade().render(TipoRender.RECORTE).dureza(1.5f)
			));
        add(new Bloco(
				"tabua_madeira", new Textura9().def("tabua_madeira"),
				new Propriedade().dureza(4)
			));
        add(new Bloco(
				"cacto",
				new Textura9().topo("cacto_topo").lados("cacto_lado"),
				new Propriedade().dureza(1)
			));
        add(new Bloco(
				"vidro", new Textura9().def("vidro"),
				new Propriedade().render(TipoRender.TRANSLUCIDO).dureza(2)
			));
        add(new Bloco(
				"tocha", new Textura9().def("tocha"),
				new Propriedade().render(TipoRender.RECORTE).emiteLuz(13)
			));
		add(new Bloco(
				"pedregulho", new Textura9().def("pedregulho"),
				new Propriedade().dureza(6)
			));
		add(new Bloco(
				"cascalho", new Textura9().def("cascalho"),
				new Propriedade().dureza(2)
			));
		add(new Bloco(
				"gelo", new Textura9().def("gelo"),
				new Propriedade().dureza(2)
			));
		add(new Bloco(
				"neve", new Textura9().def("neve"),
				new Propriedade().dureza(1)
			));
		add(new Bloco(
				"coral_rosa", new Textura9().def("coral_rosa"),
				new Propriedade().dureza(1.5f)
			));
		add(new Bloco(
				"coral_azul", new Textura9().def("coral_azul"),
				new Propriedade().dureza(1.5f)
			));
		add(new Bloco(
				"coral_amarelo", new Textura9().def("coral_amarelo"),
				new Propriedade().dureza(1.5f)
			));
		add(new Bloco(
				"capim", new Textura9().def("capim"),
				new Propriedade().render(TipoRender.RECORTE)
				.modeloX().colisao(false)
			));
		add(new Bloco(
				"tulipa", new Textura9().def("tulipa"),
				new Propriedade().render(TipoRender.RECORTE)
				.modeloX().colisao(false)
			));
		add(new Bloco(
				"tulipa_luminosa", new Textura9().def("tulipa"),
				new Propriedade().render(TipoRender.RECORTE)
				.modeloX().colisao(false).emiteLuz(5)
			));
		add(new Bloco(
				"iris_azul", new Textura9().def("iris_azul"),
				new Propriedade().render(TipoRender.RECORTE)
				.modeloX().colisao(false).emiteLuz(1)
			));
		add(new Bloco(
				"arenito", new Textura9().def("arenito"),
				new Propriedade().dureza(7)
			));
		add(new Bloco(
				"pilar_arenito",
				new Textura9().topo("pilar_arenito_topo").lados("pilar_arenito_lado"),
				new Propriedade().dureza(7)
			));
		add(new Bloco(
				"bau",
				new Textura9().topo("tabua_madeira").lados("bau_lado")
				.frente("bau_frente"),
				new Propriedade().dureza(3)
			));
		add(new Bloco(
				"bloco_nulo", new Textura9().def("nulo"),
				new Propriedade().render(TipoRender.AR).colisao(false)
			));
		add(new Bloco(
				"bloco_estrutura", new Textura9().def("bloco_estrutura"),
				new Propriedade()
			));
		addSom("grama", "grama_1", "terra_1", "terra_2", "terra_3");
		addSom("terra", "terra_1", "terra_2", "terra_3");
		addSom("areia", "terra_1", "terra_2", "terra_3");
		addSom("cascalho", "terra_1", "terra_2", "terra_3");
		addSom("pedra", "pedra_1", "pedra_2");
		addSom("folha", "terra_1", "terra_2", "terra_3");
		addSom("tabua_madeira", "madeira_1", "madeira_2", "madeira_3");
		addSom("tocha", "madeira_1", "madeira_2", "madeira_3");

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
		// bau
		BlocoBau.iniciar(texIds.get("bau"), fonte);
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
		blocos.clear();
		numIds.clear();
		texIds.clear();
		sons.clear();
	}

	public final String texturaId(int faceId) {
        switch(faceId) {
            case 0: return textura.topo;
            case 1: return textura.base;
			case 2: return textura.leste;
			case 3: return textura.oeste;
			case 4: return textura.sul;
            default: return textura.norte;
        }
    }
}
