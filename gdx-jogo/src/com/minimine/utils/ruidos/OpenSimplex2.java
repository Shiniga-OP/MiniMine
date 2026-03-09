package com.minimine.utils.ruidos;
/*
 * OpenSimplex2S(SuperSimplex), variante borrada
 * implementação estatica, semente passada em cada chamada, sem estado, sem alocação
 
 * por que OpenSimplex2S e não OpenSimplex2F:
 *   2F é mais rapido mas produz bordas mais duras, bom pra texturas tecnicas
 *   2S usa kernel maior(raio 3/4 vs 2/3), resultado mais suave e isotropico
 *   pra terreno, 2S é sempre preferivel
 
 * 2D: Simplex modificado com kernel maior e tabela de 24 gradientes
 *       resolve o problema de repetição direcional do Simplex original
 
 * 3D, re-orientado 8-ponto BCC ruido
 *       constroi um lattice BCC como união de dois lattices cubicos rotacionados
 *       muito mais isotropico que Simplex 3D em qualquer orientação de corte
 *       dois sabores:
 *         ruido3XY: planos XY são primarios(bom para altura + tempo)
 *         ruido3XZ: planos XZ são primarios(bom para terreno: X/Z horizontal, Y vertical)
 
 * normalização:
 *   ruido2 retorna aproximadamente [-1, 1]
 *   ruido3 retorna aproximadamente [-1, 1]
 *   "aproximadamente" porque simplex não tem envelope exato
 */
public final class OpenSimplex2 {
    // === CONSTANTES ===

    // 2D: fator de skew e de-skew do triangulo para o simplex
    public static final float SKEW2 = 0.366025403784438646f; // (sqrt(3) - 1) / 2
    public static final float UNSKEW2 = 0.211324865405187117f; // (3 - sqrt(3)) / 6

    // 2D: raio do kernel ao quadrado, OpenSimplex2S usa 9/4(maior que Simplex padrão 1/2)
    // raio maior -> transição mais suave entre contribuições de vertice
    public static final float KERNEL2 = 0.5625f; // (3/4)^2

    // normalização 2D, escala o resultado para ficar proximo de [-1, 1]
    public static final float NORM2   = 47.0f;

    // 3D: rotação BCC(1/sqrt(3) - 1) / 3 e 1/sqrt(3) * 2/3
    public static final float ROTATE3 = -0.16666666666666666f; // (1/sqrt(3) - 1) / 3
    public static final float FALLBACK3 = 0.6666666666666666f; // 2/3

    // 3D: raio do kernel ao quadrado
    public static final float KERNEL3 = 0.6f;

    // normalização 3D
    public static final float NORM3   = 32.0f;

    // ===TABELAS DE GRADIENTE ===
    // 2D: 24 gradientes unitarios distribuidos simetricamente no circulo
    // 24 direções em vez de 12 -> reduz repetição visual em ruido com limite
    // cada par(gx, gy) representa uma direção unitaria
    public static final float[] GRAD2 = {
		0.130526192220052f,  0.99144486137381f,
		0.38268343236509f,   0.923879532511287f,
		0.608761429008721f,  0.793353340291235f,
		0.793353340291235f,  0.608761429008721f,
		0.923879532511287f,  0.38268343236509f,
		0.99144486137381f,   0.130526192220051f,
		0.99144486137381f,  -0.130526192220051f,
		0.923879532511287f, -0.38268343236509f,
		0.793353340291235f, -0.608761429008721f,
		0.608761429008721f, -0.793353340291235f,
		0.38268343236509f,  -0.923879532511287f,
		0.130526192220052f, -0.99144486137381f,
        -0.130526192220052f, -0.99144486137381f,
        -0.38268343236509f,  -0.923879532511287f,
        -0.608761429008721f, -0.793353340291235f,
        -0.793353340291235f, -0.608761429008721f,
        -0.923879532511287f, -0.38268343236509f,
        -0.99144486137381f,  -0.130526192220052f,
        -0.99144486137381f,   0.130526192220051f,
        -0.923879532511287f,  0.38268343236509f,
        -0.793353340291235f,  0.608761429008721f,
        -0.608761429008721f,  0.793353340291235f,
        -0.38268343236509f,   0.923879532511287f,
        -0.130526192220052f,  0.99144486137381f,
    };
    // 3D: 24 gradientes nas arestas do cubo com comprimento uniforme
    // dois tipos de vetor: arestas longas(±1, ±1, 0) e curtas(0, ±1, ±1)
    // a correção de comprimento ta embutida na normalização
    public static final float[] GRAD3 = {
		0f,  1f,  1f,   0f,  1f, -1f,   0f, -1f,  1f,   0f, -1f, -1f,
		1f,  0f,  1f,   1f,  0f, -1f,  -1f,  0f,  1f,  -1f,  0f, -1f,
		1f,  1f,  0f,   1f, -1f,  0f,  -1f,  1f,  0f,  -1f, -1f,  0f,
		0f,  1f,  1f,   0f,  1f, -1f,   0f, -1f,  1f,   0f, -1f, -1f,
		1f,  0f,  1f,   1f,  0f, -1f,  -1f,  0f,  1f,  -1f,  0f, -1f,
		1f,  1f,  0f,   1f, -1f,  0f,  -1f,  1f,  0f,  -1f, -1f,  0f,
    };
	
