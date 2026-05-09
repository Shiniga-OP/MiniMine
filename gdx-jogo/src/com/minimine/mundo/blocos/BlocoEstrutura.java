package com.minimine.mundo.blocos;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.micro.componentes.CaixaDialogo;
import com.micro.componentes.CampoTexto;
import com.micro.janelas.PainelFatiado;
import com.micro.util.Acao;
import com.micro.util.Ancora;
import com.minimine.utils.ArquivosUtil;
import com.minimine.mundo.Mundo;
import com.minimine.mundo.Chave;
import com.minimine.mundo.chunks.ChunkUtil;
import com.minimine.mundo.chunks.Chunk;
import java.util.ArrayList;
import java.util.List;
/*
 * classe auxiliar que monta a InterfaceBloco do bloco_estrutura
 * chamada por Bloco.iniciarInterfaces()

 * interface:
 *   - campo "Nome": nome do arquivo .minies a salvar
 *   - campos Larg/Alt/Prof: dimensões da bounding box (padrão 16x16x16)
 *   - campos CX/CY/CZ: offset/âncora da bcaixa em relação ao bloco_estrutura (padrão 0,0,1)
 *   - botão "Salvar": varre a região, descarta ar e bloco_nulo, salva .minies
 *   - botão "Carregar": lê o .minies pelo nome e coloca no mundo usando CX/CY/CZ
 *   - botão "Fechar": fecha sem salvar

 * bloco_nulo:
 *   - marca "ar explícito" dentro da estrutura
 *   - transparente=true, solido=false, culling=false
 *   - não é gravado no .minies(descartado igual ao ar real)

 * formato .minies(binario simples, sem ZIP):
 *   [int]versão do formato(= 1)
 *   [utf]nome
 *   [int]largura
 *   [int]altura
 *   [int]profundidade
 *   [int]ancora X
 *   [int]ancora Y
 *   [int]ancora Z
 *   [int]total de blocos salvos
 *   para cada bloco:
 *     [int] x local(0..largura-1)
 *     [int] y local(0..altura-1)
 *     [int] z local(0..profundidade-1)
 *     [utf] id do bloco
 *     [short] meta do bloco

 * origem é preenchido pelo abrir() e lido pelo salvar()

 * meta do bloco_estrutura: slots consecutivos em Y a partir da posição do bloco:
 *   Y+0 = larg(1..64, padrão 16) lido como sem sinal(& 0xFFFF)
 *   Y+1 = alt(1..256, padrão 16) lido como sem sinal (& 0xFFFF)
 *   Y+2 = prof(1..64, padrão 16) lido como sem sinal (& 0xFFFF)
 *   Y+3 = cx(-64..64, padrão 0) lido como com sinal(short direto)
 *   Y+4 = cy(-64..64, padrão 0) lido como com sinal(short direto)
 *   Y+5 = cz(-64..64, padrão 1) lido como com sinal(short direto)
 
 * Bcaixas
 *   lista de posições de bloco_estrutura ativos no mundo
 *   cada entrada: int[3] = { x global, y global, z global }
 *   o Render lê larg/alt/prof/cx/cy/cz do meta em tempo real a cada frame
 *   alimentada pelo EventoBloco(aoColocar/aoDestruir)
 */
public class BlocoEstrutura {
    // guarda a posição do bloco_estrutura com interface aberta
    public static final int[] origem = new int[3];

    // slots de meta (offset em Y relativo ao bloco)
    public static final int META_LARG = 0;
    public static final int META_ALT  = 1;
    public static final int META_PROF = 2;
    public static final int META_CX   = 3;
    public static final int META_CY   = 4;
    public static final int META_CZ   = 5;

    // dimensões padrão
    public static final int DEF_LARG = 16;
    public static final int DEF_ALT  = 16;
    public static final int DEF_PROF = 16;
    // posição padrão: 1 bloco à frente em Z
    public static final int DEF_CX   = 0;
    public static final int DEF_CY   = 0;
    public static final int DEF_CZ   = 1;
    /*
     * lista de posições de bloco_estrutura ativos
     * cada int[3] = { x global, y global, z global }
     * dimensões e posições lidos do meta a cada frame pelo Render
     */
    public static final List<int[]> bcaixas = new ArrayList<int[]>();

