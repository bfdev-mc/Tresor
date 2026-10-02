import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.imageio.ImageIO;

/**
 * Erzeugt das Tresor-Resource-Pack (Texturen, Modelle, GUI-Hintergrund) komplett per Code.
 * Aufruf: java tools/GeneratePack.java   (im Projektordner)
 * Ergebnis: resourcepack/ (Quelle), build/TresorPack.zip und build/preview_*.png (Vorschau)
 */
public class GeneratePack {
    static final Path ROOT = Path.of("resourcepack");
    static final Path ASSETS = ROOT.resolve("assets/tresor");

    // Gunmetal-Palette
    static final Color M0 = new Color(30, 33, 38), M1 = new Color(50, 54, 62), M2 = new Color(72, 78, 88),
            M3 = new Color(98, 105, 117), M4 = new Color(132, 140, 153), M5 = new Color(172, 180, 192),
            M6 = new Color(214, 220, 229);
    static final Color GOLD0 = new Color(120, 84, 22), GOLD1 = new Color(176, 130, 40), GOLD2 = new Color(224, 182, 70),
            GOLD3 = new Color(255, 232, 150);
    static final Color RED = new Color(214, 54, 44), GREEN = new Color(80, 232, 120);

    // ------------------------------------------------------------------ Modell-Beschreibung

    /** Quader mit Texturen je Seite (Namen aus der Textur-Map des Modells). */
    record Box(double x1, double y1, double z1, double x2, double y2, double z2,
               String north, String east, String south, String west, String up, String down) {
        static Box of(double x1, double y1, double z1, double x2, double y2, double z2, String front, String other) {
            return new Box(x1, y1, z1, x2, y2, z2, other, other, front, other, other, other);
        }

        String json() {
            return "{\"from\":[" + x1 + "," + y1 + "," + z1 + "],\"to\":[" + x2 + "," + y2 + "," + z2 + "],\"faces\":{"
                    + face("north", north) + "," + face("east", east) + "," + face("south", south) + ","
                    + face("west", west) + "," + face("up", up) + "," + face("down", down) + "}}";
        }

        static String face(String n, String t) {
            return "\"" + n + "\":{\"texture\":\"#" + t + "\"}";
        }
    }

    static List<Box> smallSafe() {
        List<Box> b = new ArrayList<>();
        b.add(new Box(0, 0, 0, 16, 16, 16, "body", "body", "frame", "body", "top", "body"));
        b.add(Box.of(1.5, 1.5, 16, 14.5, 14.5, 16.6, "door", "frame"));          // Tuerplatte
        b.add(Box.of(4.5, 7, 16.6, 8.5, 11, 17.5, "knob", "chrome"));            // Drehknopf
        b.add(Box.of(10.4, 6.6, 16.6, 12.6, 8.8, 17.2, "chrome", "chrome"));      // Griff-Nabe
        b.add(Box.of(10.9, 3.6, 17.2, 12.1, 8.2, 18.2, "chrome", "chrome"));      // Griff
        b.add(Box.of(0.2, 3, 16, 1.4, 5.6, 17, "frame", "frame"));               // Scharniere
        b.add(Box.of(0.2, 10.4, 16, 1.4, 13, 17, "frame", "frame"));
        return b;
    }

    static List<Box> largeSafe() {
        List<Box> b = new ArrayList<>();
        b.add(new Box(0, 0, 0, 16, 16, 16, "body", "body", "frame", "body", "top", "body"));
        b.add(Box.of(1, 1, 16, 15, 15, 16.6, "door", "frame"));
        b.add(Box.of(3.4, 6.4, 16.6, 6.6, 9.6, 17.4, "knob", "chrome"));         // Rad-Nabe links
        b.add(Box.of(7.1, 4, 16.6, 8.1, 11, 17.5, "chrome", "chrome"));          // Griff links
        b.add(Box.of(8.1, 4, 16.6, 8.9, 11, 17.5, "chrome", "chrome"));          // Griff rechts
        b.add(Box.of(0.2, 2.6, 16, 1.4, 5.2, 17, "frame", "frame"));
        b.add(Box.of(0.2, 10.8, 16, 1.4, 13.4, 17, "frame", "frame"));
        b.add(Box.of(14.6, 2.6, 16, 15.8, 5.2, 17, "frame", "frame"));
        b.add(Box.of(14.6, 10.8, 16, 15.8, 13.4, 17, "frame", "frame"));
        return b;
    }

