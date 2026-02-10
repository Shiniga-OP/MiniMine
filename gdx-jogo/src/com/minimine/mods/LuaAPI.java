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
import com.minimine.graficos.EmissorParticulas;

public class LuaAPI {
	public static Globals globais;
	public static String att;
	public static LuaFunction aoAjustar, ajuste, porFrame, frame;
	public static int v, h;
	public static float delta;
	public static String pacote;
	public static boolean existeAtt = false;
	
	public static void iniciar() {
		pacote = Inicio.externo+"/MiniMine/mods/";
		
		String script = "";
		
		globais = JsePlatform.standardGlobals();
		
		globais.set("mundo", CoerceJavaToLua.coerce(Jogo.mundo));
		globais.set("jogador", CoerceJavaToLua.coerce(Jogo.render.ui.jg));
		globais.set("ui", CoerceJavaToLua.coerce(Jogo.render.ui));
		globais.set("util", CoerceJavaToLua.coerce(new Util()));
		globais.set("biomas", CoerceJavaToLua.coerce(new Biomas()));
		globais.set("chunkutil", CoerceJavaToLua.coerce(new ChunkUtil()));
		globais.set("texutil", CoerceJavaToLua.coerce(new Texturas()));
		globais.set("nuvens", CoerceJavaToLua.coerce(new NuvensUtil()));
		globais.set("ciclo", CoerceJavaToLua.coerce(new DiaNoiteUtil()));
		globais.set("gdx", CoerceJavaToLua.coerce(new Gdx()));
		globais.set("lua", CoerceJavaToLua.coerce(new LuaAPI()));
		globais.set("arquivos", CoerceJavaToLua.coerce(new ArquivosUtil()));
		globais.set("audio", CoerceJavaToLua.coerce(new Audio()));
		globais.set("cenas", CoerceJavaToLua.coerce(new Cenas()));
		globais.set("bloco", CoerceJavaToLua.coerce(new Bloco()));
		globais.set("particulas", CoerceJavaToLua.coerce(new EmissorParticulas()));
		
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
}