    // === HASH ===
    // hash de bits rapido, avalanche completo
    // combina semente com coordenadas do lattice de forma independente por eixo
    public static int hash(long semente, int xp, int yp) {
        long h = semente ^ ((long)xp * 0x5205402B9270C86FL) ^ ((long)yp * 0x598CD327003817B5L);
        h = (h ^ (h >>> 33)) * 0xff51afd7ed558ccdL;
        h = (h ^ (h >>> 33)) * 0xc4ceb9fe1a85ec53L;
        return (int)(h ^ (h >>> 33));
    }

    public static int hash(long semente, int xp, int yp, int zp) {
        long h = semente ^ ((long)xp * 0x5205402B9270C86FL)
			^ ((long)yp * 0x598CD327003817B5L)
			^ ((long)zp * 0x4A8C4E8E1E3F6B29L);
        h = (h ^ (h >>> 33)) * 0xff51afd7ed558ccdL;
        h = (h ^ (h >>> 33)) * 0xc4ceb9fe1a85ec53L;
        return (int)(h ^ (h >>> 33));
    }
    /*
     * ruido 2D OpenSimplex2S
     * retorna valor aproximadamente em [-1, 1]
     * kernel estendido(raio 3/4) para suavidade maior
     */
    public static float ruido2(long semente, float x, float y) {
        // transforma para coordenadas do lattice simplex
        float skew = (x + y) * SKEW2;
        int i = floorRapido(x + skew);
        int j = floorRapido(y + skew);

        // coordenadas dentro da célula skewed
        float unskew = (i + j) * UNSKEW2;
        float x0 = x - (i - unskew);
        float y0 = y - (j - unskew);

        float valor = 0.0f;

        // contribuição dos vertices do triangulo
        // OpenSimplex2S avalia 4 vertices(não 3 como Simplex) pro kernel maior
        // vertice 0: sempre(i, j)
        valor += contribuicao2(semente, i, j, x0, y0);

        // vertice 1 e 2: os dois da segunda metade do losango
        float x1 = x0 - 1.0f + UNSKEW2;
        float y1 = y0 + UNSKEW2;
        float x2 = x0 + UNSKEW2;
        float y2 = y0 - 1.0f + UNSKEW2;

        valor += contribuicao2(semente, i + 1, j,     x1, y1);
        valor += contribuicao2(semente, i,     j + 1, x2, y2);

        // vertice 3: canto oposto do losango
        float x3 = x0 - 1.0f + 2.0f * UNSKEW2;
        float y3 = y0 - 1.0f + 2.0f * UNSKEW2;
        valor += contribuicao2(semente, i + 1, j + 1, x3, y3);

        return valor * NORM2;
    }

