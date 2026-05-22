package com.minimine.graficos;

import com.badlogic.gdx.graphics.g3d.Model;
import java.util.HashMap;
import net.mgsx.gltf.loaders.gltf.GLTFLoader;
import com.badlogic.gdx.Gdx;
import net.mgsx.gltf.scene3d.scene.SceneAsset;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.minimine.inventario.ItemRegistro;
import com.minimine.inventario.Item;

public class Modelos {
	public static final HashMap<String, SceneAsset> modelosGltf = new HashMap<>();
	public static final HashMap<String, Model> modelosItens = new HashMap<>();

	public static final float TAM_ITEM = 0.35f;
	public static final int PIXELS = 16;
	public static final float TAM_PIXEL = TAM_ITEM / PIXELS;

	// posição do item no espaço de camera
	// aplicados via transformação em Jogador.render()
	public static final float ITEM_OX = 0.10f;
	public static final float ITEM_OY = -0.45f;
	public static final float ITEM_OZ = -0.55f;

	public static Model obterModelo(String caminho, boolean interno) {
		if(modelosGltf.containsKey(caminho)) return modelosGltf.get(caminho).scene.model;
		final SceneAsset ativoCena = new GLTFLoader().load(interno ? Gdx.files.internal(caminho) : Gdx.files.absolute(caminho));
		modelosGltf.put(caminho, ativoCena);
		return modelosGltf.get(caminho).scene.model;
	}

	public static Model obterModelo(String caminho) {
		return obterModelo(caminho, true);
	}

