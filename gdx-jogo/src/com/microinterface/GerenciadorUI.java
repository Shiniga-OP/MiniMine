package com.microinterface;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import java.util.ArrayList;

public class GerenciadorUI {
    public ArrayList<Componente> componentes = new ArrayList<Componente>();
    public ArrayList<CaixaDialogo> dialogos = new ArrayList<CaixaDialogo>();
    public CampoTexto campoEmFoco = null;

    public void add(Componente componente) {
        componentes.add(componente);
    }

    public void rm(Componente componente) {
        componentes.remove(componente);
    }

    public void addDialogo(CaixaDialogo dialogo) {
        dialogos.add(dialogo);
    }

    public void rmDialogo(CaixaDialogo dialogo) {
        dialogos.remove(dialogo);
    }

    public void limpar() {
        componentes.clear();
        dialogos.clear();
        campoEmFoco = null;
    }

    public void defFocoTexto(CampoTexto campo) {
        if(campoEmFoco != null && campoEmFoco != campo) {
            campoEmFoco.defFoco(false);
        }
        campoEmFoco = campo;
    }

    public boolean processarToque(float x, float y, boolean pressionado) {
        // primeiro verificar dialogos(prioridade)
        for(int i = dialogos.size() - 1; i >= 0; i--) {
            CaixaDialogo dialogo = dialogos.get(i);
            if(dialogo.ativa) {
                // verificar se tocou em um CampoTexto dentro do dialogo
                for(Componente comp : dialogo.componentes) {
                    if(comp instanceof CampoTexto) {
                        CampoTexto campo = (CampoTexto) comp;
                        float campoXGlobal = dialogo.x + campo.x;
                        float campoYGlobal = dialogo.y + campo.y;
                        if(x >= campoXGlobal && x <= campoXGlobal + campo.largura &&
                           y >= campoYGlobal && y <= campoYGlobal + campo.altura && !pressionado) {
                            defFocoTexto(campo);
                        }
                    }
                }

                if(dialogo.aoTocar(x, y, pressionado)) {
                    return true;
                }
            }
        }
        // depois verificar componentes normais
        for(int i = componentes.size() - 1; i >= 0; i--) {
            Componente comp = componentes.get(i);

            if(comp instanceof CampoTexto) {
                CampoTexto campo = (CampoTexto) comp;
                if(campo.contem(x, y) && !pressionado) {
                    defFocoTexto(campo);
                }
            }
            if(comp.aoTocar(x, y, pressionado)) return true;
        }
        // se tocou fora de qualquer campo, remover o foco
        if(!pressionado && campoEmFoco != null) {
            boolean tocouNoCampo = false;

            // verificar se tocou no campo ativo(pode ta em um dialogo)
            for(CaixaDialogo dialogo : dialogos) {
                if(dialogo.ativa) {
                    for(Componente comp : dialogo.componentes) {
                        if(comp == campoEmFoco) {
                            float campoXGlobal = dialogo.x + campoEmFoco.x;
                            float campoYGlobal = dialogo.y + campoEmFoco.y;
                            tocouNoCampo = x >= campoXGlobal && x <= campoXGlobal + campoEmFoco.largura &&
								y >= campoYGlobal && y <= campoYGlobal + campoEmFoco.altura;
                            break;
                        }
                    }
                }
            }

            if(!tocouNoCampo) {
                tocouNoCampo = campoEmFoco.contem(x, y);
            }

            if(!tocouNoCampo) {
                campoEmFoco.defFoco(false);
                campoEmFoco = null;
            }
        }
        return false;
    }

    public void processarArraste(float x, float y) {
        for(int i = dialogos.size() - 1; i >= 0; i--) {
            CaixaDialogo dialogo = dialogos.get(i);
            if(dialogo.ativa) {
                dialogo.aoArrastar(x, y);
            }
        }
    }

    public boolean processarTecla(int keycode) {
        if(campoEmFoco != null) {
            return campoEmFoco.processarTecla(keycode);
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
        for(int i = 0; i < componentes.size(); i++) {
            componentes.get(i).desenhar(pincel, delta, 0, 0);
        }
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
}

