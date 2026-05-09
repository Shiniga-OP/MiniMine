package com.minimine.mundo.geracao;

import com.minimine.mundo.chunks.Chunk;
import com.minimine.mundo.Chave;
import com.minimine.mundo.Mundo;
import com.minimine.utils.ruidos.OpenSimplex2;
import com.minimine.mundo.blocos.Bloco;
import com.minimine.mundo.chunks.ChunkProcesso;
/*
 * orquestrador de geração de chunk
 * thread-segura: toda a geração opera sobre ContextoGeracao local por thread
 
 * MotorGeracao, TerranoBase e GeradorRios são imutaveis após construção
 * contem apenas parametros e sementes
 * multiplas threads podem chamar gerarChunk() simultaneamente sem concorrencia
 * porque cada chamada usa seu proprio ContextoGeracao via ThreadLocal
 
 * processo de estados:
 *   0 -> 1  gerarChunk: terreno + biomas + agua + vegetacao(so escrita local)
 *   1 -> 2  colocarEstruturas: estruturas proprias + aplica pendentes recebidas
 *  vizinha estado 1 -> escreve direto
 *  vizinha outro est -> enfileira em Mundo.filaEstrutura
 *   2 -> 3  calcularLuz:  propagação de luz
 *   3 -> 4  gerarMalha: malha de renderização
*/
public final class MotorGeracao {
    public static final int NIVEL_MAR = 62;

    public final long semente;
    public final TerranoBase terreno;
    public final GeradorRios rios;
    public final GeradorTuneis tuneis;
    public final RegistroBiomas registro;

    // parametros de ruidos de bioma, imutaveis, compartilhaveis
    public final long semCalor, semUmidade, sempreenchimento;
    public final float espalharCalor, espalharUmidade, espalharPreen;
    public final int octCalor, octUmidade, octPreen;
    public final float perCalor, perUmidade, perPreen;
    public final float escalaPreen;

    // ContextoGeracao por thread, elimina alocação por chunk e concorrencia
    public final ThreadLocal<ContextoGeracao> ctxLocal = new ThreadLocal<ContextoGeracao>() {
        @Override protected ContextoGeracao initialValue() { return new ContextoGeracao(Mundo.Y_CHUNK); }
    };
    public static int PEDRA, AGUA;

    public MotorGeracao(long semente, RegistroBiomas registro) {
        this.semente = semente;
        this.registro = registro;
        this.terreno = new TerranoBase(semente, NIVEL_MAR);
        this.rios = new GeradorRios(semente, NIVEL_MAR);
        this.tuneis = new GeradorTuneis(semente);

        semCalor = semente ^ 0xCAFEBABE87654321L;
        semUmidade = semente ^ 0x4F3C2B1A9E8D7C6BL;
        sempreenchimento = semente ^ 0xB3A4C5D6E7F80192L;
        espalharCalor = 1000f;
		octCalor = 3;
		perCalor = 0.5f;
        espalharUmidade = 1000f;
		octUmidade = 3;
		perUmidade = 0.5f;
        espalharPreen = 150f;
		octPreen = 3;
		perPreen = 0.7f;
        escalaPreen = 1.2f;

        PEDRA = Bloco.texIds.get("pedra").tipo;
        AGUA  = Bloco.texIds.get("agua").tipo;
    }