    public static float contribuicao2(long semente, int xi, int yi, float dx, float dy) {
        float t = KERNEL2 - dx * dx - dy * dy;
        if(t <= 0.0) return 0.0f;
        t *= t;
        // indice no array de gradientes: hash -> [0, 24) -> *2 para pegar par(gx, gy)
        int gi = (hash(semente, xi, yi) & 23) * 2;
        return t * t * (GRAD2[gi] * dx + GRAD2[gi | 1] * dy);
    }
    /*
     * ruido 3D com planos XZ primarios, recomendado para terreno
     * X e Z são as coordenadas horizontais, Y é vertical(ou tempo)
     * cortes horizontais(Y fixo) tem maxima isotropia
     * uso típico: ruido3XZ(semente, x * escala, y * escalaY, z * escala)
     */
    public static float ruido3XZ(long semente, float x, float y, float z) {
        // rotação que alinha o eixo Y com a diagonal principal do BCC
        // e mantém XZ no plano de melhor isotropia
        float xz = x + z;
        float s2 = xz * ROTATE3;
        float yr = y * FALLBACK3;
        float xr = x + s2 + yr;
        float zr = z + s2 + yr;
        float yr2 = xz * (-FALLBACK3) + yr;

        return ruido3Base(semente, xr, yr2, zr);
    }
    /*
     * ruido 3D com planos XY primarios
     * util pra animações onde Z é o tempo, ou cavernas onde Y/Z são primarios
     */
    public static float ruido3XY(long semente, float x, float y, float z) {
        float xy = x + y;
        float s2 = xy * ROTATE3;
        float zr = z * FALLBACK3;
        float xr = x + s2 + zr;
        float yr = y + s2 + zr;
        float zr2 = xy * (-FALLBACK3) + zr;

        return ruido3Base(semente, xr, yr, zr2);
    }
    /*
     * base do ruido 3D, re-orientado 8-ponto BCC
     * recebe coordenadas ja rotacionadas
     * o lattice BCC é construido como dois lattices cubicos posição por(0.5, 0.5, 0.5)
     * pra cada lattice, encontra os 4 vertices mais proximos
     * total: 8 vertices contribuindo mais do que Simplex 3D(4 vertices)
     * resultado: muito mais isotropico, menos artefatos direcionais
     */
    public static float ruido3Base(long semente, float xr, float yr, float zr) {
        // celula base no lattice
        int xrb = floorRapido(xr);
        int yrb = floorRapido(yr);
        int zrb = floorRapido(zr);

        // fração dentro da celula
        float xri = xr - xrb;
        float yri = yr - yrb;
        float zri = zr - zrb;

        // determinamos quais dos 8 vertices do cubo contribuem
        // baseado na proximidade ao longo de cada eixo
        float xNeg = xri - 0.5f;
        float yNeg = yri - 0.5f;
        float zNeg = zri - 0.5f;

        float valor = 0.0f;

        // primeiro half-lattice: 4 vertices
        valor += contribuicao3(semente, xrb, yrb, zrb, xri,  yri,  zri );
        valor += contribuicao3(semente, xrb + 1, yrb, zrb, xNeg, yri, zri );
        valor += contribuicao3(semente, xrb, yrb + 1, zrb, xri, yNeg, zri );
        valor += contribuicao3(semente, xrb, yrb, zrb + 1, xri, yri, zNeg);

        // segundo half-lattice: posição por(0.5, 0.5, 0.5) no espaço rotacionado
        // semente diferente para independencia entre os dois lattices
        long semente2 = semente ^ 0x6A09E667F3BCC909L;
        valor += contribuicao3(semente2, xrb + 1, yrb + 1, zrb, xNeg, yNeg, zri );
        valor += contribuicao3(semente2, xrb + 1, yrb, zrb + 1, xNeg, yri, zNeg);
        valor += contribuicao3(semente2, xrb, yrb + 1, zrb + 1, xri,  yNeg, zNeg);
        valor += contribuicao3(semente2, xrb + 1, yrb + 1, zrb + 1, xNeg, yNeg, zNeg);

        return valor * NORM3;
    }

    public static float contribuicao3(long semente, int xi, int yi, int zi, float dx, float dy, float dz) {
        float t = KERNEL3 - dx * dx - dy * dy - dz * dz;
        if(t <= 0.0f) return 0.0f;
        t *= t;
        int gi = (hash(semente, xi, yi, zi) & 23) * 3;
        return t * t * (GRAD3[gi] * dx + GRAD3[gi + 1] * dy + GRAD3[gi + 2] * dz);
    }
    /*
     * fBm 2D
     * soma oitavas de ruido com frequencia e amplitude crescentes/decrescentes
     * persistencia: quanto cada oitava contribui(tipicamente 0.5)
     * lacunaridade: quanto a frequencia aumenta por oitava(tipicamente 2.0)
     */
    public static float ruido2Fractal(long semente, float x, float y, int oitavas, float persistencia, float lacunaridade) {
        float total = 0.0f;
        float amplitude = 1.0f;
        float frequencia = 1.0f;
        float maximo = 0.0f;

        for(int i = 0; i < oitavas; i++) {
            total += ruido2(semente, x * frequencia, y * frequencia) * amplitude;
            maximo += amplitude;
            // sementes diferentes por oitava evitam correlação entre camadas
            semente      = semente * 6364136223846793005L + 1442695040888963407L;
            amplitude *= persistencia;
            frequencia *= lacunaridade;
        }
        return total / maximo;
    }
	
    // fBm 3D XZ para terreno
    public static float ruido3XZFractal(long semente, float x, float y, float z, int oitavas, float persistencia, float lacunaridade) {
        float total = 0.0f;
        float amplitude = 1.0f;
        float frequencia = 1.0f;
        float maximo = 0.0f;

        for(int i = 0; i < oitavas; i++) {
            total += ruido3XZ(semente, x * frequencia, y * frequencia, z * frequencia) * amplitude;
            maximo += amplitude;
            semente  = semente * 6364136223846793005L + 1442695040888963407L;
            amplitude *= persistencia;
            frequencia *= lacunaridade;
        }
        return total / maximo;
    }
	
    // fBm 3D XY para cavernas e volumes
    public static float ruido3XYFractal(long semente, float x, float y, float z, int oitavas, float persistencia, float lacunaridade) {
        float total = 0.0f;
        float amplitude = 1.0f;
        float frequencia = 1.0f;
        float maximo = 0.0f;

        for(int i = 0; i < oitavas; i++) {
            total += ruido3XY(semente, x * frequencia, y * frequencia, z * frequencia) * amplitude;
            maximo += amplitude;
            semente = semente * 6364136223846793005L + 1442695040888963407L;
            amplitude *= persistencia;
            frequencia *= lacunaridade;
        }
        return total / maximo;
    }

	// util:
    public static int floorRapido(float x) {
        int xi = (int)x;
        return x < xi ? xi - 1 : xi;
    }
}

