import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import javax.imageio.ImageIO;

public class GenFonte {
    public static void main(String[] args) throws Exception {
        if(args.length < 3) {
            System.out.println("Uso: java GenFonte <arquivo.ttf> <tamanho> <nome-saida>");
            System.out.println("Exemplo: java GenFonte pixel.ttf 16 pixel-16");
            return;
        }
        String ttfCaminho = args[0];
        int tam = Integer.parseInt(args[1]);
        String saidaNome = args[2];
        if(saidaNome.endsWith(".fnt") || saidaNome.endsWith(".png"))
            saidaNome = saidaNome.substring(0, saidaNome.lastIndexOf('.'));

        String pngNome = saidaNome + ".png";
        String fntNome = saidaNome + ".fnt";

        Font fonte = Font.createFont(Font.TRUETYPE_FONT, new File(ttfCaminho)).deriveFont((float)tam);

        String chars =
            " !\"#$%&'()*+,-./" +
            "0123456789:;<=>?@" +
            "ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`" +
            "abcdefghijklmnopqrstuvwxyz{|}~" +
            "ÁÃÀÂÄÅÆÊÉÈËĖĘĒÜÚÛÙŪÎÍÌĮÏĪÓÕÔÒÖŒØŌČÇĆ" +
            "áãàâäåæªêéèëėęēüúûùūîíìįïīóõôòöœøōºčçć";

        BufferedImage tmp = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gtmp = tmp.createGraphics();
        gtmp.setFont(fonte);
        FontMetrics fm = gtmp.getFontMetrics();
        FontRenderContext frc = gtmp.getFontRenderContext();
        int ascendente = fm.getAscent();

        int espaco = 1;
        int paginaV = 512;

        // acha ymin/ymax real usando GlyphVector(pixels reais)
        int yminGlobal = Integer.MAX_VALUE;
        int ymaxGlobal = Integer.MIN_VALUE;
        for(int i = 0; i < chars.length(); i++) {
            char ch = chars.charAt(i);
            if(ch == ' ') continue;
            GlyphVector gv = fonte.createGlyphVector(frc, String.valueOf(ch));
            Rectangle2D vb = gv.getVisualBounds();
            int ymin = (int)Math.floor(vb.getY()) + ascendente;
            int ymax = (int)Math.ceil(vb.getY() + vb.getHeight()) + ascendente;
            if(ymin < yminGlobal) yminGlobal = ymin;
            if(ymax > ymaxGlobal) ymaxGlobal = ymax;
        }
        int glyphH = ymaxGlobal - yminGlobal;
        int linhaH = glyphH + espaco * 2;

        // monta lista de glifos: {codepoint, gw, xoff, xadv, tx, ty}
        ArrayList<int[]> glyphs = new ArrayList<int[]>();
        int x = espaco, y = espaco;
        for(int i = 0; i < chars.length(); i++) {
            char ch = chars.charAt(i);
            GlyphVector gv = fonte.createGlyphVector(frc, String.valueOf(ch));
            Rectangle2D vb = gv.getVisualBounds();
            int gw = (int)Math.ceil(vb.getWidth());
            int xoff = (int)Math.floor(vb.getX());
            int xadv = (int)Math.round(gv.getLogicalBounds().getWidth());

            int celulaV = Math.max(gw, 1) + espaco * 2;
            if(x + celulaV > paginaV) {
                x = espaco;
                y += linhaH;
            }
            glyphs.add(new int[]{(int) ch, gw, xoff, xadv, x, y});
            x += celulaV;
        }
        gtmp.dispose();

        int imgH = y + linhaH + espaco;
        int texH = 1;
        while(texH < imgH) texH <<= 1;

        BufferedImage img = new BufferedImage(paginaV, texH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        g.setFont(fonte);
        g.setColor(java.awt.Color.WHITE);

        for(int i = 0; i < glyphs.size(); i++) {
            int[] gl = glyphs.get(i);
            int cp = gl[0], gw = gl[1], xoff = gl[2], tx = gl[4], ty = gl[5];
            if(gw == 0) continue;
            g.drawString(String.valueOf((char) cp),
                tx + espaco - xoff,
                ty + espaco - yminGlobal + ascendente);
        }
        g.dispose();

        ImageIO.write(img, "PNG", new File(pngNome));

        int base = ascendente - yminGlobal + espaco;

        PrintWriter fw = new PrintWriter(new FileWriter(fntNome));
        fw.printf("info face=\"pixel\" size=%d bold=0 italic=0 charset=\"\" unicode=1 stretchH=100 smooth=0 aa=0 padding=%d,%d,%d,%d spacing=0,0%n",
            tam, espaco, espaco, espaco, espaco);
        fw.printf("common lineHeight=%d base=%d scaleW=%d scaleH=%d pages=1 packed=0%n",
            linhaH, base, paginaV, texH);
        fw.printf("page id=0 file=\"%s\"%n", pngNome);
        fw.printf("chars count=%d%n", glyphs.size());
        for (int i = 0; i < glyphs.size(); i++) {
            int[] gl = glyphs.get(i);
            fw.printf("char id=%d x=%d y=%d width=%d height=%d xoffset=0 yoffset=0 xadvance=%d page=0 chnl=15%n",
                gl[0], gl[4], gl[5], gl[1] + espaco * 2, linhaH, gl[3]);
        }
        fw.close();

        System.out.printf("Gerado: %s + %s  (%dx%d)%n", fntNome, pngNome, paginaV, texH);
        System.out.printf("  ascendente=%d ymin=%d ymax=%d glyphH=%d base=%d%n",
            ascendente, yminGlobal, ymaxGlobal, glyphH, base);
    }
}