    // ESTADO 0 -> 1: terreno + biomas + água + vegetação
    public void gerarChunk(Chunk chunk) {
        final int chunkX = chunk.x << 4;
        final int chunkZ = chunk.z << 4;
        final ContextoGeracao ctx = ctxLocal.get();

        // === FASE 1: pré-calculo de todos os ruidos arrays ===
        terreno.calcularChunk(chunkX, chunkZ, ctx);
        rios.calcularChunk(chunkX, 0, chunkZ, Mundo.Y_CHUNK, ctx);

        calcular2D(semCalor, espalharCalor, octCalor, perCalor, 2.0f, chunkX, chunkZ, ctx.calorMapa);
        calcular2D(semUmidade, espalharUmidade, octUmidade, perUmidade, 2.0f, chunkX, chunkZ, ctx.umidadeMapa);
        calcular2Dpreenchimento(chunkX, chunkZ, ctx.preenProfMapa);

        // normaliza calor e umidade para [0,1]
        for(int i = 0; i < 16 * 16; i++) {
            ctx.calorMapa[i] = Math.max(0f, Math.min(1f, ctx.calorMapa[i] * 0.5f + 0.5f));
            ctx.umidadeMapa[i] = Math.max(0f, Math.min(1f, ctx.umidadeMapa[i] * 0.5f + 0.5f));
        }
        // === FASE 2: gerar terreno ===
        for(int z = 0; z < 16; z++) {
            for(int x = 0; x < 16; x++) {
                final int superficieY = terreno.obterAltura(x, z, ctx);
                for(int y = 0; y < Mundo.Y_CHUNK; y++) {
                    if(y <= superficieY && !rios.eCanal(x, y, z, y, superficieY, ctx)) {
                        ChunkProcesso.util.defBloco(x, y, z, PEDRA, chunk);
                    }
                }
            }
        }
        // === FASE 3: escavar tuneis ===
        tuneis.escavar(chunk, chunkX, chunkZ);

        // === FASE 4: gerar biomas ===
        for(int z = 0; z < 16; z++) {
            for(int x = 0; x < 16; x++) {
                final int idc2d = (z << 4) + x;

                int topoColuna = terreno.obterAltura(x, z, ctx);
                while(topoColuna > 0 && ChunkProcesso.util.obterBloco(x, topoColuna, z, chunk) == 0) topoColuna--;

                float calor = ctx.calorMapa[idc2d];
                final float umidade = ctx.umidadeMapa[idc2d];
                calor = Math.max(0f, Math.min(1f, calor - (float)((topoColuna - NIVEL_MAR) * 0.004)));

                final DadosBioma bioma = registro.selecionar(calor, umidade, topoColuna);
                ctx.biomaMapa[idc2d] = bioma;

                final DadosBioma.Superficie s = bioma.superficie;
                final float preenchimentoVal = Math.max(0f, ctx.preenProfMapa[idc2d]);
                final int profpreenchimento = s.profTopo + s.profSubtopo + (int)preenchimentoVal;

                int profAtual = 0;
                for(int y = topoColuna; y >= 1; y--) {
                    if(ChunkProcesso.util.obterBloco(x, y, z, chunk) == 0) {
                        if(profAtual > 0) break;
                        continue;
                    }
                    if(profAtual < s.profTopo) {
                        ChunkProcesso.util.defBloco(x, y, z, s.topo, chunk);
                    } else if(profAtual < profpreenchimento) {
                        ChunkProcesso.util.defBloco(x, y, z, s.subtopo, chunk);
                    } else {
                        ChunkProcesso.util.defBloco(x, y, z, s.interior, chunk);
                        break;
                    }
                    profAtual++;
                }
            }
        }
        // === FASE 5: preencher água ===
        for(int z = 0; z < 16; z++) {
            for(int x = 0; x < 16; x++) {
                for(int y = NIVEL_MAR; y >= 0; y--) {
                    if(ChunkProcesso.util.obterBloco(x, y, z, chunk) == 0) {
                        ChunkProcesso.util.defBloco(x, y, z, AGUA, chunk);
                        ChunkProcesso.util.defMeta(x, y, z, (short)7, chunk);
                    }
                }
            }
        }
        // === FASE 6: vegetação(1 bloco, so escrita local, seguro aqui) ===
        colocarVegetacao(chunk, chunkX, chunkZ, ctx);
        chunk.dadosProntos = true;
    }
    /*
     * coloca vegetação de 1 bloco por coluna escrita puramente local, sem acessar vizinhas
     * chamado ao fim de gerarChunk(estado 0->1)
     * precalcula topoMapa para reuso em colocarEstruturas
    */
    public void colocarVegetacao(Chunk chunk, int chunkX, int chunkZ, ContextoGeracao ctx) {
        for(int z = 0; z < 16; z++) {
            for(int x = 0; x < 16; x++) {
                int idc2d = (z << 4) + x;
                final DadosBioma bioma = ctx.biomaMapa[idc2d];
                if(bioma == null) continue;

                // calcula e guarda topo para reuso em colocarEstruturas
                int topo = Mundo.Y_CHUNK - 1;
                while(topo > 0 && ChunkProcesso.util.obterBloco(x, topo, z, chunk) == 0) topo--;
                ctx.topoMapa[idc2d] = topo;

                if(topo <= NIVEL_MAR) continue;
                final int yDec = topo + 1;
                if(yDec >= Mundo.Y_CHUNK) continue;

                long semCol = semente ^ ((chunkX + x) * 374761393L) ^ ((chunkZ + z) * 668265263L);

                DadosBioma.EntradaVegetacao[] veg = bioma.vegetacao;
                for(int i = 0; i < veg.length; i++) {
                    semCol = lcg(semCol);
                    final float r = (semCol >>> 1) / (float)(Long.MAX_VALUE);
                    if(r < veg[i].chance) {
                        ChunkProcesso.util.defBloco(x, yDec, z, veg[i].id, chunk);
                        break; // so uma vegetação por coluna
                    }
                }
            }
        }
    }

