package com.minimine.utils;

import com.minimine.mundo.Mundo;
import java.io.File;
import com.minimine.Inicio;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.FileInputStream;
import java.util.Map;
import java.io.DataOutputStream;
import java.io.BufferedOutputStream;
import java.io.OutputStream;
import com.minimine.ui.UI;
import java.io.InputStream;
import java.io.DataInputStream;
import java.io.BufferedInputStream;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.Gdx;
import java.io.FileWriter;
import com.badlogic.gdx.math.Matrix4;
import com.minimine.entidades.Jogador;
import com.minimine.entidades.Entidade;
import com.minimine.entidades.Criatura;
import com.minimine.entidades.ItemMundo;
import java.io.FileReader;
import java.util.List;
import com.minimine.utils.arrays.FloatArrayUtil;
import com.minimine.utils.arrays.ShortArrayUtil;
import com.badlogic.gdx.graphics.Texture;
import com.minimine.inventario.Inventario;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.util.zip.ZipInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import com.minimine.mundo.Chave;
import java.util.concurrent.ConcurrentHashMap;
import com.minimine.mundo.chunks.Chunk;
import com.minimine.mundo.blocos.Bloco;
import com.badlogic.gdx.graphics.Mesh;
import com.minimine.graficos.Texturas;
import com.minimine.cenas.Jogo;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.minimine.mundo.blocos.BlocoEstrutura;
import com.minimine.utils.MemNativa;
import com.badlogic.gdx.files.FileHandle;
import com.minimine.inventario.ItemRegistro;
import com.minimine.inventario.Item;
import com.minimine.mundo.chunks.ChunkProcesso;
import java.net.URI;
import java.util.zip.ZipFile;
import java.util.Enumeration;

public final class ArquivosUtil {
    public static final int[] VERSAO = { 0, 0, 1 };
	public static final String versao = "v" + VERSAO[0] + "." + VERSAO[1] + "." + VERSAO[2];
	public static boolean debug = true;

    public static final int VERSAO_MINIES = 1;
    public static final String ID_BLOCO_NULO = "bloco_nulo";

