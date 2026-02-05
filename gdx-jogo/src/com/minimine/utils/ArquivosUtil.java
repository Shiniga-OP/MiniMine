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
import com.minimine.cenas.Jogador;
import java.io.FileReader;
import java.util.List;
import com.minimine.utils.arrays.FloatArrayUtil;
import com.minimine.utils.arrays.ShortArrayUtil;
import com.badlogic.gdx.graphics.Texture;
import com.minimine.cenas.Inventario;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.util.zip.ZipInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import com.minimine.mundo.Chave;
import java.util.concurrent.ConcurrentHashMap;
import com.minimine.mundo.Chunk;
import com.minimine.mundo.ChunkUtil;
import com.minimine.mundo.blocos.Bloco;
import com.badlogic.gdx.graphics.Mesh;
import com.minimine.graficos.Texturas;
import com.minimine.cenas.Jogo;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class ArquivosUtil {
    public static final String VERSAO = "v0.0.1";
	public static boolean debug = true;

    // salva o mundo compactado(.mini), e faz escrita atomica para evitar arquivos truncados
    public static void svMundo(Mundo mundo, Jogador jogador) {
        File pasta = new File(Inicio.externo + "/MiniMine/mundos");
        if(!pasta.exists()) pasta.mkdirs();
        File destino = new File(pasta, Mundo.decodificarNome(mundo.nome) + ".mini");
        File tmp = new File(pasta, Mundo.decodificarNome(mundo.nome) + ".mini.tmp");

        try {
            // escreve em arquivo temporario
            ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(tmp)));
            DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(zos));

            try {
                // versao.txt
                zos.putNextEntry(new ZipEntry("versao.txt"));
                byte[] vt = VERSAO.getBytes(Charset.forName("UTF-8"));
                zos.write(vt);
                zos.closeEntry();
                // mundo.bin(escreve diretamente no zip usando o mesmo DataOutputStream)
                zos.putNextEntry(new ZipEntry("mundo.bin"));
                gravarMundo(dos, mundo);
                dos.flush();
                zos.closeEntry();
                // jogador.bin
                zos.putNextEntry(new ZipEntry("jogador.bin"));
                gravarJogador(dos, jogador);
                dos.flush();
                zos.closeEntry();
                // inventario.bin
                zos.putNextEntry(new ZipEntry("inventario.bin"));
                gravarInventario(dos, jogador);
                dos.flush();
                zos.closeEntry();
                // ciclo.bin
                zos.putNextEntry(new ZipEntry("ciclo.bin"));
                gravarCiclo(dos);
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
        }
    }
    // carrega o mundo, nao marca Mundo.carregado a menos que o carregamento seja concluído com sucesso
    public static void crMundo(Mundo mundo, Jogador jogador) {
        File arquivo = new File(Inicio.externo + "/MiniMine/mundos/" + mundo.nome + ".mini");
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
						if(!VERSAO.equals(v)) {
							if(debug) Gdx.app.log("ArquivosUtil", "[AVISO] a versao "+v+" do mundo não e a mais atual "+VERSAO);
						}
                        if(debug) Gdx.app.log("ArquivosUtil", "[DEBUG] versao.txt: " + v);
                    } else if("mundo.bin".equals(nome)) {
                        lerMundo(dis, mundo);
                        if(debug) Gdx.app.log("ArquivosUtil", "[DEBUG] mundo.bin lido");
                    } else if("jogador.bin".equals(nome)) {
                        lerJogador(dis, jogador);
                        if(debug) Gdx.app.log("ArquivosUtil", "[DEBUG] jogador.bin lido");
                    } else if("inventario.bin".equals(nome)) {
                        lerInventario(dis, jogador);
                        if(debug) Gdx.app.log("ArquivosUtil", "[DEBUG] inventario.bin lido");
                    } else if("ciclo.bin".equals(nome)) {
                        DiaNoiteUtil.tempo = dis.readFloat();
                        DiaNoiteUtil.tempo_velo = dis.readFloat();
                        if(debug) Gdx.app.log("ArquivosUtil", "[DEBUG] ciclo.bin lido");
                    } else {
                        // garante consumo da entrada
                        byte[] pularBuf = new byte[512];
                        while(zis.read(pularBuf) > 0) {}
                        if(debug) Gdx.app.log("ArquivosUtil", "[AVISO] entrada desconhecida: " + nome);
                    }
                } catch(Throwable inner) {
                    Gdx.app.log("ArquivosUtil", "[ERRO] falha ao processar entrada '" + nome + "': " + inner.getMessage());
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
        Mundo.carregado = sucesso;
        if(sucesso && debug) Gdx.app.log("ArquivosUtil", "[AVISO] mundo carregado");
    }
	// gravadores e leitores de binarios:
    public static void gravarMundo(DataOutputStream dos, Mundo mundo) throws IOException {
        // seed
        dos.writeLong(mundo.semente);
        // quantos chunks salvos
        dos.writeInt(mundo.chunksMod.size());
        for(Map.Entry<Long, Chunk> e : mundo.chunksMod.entrySet()) {
            long chave = e.getKey();
            Chunk chunk = e.getValue();
            int cx = Mundo.TAM_CHUNK;
            int cy = Mundo.Y_CHUNK;
            int cz = Mundo.TAM_CHUNK;
            dos.writeLong(chave);
            int totalNaoAr = 0;
            for(int x = 0; x < cx; x++) {
                for(int y = 0; y < cy; y++) {
                    for(int z = 0; z < cz; z++) {
                        int b = ChunkUtil.obterBloco(x, y, z, chunk);
                        if(b != 0) totalNaoAr++;
                    }
                }
            }
            dos.writeInt(totalNaoAr);

            for(int x = 0; x < cx; x++) {
                for(int y = 0; y < cy; y++) {
                    for(int z = 0; z < cz; z++) {
                        int b = ChunkUtil.obterBloco(x, y, z, chunk);
                        if(b != 0) {
							CharSequence bloco = Bloco.numIds.get(b).nome;
                            dos.writeInt(x);
                            dos.writeInt(y);
                            dos.writeInt(z);
                            dos.writeUTF(""+bloco);
                        }
                    }
                }
            }
        }
        dos.flush();
    }
    public static void gravarJogador(DataOutputStream dos, Jogador jogador) throws IOException {
        dos.writeInt(jogador.modo);
        dos.writeFloat(jogador.posicao.x);
        dos.writeFloat(jogador.posicao.y);
        dos.writeFloat(jogador.posicao.z);
        dos.writeFloat(jogador.yaw);
        dos.writeFloat(jogador.tom);
        dos.writeUTF(""+jogador.item);
        dos.writeInt(jogador.ALCANCE);
        dos.writeInt(jogador.inv != null ? jogador.inv.slotSelecionado : 0);
		dos.writeFloat(jogador.velo);
		dos.writeBoolean(jogador.agachado);
		dos.writeBoolean(jogador.nasceu);
        dos.flush();
    }

    public static void gravarInventario(DataOutputStream dos, Jogador jogador) throws IOException {
		try {
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
		} catch(Exception e) {
			Gdx.app.log("ArquivosUtil", "[ERRO] ao gravar inv: "+e);
		}
    }

    public static void gravarCiclo(DataOutputStream dos) throws IOException {
        dos.writeFloat(DiaNoiteUtil.tempo);
        dos.writeFloat(DiaNoiteUtil.tempo_velo);
        dos.flush();
    }

	// leitores
    public static void lerMundo(DataInputStream dis, Mundo mundo) throws IOException {
        mundo.semente = dis.readLong();
        int totalChunks = dis.readInt();

        for(int i = 0; i < totalChunks; i++) {
            int chunkX = dis.readInt();
            int chunkZ = dis.readInt();

            Chunk chunk = new Chunk();
            ChunkUtil.compactar(ChunkUtil.bitsPraMaxId(chunk.maxIds), chunk);
            chunk.x = chunkX;
            chunk.z = chunkZ;

            int totalNaoAr = dis.readInt();
            for(int k = 0; k < totalNaoAr; k++) {
                int x = dis.readInt();
                int y = dis.readInt();
                int z = dis.readInt();
                CharSequence id = dis.readUTF();
                ChunkUtil.defBloco(x, y, z, id, chunk);
            }
			chunk.malha = new Mesh(true, Jogo.render.maxVerts, Jogo.render.maxIndices, Jogo.render.atriburs);
            if(mundo.chunksMod == null) mundo.chunksMod = new ConcurrentHashMap<Long, Chunk>();
            if(mundo.chunks == null) mundo.chunks = new ConcurrentHashMap<Long, Chunk>();

            mundo.chunksMod.put(Chave.calcularChave(chunkX, chunkZ), chunk);
			mundo.chunks.put(Chave.calcularChave(chunkX, chunkZ), chunk);

            chunk.att = true;
			chunk.dadosProntos = true;
			mundo.estados.put(Chave.calcularChave(chunkX, chunkZ), 1);
        }
    }

    public static void lerJogador(DataInputStream dis, Jogador jogador) throws IOException {
        jogador.modo = dis.readInt();
        jogador.posicao = new Vector3(dis.readFloat(), dis.readFloat(), dis.readFloat());
        jogador.yaw = dis.readFloat();
        jogador.tom = dis.readFloat();
        jogador.item = dis.readUTF();
        jogador.ALCANCE = dis.readInt();
        if(jogador.inv == null) jogador.inv = new Inventario();
        jogador.inv.slotSelecionado = dis.readInt();
		jogador.velo = dis.readFloat();
		jogador.agachado = dis.readBoolean();
		jogador.nasceu = dis.readBoolean();
    }

    public static void lerInventario(DataInputStream dis, Jogador jogador) throws IOException {
		try {
			int total = dis.readInt();
			if(jogador.inv == null || total == 0) jogador.inv = new Inventario();
			if(jogador.inv.itens == null || (jogador.inv.itens.length != total && total != 0)) jogador.inv.itens = new Inventario.Item[total];

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
					String nome = dis.readUTF();
					int quantidade = dis.readInt();

					Texture textura = Texturas.texs.get(nome + "_lado");
					if(textura == null) textura = Texturas.texs.get(nome);
					if(textura == null) textura = Texturas.texs.get(nome + "_topo");
					if(textura == null) {
						Gdx.app.log("ArquivosUtil", "[ERRO] textura do item nao encontrada: " + nome + " (slot " + i + ") - ignorando item");
						jogador.inv.itens[i] = null;
						continue;
					}
					jogador.inv.itens[i] = new Inventario.Item(nome, textura, quantidade);
				} else {
					jogador.inv.itens[i] = null;
				}
			}
		} catch(Exception e) {
			Gdx.app.log("ArquivosUtil", "[ERRO] ao carregar inv: "+e);
		}
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
		StringBuilder sb = new StringBuilder();    
		FileReader fr = null;    

		try {    
			fr = new FileReader(new File(caminho));    

			char[] buff = new char[1024];    
			int tamanho = 0;    

			while((tamanho = fr.read(buff)) > 0) sb.append(new String(buff, 0, tamanho));    
		} catch(Exception e) {    
			e.printStackTrace();    
		} finally {    
			if(fr != null) {    
				try {    
					fr.close();    
				} catch(Exception e) {    
					Gdx.app.log("ArquivosUtil", "[ERRO]: lendo "+e.getMessage()+" \""+caminho+"\"");
				}    
			}    
		}    
		return sb.toString();    
	}    

	public static void escrever(String caminho, String texto) {
        caminho = caminho.replace("/", File.separator);
		criar(caminho);    
		FileWriter escritor = null;    
		try {    
			escritor = new FileWriter(new File(caminho), false);    
			escritor.write(texto);    
			escritor.flush();    
		} catch(Exception e) {    
			e.printStackTrace();    
		} finally {    
			try {    
				if(escritor != null) escritor.close();    
			} catch(Exception e) {    
				Gdx.app.log("ArquivosUtil", "[ERRO]: escrevendo "+e.getMessage()+" caminho \""+caminho+"\"");    
			}    
		}    
	}    

	public static void delete(String caminho) {    
        caminho = caminho.replace("/", File.separator);
		File arquivo = new File(caminho);    

		if(!arquivo.exists()) return;    
		if(arquivo.isFile()) {    
			arquivo.delete();    
			return;    
		}    
		File[] arquivos = arquivo.listFiles();    

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
		List<String> lista = new ArrayList<>();
		File dir = new File(caminho);    
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
		File dir = new File(caminho);    
		if(!dir.exists() || dir.isFile()) return;    

		File[] listaArquivos = dir.listFiles();    
		if(listaArquivos==null || listaArquivos.length <= 0) return;    

		if(lista==null) return;    
		lista.clear();    
		for(File arquivo : listaArquivos) {    
			lista.add(arquivo.getAbsolutePath());    
		}    
	}    

	public static boolean existe(String caminho) {   
        caminho = caminho.replace("/", File.separator); 
		File arquivo = new File(caminho);    
		return arquivo.exists();    
	}    

	public static void criarDir(String caminho) {    
        caminho = caminho.replace("/", File.separator);
		if(!existe(caminho)) {    
			File arquivo = new File(caminho);    
			arquivo.mkdirs();    
		}    
	}
}
