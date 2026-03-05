package com.minimine.mundo.geracao;

import com.minimine.utils.ruidos.OpenSimplex2;
import com.minimine.utils.ruidos.CristaRuido2D;
import com.minimine.utils.ruidos.CelularRuido2D;
/*
 * sementes derivadas por XOR de constantes distintas
 * cada campo do mundo tem sua propria semente -> campos independentes, sem correlação
 * as instancias de CristaRuido2D e CelularRuido2D ainda existem porque são diferentes
 */
public final class ContextoGeracao {
    // sementes derivadas, cada uma representa um campo independente
    public final long semente;
    public final long sementeRuido;
    public final long sementeCrista;
    public final long sementeRuido3d;
    public final long sementeCavernas;
    public final long sementeCavernasProfundas;
    public final long sementeRioRuido;
    public final long sementeRioDesvio;

    public final DominioDeformacao dominio;
    public final CristaRuido2D crista;
    public final CelularRuido2D celular;
    public final ErosaoHidraulica erosao;

    public ContextoGeracao(long semente) {
        this.semente = semente;

        // sementes derivadas
        this.sementeRuido = semente ^ 0x9E3779B9L;
        this.sementeCrista = semente ^ 0x5DEECE66DL;
        this.sementeRuido3d = semente ^ 0x61C88647L;
        this.sementeCavernas = semente ^ 0x1A2B3C4DL;
        this.sementeCavernasProfundas = semente ^ 0x9F8E7D6CL;
        this.sementeRioRuido = semente ^ 0xDEADBEEF12345678L;
        this.sementeRioDesvio = semente ^ 0xCAFEBABE87654321L;

        // dominio e erosão ainda são instâncias, tem estado proprio(domínio usa 3 campos internos)
        this.dominio = new DominioDeformacao(semente);
        this.crista = new CristaRuido2D(sementeCrista);
        this.celular = new CelularRuido2D(semente ^ 0x4F3C2B1AL);
        this.erosao = new ErosaoHidraulica(semente, dominio);
    }
}

