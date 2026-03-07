# MiniMine

refeito com LibGDX.

## Requisitos mínimos(testados):

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
* Gerenciamento de chunks dinâmico.
* Motor de geração.
* Colisão.
* Gravidade.
* Iluminação global.
* Interface de botões de movimentação.
* Debug visual.
* Sistema de construção.
* Barra Rápida.
* Gerenciador de recursos nativos.
* Sistema de ciclo noturno e diário.
* Nuvens.
* Salvamento dinamico de mundos binario.
* Aúdio (ainda temporario até aúdios gravados manualmente).
* Agachamento (evita cair de bordas dos blocos).
* Tela de menu e configurações padrão.
* Divisão de biomas.
* Água animada via *Gdx.gl.glTexSubImage2D(...)*.
* Névoa no horizonte.
* Inventario.
* Menu de pause.
* Partículas ao quebrar blocos.
* 1-2 músicas.
* Divisão de biomas com rúido celular.
* Entidades.
* Sistema de nascimento de entidades baseado em bioma.
* Auto atualização quando ligado a internet.
* MJson para análise e conversão de JSON string para objetos Java em Java 7.
* Biomas escritos em JSON.

## Modos de jogo:
* 0: espectador. Não sofre gravidade ou colisão com blocos. Seus recursos não acabam
* 1: criativo. Não sofre com gravidade mas colide com blocos. Seus recursos não acabam
* 2: sobrevivencia. Sofre com gravidade e colide com blocos. Seus recursos acabam.

## Blocos:
* Grama.
* Terra.
* Pedra.
* Água.
* Areia.
* Tronco de madeira.
* Bloco de folhas.
* Tabua de madeira.
* Cacto.
* Vidro.
* Tocha.
* Pedregulho.
* Cascalho.
* Bloco de gelo.
* Bloco de neve.
* Bloco de coral rosa.
* Bloco de coral azul
* Bloco de coral amarelo.
* Capim.
* Tulipa.
* Íris azul.

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
Camada de decoração.

## Entidades:
* Foca: Mar Congelado/Tundra.
* Capivara: Costa/Floresta.

## Biomas:

```
[DadosBioma]: carregado: costa
[DadosBioma]: carregado: deserto
[DadosBioma]: carregado: dunas
[DadosBioma]: carregado: floresta
[DadosBioma]: carregado: floresta_costeira
[DadosBioma]: carregado: montanha
[DadosBioma]: carregado: oceano
[DadosBioma]: carregado: oceano_abissal
[DadosBioma]: carregado: oceano_congelado
[DadosBioma]: carregado: oceano_quente
[DadosBioma]: carregado: picos_gelados
[DadosBioma]: carregado: planicie
[DadosBioma]: carregado: serrania
[DadosBioma]: carregado: tundra
```
em log mesmo porque eu to com preguiça de ficar listando.

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
* Operações bit a bit.
* Tabela de Log2 pra pacotes de chunks.
* Cache de alturas para geração de biomas.
* Travas de segurança para otimização de loops em caso de imutalidado do pacote.

## Ruídos utilitários:
* Simplex2D.java
* Simplex3D.java
* CristaRuido2D.java
* CelularRuido2D.java
* OpenSimplex2.java

## compatibilidade:
* Android 4 até Android 15.
* Linux 32-bit e 64-bit.
* Windows XP até Windows 11.

## Desempenho:
FPS de 40 a 60 padrão testado com até 121 chunks ativas (raio de 5).

## uso de mémoria testada:
50 MBs do heap java & 24 MBs do heap nativo. (121 chunks ativas)

## Adicionais:

caso o jogo crashe ou você não tenha visão completa dos logs, visite *MiniMine/debug/logs.txt*, onde logs são acumulados.

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
* Marin

**Canais:**
- [Shiniga-OP](https://youtube.com/@shiniga-op?si=A78wk-sm3EJvgavE)
- [VDLN7](https://youtube.com/@violetbrasilofc?si=Ip8AkZdPnDDdFjGm)
- [Green](https://youtube.com/@greenlevelcreatordev?si=q1HhyS115FbbPhOI)
- [Dorganhozo](https://youtube.com/@dorganzo?si=phKKbJ4P5C87TMJ0)