    public static void addBcaixa(int x, int y, int z) {
        defDimensoes(x, y, z, DEF_LARG, DEF_ALT, DEF_PROF);
        defPos(x, y, z, DEF_CX, DEF_CY, DEF_CZ);
        bcaixas.add(new int[]{ x, y, z });
    }

    public static void rmBcaixa(int x, int y, int z) {
        for(int i = bcaixas.size() - 1; i >= 0; i--) {
            int[] b = bcaixas.get(i);
            if(b[0] == x && b[1] == y && b[2] == z) {
                bcaixas.remove(i);
                return;
            }
        }
    }

    // le dimensões do meta sem sinal
    public static int obterLarg(int x, int y, int z) {
        int v = Mundo.obterMetaMundo(x, y + META_LARG, z) & 0xFFFF;
        return v == 0 ? DEF_LARG : v;
    }

    public static int obterAlt(int x, int y, int z) {
        int v = Mundo.obterMetaMundo(x, y + META_ALT, z) & 0xFFFF;
        return v == 0 ? DEF_ALT : v;
    }

    public static int obterProf(int x, int y, int z) {
        int v = Mundo.obterMetaMundo(x, y + META_PROF, z) & 0xFFFF;
        return v == 0 ? DEF_PROF : v;
    }

    // le posições do meta com sinal(podem ser negativos)
    public static int obterCX(int x, int y, int z) {
        return (int)Mundo.obterMetaMundo(x, y + META_CX, z);
    }

    public static int obterCY(int x, int y, int z) {
        return (int)Mundo.obterMetaMundo(x, y + META_CY, z);
    }

    public static int obterCZ(int x, int y, int z) {
        short v = Mundo.obterMetaMundo(x, y + META_CZ, z);
        // slot nunca gravado retorna 0; aplica padrão
        return v == 0 ? DEF_CZ : (int)v;
    }

    // grava larg/alt/prof no meta
    public static void defDimensoes(int x, int y, int z, int larg, int alt, int prof) {
        Mundo.defMetaMundo(x, y + META_LARG, z, (short) larg);
        Mundo.defMetaMundo(x, y + META_ALT,  z, (short) alt);
        Mundo.defMetaMundo(x, y + META_PROF, z, (short) prof);
    }

    // grava cx/cy/cz no meta
    public static void defPos(int x, int y, int z, int cx, int cy, int cz) {
        Mundo.defMetaMundo(x, y + META_CX, z, (short) cx);
        Mundo.defMetaMundo(x, y + META_CY, z, (short) cy);
        Mundo.defMetaMundo(x, y + META_CZ, z, (short) cz);
    }

