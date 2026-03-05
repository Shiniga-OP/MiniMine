package com.minimine.mundo.geracao;

import com.minimine.utils.ruidos.Simplex2D;
import com.minimine.utils.ruidos.Simplex3D;
import com.minimine.utils.ruidos.CristaRuido2D;
import com.minimine.utils.ruidos.CelularRuido2D;

// todos os geradores de ruido do mundo
public final class ContextoGeracao {
    public final DominioDeformacao dominio;
    public final CristaRuido2D crista;
    public final Simplex2D ruido;
    public final Simplex3D ruido3d;
    public final Simplex3D cavernas;
    public final Simplex3D cavernasProfundas;
    public final Simplex2D rioRuido;
    public final Simplex2D rioDesvio;
    public final CelularRuido2D celular;
    public final ErosaoHidraulica erosao;

    public ContextoGeracao(long semente) {
        this.dominio = new DominioDeformacao(semente);
        this.crista = new CristaRuido2D(semente ^ 0x5DEECE66DL);
        this.ruido = new Simplex2D(semente ^ 0x9E3779B9L);
        this.ruido3d = new Simplex3D(semente ^ 0x61C88647L);
        this.cavernas = new Simplex3D(semente ^ 0x1A2B3C4DL);
        this.cavernasProfundas = new Simplex3D(semente ^ 0x9F8E7D6CL);
        this.celular = new CelularRuido2D(semente ^ 0x4F3C2B1AL);
        this.rioRuido = new Simplex2D(semente ^ 0xDEADBEEF12345678L);
        this.rioDesvio = new Simplex2D(semente ^ 0xCAFEBABE87654321L);
        this.erosao = new ErosaoHidraulica(semente, 512, 8.0);
        this.erosao.simularGotas(500, this.dominio);
    }
}