    public static void main(String[] a) throws Exception {
        if (Files.exists(ROOT)) {
            try (var s = Files.walk(ROOT)) {
                s.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
            }
        }
        Files.createDirectories(Path.of("build"));
        write(ROOT.resolve("pack.mcmeta"), """
                {"pack":{"description":"Tresor - Tresor-Block und Zahlenfeld","min_format":[82,0],"max_format":[999,0]}}
                """);

        // ---- Texturen
        Map<String, BufferedImage> small = Map.of(
                "body", body(M3, M4, M1, false), "top", top(M3, M4, M1, false), "frame", frame(M1, M2, M0, false),
                "door", doorSmall(), "knob", knob(false), "chrome", chrome());
        Map<String, BufferedImage> large = Map.of(
                "body", body(M2, M3, M0, true), "top", top(M2, M3, M0, true), "frame", frame(M0, M1, M0, true),
                "door", doorLarge(), "knob", knob(true), "chrome", chromeGold());
        for (var e : small.entrySet()) save("textures/block/small_" + e.getKey(), e.getValue());
        for (var e : large.entrySet()) save("textures/block/large_" + e.getKey(), e.getValue());

        // ---- Block-Modelle
        model("safe_small", "small", smallSafe());
        model("safe_large", "large", largeSafe());
        for (String n : new String[] {"safe_small", "safe_large"}) {
            write(ASSETS.resolve("items/" + n + ".json"),
                    "{\"model\":{\"type\":\"minecraft:model\",\"model\":\"tresor:block/" + n + "\"}}");
        }
        preview("build/preview_small.png", smallSafe(), small);
        preview("build/preview_large.png", largeSafe(), large);

        // ---- GUI-Items
        for (int i = 0; i <= 9; i++) digitKey("key_" + i, String.valueOf(i));
        save("textures/item/key_clear", symbolKey(false));
        save("textures/item/key_ok", symbolKey(true));
        save("textures/item/lcd_on", lcd(true));
        save("textures/item/lcd_off", lcd(false));
        for (String n : new String[] {"key_0", "key_1", "key_2", "key_3", "key_4", "key_5", "key_6", "key_7",
                "key_8", "key_9", "key_clear", "key_ok", "lcd_on", "lcd_off"}) {
            write(ASSETS.resolve("models/item/" + n + ".json"),
                    "{\"parent\":\"minecraft:item/generated\",\"textures\":{\"layer0\":\"tresor:item/" + n + "\"}}");
            write(ASSETS.resolve("items/" + n + ".json"),
                    "{\"model\":{\"type\":\"minecraft:model\",\"model\":\"tresor:item/" + n + "\"}}");
        }

        // ---- GUI-Hintergrund (ueber die Titel-Schrift)
        BufferedImage bg = keypadBackground();
        save("textures/font/keypad", bg);
        write(ASSETS.resolve("font/gui.json"), """
                {"providers":[
                 {"type":"space","advances":{"\\uF000":-8}},
                 {"type":"bitmap","file":"tresor:font/keypad.png","ascent":13,"height":186,"chars":["\\uE000"]}
                ]}
                """);
        ImageIO.write(bg, "png", new File("build/preview_gui.png"));

        zip();
        System.out.println("Pack erzeugt.");
    }

    // ------------------------------------------------------------------ Zeichen-Helfer

    static final int T = 64;

    static BufferedImage img(int w, int h) {
        return new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
    }

    static Graphics2D gfx(BufferedImage i) {
        Graphics2D g = i.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        return g;
    }

    static Color shade(Color c, int d) {
        return new Color(Math.max(0, Math.min(255, c.getRed() + d)), Math.max(0, Math.min(255, c.getGreen() + d)),
                Math.max(0, Math.min(255, c.getBlue() + d)), c.getAlpha());
    }

