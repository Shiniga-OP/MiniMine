# MiniMine

refeito com LibGDX.

## Requisitos mínimos:

## PC/Notebook:
* Sistema Operacional: Windows/Linux
* Java: 7.
* RAM: 350 MBs.
* OpenGL: 2.

## Celular:
* Sistema Operacional: Android.
* Java: 7.
* RAM: 200 MBs.
* OpenGL: 2.

## Já feito:
1. Gerenciamento de chunks dinâmico.
2. Motor de geração.
3. Colisão.
4. Gravidade.
5. Iluminação global.
6. Interface de botões de movimentação.
7. Debug visual.
8. Sistema de construção.
9. Barra Rápida.
10. Mods por Lua no armazenamento externo.
11. Sistema de ciclo noturno e diário.
12. Nuvens.
13. Salvamento dinamico de mundos binario.
14. Aúdio (ainda temporario até aúdios gravados manualmente).
15. Agachamento (evita cair de bordas dos blocos).
16. Tela de menu e configurações padrão.
17. Divisão de biomas.
18. Água animada via *Gdx.gl.glTexSubImage2D(...)*.
19. Névoa no horizonte.
20. Inventario.
21. Menu de pause.
22. Partículas ao quebrar blocos.
23. 1-2 músicas.
24. Divisão de biomas com rúido celular.
25. Entidades.
26. Sistema de nascimento de entidades baseado em bioma.
27. Auto atualização quando ligado a internet.

## Modos de jogo:
* 0: espectador. Não sofre gravidade ou colisão com blocos. Seus recursos não acabam
* 1: criativo. Não sofre com gravidade mas colide com blocos. Seus recursos não acabam
* 2: sobrevivencia. Sofre com gravidade e colide com blocos. Seus recursos acabam.

você pode descobrir mais sobre a API Lua em doc.txt.

## Blocos:
0. Ar.
1. Grama.
2. Terra.
3. Pedra.
4. Água.
5. Areia.
6. Tronco de madeira.
7. Bloco de folhas.
8. Tabua de madeira.
9. Cacto.
10. Vidro.
11. Tocha.
12. Pedregulho.
13. Cascalho.
14. Bloco de gelo.
15. Bloco de neve.
16. Bloco de coral rosa.
17. Bloco de coral azul
18. Bloco de coral amarelo.
19. Capim.
20. Tulipa.
21. Íris azul.

## Receitas:
ao clicar no botão de receitas, você pode obter um novo bloco apartir dessa receita.

selecione o bloco em questão como item atual, e clique no botão de receita.

essas são as receitas atuais e seus resultados:

* tronco = tabuas_madeira
* areia = vidro
* folha = tocha

## Geração feita:

Dominio de Deformação.
Erosão Hidraulica.

## Biomas:

1. OCEANO.
2. OCEANO_COSTEIRO.
3. OCEANO_QUENTE.
4. OCEANO_ABISSAL.
5. PLANICIES.
6. PLANICIES_MONTANHOSAS.
7. FLORESTA.
8. FLORESTA_COSTEIRA.
9. FLORESTA_MONTANHOSA.
10. DESERTO.
11. COLINAS_DESERTO.
12. MAR_CONGELADO.
13. TUNDRA.
14. PICOS_GELADOS.

## Otimizações:
* Geração em Thread separada com ExecutorService.
* Chaves do tipo *long* para obtenção de chunks.
* Descarte de faces sobrepostas.
* Não renderizar chunks fora do raio de visão.
* Compressão baseada em paleta.
* Otimização com variaveis locais pra compilação em tempo de  execução.
* Cache de chunks modificadas sem Malha.
* O Guloso(Malha Gulosa).
* Pré-computação de erosão.
* Reuso de Arrays utilizados na geração de dados das chunks.
* Iluminação feita por vértices, com niveis por blocos (0-15).
* Sistema hibrido de iluminação e ciclos diários com vértices e shader.
* Compactação de dados de luz em 1 único atributo de vértice.
* Compactação de limites do atlas para *O Guloso* funcionar com 1 atributo de vértice.
* Compactação de posição dos blocos em 1 atributo de vértice.
* Buffer de até 256 texturas únicas para dados do atlas (evita mais atributos de vértice).
* Carregamento e descarregamento dinamico de entidades.
* Cache de fila de luz.
* Cache de luz temporaria.
* Cache de fatias em ChunkMalha.java.
* Reutilização de texturas 2D para criação de menus e botões via fatiação.
* Geração inteligente que econimiza cálculos com base em uma variavel previsivel.
* Cache de tipos de biomas(também ajuda na transição mais caotica de biomas com neve e areia).

