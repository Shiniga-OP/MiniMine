package com.minimine.cenas;

import com.minimine.graficos.Texturas;
import com.minimine.ui.InterUtil;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.minimine.Cenas;
import com.minimine.utils.ArquivosUtil;
import com.minimine.Inicio;
import com.minimine.ui.UI;

public class Intro implements Screen {
	public PerspectiveCamera camera;
    public ModelBatch mb;
    public Environment ambiente;
    public ModelInstance clone;
	public CharSequence mensagem = "Carregando";

	public SpriteBatch sb;
    public BitmapFont fonte;

    public float cuboX = 0;
    public float cuboY = 0;
	public int contagem = 0;

    // camera
    public float yaw = 180f, tom = -20f;
    public int telaV, telaH, frame = 0, frame2 = 0;

	// sistema de variações
	public int variacao;
	public float tempoDecorrido = 0, alfa = 1f;

	// textura 2D
	public Sprite texturaToda;

	// esferas bolhas
	public class Bolha {
		ModelInstance modelo;
		Vector3 posicao;
		float velocidade;
		float oscilacao;
	}
	public Bolha[] bolhas;
	public boolean explosaoIniciada = false;

    @Override
    public void show() {
		telaV = Gdx.graphics.getWidth();
		telaH = Gdx.graphics.getHeight();

		variacao = Math.random() > 0.5 ? MathUtils.random(0, 2) : MathUtils.random(0, 2);

        camera = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		
		camera.position.set(10f, 6f, 20f);
		camera.lookAt(0, 0, 0);
		
		camera.near = 0.1f;
		camera.far = 300f;
		camera.update();

		ambiente = new Environment();  
		ambiente.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));  
		ambiente.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));  

        mb = new ModelBatch();

		Texture cuboTextura = new Texture(Gdx.files.internal("ui/focado.png"));
		Material material = new Material(new TextureAttribute(TextureAttribute.Diffuse, cuboTextura));
		ModelBuilder mb2 = new ModelBuilder();

		if(variacao == 0) {
			// cubo girando
			Model modelo = mb2.createBox(5f, 5f, 5f, material, Usage.Position | Usage.Normal | Usage.TextureCoordinates);
			clone = new ModelInstance(modelo);
			clone.transform.translate(10, 0, 0);
		} else if(variacao == 1) {
			variacao = 0;
			Model modelo = mb2.createBox(5f, 5f, 5f, material, Usage.Position | Usage.Normal | Usage.TextureCoordinates);
			clone = new ModelInstance(modelo);
			clone.transform.translate(10, 0, 0);
		} else if(variacao == 2) {
			// explosão de esferas
			Model modelo = mb2.createBox(5f, 5f, 5f, material, Usage.Position | Usage.Normal | Usage.TextureCoordinates);
			clone = new ModelInstance(modelo);
			clone.transform.translate(10, 0, 0);

			// cria esferas(serão usadas apos explosão)
			bolhas = new Bolha[50];
			
			for(int i = 0; i < bolhas.length; i++) {
				bolhas[i] = new Bolha();
				float tam = MathUtils.random(0.1f, 0.7f);
				bolhas[i].modelo = new ModelInstance(mb2.createSphere(tam, tam, tam, 10, 10, material, Usage.Position | Usage.Normal | Usage.TextureCoordinates));
				bolhas[i].posicao = new Vector3(5, 0, 0);
				bolhas[i].velocidade = MathUtils.random(2f, 20f);
				bolhas[i].oscilacao = MathUtils.random(0f, 360f);
			}
		}
		sb = new SpriteBatch();
        fonte = InterUtil.carregarFonte("ui/fontes/pixel.ttf", 30);
		
		Menu.procurarAtt();
    }

    @Override  
	public void render(float delta) {  
		tempoDecorrido += delta;

		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
		
		if(variacao == 0) {
			renderVariacao1(delta);
		} else if(variacao == 1) {
			renderVariacao1(delta);
		} else if(variacao == 2) {
			renderVariacao3(delta);
		}
		sb.begin();  
		fonte.draw(sb, "100/"+contagem+"%",Gdx.graphics.getWidth() / 2f, Gdx.graphics.getHeight() / 1.5f);  
		fonte.draw(sb, mensagem,Gdx.graphics.getWidth() / 2.5f, Gdx.graphics.getHeight() / 2);  
		sb.end();

		if(frame % 10 == 0) {
			if(mensagem.equals("Carregando")) mensagem = "Carregando.";
			else if(mensagem.equals("Carregando.")) mensagem = "Carregando..";
			else if(mensagem.equals("Carregando..")) mensagem = "Carregando...";
			else mensagem = "Carregando";

			if(contagem >= 100) {
				Inicio.defTela(Cenas.menu);
			}
		}
		if(frame2 % 1 == 0) {
			contagem++;
			alfa -= 0.01f;
		}
		frame++;
		frame2++;
	}

	// cubo girando
	public void renderVariacao1(float delta) {
		UI.attCamera(camera.direction, yaw, tom);
		camera.up.set(0, 1, 0);
		camera.update();  

		clone.transform.rotate(Vector3.Y, 50 * delta);  

		mb.begin(camera);  
		mb.render(clone, ambiente);  
		mb.end();
	}
	
	// cubo explode em bolhas
	public void renderVariacao3(float delta) {
		UI.attCamera(camera.direction, yaw, tom);
		camera.up.set(0, 1, 0);
		camera.update();

		// explodir apos 2 segundos
		if(tempoDecorrido > 1f && !explosaoIniciada) {
			explosaoIniciada = true;

			// da direções aleatorias pra cada bolha
			for(Bolha bolha : bolhas) {
				float angulo = MathUtils.random(0f, 360f);
				float raio = MathUtils.random(1f, 10f);
				bolha.posicao.x = 10 + MathUtils.cosDeg(angulo) * raio;
				bolha.posicao.z = MathUtils.sinDeg(angulo) * raio;
			}
		}
		mb.begin(camera);

		if(!explosaoIniciada) {
			// mostra cubo girando
			clone.transform.rotate(Vector3.Y, 50 * delta);
			mb.render(clone, ambiente);
		} else {
			// mostra bolhas subindo
			for(Bolha bolha : bolhas) {
				// sube
				bolha.posicao.y += bolha.velocidade * delta;

				// oscilação lateral
				bolha.oscilacao += 100 * delta;
				float deslocamentoX = MathUtils.sinDeg(bolha.oscilacao) * 0.5f;

				bolha.modelo.transform.setToTranslation(
					bolha.posicao.x + deslocamentoX,
					bolha.posicao.y,
					bolha.posicao.z
				);
				mb.render(bolha.modelo, ambiente);
			}
		}
		mb.end();
	}

    @Override
    public void resize(int v, int h) {
        camera.viewportWidth = v;
        camera.viewportHeight = h;
        camera.update();

        sb.getProjectionMatrix().setToOrtho2D(0, 0, v, h);
		Gdx.gl.glViewport(0, 0, v, h);
    }

    @Override
    public void dispose() {
        mb.dispose();
        sb.dispose();
    }
    @Override public void hide() {
		dispose();
	}
    @Override public void pause() {}
    @Override public void resume() {};
}

