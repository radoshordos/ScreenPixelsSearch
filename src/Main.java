import java.awt.AWTException;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.JOptionPane;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;




public class Main {
    private static long lastAlertTime = 0L;
    private static final long ALERT_INTERVAL = 30000L;
    private static final long SCREEN_INTERVAL = 50L;
    private static final long SNAPSHOT_INTERVAL = 10000L;
    private static final String SNAPSHOT_DIRECTORY = "snapshots/";


    public static void main(String[] args) {
        // Název a cesta k externímu konfiguračnímu souboru
        String configFilePath = "config.xml";
        startSnapshotTimer(); // Spuštění časovače pro ukládání snímků

        // Nekonečná smyčka pro pravidelné opakování akce
        while (true) {
            // Vyfotí obrazovku
            BufferedImage screenshot = captureScreen();

            // Načte kombinace barev ze souboru konfigurace
            List<List<Color>> colorCombinations = readColorCombinationsFromConfig(configFilePath);

            // Kontrola, zda obsahuje dvě kombinace tří pixelů za sebou definované barvy
            for (List<Color> colors : colorCombinations) {
                if (containsThreeConsecutivePixelsOfColors(screenshot, colors) && canShowAlert()) {
                    // Získání pozic pixelů s definovanými barvami
                    List<String> positions = getPositionsOfPixels(screenshot, colors);
                    // Vytvoření textové zprávy s pozicemi pixelů
                    String message = "Nalezena kombinace tří pixelů za sebou v definovaných barvách na pozicích:\n";
                    for (String position : positions) {
                        message += position + "\n";
                    }
                    // Zobrazení vyskakovacího okna s textem
                    System.out.printf("\033[1;33m%s\033[0m%n", message);
                    JOptionPane.showMessageDialog(null, message);

                    lastAlertTime = System.currentTimeMillis(); // Aktualizace času posledního zobrazení alertu
                    break;
                }
            }

            // Přestávka jednu sekundu
            try {
                Thread.sleep(SCREEN_INTERVAL);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static BufferedImage captureScreen() {
        try {
            // Získání velikosti obrazovky
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            Rectangle screenRectangle = new Rectangle(screenSize);

            // Vytvoření instance třídy Robot pro snímání obrazovky
            Robot robot = new Robot();
            return robot.createScreenCapture(screenRectangle);
        } catch (AWTException e) {
            e.printStackTrace();
            return null;
        }
    }


    public static List<List<Color>> readColorCombinationsFromConfig(String filePath) {
        List<List<Color>> colorCombinations = new ArrayList<>();

        try {
            // Vytvoření parseru XML
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();

            // Načtení XML souboru
            Document document = builder.parse(new File(filePath));

            // Získání všech prvků "combination" ze souboru
            NodeList combinationNodes = document.getElementsByTagName("combination");
            for (int i = 0; i < combinationNodes.getLength(); i++) {
                Element combinationElement = (Element) combinationNodes.item(i);
                // Vytvoření nové kombinace
                List<Color> colors = new ArrayList<>();
                // Získání všech prvků "color" uvnitř kombinace
                NodeList colorNodes = combinationElement.getElementsByTagName("color");
                for (int j = 0; j < colorNodes.getLength(); j++) {
                    Element colorElement = (Element) colorNodes.item(j);
                    // Převedení hodnot RGB na barvu a přidání do kombinace
                    int red = Integer.parseInt(colorElement.getAttribute("red"));
                    int green = Integer.parseInt(colorElement.getAttribute("green"));
                    int blue = Integer.parseInt(colorElement.getAttribute("blue"));
                    Color color = new Color(red, green, blue);
                    colors.add(color);
                }
                // Přidání kombinace do seznamu kombinací
                colorCombinations.add(colors);
            }
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }

        return colorCombinations;
    }

    public static boolean containsThreeConsecutivePixelsOfColors(BufferedImage image, List<Color> colors) {
        // Procházení každého pixelu snímku kromě prvního a posledního řádku a sloupce
        for (int x = 1; x < image.getWidth() - 1; x++) {
            for (int y = 1; y < image.getHeight() - 1; y++) {
                // Získání barev sousedních pixelů
                Color currentPixel = new Color(image.getRGB(x, y));
                Color leftPixel = new Color(image.getRGB(x - 1, y));
                Color rightPixel = new Color(image.getRGB(x + 1, y));

                // Kontrola, zda jsou tři sousední pixely v definovaných barvách
                if (colors.contains(currentPixel) && colors.contains(leftPixel) && colors.contains(rightPixel)) {
                    return true; // Pokud jsou tři pixely za sebou v definovaných barvách, vrátíme true
                }
            }
        }

        return false; // Pokud není nalezeno tři pixely za sebou v definovaných barvách, vrátíme false
    }

    public static List<String> getPositionsOfPixels(BufferedImage image, List<Color> colors) {
        List<String> positions = new ArrayList<>();

        // Procházení každého pixelu snímku
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                // Získání barvy pixelu
                Color pixelColor = new Color(image.getRGB(x, y));
                // Pokud je barva pixelu jedna z definovaných barev, přidejme jeho pozici do seznamu
                if (colors.contains(pixelColor)) {
                    positions.add("(" + x + ", " + y + ")");
                    break;
                }
            }
        }

        return positions;
    }

    public static boolean canShowAlert() {
        long currentTime = System.currentTimeMillis();
        return currentTime - lastAlertTime >= ALERT_INTERVAL; // Vrátíme true, pokud uplynulo více než ALERT_INTERVAL od posledního zobrazení alertu
    }

    public static void startSnapshotTimer() {
        java.util.Timer timer = new java.util.Timer();
        timer.scheduleAtFixedRate(new java.util.TimerTask() {
            @Override
            public void run() {
                saveSnapshot(); // Metoda pro uložení snímku
            }
        }, SNAPSHOT_INTERVAL, SNAPSHOT_INTERVAL); // Interval 10 minut
    }

    public static void saveSnapshot() {
        try {
            String fileName = "snapshot_" + System.currentTimeMillis() + ".png"; // Název souboru se časovým razítkem
            File file = new File(SNAPSHOT_DIRECTORY + fileName);
            BufferedImage screenshot = captureScreen();
            ImageIO.write(screenshot, "png", file); // Uložení snímku jako PNG
            System.out.println("Snapshot uložen do: " + file.getAbsolutePath()); // Vypsání cesty k uloženému souboru do konzole
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

