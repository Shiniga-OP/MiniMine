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
    public float yaw = 180f;
    public float tom = -20f;

    public int telaV;
    public int telaH;
	public int frame = 0, frame2 = 0;

    @Override
    public void show() {
		telaV = Gdx.graphics.getWidth();
		telaH = Gdx.graphics.getHeight();

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
		Model modelo = mb2.createBox(5f, 5f, 5f, material, Usage.Position | Usage.Normal | Usage.TextureCoordinates);

        clone = new ModelInstance(modelo);
		
		clone.transform.translate(10, 0, 0);
		
		sb = new SpriteBatch();
        fonte = InterUtil.carregarFonte("ui/fontes/pixel.ttf", 30);
    }

    @Override  
	public void render(float delta) {  
		Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());  
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);  

		UI.attCamera(camera, yaw, tom);

		camera.update();  

		clone.transform.rotate(Vector3.Y, 50 * delta);  

		mb.begin(camera);  
		mb.render(clone, ambiente);  
		mb.end();  

		sb.begin();  
		
		fonte.draw(sb, "100/"+contagem+"%",Gdx.graphics.getWidth() / 2f, Gdx.graphics.getHeight() / 1.5f);  
		fonte.draw(sb, mensagem,Gdx.graphics.getWidth() / 2.5f, Gdx.graphics.getHeight() / 2);  
		
		if(frame % 10 == 0) {
			if(mensagem.equals("Carregando")) mensagem = "Carregando.";
			else if(mensagem.equals("Carregando.")) mensagem = "Carregando..";
			else if(mensagem.equals("Carregando..")) mensagem = "Carregando...";
			else mensagem = "Carregando";
			
			if(contagem >= 100) {
				Inicio.defTela(Cenas.menu);
			}
		}
		if(frame2 % 60 == 0) {
			contagem++;
		}
		frame++;
		sb.end();  
	}

    @Override
    public void resize(int v, int h) {
        camera.viewportWidth = v;
        camera.viewportHeight = h;
        camera.update();

        sb.getProjectionMatrix().setToOrtho2D(0, 0, v, h);
    }

    @Override
    public void dispose() {
        mb.dispose();
        sb.dispose();
        fonte.dispose();
    }
    @Override public void hide() {}
    @Override public void pause() {}
    @Override public void resume() {};
}