    // ESTADO 1 -> 2: estruturas
    // chamado por Mundo.processarEstruturas apos vizinhos atingirem estado >= 1
    // ctx.topoMapa e ctx.biomaMapa ja recalculados por Mundo.processarEstruturas
    public void colocarEstruturas(Chunk chunk, int chunkX, int chunkZ, ContextoGeracao ctx) {
        for(int z = 0; z < 16; z++) {
            for(int x = 0; x < 16; x++) {
                int idc2d = z * 16 + x;
                final DadosBioma bioma = ctx.biomaMapa[idc2d];
                if(bioma == null) continue;

                final int topo = ctx.topoMapa[idc2d];
                if(topo <= NIVEL_MAR) continue;
                final int yDec = topo + 1;
                if(yDec >= Mundo.Y_CHUNK) continue;

                // semente deterministica por coluna mesma derivação da vegetação
                long semCol = semente ^ ((chunkX + x) * 374761393L) ^ ((chunkZ + z) * 668265263L);

                // avança o lcg pelo numero de entradas de vegetação para manter sequencia consistente
                DadosBioma.EntradaVegetacao[] veg = bioma.vegetacao;
                for(int i = 0; i < veg.length; i++) semCol = lcg(semCol);

                final DadosBioma.EntradaEstrutura[] estr = bioma.estruturas;
                for(int i = 0; i < estr.length; i++) {
                    semCol = lcg(semCol);
                    final float r = (semCol >>> 1) / (float)(Long.MAX_VALUE);
                    if(r < estr[i].chance) {
                        if(estr[i].blocoBaixo >= 0 && ChunkProcesso.util.obterBloco(x, topo, z, chunk) != estr[i].blocoBaixo) break;
                        if(ChunkProcesso.util.obterBloco(x, yDec, z, chunk) != 0) break;
                        colocarEstrutura(estr[i], x, topo, z, chunk);
                        break;
                    }
                }
            }
        }
    }
    /*
     * coloca uma estrutura a partir da ancora(ox, oy, oz) em coordenadas locais do chunk
     
     * regra de escrita para blocos que extrapolam:
     *   vizinha em estado 1 exatamente -> escreve direto(ainda na janela de dados)
     *   vizinha em qualquer outro estado -> enfileira em Mundo.filaEstrutura
     
     * chunks modificadas pelo jogador (chunksMod) nunca recebem escrita de geração
     */
    public void colocarEstrutura(DadosBioma.EntradaEstrutura e, int ox, int oy, int oz, Chunk chunk) {
        final int posX = ox - (e.larg >> 1);
        final int posZ = oz - (e.prof >> 1);

        // pré-carrega estado e referencia das 8 vizinhas
        // indice = (dcx+1)*3+(dcz+1), dcx/dcz em {-1,0,1}, indice 4 = propria(não usado)
        final Chunk[] vizinhos = new Chunk[9];
        final boolean[] ehFila = new boolean[9]; // true = deve enfileirar, false = escreve direto
        final boolean[] vizinhoMod = new boolean[9];

        for(int dcx = -1; dcx <= 1; dcx++) {
            for(int dcz = -1; dcz <= 1; dcz++) {
                if(dcx == 0 && dcz == 0) continue;
                final int idc = (dcx + 1) * 3 + (dcz + 1);
                final long chave = Chave.calcularChave(chunk.x + dcx, chunk.z + dcz);
                vizinhoMod[idc] = Mundo.chunksMod.containsKey(chave);
                if(!vizinhoMod[idc]) {
                    final Chunk c  = Mundo.obterChunk(chave);
                    final int estado = Mundo.estados.getOrDefault(chave, 0);
                    if(c != null && estado == 1) {
                        // vizinha ainda na janela de dados: escreve direto
                        vizinhos[idc] = c;
                        ehFila[idc] = false;
                    } else {
                        // vizinha não existe ou ja passou do estado 1: enfileira
                        vizinhos[idc] = c; // pode ser null, Mundo.enfileirarEstrutura usa so a chave
                        ehFila[idc] = true;
                    }
                }
            }
        }
        for(int i = 0; i < e.lx.length; i++) {
            final int id = e.blocoIds[i];
            if(id < 0) continue;
            final int bx = posX + (e.lx[i] - e.ancX);
            final int by = oy + (e.ly[i] - e.ancY);
            final int bz = posZ + (e.lz[i] - e.ancZ);

            if(by < 0 || by >= Mundo.Y_CHUNK) continue;

            if(bx >= 0 && bx <= 15 && bz >= 0 && bz <= 15) {
                // bloco dentro da própria chunk: escreve direto sempre
                ChunkProcesso.util.defBloco(bx, by, bz, id, chunk);
                if(e.blocoMeta[i] != 0) ChunkProcesso.util.defMeta(bx, by, bz, e.blocoMeta[i], chunk);
            } else {
                final int dcx = bx < 0 ? -1 : (bx > 15 ? 1 : 0);
                final int dcz = bz < 0 ? -1 : (bz > 15 ? 1 : 0);
                final int idc = (dcx + 1) * 3 + (dcz + 1);

                // nunca escreve em chunk modificada pelo jogador
                if(vizinhoMod[idc]) continue;

                // coordenadas locais na chunk alvo
                final int gx = (chunk.x << 4) + bx;
                final int gz = (chunk.z << 4) + bz;
                final int lx = gx & 0xF;
                final int lz = gz & 0xF;

                if(ehFila[idc]) {
                    // vizinha não está em estado 1: enfileira para aplicar depois
                    Mundo.enfileirarEstrutura(
					Chave.calcularChave(chunk.x + dcx, chunk.z + dcz),
					new EstruturaPendente(lx, by, lz, id, e.blocoMeta[i])
					);
                } else {
                    // vizinha em estado 1: escreve direto
                    final Chunk alvo = vizinhos[idc];
                    if(alvo == null) continue;
                    ChunkProcesso.util.defBloco(lx, by, lz, id, alvo);
                    if(e.blocoMeta[i] != 0) ChunkProcesso.util.defMeta(lx, by, lz, e.blocoMeta[i], alvo);
                }
            }
        }
    }
	