    // iniciar(), chamado por Bloco.iniciarInterfaces()
    public static void iniciar(
		final Bloco blocoEstrutura,
		final PainelFatiado base,
		final BitmapFont fonte) {

        if(blocoEstrutura == null) return;

        final CaixaDialogo dialogo = new CaixaDialogo(
			base, fonte, 3f, new ShapeRenderer());
        dialogo.largura = 440;
        dialogo.altura  = 320;

        // nome da estrutura
        final CampoTexto campoNome = new CampoTexto(base, fonte, 20, 280, 400, 44, 3f);
        campoNome.padrao = "Nome da estrutura";
        campoNome.limiteCaracteres = 48;
        dialogo.add(campoNome);

        // dimensões
        final CampoTexto campoLarg = new CampoTexto(base, fonte,  20, 228, 80, 40, 3f);
        campoLarg.padrao = "Larg"; campoLarg.limiteCaracteres = 4;
        dialogo.add(campoLarg);

        final CampoTexto campoAlt = new CampoTexto(base, fonte, 110, 228, 80, 40, 3f);
        campoAlt.padrao  = "Alt";  campoAlt.limiteCaracteres  = 4;
        dialogo.add(campoAlt);

        final CampoTexto campoProf = new CampoTexto(base, fonte, 200, 228, 80, 40, 3f);
        campoProf.padrao = "Prof"; campoProf.limiteCaracteres = 4;
        dialogo.add(campoProf);

        // posição da bcaixa
        final CampoTexto campoCX = new CampoTexto(base, fonte,  20, 176, 80, 40, 3f);
        campoCX.padrao = "CX"; campoCX.limiteCaracteres = 5;
        dialogo.add(campoCX);

        final CampoTexto campoCY = new CampoTexto(base, fonte, 110, 176, 80, 40, 3f);
        campoCY.padrao = "CY"; campoCY.limiteCaracteres = 5;
        dialogo.add(campoCY);

        final CampoTexto campoCZ = new CampoTexto(base, fonte, 200, 176, 80, 40, 3f);
        campoCZ.padrao = "CZ"; campoCZ.limiteCaracteres = 5;
        dialogo.add(campoCZ);

        com.minimine.ui.UI.gerenciador.addDialogo(dialogo);

        blocoEstrutura.ui = new InterfaceBloco() {
            boolean aberta = false;

            @Override
            public void abrir(int x, int y, int z) {
                if(aberta) fechar();

                Bloco.ABERTO = true;
                aberta = true;
                origem[0] = x;
                origem[1] = y;
                origem[2] = z;
                com.minimine.ui.UI.modoTexto = true;
                Gdx.input.setCursorCatched(false);

                // preenche campos com valores atuais do meta
                int larg = obterLarg(x, y, z);
                int alt  = obterAlt(x, y, z);
                int prof = obterProf(x, y, z);
                int cx = obterCX(x, y, z);
                int cy = obterCY(x, y, z);
                int cz = obterCZ(x, y, z);
                campoLarg.texto = larg == DEF_LARG ? "" : String.valueOf(larg);
                campoAlt.texto = alt  == DEF_ALT ? "" : String.valueOf(alt);
                campoProf.texto = prof == DEF_PROF  ? "" : String.valueOf(prof);
                campoCX.texto = cx == DEF_CX ? "" : String.valueOf(cx);
                campoCY.texto = cy == DEF_CY ? "" : String.valueOf(cy);
                campoCZ.texto = cz == DEF_CZ ? "" : String.valueOf(cz);

                dialogo.x = Gdx.graphics.getWidth()  / 2f - dialogo.largura / 2f;
                dialogo.y = Gdx.graphics.getHeight() / 2f - dialogo.altura  / 2f;

                dialogo.mostrar(
                    "Bloco de Estrutura (" + x + ", " + y + ", " + z + ")",
                    "",
                    new CaixaDialogo.Fechar() {
                        @Override public void aoFechar(boolean confirmou) { fechar(); }
                    });
            }

            @Override
            public void fechar() {
                if(!aberta) return;
                aberta = false;
                com.minimine.ui.UI.modoTexto = false;
                Gdx.input.setCursorCatched(true);
                dialogo.fechar(false);
                Bloco.ABERTO = false;
            }

            @Override
            public void renderizar(SpriteBatch sb, BitmapFont f, float delta) {
                if(aberta) {
                    int larg = clamp(praInt(campoLarg.texto, DEF_LARG), 1, 64);
                    int alt  = clamp(praInt(campoAlt.texto, DEF_ALT), 1, 256);
                    int prof = clamp(praInt(campoProf.texto, DEF_PROF), 1, 64);
                    int cx = clamp(praInt(campoCX.texto, DEF_CX), -64, 64);
                    int cy = clamp(praInt(campoCY.texto, DEF_CY), -64, 64);
                    int cz = clamp(praInt(campoCZ.texto, DEF_CZ), -64, 64);
                    defDimensoes(origem[0], origem[1], origem[2], larg, alt, prof);
                    defPos(origem[0], origem[1], origem[2], cx, cy, cz);
                }
            }
            @Override public boolean aberta() { return aberta; }

            @Override
            public boolean processarToque(int x, int y, boolean pressionado) {
                return com.minimine.ui.UI.gerenciador.processarToque(x, y, pressionado);
            }

            @Override public void liberar() { dialogo.liberar(); }
        };
        // botão salvar
        dialogo.addBotao("Salvar", base, Ancora.CENTRO_DIREITO, -10, new Acao() {
				@Override public void exec() {
					salvar(blocoEstrutura.ui,
						   campoNome, campoLarg, campoAlt, campoProf,
						   campoCX, campoCY, campoCZ);
				}
			});
        // botão carregar
        dialogo.addBotao("Carregar", base, Ancora.CENTRO, 0, new Acao() {
				@Override public void exec() {
					carregar(blocoEstrutura.ui, campoNome);
				}
			});
        // botão fechar
        dialogo.addBotao("Fechar", base, Ancora.CENTRO_ESQUERDO, 10, new Acao() {
				@Override public void exec() { blocoEstrutura.ui.fechar(); }
			});
    }

