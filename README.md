# MiniMine

refeito com LibGDX.

## Já feito:
1. Gerenciamento de chunks dinâmico.
2. Geração de terreno com Ruido Simplex 2D e 3D pras cavernas, manual.
3. Colisão.
4. Gravidade.
5. Iluminação por vértices.
6. Interface de botões de movimentação.
7. Debug visual.
8. Sistema de construção.
9. Hotbar.
10. Mods por Lua no armazenamento externo. (beta)
11. Sistema de ciclo noturno e diário.
12. Nuvens.
13. Salvamento dinamico de mundos binario.
14. Aúdio (ainda temporario até aúdios gravados manualmente).
15. Agachamento (evita cair de bordas dos blocos).
16. Tela de menu e configurações padrão.
17. Divisão de biomas.
18. Água animada via *Gdx.gl.glTexSubImage2D(...)*.
19. Névoa no horizonte.
20. Motor de geração. (beta)

## Modos de jogo:
0: espectador. Não sofre gravidade ou colisão com blocos. Seus recursos não acabam
1: criativo. Não sofre com gravidade mas colide com blocos. Seus recursos não acabam
2: sobrevivencia. Sofre com gravidade e colide com blocos. Seus recursos acabam.

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

## Receitas:
ao clicar no botão de receitas, você pode obter um novo bloco apartir dessa receita.

selecione o bloco em questão como item atual, e clique no botão de receita.

essas são as receitas atuais e seus resultados:

tronco = tabuas_madeira
areia = vidro
folha = tocha

## Geração feita:

Dominio de Deformação.
Erosão Hidraulica.

## Otimizações:
Geração em Thread separada com ExecutorService.
Chaves do tipo *long* para obtenção de chunks.
Reuso de 1 objeto Matrix4 para todas as chunks.
Face culling global.
Frustrum culling.
Compressão baseada em paleta.
Otimização na atualização com variaveis locais (JIT).
Cache de chunks modificadas sem Malha.
O Guloso (Greedy Mesh).
Pré-computação de erosão.

## Ruídos utilitários:
1. PerlinNoise2D.java
2. PerlinNoise3D.java
3. SimplexNoise2D.java
4. SimplexNoise3D.java
5. Simplex2D.java (atual)
6. Simplex3D.java (atual)
7. RidgeNoise2D.java 

## compatibilidade:
* Android 4 até Android 14.
* Linux 32-bit e 64-bit.
* Windows XP até Windows 10.

## Desempenho:
FPS de 30 a 55 padrão testado com até 111 chunks ativas (raio de 5).

## Mods Lua:
você pode criar mods achando a pasta *MiniMine/mods/* no armazenamento externo. Adicione os arquivos Lua necéssarios:

att.lua // será chamado no loop principal

e para adicionar mais de um arquivo individual, adicione o caminho relativo a pasta atual em *MiniMine/mods/arquivos.mini*. os arquivos são separados e carregados por quebra de linha.

## Mods JavaScript (descontinuado por consuno excessivo de Threads):

adicione seus scripts em "MiniMine/mods/arquivos.html".

sem documentação ainda.

## Adicionais:

caso o jogo crashe ou você não tenha visão completa dos logs, visite *MiniMine/debug/logs.txt*, onde logs são acumulados a cada entrada em um mundo.

## Dispositivos usados para testes:
Celular:
* Modelo: Motorola G41.
* Memória RAM: 4 GB.
* Armazenamento: 128 GB.
* Processador: 8 núcleos, velocidade clock 500 MHz - 2.00 GHz. ARM64.
* OpenGL ES: 3.2.
* JVM: Java VM ART 2.1.0.
* Sistema Operacional: Android 12 64-bit.
* FPS padrão: 29-59.

PC:
* Placa Mãe: dell optiPlex 780.
* Processador: intel core 2 quad.
* Memória RAM: 4GB(2x2GB DDR3).
* Armazenamento: SSD 256GB + HD 500GB.
* Video: intel 4 series(Integrada).
* OpenGL: 2.1.
* Sistema Operaciobal: Linux Mint 64-bit.
* FPS padrão: 50-100.

Notebook:
* Modelo: Aspire ES 15.
* Processador: Intel Celeron Quad Core N3450.
* Memória RAM: 4GB DDR3 L.
* Armazenamento: HD 500 GBs.
* Video: Intel HD Graphics.
* OpenGL: 4.5.
* Sistema Operacional: Windows 10.
* FPS padrão: 100-255.

# Comandos de teclado

## Créditos:

**Programação**:
Shiniga-OP
Green
Xaniim

**Efeitos sonoros**:
Shiniga-OP
VDLN7

**Pixel art**:
Shiniga-OP

**Teste da API de mods e documentação ou ambiente de execução**:
VDLN7
Green
Dorganhozo

**Canais:**:
Shiniga-OP: https://youtube.com/@shiniga-op?si=A78wk-sm3EJvgavE
VDLN7: https://youtube.com/@violetbrasilofc?si=Ip8AkZdPnDDdFjGm
Green: https://youtube.com/@greenlevelcreatordev?si=q1HhyS115FbbPhOI
Dorganhozo: https://youtube.com/@dorganzo?si=phKKbJ4P5C87TMJ0