## Ruídos utilitários:
3. Simplex2D.java
4. Simplex3D.java
5. CristaRuido2D.java
6. CelularRuido2D.java

## compatibilidade:
* Android 4 até Android 15.
* Linux 32-bit e 64-bit.
* Windows XP até Windows 11.

## Desempenho:
FPS de 40 a 60 padrão testado com até 112 chunks ativas (raio de 5).

## uso de mémoria testada:
50 MBs do heap java & 22 MBs do heap nativo. (112 chunks ativas)

## Mods Lua:
você pode criar mods achando a pasta *MiniMine/mods/* no armazenamento externo. Adicione os arquivos Lua necéssarios:

att.lua // será chamado no loop principal

e para adicionar mais de um arquivo individual, adicione o caminho relativo a pasta atual em *MiniMine/mods/arquivos.mini*. os arquivos são separados e carregados por quebra de linha.

## AVISO
o jogo ainda não foi lançado e a API Lua ainda pode mudar bastante.

## Adicionais:

caso o jogo crashe ou você não tenha visão completa dos logs, visite *MiniMine/debug/logs.txt*, onde logs são acumulados a cada entrada em um mundo.

## Dispositivos usados para testes:
## Celular:
* Modelo: Motorola G41.
* Memória RAM: 4 GB.
* Armazenamento: 128 GB.
* Processador: 8 núcleos, velocidade clock 500 MHz - 2.00 GHz. ARM64.
* OpenGL ES: 3.2.
* JVM: Java VM ART 2.1.0.
* Sistema Operacional: Android 12 64-bit.
* FPS padrão: 40-60.

## PCs:
### Melhor condição
* Placa Mãe: Dell OptiPlex 780.
* Processador: Intel Core 2 Quad.
* Memória RAM: 4GB(2x2GB DDR3).
* Armazenamento: SSD 256GB.
* Video: Intel 4 Series(Integrada).
* OpenGL: 2.1.
* Sistema Operacional: Linux Mint XCFE 64-bit.
* FPS padrão: 45-64.

### Pior condição
* Placa Mãe: PCWare IPX1800E2.
* Processador: Intel Celeron CPU J1800.
* Memória RAM: 2GB(DDR3).
* Armazenamento: HD 150GB.
* Video: Intel HD Bay Trail(Integrada)
* OpenGL: 4.2.
* Sistema Operacional: Debian GNU/Linux 12 (bookworm) i686.
* FPS padrão: 83-142.


## Notebook:
* Modelo: Aspire ES 15.
* Processador: Intel Celeron Quad Core N3450.
* Memória RAM: 4GB DDR3 L.
* Armazenamento: HD 500 GBs.
* Video: Intel HD Graphics.
* OpenGL: 4.5.
* Sistema Operacional: Windows 10.
* FPS padrão: 100-255.

# Comandos de teclado
* **WASD**: controles de movimento.
* **ESPAÇO**: pula/voa.
* **SHIFT**: agacha/desce.
* **DIREITA/ESQUERDA**: com o mouse quebra e coloca blocos.
* **E**: abre o inventario.
* **T**: abre o chat.
* **F1**: abre o modo de debug.
* **R**: fabrica algo com base no item selecionado.
* **ESC**: abre o menu de pausa durante o jogo.

## paleta de cores:
* blocos/icones/botões: unseven.

## Créditos:

**Programação**:
* Shiniga-OP
* Green
* Xaniim
* Dorganhozo

**Efeitos sonoros**:
* Shiniga-OP
* VDLN7

**Pixel arte**:
* Shiniga-OP
* Marin

**Modelagem 3D**:
* Shiniga-OP
* Green

**Teste da API de mods e documentação ou ambiente de execução**:
* VDLN7
* Green
* Dorganhozo

**Canais:**
- [Shiniga-OP](https://youtube.com/@shiniga-op?si=A78wk-sm3EJvgavE)
- [VDLN7](https://youtube.com/@violetbrasilofc?si=Ip8AkZdPnDDdFjGm)
- [Green](https://youtube.com/@greenlevelcreatordev?si=q1HhyS115FbbPhOI)
- [Dorganhozo](https://youtube.com/@dorganzo?si=phKKbJ4P5C87TMJ0)
