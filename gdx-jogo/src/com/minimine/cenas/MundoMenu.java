package com.minimine.cenas;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.InputProcessor;
import com.minimine.ui.Botao;
import com.minimine.ui.Texto;
import com.minimine.Inicio;
import com.minimine.Cenas;
import com.minimine.graficos.Texturas;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import com.minimine.ui.InterUtil;
import com.minimine.utils.ArquivosUtil;
import com.badlogic.gdx.Input;
import com.minimine.ui.Dialogo;
import com.minimine.mundo.Mundo;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class MundoMenu implements Screen, InputProcessor {
    public SpriteBatch sb;
    public BitmapFont fonte;
    public List<Botao> botoes = new ArrayList<>();
    public List<Texto> textos = new ArrayList<>();
    public List<String> nomesMundos = new ArrayList<>();
    public boolean recarregarInterface = false;
    public long tempoInicio = 0L;
    public Botao botaoApertado = null;
	public Dialogo dialogo;
    
    @Override
    public void show() {
		ArquivosUtil.debug = true;
        sb = new SpriteBatch();
        fonte = InterUtil.carregarFonte("ui/fontes/pixel.ttf", 50);
        Gdx.input.setInputProcessor(this);
		dialogo = new Dialogo();
        carregarMundos();
        criarInterface();
    }

    public void carregarMundos() {
        nomesMundos.clear();
        File pastaMundos = new File(Inicio.externo + "/MiniMine/mundos");
        if(pastaMundos.exists() && pastaMundos.isDirectory()) {
            File[] arquivos = pastaMundos.listFiles();
            if(arquivos != null) {
                for(int i = 0; i < arquivos.length; i++) {
                    File arquivo = arquivos[i];
                    if(arquivo.isFile() && arquivo.getName().endsWith(".mini")) {
                        String nome = arquivo.getName().replace(".mini", "");
                        nomesMundos.add(nome);
                    }
                }
            }
        }
    }

    public void criarInterface() {
        botoes.clear();
        textos.clear();
        textos.add(new Texto("Mundos", 0, 0) {
				@Override
				public void aoAjustar(int v, int h) {
					GlyphLayout l = new GlyphLayout(fonte, texto);
					x = (v - l.width) / 2f;
					y = h - 100;
				}
			});
        botoes.add(new Botao(Texturas.texs.get("botao_opcao"), 0, 0, 400, 100, "criar_mundo") {
				@Override
				public void aoTocar(int tx, int ty, int p) {
					criarNovoMundo();
				}
				@Override
				public void aoAjustar(int v, int h) {
					defPos((v - tamX) / 2, h - 220);
				}
			});
        textos.add(new Texto("Novo Mundo", 0, 0) {
				@Override
				public void aoAjustar(int v, int h) {
					GlyphLayout l = new GlyphLayout(fonte, texto);
					x = (v - l.width) / 2f;
					y = h - 180;
				}
			});
        // lista de mundos
        int inicioY = Gdx.graphics.getHeight() - 350;
        for(int i = 0; i < nomesMundos.size(); i++) {
            final String nomeMundo = URLDecoder.decode(nomesMundos.get(i), StandardCharsets.UTF_8);
            final int yPos = inicioY - (i * 120);

            Botao mundoBt = new Botao(Texturas.texs.get("botao_opcao"), 0, yPos, 500, 100, "") {
                @Override
                public void aoTocar(int tx, int ty, int p) {
                    Mundo.nome = nomeMundo;
                    Inicio.defTela(Cenas.jogo);
                }
                @Override
                public void aoAjustar(int v, int h) {
                    defPos((v - tamX) / 2, yPos);
                }
            };
            botoes.add(mundoBt);
            textos.add(new Texto(nomeMundo, 0, 0) {
					@Override
					public void aoAjustar(int v, int h) {
						GlyphLayout l = new GlyphLayout(fonte, texto);
						x = (v - l.width) / 2f;
						y = yPos + 55;
					}
				});
        }
        botoes.add(new Botao(Texturas.texs.get("botao_e"), 50, 50, 100, 100, "voltar") {
				@Override
				public void aoTocar(int tx, int ty, int p) {
					Inicio.defTela(Cenas.menu);
				}
				@Override
				public void aoAjustar(int v, int h) {
					defPos(50, 50);
				}
			});
        if(nomesMundos.isEmpty()) {
            textos.add(new Texto("Nenhum mundo salvo", 0, 0) {
					@Override
					public void aoAjustar(int v, int h) {
						GlyphLayout l = new GlyphLayout(fonte, texto);
						x = (v - l.width) / 2f;
						y = h / 2;
					}
				});
        }
        int v = Gdx.graphics.getWidth();
        int h = Gdx.graphics.getHeight();
        for(Botao b : botoes) b.aoAjustar(v, h);
        for(Texto t : textos) t.aoAjustar(v, h);
    }

	public void criarNovoMundo() {
		// primeiro dialogo: pede o Nome
		dialogo.abrir("Nome do Mundo", new Dialogo.Acao() {
			@Override public void aoDigitar(char x) {}
			    @Override 
                public void aoFechar() {
                    Gdx.input.setCursorCatched(false);
                }
				@Override
				public void aoConfirmar() {
					if(dialogo.texto.trim().isEmpty()) return;
					Mundo.nome = dialogo.texto;
					
					// segundo dialogo: pede a Semente
					dialogo.abrir("Semente", new Dialogo.Acao() {
						@Override public void aoFechar() {}
							@Override public void aoDigitar(char x) {}
							@Override
							public void aoConfirmar() {
								int semente;
								try {
									semente = Integer.parseInt(dialogo.texto.trim());
								} catch(Exception e) {
									semente = 0;
								}
								Mundo.semente = semente;
								Inicio.defTela(Cenas.jogo);
							}
						});
				}
			});
	}

    @Override
    public void render(float delta) {
        // recarrega a interface se necessario(evita ConcurrentModificationException)
        if(recarregarInterface) {
            carregarMundos();
            criarInterface();
            recarregarInterface = false;
        }
        Gdx.gl.glClearColor(0.2f, 0.2f, 0.3f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        sb.begin();
        for(Botao bt : botoes) {
            bt.porFrame(delta, sb, fonte);
        }
        for(Texto t : textos) {
            t.porFrame(delta, sb, fonte);
        }
		if(dialogo.visivel) dialogo.porFrame(delta, sb, fonte);
        sb.end();
    }

    @Override
    public void resize(int v, int h) {
        for(Botao b : botoes) b.aoAjustar(v, h);
        for(Texto t : textos) t.aoAjustar(v, h);
    }

    @Override
    public void dispose() {
        if(sb != null) sb.dispose();
        if(fonte != null) fonte.dispose();
    }

    @Override
    public boolean touchDown(int telaX, int telaY, int p, int b) {
        int y = Gdx.graphics.getHeight() - telaY;
		
		if(dialogo.visivel) {
			dialogo.verificarToque(telaX, y);
			return true; // bloqueia outros cliques se o dialogo estiver aberto
		}
        List<Botao> botoesCopia = this.botoes;

        for(Botao bt : botoesCopia) {
            if(bt.hitbox.contains(telaX, y)) {
                bt.aoTocar(telaX, y, p);
                break;
            }
        }
        return true;
    }
    @Override public boolean touchDragged(int telaX, int telaY, int p) {return false;}
    @Override public boolean touchUp(int telaX, int telaY, int p, int b) {return false;}
    @Override public void hide() { dispose(); }
    @Override public void pause(){}
    @Override public void resume(){}
    @Override public boolean keyDown(int p){return false;}
    @Override 
	public boolean keyTyped(char caractere) { 
		if(dialogo.visivel) {
			dialogo.digitando(caractere);
			return true;
		}
		return false; 
	}
    @Override public boolean keyUp(int p){return false;}
    @Override public boolean mouseMoved(int p, int p1){return false;}
    @Override public boolean scrolled(float p, float p1){return false;}
}
