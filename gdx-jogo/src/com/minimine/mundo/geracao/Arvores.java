package com.minimine.mundo.geracao;

import com.minimine.mundo.Mundo;
import com.minimine.mundo.ChunkUtil;
import com.minimine.mundo.Chunk;

public class Arvores {
	// arvore normal
    public static void gerarArvoreNormal(Chunk chunk, int x, int y, int z, int altura) {
        // tronco
        for(int ty = 0; ty < altura; ty++) {
            if(y + ty < Mundo.Y_CHUNK) {
                ChunkUtil.defBloco(x, y + ty, z, "tronco", chunk);
            }
        }
        // folhas camada por camada
        int topoFolha = y + altura;
        // topo
        if(topoFolha < Mundo.Y_CHUNK) {
            ChunkUtil.defBloco(x, topoFolha, z, "folha", chunk);
        }
        // camada -1(cruz)
        int camada1 = topoFolha - 1;
        if(camada1 < Mundo.Y_CHUNK) {
            addFolha(chunk, x, camada1, z);
            addFolha(chunk, x + 1, camada1, z);
            addFolha(chunk, x - 1, camada1, z);
            addFolha(chunk, x, camada1, z + 1);
            addFolha(chunk, x, camada1, z - 1);
        }
        // camada -2 e -3(quadrado 3x3)
        for(int camada = topoFolha - 2; camada >= topoFolha - 3 && camada >= 0; camada--) {
            if(camada < Mundo.Y_CHUNK) {
                for(int dx = -1; dx <= 1; dx++) {
                    for(int dz = -1; dz <= 1; dz++) {
                        if(dx == 0 && dz == 0) continue; // pula o tronco
                        addFolha(chunk, x + dx, camada, z + dz);
                    }
                }
            }
        }
    }
    // arvore com copa mais larga
    public static void gerarArvoreLarga(Chunk chunk, int x, int y, int z, int altura) {
        // tronco
        for(int ty = 0; ty < altura; ty++) {
            if(y + ty < Mundo.Y_CHUNK) {
                ChunkUtil.defBloco(x, y + ty, z, "tronco", chunk);
            }
        }
        int topoFolha = y + altura;

        // topo
        if(topoFolha < Mundo.Y_CHUNK) {
            ChunkUtil.defBloco(x, topoFolha, z, "folha", chunk);
        }
        // camada -1 (3x3)
        int camada1 = topoFolha - 1;
        if(camada1 < Mundo.Y_CHUNK) {
            for(int dx = -1; dx <= 1; dx++) {
                for(int dz = -1; dz <= 1; dz++) {
                    addFolha(chunk, x + dx, camada1, z + dz);
                }
            }
        }
        // camadas -2, -3(5x5 sem cantos)
        for(int camada = topoFolha - 2; camada >= topoFolha - 3 && camada >= 0; camada--) {
            if(camada < Mundo.Y_CHUNK) {
                for(int dx = -2; dx <= 2; dx++) {
                    for(int dz = -2; dz <= 2; dz++) {
                        // pula cantos e centro (tronco)
                        if(Math.abs(dx) == 2 && Math.abs(dz) == 2) continue;
                        if(dx == 0 && dz == 0) continue;
                        addFolha(chunk, x + dx, camada, z + dz);
                    }
                }
            }
        }
    }

    // arvore conica(pinheiro)
    public static void gerarArvoreConica(Chunk chunk, int x, int y, int z, int altura) {
        // tronco mais alto
        int alturaTronco = altura + 2;
        for(int ty = 0; ty < alturaTronco; ty++) {
            if(y + ty < Mundo.Y_CHUNK) {
                ChunkUtil.defBloco(x, y + ty, z, "tronco", chunk);
            }
        }

        int base = y + 2; // começa folhas mais baixo

        // camadas de folhas formando cone
        // base larga(5x5)
        if(base < Mundo.Y_CHUNK) {
            for(int dx = -2; dx <= 2; dx++) {
                for(int dz = -2; dz <= 2; dz++) {
                    if(Math.abs(dx) + Math.abs(dz) <= 3) {
                        addFolha(chunk, x + dx, base, z + dz);
                    }
                }
            }
        }
        // meio(3x3)
        for(int camada = base + 1; camada <= base + 3 && camada < Mundo.Y_CHUNK; camada++) {
            for(int dx = -1; dx <= 1; dx++) {
                for(int dz = -1; dz <= 1; dz++) {
                    if(Math.abs(dx) + Math.abs(dz) <= 2) {
                        addFolha(chunk, x + dx, camada, z + dz);
                    }
                }
            }
        }
        // topo(cruz + centro)
        int topo = base + 4;
        if(topo < Mundo.Y_CHUNK) {
            addFolha(chunk, x, topo, z);
            addFolha(chunk, x + 1, topo, z);
            addFolha(chunk, x - 1, topo, z);
            addFolha(chunk, x, topo, z + 1);
            addFolha(chunk, x, topo, z - 1);
        }
        // ponta
        if(topo + 1 < Mundo.Y_CHUNK) {
            ChunkUtil.defBloco(x, topo + 1, z, "folha", chunk);
        }
    }

    // coloca folha apenas se a posição estiver vazia e dentro dos limites da chunk
    public static void addFolha(Chunk chunk, int x, int y, int z) {
        if(x < 0 || x >= 16 || z < 0 || z >= 16 || y < 0 || y >= Mundo.Y_CHUNK) {
            return;
        }
        int blocoAtual = ChunkUtil.obterBloco(x, y, z, chunk);
        if(blocoAtual == 0) { // apenas se estiver vazio
            ChunkUtil.defBloco(x, y, z, "folha", chunk);
        }
    }
}
