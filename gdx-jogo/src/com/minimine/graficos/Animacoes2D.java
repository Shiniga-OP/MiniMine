package com.minimine.graficos;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.Gdx;
import java.util.ArrayList;
import java.util.List;
import com.badlogic.gdx.graphics.GL20;

public class Animacoes2D {
    // lista de animações ativas
    public static List<InfoAnimacao> animacoes = new ArrayList<>();

    public static class InfoAnimacao {
        public String nomeDestino; // nome da região no atlas principal
        public Pixmap[] quadros;   // os pixels de cada quadro da animação
        public int quadroAtual = 0;
        public int totalQuadros;
        public float tempoPorQuadro;
        public float acumulador = 0f;

        // coordenadas de destino no atlas principal
        public int destX, destY;

        public InfoAnimacao(String nomeDestino, TextureRegion[] regioes, float fps) {
            this.nomeDestino = nomeDestino;
            this.totalQuadros = regioes.length;
            this.tempoPorQuadro = 1f / fps;
            this.quadros = new Pixmap[totalQuadros];

            // 1. descobrir onde desenhar no atlas principal (Texturas.blocos)
            TextureRegion destino = Texturas.atlas.obter(nomeDestino);
            if(destino == null) {
                Gdx.app.log("Animacoes2D", "[ERRO] Destino não encontrado: " + nomeDestino);
                return;
            }
            // converte coordenadas UV(0..1) para Pixels(0..Largura)
            this.destX = (int)(destino.getU() * destino.getTexture().getWidth());
            this.destY = (int)(destino.getV() * destino.getTexture().getHeight());

            // 2. extrai os pixels de cada quadro
            for(int i = 0; i < totalQuadros; i++) {
                this.quadros[i] = extrairPixmap(regioes[i]);
            }
        }

        public void liberar() {
            for(Pixmap p : quadros) {
                if(p != null) p.dispose();
            }
        }
    }
    /**
     * extrai os dados de pixel de uma TextureRegion
     * necessario pra copiar da textura de origem pra memória
     */
    public static Pixmap extrairPixmap(TextureRegion region) {
        Texture textura = region.getTexture();
        if(!textura.getTextureData().isPrepared()) {
            textura.getTextureData().prepare();
        }
        Pixmap pixmapCompleto = textura.getTextureData().consumePixmap();

        // cria um novo Pixmap so com o pedaço
        Pixmap recorte = new Pixmap(region.getRegionWidth(), region.getRegionHeight(), pixmapCompleto.getFormat());

        // copia a area específica
        recorte.drawPixmap(pixmapCompleto, 
		0, 0, // destino x, y
		region.getRegionX(), region.getRegionY(), // origem x, y
		region.getRegionWidth(), region.getRegionHeight() // largura, altura
		);
        // o consumePixmap pode não retornar um novo se for FileTextureData, 
        // mas se a textura for gerenciada pelo AssetManager ou carregada manualmente,
        // é seguro descartar se tiver certeza que criou uma copia
        // no caso do GDX padrão, consumePixmap retorna o pixmap interno, então não deve dar dispose no pixmapCompleto
        // a menos que tenha certeza absoluta, pra segurança, deixa o GC ou o dispose da textura lidar com isso,
        // ja que so le os dados
        return recorte;
    }

    public static void add(String nomeDestino, TextureRegion[] frames, float fps) {
        if(frames == null || frames.length == 0) return;

        try {
            InfoAnimacao anim = new InfoAnimacao(nomeDestino, frames, fps);
            animacoes.add(anim);
            Gdx.app.log("Animacoes2D", "Animação adicionada para: " + nomeDestino);
        } catch(Exception e) {
            Gdx.app.log("Animacoes2D", "[ERRO] Falha ao criar animação: " + e.getMessage());
        }
    }

    public static void att(float delta) {
        for(InfoAnimacao anim : animacoes) {
            anim.acumulador += delta;

            if(anim.acumulador >= anim.tempoPorQuadro) {
                anim.acumulador -= anim.tempoPorQuadro; // Mantém o resto para precisão
                anim.quadroAtual = (anim.quadroAtual + 1) % anim.totalQuadros;
                attTextura(anim);
            }
        }
    }

    public static void attTextura(InfoAnimacao anim) {
		Pixmap p = anim.quadros[anim.quadroAtual];
		
		if(p == null) {
			throw new RuntimeException("[Animacoes2D]: [ERRO] quadro é null, quantidade de quadros: "+anim.quadros.length+", quadro acessado: "+anim.quadroAtual);
		}
		// 1. vincula a textura que queremos alterar
		Texturas.blocos.bind();

		// 2. usa o OpenGL pra carimbar os pixels
		Gdx.gl.glTexSubImage2D(
			GL20.GL_TEXTURE_2D, 
			0, // Nível de detalhe(mipmaps)
			anim.destX, // posição X no atlas
			anim.destY, // posição Y no atlas
			p.getWidth(), // largura do quadro
			p.getHeight(), // altura do quadro
			p.getGLFormat(),// formato(RGBA)
			p.getGLType(), // tipo de dado(UNSIGNED_BYTE)
			p.getPixels() // os dados brutos dos pixels
		);
	}
    
    public static void liberar() {
        for(InfoAnimacao anim : animacoes) {
            anim.liberar();
        }
        animacoes.clear();
    }
}
