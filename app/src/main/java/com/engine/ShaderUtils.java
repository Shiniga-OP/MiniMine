package com.engine;
import android.opengl.GLES30;
import android.content.Context;
import java.io.InputStream;

public class ShaderUtils {
    public int id;

    public ShaderUtils(String vert, String frag) {
        int vs = compilar(GLES30.GL_VERTEX_SHADER, vert);
        int fs = compilar(GLES30.GL_FRAGMENT_SHADER, frag);
        id = GLES30.glCreateProgram();
        GLES30.glAttachShader(id, vs);
        GLES30.glAttachShader(id, fs);
        GLES30.glLinkProgram(id);
        int[] status = new int[1];
        GLES30.glGetProgramiv(id, GLES30.GL_LINK_STATUS, status, 0);
        if(status[0] == 0) {
            String log = GLES30.glGetProgramInfoLog(id);                
            System.out.println("erro ao linkar programa:\n" + log);
        }
        GLES30.glDeleteShader(vs);
        GLES30.glDeleteShader(fs);
    }

    public int compilar(int tipo, String fonte) {
        int shader = GLES30.glCreateShader(tipo);
        GLES30.glShaderSource(shader, fonte);
        GLES30.glCompileShader(shader);
        int[] status = new int[1];
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, status, 0);
        if(status[0] == 0) {
            String log = GLES30.glGetShaderInfoLog(shader);
            System.out.println("erro compilando shader: " + log);
        }
        return shader;
    }
	
	public static String lerShaderDoRaw(Context ctx, int resId) {
        try {
            InputStream is = ctx.getResources().openRawResource(resId);
            byte[] buffer = new byte[is.available()];
            is.read(buffer);
            is.close();
            return new String(buffer);
        } catch(Exception e) {
            return null;
        }
    }

    public void usar() {
        GLES30.glUseProgram(id);
    }

    public static String obterVert2D() {
        return
            "#version 300 es\n"+ 
            "layout(location = 0) in vec2 aPos;\n"+
            "layout(location = 1) in vec2 aTex;\n"+
            "uniform mat4 uProjecao;\n"+
            "out vec2 vTex;\n"+
            "void main() {\n"+
            "gl_Position = uProjecao * vec4(aPos, 0.0, 1.0);\n"+
            "vTex = aTex;\n"+
            "}";
    }

    public static String obterFrag2D() {
		return
			"#version 300 es\n"+
			"precision mediump float;\n"+
			"in vec2 vTex;\n"+
			"uniform sampler2D uTextura;\n"+
			"out vec4 fragCor;\n"+
			"void main() {\n"+
			"vec4 cor = texture(uTextura, vTex);\n"+
			"if(cor.a < 0.01) discard;\n"+  // ignora pixels totalmente transparentes
			"fragCor = cor;\n"+
			"}";
	}

	public static String obterVert3D() {
		return
			"#version 300 es\n"+
			"layout(location = 0) in vec3 aPos;\n"+
			"layout(location = 1) in vec2 aTex;\n"+
			"uniform mat4 uMVP;\n"+
			"out vec2 vTex;\n"+
			"void main() {\n"+
			"gl_Position = uMVP * vec4(aPos, 1.0);\n"+
			"vTex = aTex;\n"+
			"}";
	}

	public static String obterFrag3D() {
		return
			"#version 300 es\n"+
			"precision mediump float;\n"+
			"in vec2 vTex;\n"+
			"uniform sampler2D uTextura;\n"+
			"out vec4 fragCor;\n"+
			"void main() {\n"+
			"vec4 cor = texture(uTextura, vTex);\n"+
			"if(cor.a < 0.5) discard;\n"+
			"fragCor = cor;\n"+
			"}";
	}
}