    // salvar()
    public static void salvar(
		final InterfaceBloco ui,
		final CampoTexto campoNome,
		final CampoTexto campoLarg,
		final CampoTexto campoAlt,
		final CampoTexto campoProf,
		final CampoTexto campoCX,
		final CampoTexto campoCY,
		final CampoTexto campoCZ) {

        String nome = campoNome.texto.trim();
        if(nome.isEmpty()) {
            Gdx.app.log("[BlocoEstrutura]", "Nome vazio — salvamento cancelado.");
            return;
        }
        nome = nome.replaceAll("[^a-zA-Z0-9_\\-]", "_");

        int larg = clamp(praInt(campoLarg.texto, DEF_LARG), 1, 64);
        int alt = clamp(praInt(campoAlt.texto, DEF_ALT), 1, 256);
        int prof = clamp(praInt(campoProf.texto, DEF_PROF), 1, 64);
        int cx = clamp(praInt(campoCX.texto, DEF_CX), -64, 64);
        int cy = clamp(praInt(campoCY.texto, DEF_CY), -64, 64);
        int cz = clamp(praInt(campoCZ.texto, DEF_CZ), -64, 64);

        // persiste tudo no meta antes de salvar
        defDimensoes(origem[0], origem[1], origem[2], larg, alt, prof);
        defPos(origem[0], origem[1], origem[2], cx, cy, cz);

        int baseX = origem[0] + cx;
        int baseY = origem[1] + cy;
        int baseZ = origem[2] + cz;

        try {
            ArquivosUtil.svEstrutura(
                nome, larg, alt, prof,
                0, 0, 0,
                baseX, baseY, baseZ);
        } catch(Exception e) {
            Gdx.app.log("[BlocoEstrutura]", "[ERRO] ao salvar: " + e.getMessage());
        }
        ui.fechar();
    }

    // carregar()
    public static void carregar(
		final InterfaceBloco ui,
		final CampoTexto campoNome) {

        String nome = campoNome.texto.trim();
        if(nome.isEmpty()) {
            Gdx.app.log("[BlocoEstrutura]", "Nome vazio: carregamento cancelado.");
            return;
        }
        nome = nome.replaceAll("[^a-zA-Z0-9_\\-]", "_");

        ArquivosUtil.DadosEstrutura dados = ArquivosUtil.crEstrutura(nome);
        if(dados == null) {
            Gdx.app.log("[BlocoEstrutura]", "[ERRO] estrutura não encontrada: " + nome);
            return;
        }
        int ox = origem[0] + obterCX(origem[0], origem[1], origem[2]);
        int oy = origem[1] + obterCY(origem[0], origem[1], origem[2]);
        int oz = origem[2] + obterCZ(origem[0], origem[1], origem[2]);
        dados.colocarMundo(ox, oy, oz);
        Gdx.app.log("[BlocoEstrutura]", "estrutura carregada: " + nome);
        ui.fechar();
    }

    // iniciarEventos(), chamado por Bloco.iniciarInterfaces()
    public static void iniciarEventos(final Bloco blocoEstrutura) {
        if(blocoEstrutura == null) return;
        blocoEstrutura.evento = new EventoBloco() {
            @Override
            public void aoColocar(int x, int y, int z) {
                addBcaixa(x, y, z);
            }
            @Override
            public void aoDestruir(int x, int y, int z) {
                rmBcaixa(x, y, z);
            }
        };
    }

    public static final int praInt(String s, int def) {
        if(s == null || s.trim().isEmpty()) return def;
        try {
			return Integer.parseInt(s.trim());
		} catch(NumberFormatException e) {
			return def;
		}
    }

    public static final int clamp(int v, int min, int max) {
        return v < min ? min : (v > max ? max : v);
    }
}