	public static void gerarPlano(Chunk chunk) {
		final int TERRA = Bloco.texIds.get("terra").tipo;
		final int GRAMA = Bloco.texIds.get("grama").tipo;
		for(int x = 0; x < 16; x++) {
			for(int z = 0; z < 16; z++) {
				ChunkProcesso.util.defBloco(x, 0, z, PEDRA, chunk);
				ChunkProcesso.util.defBloco(x, 1, z, TERRA, chunk);
				ChunkProcesso.util.defBloco(x, 2, z, GRAMA, chunk);
			}
		}
	}

    // gerador congruente linear
    public static final long lcg(final long s) {
        return s * 6364136223846793005L + 1442695040888963407L;
    }

    // === UTIL ===
    public void calcular2D(long sem, float espalhar, int oct, float persist, float lac,
	int origemX, int origemZ, float[] saida) {
        final float freq = 1.0f / espalhar;
        for(int z = 0; z < 16; z++) {
            for(int x = 0; x < 16; x++) {
                saida[(z << 4) + x] = OpenSimplex2.ruido2Fractal(
                    sem, (origemX + x) * freq, (origemZ + z) * freq, oct, persist, lac);
            }
        }
    }

    public void calcular2Dpreenchimento(int origemX, int origemZ, float[] saida) {
        final float freq = 1.0f / espalharPreen;
        for(int z = 0; z < 16; z++) {
            for(int x = 0; x < 16; x++) {
                saida[(z << 4) + x] = escalaPreen * OpenSimplex2.ruido2Fractal(
                    sempreenchimento, (origemX + x) * freq, (origemZ + z) * freq, octPreen, perPreen, 2.0f);
            }
        }
    }