	// le a textura pixel a pixel(16x16) e constroi um modelo voxel 2.5D:
	// - pixels transparentes(alfa < 128) são ignorados
	// - para cada pixel visivel, gera apenas as faces cujo vizinho é transparente
	// - 6 faces possíveis por pixel: frente(+Z), tras(-Z), esquerda(-X), direita(+X), topo(+Y), base(-Y)
	// - UV de cada face aponta pro pixel correto no atlas
	// - malha centrada em(0,0,0); posição e rotação aplicadas via transform em Jogador.render()
	public static ModelInstance modeloItem(String item) {
		if(modelosItens.containsKey(item)) return new ModelInstance(modelosItens.get(item));

		if(item.equals("ar")) return null;

		Item reg = ItemRegistro.obter(item);
		if(reg == null) return null;

		final TextureRegion tex = reg.textura;
		if(tex == null) return null;

		// le os pixels da textura
		final Texture textura = tex.getTexture();
		if(!textura.getTextureData().isPrepared()) textura.getTextureData().prepare();
		final Pixmap pixmap = textura.getTextureData().consumePixmap();

		// coordenadas do tile no pixmap(em pixels inteiros)
		final int tileX = (int)(tex.getU() * pixmap.getWidth());
		final int tileY = (int)(tex.getV() * pixmap.getHeight());
		final int tileW = (int)((tex.getU2() - tex.getU()) * pixmap.getWidth());
		final int tileH = (int)((tex.getV2() - tex.getV()) * pixmap.getHeight());

		// le a grade de alfa: verdadeiro = pixel visivel
		final boolean[][] visivel = new boolean[PIXELS][PIXELS];
		for(int py = 0; py < PIXELS; py++) {
			for(int px = 0; px < PIXELS; px++) {
				int imgX = tileX + px * tileW / PIXELS;
				int imgY = tileY + py * tileH / PIXELS;
				int cor = pixmap.getPixel(imgX, imgY);
				visivel[px][py] = (cor & 0xFF) >= 128; // canal alfa
			}
		}
		pixmap.dispose();

		// UV de cada pixel no atlas
		final float uMin = tex.getU();
		final float vMin = tex.getV();
		final float uPasso = (tex.getU2() - uMin) / PIXELS;
		final float vPasso = (tex.getV2() - vMin) / PIXELS;

		// origem da malha para centralizar em(0,0,0)
		final float origem = -TAM_ITEM * 0.5f;

		// pré-aloca generosamente: no pior caso PIXELS*PIXELS*6 faces
		final int maxQuads = PIXELS * PIXELS * 6;
		final float[] verts = new float[maxQuads * 4 * 5]; // 4 verts * 5 floats
		final short[] indices = new short[maxQuads * 6];
		int vi = 0, ii = 0;

		for(int py = 0; py < PIXELS; py++) {
			for(int px = 0; px < PIXELS; px++) {
				if(!visivel[px][py]) continue;

				// UV deste pixel no atlas
				float u0 = uMin + uPasso * px;
				float u1 = u0 + uPasso;
				// V: py=0 é topo da imagem, vMax é baixo
				float v0 = vMin + vPasso * py;
				float v1 = v0 + vPasso;

				// posição 3D deste voxel-pixel
				// X cresce pra direita, Y cresce pra cima(py=0 é topo → Y maximo)
				float x0 = origem + TAM_PIXEL * px;
				float x1 = x0 + TAM_PIXEL;
				float y1 = -origem - TAM_PIXEL * py; // topo do pixel
				float y0 = y1 - TAM_PIXEL; // base do pixel
				float z0 = origem; // face traseira
				float z1 = origem + TAM_PIXEL; // face frontal(espessura = 1 pixel)

				// helper inline: adiciona quad e indices
				// normal +Z(frente): visível se não tem vizinho na frente, sempre exposta pois é a face do item
				// normal -Z(trás): idem
				// normais X e Y: so emite se o vizinho nessa direção for transparente

				// === face +Z(frente) ===
				// sempre visivel
				{
					short b = (short)(vi / 5);
					// BL
					verts[vi++]=x0;
					verts[vi++]=y0;
					verts[vi++]=z1;
					verts[vi++]=u0;
					verts[vi++]=v1;
					// TL
					verts[vi++]=x0;
					verts[vi++]=y1;
					verts[vi++]=z1;
					verts[vi++]=u0;
					verts[vi++]=v0;
					// TR
					verts[vi++]=x1;
					verts[vi++]=y1;
					verts[vi++]=z1;
					verts[vi++]=u1;
					verts[vi++]=v0;
					// BR
					verts[vi++]=x1;
					verts[vi++]=y0;
					verts[vi++]=z1;
					verts[vi++]=u1;
					verts[vi++]=v1;
					indices[ii++]=b;
					indices[ii++]=(short)(b+1);
					indices[ii++]=(short)(b+2);
					indices[ii++]=(short)(b+2);
					indices[ii++]=(short)(b+3);
					indices[ii++]=b;
				}
				// === face -Z(tras) ===
				{
					short b = (short)(vi / 5);
					// BR
					verts[vi++]=x1;
					verts[vi++]=y0;
					verts[vi++]=z0;
					verts[vi++]=u1;
					verts[vi++]=v1;
					// TR
					verts[vi++]=x1;
					verts[vi++]=y1;
					verts[vi++]=z0;
					verts[vi++]=u1;
					verts[vi++]=v0;
					// TL
					verts[vi++]=x0;
					verts[vi++]=y1;
					verts[vi++]=z0;
					verts[vi++]=u0;
					verts[vi++]=v0;
					// BL
					verts[vi++]=x0;
					verts[vi++]=y0;
					verts[vi++]=z0;
					verts[vi++]=u0;
					verts[vi++]=v1;
					indices[ii++]=b;
					indices[ii++]=(short)(b+1);
					indices[ii++]=(short)(b+2);
					indices[ii++]=(short)(b+2);
					indices[ii++]=(short)(b+3);
					indices[ii++]=b;
				}

				// === face +X(direita) === so se não tem vizinho visivel a direita
				if(px + 1 >= PIXELS || !visivel[px+1][py]) {
					short b = (short)(vi / 5);
					verts[vi++]=x1;
					verts[vi++]=y0;
					verts[vi++]=z1;
					verts[vi++]=u1;
					verts[vi++]=v1;
					verts[vi++]=x1;
					verts[vi++]=y1;
					verts[vi++]=z1;
					verts[vi++]=u1;
					verts[vi++]=v0;
					verts[vi++]=x1;
					verts[vi++]=y1;
					verts[vi++]=z0;
					verts[vi++]=u1;
					verts[vi++]=v0;
					verts[vi++]=x1;
					verts[vi++]=y0;
					verts[vi++]=z0;
					verts[vi++]=u1;
					verts[vi++]=v1;
					indices[ii++]=b;
					indices[ii++]=(short)(b+1);
					indices[ii++]=(short)(b+2);
					indices[ii++]=(short)(b+2);
					indices[ii++]=(short)(b+3);
					indices[ii++]=b;
				}
				// === face -X(esquerda) === so se não tem vizinho visivel a esquerda
				if(px - 1 < 0 || !visivel[px-1][py]) {
					short b = (short)(vi / 5);
					verts[vi++]=x0;
					verts[vi++]=y0;
					verts[vi++]=z0;
					verts[vi++]=u0;
					verts[vi++]=v1;
					verts[vi++]=x0;
					verts[vi++]=y1;
					verts[vi++]=z0;
					verts[vi++]=u0;
					verts[vi++]=v0;
					verts[vi++]=x0;
					verts[vi++]=y1;
					verts[vi++]=z1;
					verts[vi++]=u0;
					verts[vi++]=v0;
					verts[vi++]=x0;
					verts[vi++]=y0;
					verts[vi++]=z1;
					verts[vi++]=u0;
					verts[vi++]=v1;
					
					indices[ii++]=b;
					indices[ii++]=(short)(b+1);
					indices[ii++]=(short)(b+2);
					indices[ii++]=(short)(b+2);
					indices[ii++]=(short)(b+3);
					indices[ii++]=b;
				}
				// === face +Y(topo) === so se não tem vizinho visivel acima(py-1 pois py=0 é topo)
				if(py - 1 < 0 || !visivel[px][py-1]) {
					short b = (short)(vi / 5);
					verts[vi++]=x0;
					verts[vi++]=y1;
					verts[vi++]=z0;
					verts[vi++]=u0;
					verts[vi++]=v0;
					verts[vi++]=x1;
					verts[vi++]=y1;
					verts[vi++]=z0;
					verts[vi++]=u1;
					verts[vi++]=v0;
					verts[vi++]=x1;
					verts[vi++]=y1;
					verts[vi++]=z1;
					verts[vi++]=u1;
					verts[vi++]=v0;
					verts[vi++]=x0;
					verts[vi++]=y1;
					verts[vi++]=z1;
					verts[vi++]=u0;
					verts[vi++]=v0;
					
					indices[ii++]=b;
					indices[ii++]=(short)(b+1);
					indices[ii++]=(short)(b+2);
					indices[ii++]=(short)(b+2);
					indices[ii++]=(short)(b+3);
					indices[ii++]=b;
				}
				// === face -Y(base) === so se não tem vizinho visivel abaixo(py+1)
				if(py + 1 >= PIXELS || !visivel[px][py+1]) {
					short b = (short)(vi / 5);
					verts[vi++]=x0;
					verts[vi++]=y0;
					verts[vi++]=z1;
					verts[vi++]=u0;
					verts[vi++]=v1;
					verts[vi++]=x1;
					verts[vi++]=y0;
					verts[vi++]=z1;
					verts[vi++]=u1;
					verts[vi++]=v1;
					verts[vi++]=x1;
					verts[vi++]=y0;
					verts[vi++]=z0;
					verts[vi++]=u1;
					verts[vi++]=v1;
					verts[vi++]=x0;
					verts[vi++]=y0;
					verts[vi++]=z0;
					verts[vi++]=u0;
					verts[vi++]=v1;
					
					indices[ii++]=b;
					indices[ii++]=(short)(b+1);
					indices[ii++]=(short)(b+2);
					indices[ii++]=(short)(b+2); 
					indices[ii++]=(short)(b+3);
					indices[ii++]=b;
				}
			}
		}
		// copia apenas a parte usada dos arrays
		float[] vertsFinais = new float[vi];
		short[] indicesFinais = new short[ii];
		System.arraycopy(verts, 0, vertsFinais, 0, vi);
		System.arraycopy(indices, 0, indicesFinais, 0, ii);

		int totalVertices = vi / 5;
		final Mesh malha = new Mesh(true, totalVertices, ii,
			new VertexAttribute(VertexAttributes.Usage.Position,             3, "a_position"),
			new VertexAttribute(VertexAttributes.Usage.TextureCoordinates,   2, "a_texCoord0")
		);
		malha.setVertices(vertsFinais);
		malha.setIndices(indicesFinais);

		final Material mat = new Material(TextureAttribute.createDiffuse(tex.getTexture()));

		final ModelBuilder mb = new ModelBuilder();
		mb.begin();
		mb.part("item", malha, GL20.GL_TRIANGLES, 0, ii, mat);
		final Model modelo = mb.end();

		modelosItens.put(item, modelo);
		return new ModelInstance(modelo);
	}

	public static void liberar() {
		for(SceneAsset m : modelosGltf.values()) {
			m.scene.model.dispose();
			m.dispose();
		}
		modelosGltf.clear();
		for(Model m : modelosItens.values()) m.dispose();
		modelosItens.clear();
	}
}

