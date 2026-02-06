package com.microinterface;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;

public class Inicio extends Game {
    @Override
    public void create() {
        setScreen(new TelaDemo());
    }

    @Override
    public void render() {
        super.render(); 
    }
}
