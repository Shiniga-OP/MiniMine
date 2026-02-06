package com.microinterface;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;

public class GerenciadorUI {
	// sistema de camadas , TreeMap ordena automaticamente por chave(numero da camada)
	public TreeMap<Integer, ArrayList<Componente>> camadas = new TreeMap<Integer, ArrayList<Componente>>();
	public ArrayList<CaixaDialogo> dialogos = new ArrayList<CaixaDialogo>();
	public CampoTexto campoEmFoco = null;
	public Componente componenteCapturado = null;
	// camadas padrão
	public static final int CAMADA_FUNDO = 0;
	public static final int CAMADA_PADRAO = 10;
	public static final int CAMADA_UI = 20;
	public static final int CAMADA_TOPO = 30;

	public GerenciadorUI() {
		// inicializa camadas padrão
		camadas.put(CAMADA_FUNDO, new ArrayList<Componente>());
		camadas.put(CAMADA_PADRAO, new ArrayList<Componente>());
		camadas.put(CAMADA_UI, new ArrayList<Componente>());
		camadas.put(CAMADA_TOPO, new ArrayList<Componente>());
	}

	// adiciona na camada padrão
	public void add(Componente componente) {
		addCamada(componente, CAMADA_PADRAO);
	}

	// adiciona em camada específica
	public void addCamada(Componente componente, int numeroCamada) {
		// cria a camada se não existir
		if(!camadas.containsKey(numeroCamada)) {
			camadas.put(numeroCamada, new ArrayList<Componente>());
		}
		camadas.get(numeroCamada).add(componente);
		// registra o gerenciador em campos de texto
		registrarCamposTexto(componente);
	}

	public void rm(Componente componente) {
		// remove de todas as camadas
		for(ArrayList<Componente> lista : camadas.values()) {
			lista.remove(componente);
		}
	}

	public void addDialogo(CaixaDialogo dialogo) {
		dialogos.add(dialogo);
		// registra o gerenciador em campos de texto dos dialogos
		registrarCamposTexto(dialogo);
	}

	public void rmDialogo(CaixaDialogo dialogo) {
		dialogos.remove(dialogo);
	}

	// registra recursivamente o gerenciador em todos os CampoTexto
	public void registrarCamposTexto(Componente componente) {
		if(componente instanceof CampoTexto) {
			((CampoTexto)componente).gerenciador = this;
		}
		// verifica se o componente tem filhos
		if(componente instanceof Painel) {
			Painel painel = (Painel) componente;
			for(Componente filho : painel.filhos) {
				registrarCamposTexto(filho);
			}
		} else if(componente instanceof CaixaDialogo) {
			CaixaDialogo dialogo = (CaixaDialogo) componente;
			for(Componente filho : dialogo.componentes) {
				registrarCamposTexto(filho);
			}
		}
	}

	public void limpar() {
		for(ArrayList<Componente> lista : camadas.values()) {
			lista.clear();
		}
		dialogos.clear();
		campoEmFoco = null;
		componenteCapturado = null;
	}

	public void defFocoTexto(CampoTexto campo) {
		if(campoEmFoco != null && campoEmFoco != campo) {
			campoEmFoco.defFoco(false);
		}
		campoEmFoco = campo;
	}

	public boolean processarToque(float x, float y, boolean pressionado) {
		// primeiro verifica dialogos (sempre no topo)
		for(int i = dialogos.size() - 1; i >= 0; i--) {
			CaixaDialogo d = dialogos.get(i);
			if(d.ativa && d.aoTocar(x, y, pressionado)) return true;
		}

		// se ta soltando o toque(pressionado = false)
		if(!pressionado) {
			// se tem um componente capturado, envia o evento so pra ele
			if(componenteCapturado != null) {
				boolean resultado = componenteCapturado.aoTocar(x, y, false);
				componenteCapturado = null;
				return resultado;
			}
			// se não tem componente capturado, processa normalmente
		}

		// processamento normal, processa camadas de cima para baixo
		ArrayList<Integer> numsCamadas = new ArrayList<Integer>(camadas.keySet());
		for(int camadaIdc = numsCamadas.size() - 1; camadaIdc >= 0; camadaIdc--) {
			int numCamada = numsCamadas.get(camadaIdc);
			ArrayList<Componente> componentesCamada = camadas.get(numCamada);

			// dentro da camada, processa de tras pra frente
			for(int i = componentesCamada.size() - 1; i >= 0; i--) {
				Componente c = componentesCamada.get(i);
				if(c.contem(x, y)) {
					if(c.aoTocar(x, y, pressionado)) {
						// so captura se ta pressionando E o componente precisa de arraste
						if(pressionado && c.capturaArraste()) {
							componenteCapturado = c;
						}
						return true;
					}
				}
			}
		}
		return false;
	}

	public void processarArraste(float x, float y) {
		// primeiro processa dialogos
		for(int i = dialogos.size() - 1; i >= 0; i--) {
			CaixaDialogo dialogo = dialogos.get(i);
			if(dialogo.ativa) {
				dialogo.aoArrastar(x, y);
			}
		}
		// se tem um componente capturado que precisa de arraste, envia o evento
		if(componenteCapturado != null) {
			componenteCapturado.aoTocar(x, y, true);
		}
	}

	public boolean processarTecla(int c) {
		if(campoEmFoco != null) {
			return campoEmFoco.processarTecla(c);
		}
		return false;
	}

	public boolean processarCaractere(char caractere) {
		if(campoEmFoco != null) {
			return campoEmFoco.processarCaractere(caractere);
		}
		return false;
	}

	public void desenhar(SpriteBatch pincel, float delta) {
		// desenha camadas em ordem crescente (de baixo para cima)
		for(Integer numCamada : camadas.keySet()) {
			ArrayList<Componente> componentesDaCamada = camadas.get(numCamada);
			for(int i = 0; i < componentesDaCamada.size(); i++) {
				componentesDaCamada.get(i).desenhar(pincel, delta, 0, 0);
			}
		}

		// desenha diálogos sempre por cima
		for(int i = 0; i < dialogos.size(); i++) {
			CaixaDialogo dialogo = dialogos.get(i);
			if(dialogo.ativa) {
				dialogo.desenhar(pincel, delta, 0, 0);
			}
		}
	}

	public boolean temDialogoAtivo() {
		for(int i = 0; i < dialogos.size(); i++) {
			if(dialogos.get(i).ativa) {
				return true;
			}
		}
		return false;
	}

	public void liberar() {
		for(ArrayList<Componente> lista : camadas.values()) {
			for(Componente c : lista) c.liberar();
		}
		for(CaixaDialogo c : dialogos) c.liberar();
		if(componenteCapturado != null) componenteCapturado.liberar();
		if(campoEmFoco != null) campoEmFoco.liberar();
	}
}