    // salva o mundo compactado(.mini), e faz escrita atomica para evitar arquivos truncados
    public static void svMundo(Mundo mundo, List<Jogador> jogadores) {
        File pasta = obter(Inicio.externo + "/MiniMine/mundos");
        if(!pasta.exists()) pasta.mkdirs();

        File destino = new File(pasta, URLEncoder.encode(mundo.nome) + ".mini");
        File tmp = new File(pasta, URLEncoder.encode(mundo.nome) + ".mini.tmp");

        try {
            // escreve em arquivo temporario
            ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(tmp)));
            DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(zos));

            try {
                // versao.txt
                zos.putNextEntry(new ZipEntry("versao.txt"));

                byte[] vt = versao.getBytes(Charset.forName("UTF-8"));
                zos.write(vt);
                zos.closeEntry();
                // mundo.bin(escreve diretamente no zip usando o mesmo DataOutputStream)
                zos.putNextEntry(new ZipEntry("mundo.bin"));
                mundo.salvar(dos);
                dos.flush();
                zos.closeEntry();
                // jogador.bin
				for(Jogador jg : jogadores) {
					zos.putNextEntry(new ZipEntry(jg.id + ".bin"));
					jg.salvar(dos);
					dos.flush();
					zos.closeEntry();
				}
                // inventario.bin
				zos.putNextEntry(new ZipEntry(jogadores.get(0).id+"_inv.bin"));
				gravarInventario(dos, jogadores.get(0));
				dos.flush();
				zos.closeEntry();
                // entidades.bin
                zos.putNextEntry(new ZipEntry("entidades.bin"));
                gravarEntidades(dos);
                dos.flush();
                zos.closeEntry();
                // ciclo.bin
                zos.putNextEntry(new ZipEntry("ciclo.bin"));
                dos.writeFloat(mundo.diaNoite.tempo);
				dos.writeFloat(mundo.diaNoite.tempo_velo);
				dos.flush();
                zos.closeEntry();
                zos.finish();
            } finally {
                try { dos.close(); } catch(Throwable t) {}
            }
            // renomeia de forma atomica quando possivel
            if(tmp.exists()) {
                if(destino.exists()) destino.delete();
                boolean ok = tmp.renameTo(destino);
                if(!ok) {
                    FileOutputStream fos = null;
                    FileInputStream fis = null;
                    try {
                        fis = new FileInputStream(tmp);
                        fos = new FileOutputStream(destino);
                        byte[] buf = new byte[8192];
                        int r;
                        while((r = fis.read(buf)) > 0) fos.write(buf, 0, r);
                        fos.flush();
                    } finally {
                        try { if(fos != null) fos.close(); } catch(Throwable t) {}
                        try { if(fis != null) fis.close(); } catch(Throwable t) {}
                    }
                    tmp.delete();
                }
            }
            if(debug) Gdx.app.log("ArquivosUtil", "[AVISO] mundo salvo");
        } catch(Throwable t) {
            Gdx.app.log("ArquivosUtil", "[ERRO] falha ao salvar mundo: " + t.getMessage());
            if(tmp.exists()) tmp.delete();
			throw new RuntimeException("[ERRO] falha ao salvar mundo: ");
        }
    }
    // carrega o mundo, nao marca Mundo.carregado a menos que o carregamento seja concluído com sucesso
    public static void crMundo(Mundo mundo, Jogador jogador) {
        File arquivo = obter(Inicio.externo + "/MiniMine/mundos/" + URLEncoder.encode(mundo.nome) + ".mini");
        if(!arquivo.exists() || arquivo.length() <= 4) {
            if(debug) Gdx.app.log("ArquivosUtil", "[INFO] .mini não existe ou é muito pequeno: " + arquivo.getAbsolutePath());
            Mundo.carregado = false;
            return;
        }
        ZipInputStream zis = null;
        boolean sucesso = false;
        try {
            zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(arquivo)));
            DataInputStream dis = new DataInputStream(new BufferedInputStream(zis));

            ZipEntry e;
            boolean qualquer = false;
            while((e = zis.getNextEntry()) != null) {
                qualquer = true;
                String nome = e.getName();
                if(debug) Gdx.app.log("ArquivosUtil", "[DEBUG] lendo entrada: " + nome);
                try {
                    if("versao.txt".equals(nome)) {
                        // ler linha simples
                        ByteArrayOutputStream tmp = new ByteArrayOutputStream();
                        byte[] buf = new byte[512];
                        int r;
                        while((r = zis.read(buf)) > 0) tmp.write(buf, 0, r);
                        String v = new String(tmp.toByteArray(), Charset.forName("UTF-8")).trim();
						if(!versao.equals(v)) {
							if(debug) Gdx.app.log("ArquivosUtil", "[AVISO] a versao "+v+" do mundo não e a mais atual "+versao);
						}
                        if(debug) Gdx.app.log("ArquivosUtil", "[DEBUG] versao.txt: " + v);
                    } else if("mundo.bin".equals(nome)) {
                        mundo.carregar(dis);
                        if(debug) Gdx.app.log("ArquivosUtil", "[DEBUG] mundo.bin lido");
                    } else if((jogador.id + ".bin").equals(nome)) {
                        jogador.carregar(dis);
                        if(debug) Gdx.app.log("ArquivosUtil", "[DEBUG] jogador.bin lido");
                    } else if((jogador.id+"_inv.bin").equals(nome)) {
                        lerInventario(dis, jogador);
                        if(debug) Gdx.app.log("ArquivosUtil", "[DEBUG] inventario.bin lido");
                    } else if("entidades.bin".equals(nome)) {
                        lerEntidades(dis);
                        if(debug) Gdx.app.log("ArquivosUtil", "[DEBUG] entidades.bin lido");
                    } else if("ciclo.bin".equals(nome)) {
                        mundo.diaNoite.tempo = dis.readFloat();
                        mundo.diaNoite.tempo_velo = dis.readFloat();
                        if(debug) Gdx.app.log("ArquivosUtil", "[DEBUG] ciclo.bin lido");
                    } else {
                        // garante consumo da entrada
                        byte[] pularBuf = new byte[512];
                        while(zis.read(pularBuf) > 0) {}
                        if(debug) Gdx.app.log("ArquivosUtil", "[AVISO] entrada desconhecida: " + nome);
                    }
                } catch(Throwable i) {
                    Gdx.app.log("ArquivosUtil", "[ERRO] falha ao processar entrada '" + nome + "': " + i.getMessage());
					throw new RuntimeException("[ERRO] falha ao carregar mundo: ");
                    // continua pra tentar carregar o maximo possivel
                } finally {
                    try { zis.closeEntry(); } catch(Throwable t) {}
                }
            }
            if(!qualquer) {
                Gdx.app.log("ArquivosUtil", "[ERRO] .mini vazio ou corrompido");
                sucesso = false;
            } else {
                sucesso = true;
            }
        } catch(Throwable t) {
            Gdx.app.log("ArquivosUtil", "[ERRO] falha geral ao ler .mini: " + t.getMessage());
            sucesso = false;
        } finally {
            try { if(zis != null) zis.close(); } catch(Throwable t) {}
        }
        if(sucesso && debug) Gdx.app.log("ArquivosUtil", "[AVISO] mundo carregado");
    }

    // tipos de entidade salvos
    public static final byte TIPO_CRIATURA = 1;
    public static final byte TIPO_ITEM = 2;

    public static void gravarEntidades(DataOutputStream dos) throws IOException {
        // conta quantas entidades salvaveis existem
        int total = 0;
        for(int i = 0; i < Mundo.entidades.size(); i++) {
            Entidade e = Mundo.entidades.get(i);
            if(e instanceof Criatura || e instanceof ItemMundo) total++;
        }
        dos.writeInt(total);
        for(int i = 0; i < Mundo.entidades.size(); i++) {
            Entidade e = Mundo.entidades.get(i);
            if(e instanceof Criatura) {
                Criatura c = (Criatura)e;
                dos.writeByte(TIPO_CRIATURA);
                dos.writeUTF(c.dados.nome);
                dos.writeFloat(c.posicao.x);
                dos.writeFloat(c.posicao.y);
                dos.writeFloat(c.posicao.z);
                dos.writeFloat(c.yaw);
                dos.writeInt(c.vida);
                // variaveis internas(sede, fome, etc)
                dos.writeInt(c.variaveis.size());
                for(java.util.Map.Entry<String, Float> v : c.variaveis.entrySet()) {
                    dos.writeUTF(v.getKey());
                    dos.writeFloat(v.getValue());
                }
                // pesos da IA(aprendizado acumulado)
                gravarPesosIA(dos, c.ia);
            } else if(e instanceof ItemMundo) {
                ItemMundo item = (ItemMundo) e;
                dos.writeByte(TIPO_ITEM);
                dos.writeUTF(item.nome);
                dos.writeInt(item.quantidade);
                dos.writeFloat(item.posicao.x);
                dos.writeFloat(item.posicao.y);
                dos.writeFloat(item.posicao.z);
                dos.writeFloat(item.tempoVida);
            }
        }
    }

    public static void lerEntidades(DataInputStream dis) throws IOException {
        Mundo.entidades.clear();
        int total = dis.readInt();
        for(int i = 0; i < total; i++) {
            byte tipo = dis.readByte();
            if(tipo == TIPO_CRIATURA) {
                String nomeD = dis.readUTF();
                float x = dis.readFloat(), y = dis.readFloat(), z = dis.readFloat();
                float yaw = dis.readFloat();
                int vida = dis.readInt();
                com.minimine.entidades.DadosCriatura dados = Mundo.registroCriaturas != null ? Mundo.registroCriaturas.criaturas.get(nomeD) : null;
                if(dados != null) {
                    Criatura c = new Criatura(dados, x, y, z);
                    c.yaw = yaw;
                    c.vida = vida;
                    int numVars = dis.readInt();
                    for(int v = 0; v < numVars; v++) {
                        String chave = dis.readUTF();
                        float val = dis.readFloat();
                        if(c.variaveis.containsKey(chave)) c.variaveis.put(chave, val);
                    }
                    // restaura pesos da IA
                    lerPesosIA(dis, c.ia);
                    Mundo.entidades.add(c);
                } else {
                    // criatura desconhecida: consome os bytes e ignora
                    if(debug) Gdx.app.log("ArquivosUtil", "[AVISO] criatura desconhecida: " + nomeD);
                    int numVars = dis.readInt();
                    for(int v = 0; v < numVars; v++) { dis.readUTF(); dis.readFloat(); }
                    lerPesosIA(dis, null); // consome bytes da IA
                }
            } else if(tipo == TIPO_ITEM) {
                String nomeI = dis.readUTF();
                int qtd = dis.readInt();
                float x = dis.readFloat(), y = dis.readFloat(), z = dis.readFloat();
                float tempoVida = dis.readFloat();
                ItemMundo item = new ItemMundo(nomeI, qtd, x, y, z);
                item.tempoVida = tempoVida;
                Mundo.entidades.add(item);
            } else {
                // tipo desconhecido: para de ler pra não corromper o stream
                if(debug) Gdx.app.log("ArquivosUtil", "[AVISO] tipo de entidade desconhecido: " + tipo);
                break;
            }
        }
    }

    public static void gravarPesosIA(DataOutputStream dos, com.minimine.entidades.IA ia) throws IOException {
        dos.writeInt(ia.ENTRADAS);
        dos.writeInt(ia.OCULTAS);
        dos.writeInt(ia.SAIDAS);
        for(int i = 0; i < ia.ENTRADAS; i++)
            for(int j = 0; j < ia.OCULTAS; j++)
                dos.writeFloat(ia.pesosEntrada[i][j]);
        for(int i = 0; i < ia.OCULTAS; i++)
            for(int j = 0; j < ia.SAIDAS; j++)
                dos.writeFloat(ia.pesosOculta[i][j]);
        for(int i = 0; i < ia.OCULTAS; i++) dos.writeFloat(ia.viesOculta[i]);
        for(int i = 0; i < ia.SAIDAS; i++) dos.writeFloat(ia.viesSaida[i]);
        dos.writeFloat(ia.taxaAtual);
        dos.writeFloat(ia.recompensaMedia);
    }

    // ia == null: apenas consome os bytes sem aplicar
    public static void lerPesosIA(DataInputStream dis, com.minimine.entidades.IA ia) throws IOException {
        int entradas = dis.readInt();
        int ocultas = dis.readInt();
        int saidas = dis.readInt();
        boolean aplicar = ia != null && ia.ENTRADAS == entradas && ia.OCULTAS == ocultas && ia.SAIDAS == saidas;
        for(int i = 0; i < entradas; i++)
            for(int j = 0; j < ocultas; j++) {
                float v = dis.readFloat();
                if(aplicar) ia.pesosEntrada[i][j] = v;
            }
        for(int i = 0; i < ocultas; i++)
            for(int j = 0; j < saidas; j++) {
                float v = dis.readFloat();
                if(aplicar) ia.pesosOculta[i][j] = v;
            }
        for(int i = 0; i < ocultas; i++) {
            float v = dis.readFloat();
            if(aplicar) ia.viesOculta[i] = v;
        }
        for(int i = 0; i < saidas; i++) {
            float v = dis.readFloat();
            if(aplicar) ia.viesSaida[i] = v;
        }
        float taxa = dis.readFloat();
        float media = dis.readFloat();
        if(aplicar) {
			ia.taxaAtual = taxa;
			ia.recompensaMedia = media;
		}
    }

    public static void gravarInventario(DataOutputStream dos, Jogador jogador) throws IOException {
		if(jogador.inv == null || jogador.inv.itens == null) {
			dos.writeInt(0);
			dos.flush();
			return;
		}
		dos.writeInt(jogador.inv.itens.length);
		for(int i = 0; i < jogador.inv.itens.length; i++) {
			if(jogador.inv.itens[i] == null) {
				dos.writeBoolean(false);
			} else {
				dos.writeBoolean(true);
				dos.writeUTF(jogador.inv.itens[i].nome+"");
				dos.writeInt(jogador.inv.itens[i].quantidade);
			}
		}
		dos.flush();
    }

    public static void lerInventario(DataInputStream dis, Jogador jogador) throws IOException {
		int total = dis.readInt();
		if(jogador.inv == null || total == 0) jogador.inv = new Inventario(jogador);
		if(jogador.inv.itens == null || (jogador.inv.itens.length != total && total != 0)) jogador.inv.itens = new Item[total];

		for(int i = 0; i < total; i++) {
			boolean temItem = false;
			try {
				temItem = dis.readBoolean();
			} catch(Throwable t) {
				Gdx.app.log("ArquivosUtil", "[ERRO] falha ao ler marcação de item slot " + i + ": " + t.getMessage());
				jogador.inv.itens[i] = null;
				continue;
			}
			if(temItem) {
				final String nome = dis.readUTF();
				final int quantidade = dis.readInt();

				final TextureRegion textura;

				final Item b = ItemRegistro.obter(nome);
				if(b != null) textura = b.textura;		
				else {
					Gdx.app.log("[Inventario]", "textura não encontrada para: " + nome);
					textura = Texturas.atlas.obter("terra");
				}
				jogador.inv.itens[i] = new Item(nome, textura, quantidade);
			} else {
				jogador.inv.itens[i] = null;
			}
		}
    }

    // svEstrutura: salva uma região do mundo como .minies
    /*
     * varre a bcaixa[baseX..baseX+larg-1, baseY..baseY+alt-1, baseZ..baseZ+prof-1],
     * descarta ar(id==0) e bloco_nulo, salva os demais com coordenadas locais

     * arquivo: MiniMine/estruturas/<nome>.minies
     * formato: veja cabeçalho de BlocoEstrutura.java
	*/
    public static void svEstrutura(
		String nome,
		int larg, int alt, int prof,
		int ancX, int ancY, int ancZ,
		int baseX, int baseY, int baseZ) throws IOException {

        File pasta = new File(Inicio.externo + "/MiniMine/estruturas");
        if(!pasta.exists()) pasta.mkdirs();

        File destino = new File(pasta, nome + ".minies");
        File tmp = new File(pasta, nome + ".minies.tmp");

        DataOutputStream dos = null;
        try {
            dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(tmp)));
            // cabeçalho
            dos.writeInt(VERSAO_MINIES);
            dos.writeUTF(nome);
            dos.writeInt(larg);
            dos.writeInt(alt);
            dos.writeInt(prof);
            dos.writeInt(ancX);
            dos.writeInt(ancY);
            dos.writeInt(ancZ);

            // primeiro passo: conta blocos validos
            int total = 0;
            for(int lx = 0; lx < larg; lx++) {
                for(int ly = 0; ly < alt; ly++) {
                    for(int lz = 0; lz < prof; lz++) {
                        int id = Mundo.obterBlocoMundo(baseX + lx, baseY + ly, baseZ + lz);
                        if(id == 0) continue;
                        Bloco b = Bloco.numIds.get(id);
                        if(b == null) continue;
                        if(ID_BLOCO_NULO.equals("" + b.nome)) continue;
                        total++;
                    }
                }
            }
            dos.writeInt(total);

            // segundo passo: escreve blocos
            for(int lx = 0; lx < larg; lx++) {
                for(int ly = 0; ly < alt; ly++) {
                    for(int lz = 0; lz < prof; lz++) {
                        int id = Mundo.obterBlocoMundo(baseX + lx, baseY + ly, baseZ + lz);
                        if(id == 0) continue;
                        Bloco b = Bloco.numIds.get(id);
                        if(b == null) continue;
                        if(ID_BLOCO_NULO.equals(b.nome)) continue;
                        dos.writeInt(lx);
                        dos.writeInt(ly);
                        dos.writeInt(lz);
                        dos.writeUTF(b.nome);
						dos.writeShort(Mundo.obterMetaMundo(baseX + lx, baseY + ly, baseZ + lz));
                    }
                }
            }
            dos.flush();
        } finally {
            try {
				if(dos != null) dos.close();
			} catch(Throwable t) {}
        }
        // escrita atomica
        if(tmp.exists()) {
            if(destino.exists()) destino.delete();
            boolean ok = tmp.renameTo(destino);
            if(!ok) {
                FileOutputStream fos = null;
                FileInputStream  fis = null;
                try {
                    fis = new FileInputStream(tmp);
                    fos = new FileOutputStream(destino);
                    byte[] buf = new byte[8192];
                    int r;
                    while((r = fis.read(buf)) > 0) fos.write(buf, 0, r);
                    fos.flush();
                } finally {
                    try {
						if(fos != null) fos.close();
					} catch(Throwable t) {}
                    try {
						if(fis != null) fis.close();
					} catch(Throwable t) {}
                }
                tmp.delete();
            }
        }
        if(debug) Gdx.app.log("ArquivosUtil", "[AVISO] estrutura salva: " + destino.getAbsolutePath());
    }
    // crEstrutura: carrega um .minies e retorna os dados
    /*
     * retorna um DadosEstrutura com todos os blocos e metadados,
     * ou null se o arquivo não existir ou estiver corrompido
	*/
	public static DadosEstrutura crEstrutura(String nome) {
		return crEstrutura(nome, true);
	}

    public static DadosEstrutura crEstrutura(String nome, boolean externo) {
        FileHandle arquivo = externo ? Gdx.files.absolute(Inicio.externo + "/MiniMine/estruturas/" + nome + ".minies") : Gdx.files.internal("estruturas/"+nome+".minies");

        if(!arquivo.exists() || arquivo.length() <= 4) {
            if(debug) Gdx.app.log("ArquivosUtil", "[INFO] .minies não encontrado: " + arquivo.path());
            return null;
        }
        DataInputStream dis = null;
        try {
            dis = new DataInputStream(new BufferedInputStream(externo ? new FileInputStream(arquivo.file()) : arquivo.read()));

            int versao = dis.readInt();
            if(versao != VERSAO_MINIES) {
                Gdx.app.log("ArquivosUtil", "[AVISO] versão .minies incompatível: " + versao);
                // tenta carregar mesmo assim, estrutura não mudou ainda
            }
            DadosEstrutura d = new DadosEstrutura();
            d.nome = dis.readUTF();
            d.larg = dis.readInt();
            d.alt = dis.readInt();
            d.prof = dis.readInt();
            d.ancX = dis.readInt();
            d.ancY = dis.readInt();
            d.ancZ = dis.readInt();

            int total = dis.readInt();
            d.lx = new int[total];
            d.ly = new int[total];
            d.lz = new int[total];
            d.ids = new String[total];
            d.meta = new short[total];

            for(int i = 0; i < total; i++) {
                d.lx[i] = dis.readInt();
                d.ly[i] = dis.readInt();
                d.lz[i] = dis.readInt();
                d.ids[i] = dis.readUTF();
                d.meta[i] = dis.readShort();
            }
            return d;
        } catch(Throwable t) {
            Gdx.app.log("ArquivosUtil", "[ERRO] falha ao ler .minies '" + nome + "': " + t.getMessage());
            return null;
        } finally {
            try {
				if(dis != null) dis.close();
			} catch(Throwable t) {}
        }
    }

    // DadosEstrutura: dados retornados por crEstrutura()
    public static final class DadosEstrutura {
        public String nome;
        public int larg, alt, prof;
        public int ancX, ancY, ancZ;
        // coordenadas locais de cada bloco
        public int[] lx, ly, lz;
        // id de string de cada bloco
        public String[] ids;
        // metadado de cada bloco
        public short[] meta;
        /*
         * coloca a estrutura no mundo com origem em(ox, oy, oz)
         * a ancora é descontada: o bloco de ancora fica em(ox, oy, oz)
         * blocos de ar(ids[i] == null ou "ar") são ignorados
         * pra sobrescrever tudo inclusive ar, chame colocarMundo(ox,oy,oz,true)
		*/
        public void colocarMundo(int ox, int oy, int oz) {
            colocarMundo(ox, oy, oz, false);
        }

        public void colocarMundo(int ox, int oy, int oz, boolean sobrescreverTudo) {
            if(lx == null) return;
            // coleta chaves de chunks afetados(incluindo vizinhos de borda)
            java.util.Set<Long> afetados = new java.util.HashSet<Long>();
            for(int i = 0; i < lx.length; i++) {
                int vx = ox + (lx[i] - ancX);
                int vy = oy + (ly[i] - ancY);
                int vz = oz + (lz[i] - ancZ);
                String id = ids[i];
                if(!sobrescreverTudo && (id == null || "ar".equals(id))) continue;
                Mundo.defBlocoMundo(vx, vy, vz, id);
                Mundo.defMetaMundo(vx, vy, vz, meta[i]);
                // chunk do bloco e vizinhos laterais(faces compartilhadas entre chunks)
                int cx = Math.floorDiv(vx, Mundo.TAM_CHUNK);
                int cz = Math.floorDiv(vz, Mundo.TAM_CHUNK);
                afetados.add(Chave.calcularChave(cx, cz));
                afetados.add(Chave.calcularChave(cx + 1, cz));
                afetados.add(Chave.calcularChave(cx - 1, cz));
                afetados.add(Chave.calcularChave(cx, cz + 1));
                afetados.add(Chave.calcularChave(cx, cz - 1));
            }
            for(long chave : afetados) {
                Chunk c = Mundo.chunks.get(chave);
                if(c != null) c.att = true;
            }
        }
    }

    // utilitarios:
	public static File obter(String caminho) {
		return new File(caminho.replace("/", File.separator));
	}

    public static void criar(String caminho) {   
        caminho = caminho.replace("/", File.separator);
		int ultimoPasso = caminho.lastIndexOf(File.separator);    
		if(ultimoPasso > 0) {    
			String dirCaminho = caminho.substring(0, ultimoPasso);    
			criarDir(dirCaminho);    
		}    
		File arquivo = new File(caminho);    
		try {    
			if(!arquivo.exists()) arquivo.createNewFile();    
		} catch(Exception e) {    
			Gdx.app.log("ArquivosUtil", "[ERRO]: criando "+e.getMessage()+File.separator+caminho+File.separator);    
		}    
	}

	public static String ler(String caminho) {    
        caminho = caminho.replace("/", File.separator);
		final StringBuilder sb = new StringBuilder();    
		FileReader fr = null;    

		try {    
			fr = new FileReader(new File(caminho));    

			final char[] buff = new char[1024];    
			int tamanho = 0;    

			while((tamanho = fr.read(buff)) > 0) sb.append(new String(buff, 0, tamanho));
			fr.close();
		} catch(Exception e) {    
			e.printStackTrace();    
		}    
		return sb.toString();    
	}    

	public static void escrever(String caminho, String texto) {
        caminho = caminho.replace("/", File.separator);
		criar(caminho);    

		try {    
			final FileWriter escritor = new FileWriter(new File(caminho), false);    
			escritor.write(texto);    
			escritor.flush();
			escritor.close();
		} catch(Exception e) {    
			e.printStackTrace();
		}    
	}    

	public static void delete(String caminho) {    
        caminho = caminho.replace("/", File.separator);
		final File arquivo = new File(caminho);    

		if(!arquivo.exists()) return;    
		if(arquivo.isFile()) {    
			arquivo.delete();    
			return;    
		}    
		final File[] arquivos = arquivo.listFiles();    

		if(arquivos != null) {    
			for(File subArquivo : arquivos) {    
				if(subArquivo.isDirectory()) {    
					delete(subArquivo.getAbsolutePath());    
				}    
				if(subArquivo.isFile()) subArquivo.delete();    
			}    
		}    
		arquivo.delete();    
	}    

	public static List<String> listar(String caminho) {
        caminho = caminho.replace("/", File.separator);
		final List<String> lista = new ArrayList<>();
		final File dir = new File(caminho);    
		if(!dir.exists() || dir.isFile()) return null;

		File[] listaArquivos = dir.listFiles();    
		if(listaArquivos == null || listaArquivos.length <= 0) return null;    

		if(lista==null) return null;    
		lista.clear();    
		for(File arquivo : listaArquivos) {    
			lista.add(arquivo.getName());    
		}
		return lista;
	}    

	public static void listarAbs(String caminho, List<String> lista) {    
        caminho = caminho.replace("/", File.separator);
		final File dir = new File(caminho);    
		if(!dir.exists() || dir.isFile()) return;    

		final File[] listaArquivos = dir.listFiles();    
		if(listaArquivos==null || listaArquivos.length <= 0) return;    

		if(lista==null) return;    
		lista.clear();    
		for(File arquivo : listaArquivos) {    
			lista.add(arquivo.getAbsolutePath());    
		}    
	}    

	public static boolean existe(String caminho) {   
        caminho = caminho.replace("/", File.separator); 
		final File arquivo = new File(caminho);    
		return arquivo.exists();    
	}    

	public static void criarDir(String caminho) {    
        caminho = caminho.replace("/", File.separator);
		if(!existe(caminho)) {    
			final File arquivo = new File(caminho);    
			arquivo.mkdirs();    
		}    
	}

	public static FileHandle[] listarAssets(FileHandle pasta) {
		FileHandle[] resultado = pasta.list(".json");
		if(resultado != null && resultado.length > 0) return resultado;

		try {
			URI uri = ArquivosUtil.class.getProtectionDomain()
				.getCodeSource()
				.getLocation()
				.toURI();
			final List<FileHandle> encontrados = new ArrayList<>();
			final String prefixo = pasta.path().replaceAll("^/+", "") + "/";

			final ZipFile jar = new ZipFile(new File(uri));
			final Enumeration<? extends ZipEntry> entradas = jar.entries();
			while(entradas.hasMoreElements()) {
				final ZipEntry e = entradas.nextElement();
				final String nome = e.getName();
				if(nome.startsWith(prefixo) && nome.endsWith(".json")) {
					encontrados.add(Gdx.files.internal(nome));
				}
			}
			jar.close();

			return encontrados.toArray(new FileHandle[0]);
		} catch(Exception e) {
			throw new RuntimeException("falha ao varrer JAR em: " + pasta.path(), e);
		}
	}
}
