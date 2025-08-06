package com.minimine;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.content.Intent;
import android.widget.EditText;
import java.util.Random;

public class InicioActivity extends Activity {
	private EditText seed, tipoMundo, pacoteTex, nomeMundo;
	private Random aleatorio = new Random();
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mundo);
		
		pacoteTex = findViewById(R.id.pacoteTex);
		seed = findViewById(R.id.seed);
		tipoMundo = findViewById(R.id.tipoMundo);
		nomeMundo = findViewById(R.id.nomeMundo);
		
		Intent dados = getIntent();
		if(dados.getBooleanExtra("dev", false)) paraJogo(null);
    }
	
	public void paraJogo(View v) {
		Intent mundo = new Intent(this, MundoActivity.class);
		
		if(!seed.getText().toString().equals("")) mundo.putExtra("seed", Integer.parseInt(seed.getText().toString()));
		else {
			mundo.putExtra("seed", aleatorio.nextInt(99)+aleatorio.nextInt(999)+aleatorio.nextInt(9999)+aleatorio.nextInt(99999));
		}
		if(!pacoteTex.getText().toString().equals("")) mundo.putExtra("pacoteTex", pacoteTex.getText().toString());
		else {
			mundo.putExtra("pacoteTex", "evolva");
		}
		if(!tipoMundo.getText().toString().equals("")) mundo.putExtra("tipoMundo", tipoMundo.getText().toString());
		else {
			mundo.putExtra("tipoMundo", "normal");
		}
		if(!nomeMundo.getText().toString().equals("")) mundo.putExtra("nomeMundo", nomeMundo.getText().toString());
		else {
			mundo.putExtra("nomeMundo", "novo mundo");
		}
		startActivity(mundo);
	}
}