    // ponto a ponto, uso esporadico
    public final  String obterBioma(int mx, int mz) {
        final int alt = terreno.calcularAlturaPonto(mx, mz);
        final float cal = Math.max(0f, Math.min(1f,
		OpenSimplex2.ruido2Fractal(semCalor, mx / espalharCalor, mz / espalharCalor,
		octCalor, perCalor, 2.0f) * 0.5f + 0.5f - ((alt - NIVEL_MAR) * 0.004f)));
        final float umi = Math.max(0f, Math.min(1f,
		OpenSimplex2.ruido2Fractal(semUmidade, mx / espalharUmidade, mz / espalharUmidade,
		octUmidade, perUmidade, 2.0f) * 0.5f + 0.5f));
        return registro.selecionar(cal, umi, alt).nome;
    }

    public int[] localizarBioma(String chave, int origemX, int origemZ) {
        if(!registro.existe(chave)) return new int[]{0, 0};
        final int passo = 64, raioMax = 100_000;
        for(int raio = passo; raio <= raioMax; raio += passo) {
            for(int dx = -raio; dx <= raio; dx += passo) {
                for(int dz = -raio; dz <= raio; dz += passo) {
                    if(Math.abs(dx) != raio && Math.abs(dz) != raio) continue;
                    int mx = origemX + dx, mz = origemZ + dz;
                    int alt = terreno.calcularAlturaPonto(mx, mz);
                    float cal = Math.max(0f, Math.min(1f,
					OpenSimplex2.ruido2Fractal(semCalor, mx / espalharCalor, mz / espalharCalor,
					octCalor, perCalor, 2.0f) * 0.5f + 0.5f - ((alt - NIVEL_MAR) * 0.004f)));
                    float umi = Math.max(0f, Math.min(1f,
					OpenSimplex2.ruido2Fractal(semUmidade, mx / espalharUmidade, mz / espalharUmidade,
					octUmidade, perUmidade, 2.0f) * 0.5f + 0.5f));
                    if(registro.selecionar(cal, umi, alt).chave.equals(chave)) return new int[]{mx, mz};
                }
            }
        }
        return new int[]{0, 0};
    }
}