    static Color alpha(Color c, int a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), a);
    }

    /** Gebuerstetes Metall mit leichtem vertikalem Verlauf. */
    static void brushed(BufferedImage im, int x, int y, int w, int h, Color base, long seed) {
        Random r = new Random(seed);
        for (int j = 0; j < h; j++) {
            int row = r.nextInt(7) - 3;
            int grad = (int) ((0.5 - (double) j / h) * 14);
            for (int i = 0; i < w; i++) {
                im.setRGB(x + i, y + j, shade(base, row + grad + r.nextInt(5) - 2).getRGB());
            }
        }
    }

    static void bevel(BufferedImage im, int x, int y, int w, int h, int t, Color light, Color dark, boolean raised) {
        Color tl = raised ? light : dark, br = raised ? dark : light;
        for (int k = 0; k < t; k++) {
            for (int i = k; i < w - k; i++) {
                im.setRGB(x + i, y + k, tl.getRGB());
                im.setRGB(x + i, y + h - 1 - k, br.getRGB());
            }
            for (int j = k; j < h - k; j++) {
                im.setRGB(x + k, y + j, tl.getRGB());
                im.setRGB(x + w - 1 - k, y + j, br.getRGB());
            }
        }
    }

    static void rivet(Graphics2D g, double cx, double cy, double r, Color light, Color dark) {
        g.setPaint(new RadialGradientPaint((float) (cx - r * 0.35), (float) (cy - r * 0.35), (float) (r * 1.6),
                new float[] {0f, 1f}, new Color[] {light, dark}));
        g.fill(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
        g.setColor(alpha(Color.BLACK, 90));
        g.draw(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
    }

    // ------------------------------------------------------------------ Block-Texturen

    static BufferedImage body(Color base, Color light, Color dark, boolean gold) {
        BufferedImage im = img(T, T);
        brushed(im, 0, 0, T, T, base, 3);
        bevel(im, 0, 0, T, T, 3, light, dark, true);
        Graphics2D g = gfx(im);
        g.setColor(alpha(dark, 150));
        g.draw(new RoundRectangle2D.Double(7.5, 7.5, T - 15, T - 15, 4, 4));
        g.setColor(alpha(light, 90));
        g.draw(new RoundRectangle2D.Double(8.5, 8.5, T - 15, T - 15, 4, 4));
        if (gold) {
            g.setColor(GOLD1);
            g.draw(new RoundRectangle2D.Double(4.5, 4.5, T - 9, T - 9, 3, 3));
        }
        for (int[] p : new int[][] {{12, 12}, {51, 12}, {12, 51}, {51, 51}}) rivet(g, p[0], p[1], 2.2, light, dark);
        g.dispose();
        return im;
    }

    static BufferedImage top(Color base, Color light, Color dark, boolean gold) {
        BufferedImage im = body(base, light, dark, gold);
        Graphics2D g = gfx(im);
        for (int k = 0; k < 5; k++) {
            g.setColor(alpha(Color.BLACK, 120));
            g.fillRoundRect(20, 22 + k * 5, 24, 2, 2, 2);
            g.setColor(alpha(light, 100));
            g.fillRect(21, 24 + k * 5, 22, 1);
        }
        g.dispose();
        return im;
    }

    /** Dunkler Rahmen, auf dem die Tuerplatte sitzt. */
    static BufferedImage frame(Color base, Color light, Color dark, boolean gold) {
        BufferedImage im = img(T, T);
        brushed(im, 0, 0, T, T, base, 5);
        bevel(im, 0, 0, T, T, 2, light, dark, true);
        Graphics2D g = gfx(im);
        if (gold) {
            g.setColor(GOLD1);
            g.draw(new RoundRectangle2D.Double(3.5, 3.5, T - 7, T - 7, 2, 2));
        }
        g.dispose();
        return im;
    }

    static BufferedImage chrome() {
        return metalStrip(M6, M2);
    }

    static BufferedImage chromeGold() {
        return metalStrip(GOLD3, GOLD0);
    }

    static BufferedImage metalStrip(Color hi, Color lo) {
        BufferedImage im = img(16, 16);
        Graphics2D g = gfx(im);
        g.setPaint(new GradientPaint(0, 0, hi, 16, 0, lo));
        g.fillRect(0, 0, 16, 16);
        g.setColor(alpha(Color.WHITE, 120));
        g.fillRect(2, 0, 2, 16);
        g.setColor(alpha(Color.BLACK, 80));
        g.fillRect(13, 0, 3, 16);
        g.dispose();
        return im;
    }

    /** Drehknopf von vorn: geriffelter Chrom-Kreis. */
    static BufferedImage knob(boolean gold) {
        BufferedImage im = img(32, 32);
        Graphics2D g = gfx(im);
        Color hi = gold ? GOLD3 : M6, mid = gold ? GOLD1 : M3, lo = gold ? GOLD0 : M1;
        g.setPaint(new RadialGradientPaint(12, 11, 22, new float[] {0f, 0.6f, 1f}, new Color[] {hi, mid, lo}));
        g.fillRect(0, 0, 32, 32);
        g.setColor(alpha(Color.BLACK, 70));
        for (int i = 3; i < 32; i += 4) g.drawLine(i, 0, i, 32);
        g.setColor(alpha(Color.BLACK, 140));
        g.drawRect(0, 0, 31, 31);
        g.dispose();
        return im;
    }

    /** Tuerplatte klein: grosses Zahlenschloss, Griff-Sockel, rote LED. */
    static BufferedImage doorSmall() {
        BufferedImage im = img(T, T);
        brushed(im, 0, 0, T, T, M4, 9);
        bevel(im, 0, 0, T, T, 2, M6, M1, true);
        Graphics2D g = gfx(im);
        g.setColor(alpha(M1, 140));
        g.draw(new RoundRectangle2D.Double(5.5, 5.5, T - 11, T - 11, 4, 4));
        g.setColor(alpha(M6, 90));
        g.draw(new RoundRectangle2D.Double(6.5, 6.5, T - 11, T - 11, 4, 4));

        // Ziffernblatt: Mittelpunkt entspricht der Position des Drehknopfs (Block 6.5/9)
        double cx = 24.6, cy = 27;
        g.setPaint(new GradientPaint(0, (float) (cy - 19), M5, 0, (float) (cy + 19), M1));
        g.fill(new Ellipse2D.Double(cx - 19, cy - 19, 38, 38));
        g.setColor(M0);
        g.fill(new Ellipse2D.Double(cx - 16.5, cy - 16.5, 33, 33));
        g.setColor(M2);
        g.fill(new Ellipse2D.Double(cx - 15, cy - 15, 30, 30));
        g.setStroke(new BasicStroke(1.2f));
        for (int k = 0; k < 40; k++) {
            double ang = Math.toRadians(k * 9 - 90);
            boolean major = k % 5 == 0;
            double r1 = major ? 10.5 : 12.5, r2 = 14;
            g.setColor(major ? M6 : M4);
            g.draw(new Line2D.Double(cx + Math.cos(ang) * r1, cy + Math.sin(ang) * r1,
                    cx + Math.cos(ang) * r2, cy + Math.sin(ang) * r2));
        }
        Path2D tri = new Path2D.Double();
        tri.moveTo(cx, cy - 20.5);
        tri.lineTo(cx - 2.5, cy - 24.5);
        tri.lineTo(cx + 2.5, cy - 24.5);
        tri.closePath();
        g.setColor(RED);
        g.fill(tri);

        // Griff-Sockel rechts
        g.setPaint(new GradientPaint(48, 0, M1, 58, 0, M3));
        g.fill(new RoundRectangle2D.Double(46, 16, 12, 38, 6, 6));
        g.setColor(M0);
        g.draw(new RoundRectangle2D.Double(46, 16, 12, 38, 6, 6));
        rivet(g, 52, 11, 2.4, new Color(255, 140, 120), RED);
        for (int[] p : new int[][] {{10, 10}, {10, 54}, {54, 58}}) rivet(g, p[0], p[1], 2, M5, M1);
        g.dispose();
        return im;
    }

    /** Tuerplatte gross: links Rad-Schloss, rechts Zahlenfeld, Mittelspalt, Goldzierleisten. */
    static BufferedImage doorLarge() {
        BufferedImage im = img(T, T);
        brushed(im, 0, 0, T, T, M2, 12);
        bevel(im, 0, 0, T, T, 2, M4, M0, true);
        Graphics2D g = gfx(im);
        g.setColor(GOLD1);
        g.draw(new RoundRectangle2D.Double(3.5, 3.5, T - 7, T - 7, 3, 3));
        // Mittelspalt
        g.setColor(M0);
        g.fillRect(30, 4, 4, T - 8);
        g.setColor(alpha(M4, 160));
        g.drawLine(29, 4, 29, T - 5);
        g.setColor(alpha(GOLD2, 160));
        g.drawLine(34, 4, 34, T - 5);

        // Linke Tuer: Rad
        double cx = 16, cy = 32;
        g.setPaint(new GradientPaint(0, (float) (cy - 14), GOLD2, 0, (float) (cy + 14), GOLD0));
        g.setStroke(new BasicStroke(3.2f));
        g.draw(new Ellipse2D.Double(cx - 12, cy - 12, 24, 24));
        g.setStroke(new BasicStroke(2f));
        g.setColor(GOLD1);
        for (int k = 0; k < 6; k++) {
            double ang = Math.toRadians(k * 60);
            g.draw(new Line2D.Double(cx, cy, cx + Math.cos(ang) * 12, cy + Math.sin(ang) * 12));
        }
        g.setColor(alpha(Color.BLACK, 70));
        g.setStroke(new BasicStroke(1f));
        g.draw(new Ellipse2D.Double(cx - 13.5, cy - 13.5, 27, 27));

        // Rechte Tuer: Zahlenfeld
        g.setColor(M0);
        g.fill(new RoundRectangle2D.Double(38, 10, 20, 44, 5, 5));
        g.setColor(GOLD1);
        g.draw(new RoundRectangle2D.Double(38, 10, 20, 44, 5, 5));
        g.setColor(new Color(14, 36, 24));
        g.fill(new RoundRectangle2D.Double(41, 14, 14, 8, 2, 2));
        g.setColor(GREEN);
        for (int k = 0; k < 4; k++) g.fillRect(43 + k * 3, 17, 2, 2);
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 3; c++) {
                g.setPaint(new GradientPaint(0, 26 + r * 6, M5, 0, 30 + r * 6, M3));
                g.fill(new RoundRectangle2D.Double(41 + c * 5, 26 + r * 6, 4, 4, 1, 1));
            }
        }
        rivet(g, 8, 8, 2, GOLD3, GOLD0);
        rivet(g, 56, 8, 2, GOLD3, GOLD0);
        rivet(g, 8, 56, 2, GOLD3, GOLD0);
        rivet(g, 56, 56, 2, GOLD3, GOLD0);
        g.dispose();
        return im;
    }

    // ------------------------------------------------------------------ GUI

    static void digitKey(String name, String label) throws IOException {
        BufferedImage im = img(32, 32);
        Graphics2D g = gfx(im);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22));
        FontMetrics fm = g.getFontMetrics();
        int x = (32 - fm.stringWidth(label)) / 2, y = (32 - fm.getHeight()) / 2 + fm.getAscent() - 1;
        g.setColor(alpha(Color.BLACK, 150));
        g.drawString(label, x + 1, y + 2);
        g.setColor(new Color(236, 240, 246));
        g.drawString(label, x, y);
        g.dispose();
        save("textures/item/" + name, im);
    }

    static BufferedImage symbolKey(boolean ok) {
        BufferedImage im = img(32, 32);
        Graphics2D g = gfx(im);
        g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        if (ok) {
            Path2D p = new Path2D.Double();
            p.moveTo(8, 17);
            p.lineTo(14, 23);
            p.lineTo(25, 9);
            g.setColor(alpha(Color.BLACK, 140));
            g.translate(1, 1.5);
            g.draw(p);
            g.translate(-1, -1.5);
            g.setColor(new Color(150, 255, 175));
            g.draw(p);
        } else {
            g.setColor(alpha(Color.BLACK, 140));
            g.draw(new Line2D.Double(10, 11.5, 22, 23.5));
            g.draw(new Line2D.Double(22, 11.5, 10, 23.5));
            g.setColor(new Color(255, 150, 140));
            g.draw(new Line2D.Double(9, 10, 21, 22));
            g.draw(new Line2D.Double(21, 10, 9, 22));
        }
        g.dispose();
        return im;
    }

    static BufferedImage lcd(boolean on) {
        BufferedImage im = img(32, 32);
        Graphics2D g = gfx(im);
        if (on) {
            g.setPaint(new RadialGradientPaint(16, 16, 13, new float[] {0f, 0.5f, 1f},
                    new Color[] {alpha(GREEN, 230), alpha(GREEN, 120), alpha(GREEN, 0)}));
            g.fillRect(0, 0, 32, 32);
            g.setColor(new Color(220, 255, 230));
            g.fill(new Ellipse2D.Double(12.5, 12.5, 7, 7));
        }
        g.dispose();
        return im;
    }

    /** 176x186: Hintergrund eines 4-Reihen-Containers. */
    static BufferedImage keypadBackground() {
        int w = 176, h = 186;
        BufferedImage im = img(w, h);
        Graphics2D g = gfx(im);
        g.setPaint(new GradientPaint(0, 0, new Color(58, 63, 72), 0, h, new Color(34, 37, 44)));
        g.fill(new RoundRectangle2D.Double(0, 0, w, h, 8, 8));
        g.setColor(new Color(10, 11, 14));
        g.setStroke(new BasicStroke(1f));
        g.draw(new RoundRectangle2D.Double(0.5, 0.5, w - 1, h - 1, 8, 8));
        g.setColor(alpha(GOLD1, 200));
        g.draw(new RoundRectangle2D.Double(3.5, 3.5, w - 7, h - 7, 6, 6));
        g.setColor(alpha(Color.WHITE, 28));
        g.draw(new RoundRectangle2D.Double(1.5, 1.5, w - 3, h - 3, 7, 7));

        // Anzeige (Reihe 0): Display mit neun Mulden
        g.setPaint(new GradientPaint(0, 16, new Color(9, 20, 14), 0, 37, new Color(18, 40, 28)));
        g.fill(new RoundRectangle2D.Double(7, 17, 162, 20, 5, 5));
        g.setColor(new Color(0, 0, 0, 160));
        g.draw(new RoundRectangle2D.Double(7, 17, 162, 20, 5, 5));
        g.setColor(alpha(GREEN, 40));
        g.draw(new RoundRectangle2D.Double(8, 18, 160, 18, 4, 4));
        for (int c = 0; c < 9; c++) {
            g.setColor(new Color(60, 120, 80, 90));
            g.fill(new Ellipse2D.Double(8 + 18 * c + 6, 18 + 6, 6, 6));
        }

        // Tasten
        int[] digitSlots = {11, 12, 13, 14, 15, 20, 21, 22, 23, 24};
        for (int s : digitSlots) {
            button(g, 8 + 18 * (s % 9), 18 + 18 * (s / 9), new Color(86, 93, 106), new Color(52, 57, 66));
        }
        button(g, 8 + 18 * (30 % 9), 18 + 18 * (30 / 9), new Color(150, 58, 52), new Color(98, 34, 32));
        button(g, 8 + 18 * (32 % 9), 18 + 18 * (32 / 9), new Color(52, 138, 78), new Color(30, 90, 52));

        // Trennlinie + Spielerinventar
        g.setColor(alpha(Color.BLACK, 120));
        g.fillRect(8, 98, w - 16, 1);
        g.setColor(alpha(Color.WHITE, 30));
        g.fillRect(8, 99, w - 16, 1);
        for (int r = 0; r < 3; r++) for (int c = 0; c < 9; c++) slot(g, 8 + c * 18, 103 + r * 18);
        for (int c = 0; c < 9; c++) slot(g, 8 + c * 18, 161);
        g.dispose();
        return im;
    }

    /** Taste: Item-Flaeche beginnt bei (x, y), 16x16. Zelle 18x18. */
    static void button(Graphics2D g, int x, int y, Color top, Color bottom) {
        g.setColor(alpha(Color.BLACK, 140));
        g.fill(new RoundRectangle2D.Double(x - 1, y, 18, 18, 5, 5));
        g.setPaint(new GradientPaint(0, y - 1, top, 0, y + 17, bottom));
        g.fill(new RoundRectangle2D.Double(x - 1, y - 1, 18, 18, 5, 5));
        g.setColor(alpha(Color.WHITE, 70));
        g.draw(new RoundRectangle2D.Double(x - 0.5, y - 0.5, 17, 17, 5, 5));
    }

    static void slot(Graphics2D g, int x, int y) {
        g.setColor(new Color(20, 22, 27, 200));
        g.fill(new RoundRectangle2D.Double(x - 1, y - 1, 18, 18, 3, 3));
        g.setColor(alpha(Color.WHITE, 38));
        g.draw(new RoundRectangle2D.Double(x - 0.5, y - 0.5, 17, 17, 3, 3));
    }

    // ------------------------------------------------------------------ Modelle

    static void model(String name, String prefix, List<Box> boxes) throws IOException {
        StringBuilder els = new StringBuilder();
        for (Box b : boxes) els.append(els.length() > 0 ? "," : "").append(b.json());
        StringBuilder json = new StringBuilder("{\"parent\":\"minecraft:block/block\",\"textures\":{"
                + "\"particle\":\"tresor:block/" + prefix + "_body\"");
        for (String t : new String[] {"body", "top", "frame", "door", "knob", "chrome"}) {
            json.append(",\"").append(t).append("\":\"tresor:block/").append(prefix).append("_").append(t).append("\"");
        }
        json.append("},\"elements\":[").append(els).append("]}");
        write(ASSETS.resolve("models/block/" + name + ".json"), json.toString());
    }

    /** Isometrische Vorschau (Vorderseite links, Ostseite rechts, Oberseite). */
    static void preview(String file, List<Box> boxes, Map<String, BufferedImage> tex) throws IOException {
        double s = 14, c30 = 0.866;
        BufferedImage out = new BufferedImage(520, 520, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = gfx(out);
        g.setColor(new Color(120, 160, 120));
        g.fillRect(0, 0, 520, 520);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        double ox = 300, oy = 400;
        List<Box> sorted = new ArrayList<>(boxes);
        sorted.sort(Comparator.comparingDouble(Box::z2).thenComparingDouble(Box::x2));
        for (Box b : sorted) {
            double dx = b.x2() - b.x1(), dy = b.y2() - b.y1(), dz = b.z2() - b.z1();
            BufferedImage e = tex.get(b.east());
            g.drawImage(e, new AffineTransform(c30 * s * dz / e.getWidth(), -0.5 * s * dz / e.getWidth(),
                    0, s * dy / e.getHeight(),
                    ox + (b.x2() - b.z2()) * c30 * s, oy + (b.x2() + b.z2()) * 0.5 * s - b.y2() * s), null);
            BufferedImage u = tex.get(b.up());
            g.drawImage(u, new AffineTransform(c30 * s * dx / u.getWidth(), 0.5 * s * dx / u.getWidth(),
                    -c30 * s * dz / u.getHeight(), 0.5 * s * dz / u.getHeight(),
                    ox + (b.x1() - b.z1()) * c30 * s, oy + (b.x1() + b.z1()) * 0.5 * s - b.y2() * s), null);
            BufferedImage f = tex.get(b.south());
            g.drawImage(f, new AffineTransform(c30 * s * dx / f.getWidth(), 0.5 * s * dx / f.getWidth(),
                    0, s * dy / f.getHeight(),
                    ox + (b.x1() - b.z2()) * c30 * s, oy + (b.x1() + b.z2()) * 0.5 * s - b.y2() * s), null);
        }
        g.dispose();
        ImageIO.write(out, "png", new File(file));
    }

    // ------------------------------------------------------------------ Dateien

    static void save(String path, BufferedImage im) throws IOException {
        Path p = ASSETS.resolve(path + ".png");
        Files.createDirectories(p.getParent());
        ImageIO.write(im, "png", p.toFile());
    }

    static void write(Path p, String s) throws IOException {
        Files.createDirectories(p.getParent());
        Files.writeString(p, s, StandardCharsets.UTF_8);
    }

    static void zip() throws IOException {
        File out = new File("build/TresorPack.zip");
        try (ZipOutputStream z = new ZipOutputStream(new FileOutputStream(out)); var s = Files.walk(ROOT)) {
            for (Path p : (Iterable<Path>) s.filter(Files::isRegularFile).sorted()::iterator) {
                z.putNextEntry(new ZipEntry(ROOT.relativize(p).toString().replace('\\', '/')));
                z.write(Files.readAllBytes(p));
                z.closeEntry();
            }
        }
    }
}
