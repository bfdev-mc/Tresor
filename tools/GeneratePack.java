import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.imageio.ImageIO;

/**
 * Erzeugt das Tresor-Resource-Pack (Texturen, Modelle, GUI-Hintergrund) komplett per Code.
 * Aufruf: java tools/GeneratePack.java   (im Projektordner)
 * Ergebnis: resourcepack/ (Quelle) und build/TresorPack.zip
 */
public class GeneratePack {
    static final Path ROOT = Path.of("resourcepack");
    static final Path ASSETS = ROOT.resolve("assets/tresor");

    // Paletten
    static final Color STEEL = new Color(142, 148, 158), STEEL_D = new Color(88, 94, 105), STEEL_L = new Color(196, 202, 212);
    static final Color DARK = new Color(52, 56, 64), DARKER = new Color(34, 37, 43);
    static final Color GOLD = new Color(222, 180, 60), GOLD_D = new Color(150, 112, 28), GOLD_L = new Color(255, 232, 140);
    static final Color NAVY = new Color(70, 78, 96), NAVY_D = new Color(44, 50, 64), NAVY_L = new Color(118, 128, 150);

    public static void main(String[] a) throws Exception {
        if (Files.exists(ROOT)) {
            try (var s = Files.walk(ROOT)) {
                s.sorted(java.util.Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
            }
        }
        write(ROOT.resolve("pack.mcmeta"), """
                {"pack":{"description":"Tresor - Tresor-Block und Zahlenfeld","min_format":[82,0],"max_format":[999,0]}}
                """);

        // ---- Block-Texturen
        save("textures/block/safe_side", side(STEEL, STEEL_D, STEEL_L, GOLD, false));
        save("textures/block/safe_side_large", side(NAVY, NAVY_D, NAVY_L, GOLD, true));
        save("textures/block/safe_front_small", frontSmall());
        BufferedImage left = frontLarge(true), right = frontLarge(false);
        BufferedImage both = new BufferedImage(64, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = both.createGraphics();
        g.drawImage(left, 0, 0, null);
        g.drawImage(right, 32, 0, null);
        g.dispose();
        save("textures/block/safe_front_large_item", both);
        save("textures/block/safe_handle", handle());

        // ---- Block-Modelle
        model("safe_small", "safe_side", "safe_front_small", "safe_handle",
                box(0, 0, 0, 16, 16, 16, true),
                box(11, 4, 16, 13, 12, 17.5, false),     // Griff
                box(4.5, 6.5, 16, 7.5, 9.5, 17, false)); // Drehknopf
        model("safe_large", "safe_side_large", "safe_front_large_item", "safe_handle",
                box(0, 0, 0, 16, 16, 16, true),
                box(5.5, 4, 16, 7.2, 12, 17.5, false),
                box(8.8, 4, 16, 10.5, 12, 17.5, false));
        for (String n : new String[] {"safe_small", "safe_large"}) {
            write(ASSETS.resolve("items/" + n + ".json"),
                    "{\"model\":{\"type\":\"minecraft:model\",\"model\":\"tresor:block/" + n + "\"}}");
        }

        // ---- GUI-Items
        for (int i = 0; i <= 9; i++) key("key_" + i, DARK, String.valueOf(i), Color.WHITE);
        key("key_clear", new Color(150, 40, 40), "C", Color.WHITE);
        key("key_ok", new Color(40, 130, 60), "OK", Color.WHITE);
        save("textures/item/lock", lockIcon());
        save("textures/item/lcd_on", lcd(true));
        save("textures/item/lcd_off", lcd(false));
        for (String n : new String[] {"key_0", "key_1", "key_2", "key_3", "key_4", "key_5", "key_6", "key_7",
                "key_8", "key_9", "key_clear", "key_ok", "lcd_on", "lcd_off", "lock"}) {
            write(ASSETS.resolve("models/item/" + n + ".json"),
                    "{\"parent\":\"minecraft:item/generated\",\"textures\":{\"layer0\":\"tresor:item/" + n + "\"}}");
            write(ASSETS.resolve("items/" + n + ".json"),
                    "{\"model\":{\"type\":\"minecraft:model\",\"model\":\"tresor:item/" + n + "\"}}");
        }

        // ---- GUI-Hintergrund (ueber die Titel-Schrift)
        save("textures/font/keypad", keypadBackground());
        write(ASSETS.resolve("font/gui.json"), """
                {"providers":[
                 {"type":"space","advances":{"\\uF000":-8}},
                 {"type":"bitmap","file":"tresor:font/keypad.png","ascent":13,"height":186,"chars":["\\uE000"]}
                ]}
                """);

        zip();
        System.out.println("Pack erzeugt.");
    }

    // ------------------------------------------------------------------ Texturen

    static BufferedImage img(int w, int h) {
        return new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
    }

    static Graphics2D gfx(BufferedImage i) {
        Graphics2D g = i.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        return g;
    }

    static Color shade(Color c, int d) {
        return new Color(Math.max(0, Math.min(255, c.getRed() + d)), Math.max(0, Math.min(255, c.getGreen() + d)),
                Math.max(0, Math.min(255, c.getBlue() + d)), c.getAlpha());
    }

    /** Gebuerstetes Metall. */
    static void brushed(BufferedImage im, int x, int y, int w, int h, Color base, long seed) {
        Random r = new Random(seed);
        for (int j = 0; j < h; j++) {
            int rowShift = r.nextInt(9) - 4;
            for (int i = 0; i < w; i++) {
                im.setRGB(x + i, y + j, shade(base, rowShift + r.nextInt(5) - 2).getRGB());
            }
        }
    }

    /** Abgeschraegter Rahmen. raised = erhoben, sonst eingelassen. */
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

    static void rivet(BufferedImage im, int cx, int cy, Color light, Color dark) {
        im.setRGB(cx, cy, light.getRGB());
        im.setRGB(cx + 1, cy, shade(dark, 20).getRGB());
        im.setRGB(cx, cy + 1, shade(dark, 20).getRGB());
        im.setRGB(cx + 1, cy + 1, dark.getRGB());
    }

    static BufferedImage side(Color base, Color dark, Color light, Color accent, boolean trim) {
        BufferedImage im = img(32, 32);
        brushed(im, 0, 0, 32, 32, base, 7);
        bevel(im, 0, 0, 32, 32, 2, light, dark, true);
        if (trim) {
            bevel(im, 3, 3, 26, 26, 1, GOLD_L, GOLD_D, true);
        }
        for (int[] p : new int[][] {{4, 4}, {26, 4}, {4, 26}, {26, 26}}) rivet(im, p[0], p[1], light, dark);
        return im;
    }

    static BufferedImage doorBase(Color base, Color dark, Color light, boolean gold) {
        BufferedImage im = img(32, 32);
        brushed(im, 0, 0, 32, 32, base, 11);
        bevel(im, 0, 0, 32, 32, 2, light, dark, true);
        brushed(im, 4, 4, 24, 24, shade(base, -14), 13);
        bevel(im, 3, 3, 26, 26, 1, dark, light, false);
        bevel(im, 4, 4, 24, 24, 1, gold ? GOLD_L : light, gold ? GOLD_D : dark, true);
        for (int[] p : new int[][] {{1, 1}, {29, 1}, {1, 29}, {29, 29}}) rivet(im, p[0], p[1], light, dark);
        return im;
    }

    static BufferedImage frontSmall() {
        BufferedImage im = doorBase(STEEL, STEEL_D, STEEL_L, false);
        Graphics2D g = gfx(im);
        // Zahlenschloss: Ring + Striche, Mittelpunkt (12,16)
        g.setColor(DARKER);
        g.fillOval(5, 9, 14, 14);
        g.setColor(STEEL_L);
        g.setStroke(new BasicStroke(1f));
        g.drawOval(5, 9, 14, 14);
        g.setColor(new Color(235, 235, 235));
        for (int k = 0; k < 12; k++) {
            double ang = Math.toRadians(k * 30);
            int x1 = (int) Math.round(12 + Math.cos(ang) * 5.2), y1 = (int) Math.round(16 + Math.sin(ang) * 5.2);
            int x2 = (int) Math.round(12 + Math.cos(ang) * 6.6), y2 = (int) Math.round(16 + Math.sin(ang) * 6.6);
            g.drawLine(x1, y1, x2, y2);
        }
        // Griffplatte
        g.setColor(DARKER);
        g.fillRoundRect(20, 6, 8, 20, 3, 3);
        g.setColor(STEEL_D);
        g.drawRoundRect(20, 6, 8, 20, 3, 3);
        // Status-LED
        g.setColor(new Color(220, 50, 40));
        g.fillOval(9, 5, 3, 3);
        // Scharniere links
        g.setColor(STEEL_D);
        g.fillRect(2, 8, 2, 4);
        g.fillRect(2, 20, 2, 4);
        g.dispose();
        return im;
    }

    static BufferedImage frontLarge(boolean left) {
        BufferedImage im = doorBase(NAVY, NAVY_D, NAVY_L, true);
        Graphics2D g = gfx(im);
        int hinge = left ? 2 : 28;
        g.setColor(GOLD_D);
        g.fillRect(hinge, 7, 2, 5);
        g.fillRect(hinge, 20, 2, 5);
        g.setColor(GOLD);
        g.fillRect(hinge, 7, 1, 5);
        g.fillRect(hinge, 20, 1, 5);
        // Griffplatte
        int hx = left ? 23 : 3;
        g.setColor(DARKER);
        g.fillRoundRect(hx, 6, 6, 20, 3, 3);
        g.setColor(GOLD_D);
        g.drawRoundRect(hx, 6, 6, 20, 3, 3);
        if (!left) {
            // Zahlenfeld: Display + 3x4 Tasten
            g.setColor(DARKER);
            g.fillRect(10, 6, 14, 22);
            g.setColor(NAVY_L);
            g.drawRect(10, 6, 14, 22);
            g.setColor(new Color(30, 90, 50));
            g.fillRect(12, 8, 10, 4);
            g.setColor(new Color(90, 255, 130));
            for (int k = 0; k < 4; k++) g.fillRect(13 + k * 2 + (k > 1 ? 1 : 0), 9, 1, 2);
            for (int r = 0; r < 4; r++) {
                for (int c = 0; c < 3; c++) {
                    g.setColor(STEEL_L);
                    g.fillRect(12 + c * 4, 14 + r * 3, 3, 2);
                    g.setColor(STEEL_D);
                    g.drawLine(12 + c * 4, 15 + r * 3, 14 + c * 4, 15 + r * 3);
                }
            }
        } else {
            // Rad-Schloss
            g.setColor(DARKER);
            g.fillOval(6, 10, 12, 12);
            g.setColor(GOLD);
            g.drawOval(6, 10, 12, 12);
            g.setColor(GOLD_L);
            for (int k = 0; k < 8; k++) {
                double ang = Math.toRadians(k * 45);
                g.drawLine((int) Math.round(12 + Math.cos(ang) * 3), (int) Math.round(16 + Math.sin(ang) * 3),
                        (int) Math.round(12 + Math.cos(ang) * 5), (int) Math.round(16 + Math.sin(ang) * 5));
            }
        }
        g.dispose();
        return im;
    }

    static BufferedImage handle() {
        BufferedImage im = img(16, 16);
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                im.setRGB(x, y, shade(GOLD, (x < 8 ? 18 : -12) - Math.abs(y - 8) * 2).getRGB());
            }
        }
        bevel(im, 0, 0, 16, 16, 1, GOLD_L, GOLD_D, true);
        return im;
    }

