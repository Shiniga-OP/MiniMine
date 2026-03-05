package com.minimine.mods;

import com.badlogic.gdx.Gdx;
import com.minimine.Inicio;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;
import com.minimine.cenas.Jogo;
import org.luaj.vm2.lib.jse.JsePlatform;
import org.luaj.vm2.LuaFunction;
import java.io.File;
import java.io.IOException;
import com.minimine.utils.ArquivosUtil;
import com.minimine.mundo.Biomas;
import com.minimine.mundo.ChunkUtil;
import com.minimine.graficos.Texturas;
import com.minimine.utils.NuvensUtil;
import com.minimine.utils.DiaNoiteUtil;
import com.minimine.Logs;
import com.minimine.audio.Audio;
import com.minimine.Cenas;
import com.minimine.mundo.blocos.Bloco;
import com.minimine.graficos.Render;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.minimine.graficos.Modelos;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.minimine.ui.InterUtil;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.minimine.entidades.Jogador;

public class LuaAPI {
	public static Globals globais;
	public static String att;
	public static LuaFunction aoAjustar, ajuste, porFrame, frame;
	public static int v, h;
	public static float delta;
	public static String pacote;
	public static boolean existeAtt = false;
	public static Jogador jogador;
	
	public static void iniciar() {
		pacote = Inicio.externo+"/MiniMine/mods/";
		
		jogador = Jogo.render.ui.jg;
		
		String script = "";
		
		globais = JsePlatform.standardGlobals();
		
		globais.set("api", CoerceJavaToLua.coerce(new LuaAPI()));
		globais.set("biomas", CoerceJavaToLua.coerce(new Biomas()));
		globais.set("jogador", CoerceJavaToLua.coerce(jogador));
		
		aoAjustar = new LuaFunction() {
			public LuaValue call(LuaValue arg) {
				arg.call(LuaValue.valueOf(v), LuaValue.valueOf(h));
				return LuaValue.NIL;
			}
		};
		porFrame = new LuaFunction() {
			public LuaValue call(LuaValue arg) {
				arg.call(LuaValue.valueOf(delta));
				frame = (LuaFunction) arg;
				return LuaValue.NIL;
			}
		};
		globais.set("log", new LuaFunction() {
				@Override
				public LuaValue call(LuaValue arg) {
					Logs.logs += arg.tojstring() + "\n";
					return LuaValue.NIL;
				}
			});
		globais.set("rescripts", new LuaFunction() {
				@Override
				public LuaValue call() {
					ajuste = null;
					att = "";
					iniciar();
					return LuaValue.NIL;
				}
			});
		globais.set("aoAjustar", aoAjustar);
		globais.set("porFrame", porFrame);
		File dir = new File(Inicio.externo+"/MiniMine/mods/");
		if(!dir.exists()) {
			dir.mkdirs();
			ArquivosUtil.criar(dir.getAbsolutePath()+"/arquivos.mini");
		}
		String[] str = Gdx.files.absolute(Inicio.externo+"/MiniMine/mods/arquivos.mini").readString().split("\n");
		for(int i = 0; i < str.length; i++) {
			if(str == null || str[i].equals("")) continue;
			script += ArquivosUtil.ler(Inicio.externo+"/MiniMine/mods/"+str[i]);
		}
		globais.load(script, "script").call();
		if(ArquivosUtil.existe(Inicio.externo+"/MiniMine/mods/att.lua")) {
			att = ArquivosUtil.ler(Inicio.externo+"/MiniMine/mods/att.lua");
			existeAtt = true;
		}
	}
	
	public static void att(float delta1) {
		if(existeAtt) globais.load(att, "script").call();
		delta = delta1;
		if(frame != null) porFrame.call(frame);
	}
	
	public static void ajustar(int ve, int ho) {
		v = ve;
		h = ho;
		if(ajuste != null) aoAjustar.call(ajuste);
	}
	
	public static void exec(String codigo) {
		globais.load(codigo, "script").call();
	}
	
	public static Bloco criarBloco(String nome, String pedacoAtlas) {
		return new Bloco(nome, pedacoAtlas);
	}
	
	public static ModelInstance obterModeloGLTF(String caminho) {
		return new ModelInstance(Modelos.obterModelo(caminho, false));
	}

	public static Texture carregarTextura(String nome, String caminho) {
		if(Texturas.texs.containsKey(nome)) Texturas.texs.get(nome).dispose();
		Texturas.texs.put(nome, new Texture(Gdx.files.absolute(Inicio.externo+"/MiniMine/mods/"+caminho)));
		return Texturas.texs.get(nome);
	}
	
	public static TextureRegion obterRegiaoAtlas(String nome, Texture textura, int x, int y, int tamX, int tamY) {
		Texturas.atlas.put(nome, new TextureRegion(textura, x, y, tamX, tamY));
		return Texturas.atlas.get(nome);
	}

	public static BitmapFont carregarFonte(String caminho, int tam) {
		return InterUtil.carregarFonte(caminho, tam);
	}

	public static Sprite criarSprite(Texture textura) {
		return new Sprite(textura);
	}
	
	public static Sprite criarSprite(TextureRegion textura) {
		return new Sprite(textura);
	}

	public static Sprite criarSprite(String textura) {
		return new Sprite(Texturas.texs.get(textura));
	}
}