    static void key(String name, Color cap, String label, Color fg) throws IOException {
        BufferedImage im = img(32, 32);
        Graphics2D g = gfx(im);
        g.setColor(DARKER);
        g.fillRoundRect(1, 1, 30, 30, 6, 6);
        g.setColor(cap);
        g.fillRoundRect(3, 3, 26, 26, 5, 5);
        g.setColor(shade(cap, 38));
        g.drawLine(5, 3, 26, 3);
        g.drawLine(3, 5, 3, 26);
        g.setColor(shade(cap, -34));
        g.drawLine(5, 28, 26, 28);
        g.drawLine(28, 5, 28, 26);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, label.length() > 1 ? 15 : 21));
        FontMetrics fm = g.getFontMetrics();
        int x = (32 - fm.stringWidth(label)) / 2, y = (32 - fm.getHeight()) / 2 + fm.getAscent() - 1;
        g.setColor(new Color(0, 0, 0, 120));
        g.drawString(label, x + 1, y + 1);
        g.setColor(fg);
        g.drawString(label, x, y);
        g.dispose();
        save("textures/item/" + name, im);
    }

    static BufferedImage lcd(boolean on) {
        BufferedImage im = img(32, 32);
        Graphics2D g = gfx(im);
        if (on) {
            g.setColor(new Color(90, 255, 130, 70));
            g.fillOval(6, 6, 20, 20);
            g.setColor(new Color(90, 255, 130, 140));
            g.fillOval(9, 9, 14, 14);
            g.setColor(new Color(180, 255, 200));
            g.fillOval(12, 12, 8, 8);
        } else {
            g.setColor(new Color(60, 130, 80, 110));
            g.fillOval(13, 13, 6, 6);
        }
        g.dispose();
        return im;
    }

    /** 176x186: schlichter Hintergrund eines 4-Reihen-Containers. */
    static BufferedImage keypadBackground() {
        int w = 176, h = 186;
        BufferedImage im = img(w, h);
        brushed(im, 0, 0, w, h, new Color(78, 84, 96), 21);
        bevel(im, 0, 0, w, h, 3, STEEL_L, DARKER, true);
        Graphics2D g = gfx(im);

        // LCD-Zeile (Reihe 0)
        g.setColor(new Color(16, 34, 24));
        g.fillRoundRect(6, 17, 164, 20, 4, 4);
        bevel(im, 6, 17, 164, 20, 1, STEEL_L, DARKER, false);

        // Tastenfeld
        int[] keySlots = {11, 12, 13, 14, 15, 20, 21, 22, 23, 24, 30, 32};
        for (int s : keySlots) cell(im, 8 + 18 * (s % 9), 18 + 18 * (s / 9));

        // Spielerinventar
        for (int r = 0; r < 3; r++) for (int c = 0; c < 9; c++) cell(im, 8 + c * 18, 103 + r * 18);
        for (int c = 0; c < 9; c++) cell(im, 8 + c * 18, 161);
        g.dispose();
        return im;
    }

    static BufferedImage lockIcon() {
        BufferedImage im = img(32, 32);
        Graphics2D g = gfx(im);
        g.setStroke(new BasicStroke(3.5f));
        g.setColor(STEEL_L);
        g.drawArc(9, 3, 14, 18, 0, 180);
        g.drawLine(9, 12, 9, 15);
        g.drawLine(23, 12, 23, 15);
        g.setColor(GOLD_D);
        g.fillRoundRect(5, 14, 22, 16, 4, 4);
        g.setColor(GOLD);
        g.fillRoundRect(6, 15, 20, 12, 3, 3);
        g.setColor(DARKER);
        g.fillOval(13, 17, 6, 6);
        g.fillRect(15, 21, 2, 5);
        g.dispose();
        return im;
    }

    /** Slot-Zelle: Item-Flaeche beginnt bei (x, y) und ist 16x16, Zelle ist 18x18 (x-1,y-1). */
    static void cell(BufferedImage im, int x, int y) {
        for (int j = -1; j < 17; j++) for (int i = -1; i < 17; i++) im.setRGB(x + i, y + j, new Color(40, 44, 52).getRGB());
        bevel(im, x - 1, y - 1, 18, 18, 1, STEEL_L, DARKER, false);
    }

    // ------------------------------------------------------------------ Modelle

    static String box(double x1, double y1, double z1, double x2, double y2, double z2, boolean body) {
        String front = body ? "#front" : "#handle", other = body ? "#body" : "#handle";
        return "{\"from\":[" + x1 + "," + y1 + "," + z1 + "],\"to\":[" + x2 + "," + y2 + "," + z2 + "],\"faces\":{"
                + "\"north\":{\"texture\":\"" + other + "\"},"
                + "\"east\":{\"texture\":\"" + other + "\"},"
                + "\"south\":{\"texture\":\"" + front + "\"},"
                + "\"west\":{\"texture\":\"" + other + "\"},"
                + "\"up\":{\"texture\":\"" + other + "\"},"
                + "\"down\":{\"texture\":\"" + other + "\"}}}";
    }

    static void model(String name, String body, String front, String handle, String... elements) throws IOException {
        String json = "{\"parent\":\"minecraft:block/block\",\"textures\":{"
                + "\"particle\":\"tresor:block/" + body + "\","
                + "\"body\":\"tresor:block/" + body + "\","
                + "\"front\":\"tresor:block/" + front + "\","
                + "\"handle\":\"tresor:block/" + handle + "\"},"
                + "\"elements\":[" + String.join(",", elements) + "]}";
        write(ASSETS.resolve("models/block/" + name + ".json"), json);
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
        Files.createDirectories(Path.of("build"));
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
