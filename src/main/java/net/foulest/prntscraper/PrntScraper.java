package net.foulest.prntscraper;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.tidy.Tidy;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * PrntScraper
 * Downloads random images from Lightshot.
 *
 * @author Foulest
 */
public class PrntScraper {

    static ScheduledExecutorService executorService = Executors.newScheduledThreadPool(10);

    public static void main(String... args) {
        System.out.println("Starting PrntScraper...");

        startScraping(0, 1000);
        startScraping(250, 1000);
        startScraping(500, 1000);
    }

    public static void shutdown() {
        System.out.println();
        System.out.println("*** Your IP address has been temporarily banned. ***");
        System.out.println("*** Turn on a VPN and try again. ***");
        System.out.println();

        executorService.shutdown();
    }

    public static void startScraping(int delay, int period) {
        executorService.scheduleAtFixedRate(() -> {
            try {
                String random = getRandomString();
                URL url = new URL("https://prnt.sc/" + random);
                File savedFile = new File(System.getProperty("java.io.tmpdir") + "/lightshot/" + random + ".png");

                // returns if image was already crawled
                if (savedFile.exists()) {
                    return;
                }

                // makes temp directory if not found
                if (!savedFile.getParentFile().exists()) {
                    savedFile.getParentFile().mkdirs();
                }

                System.out.println("Trying " + "https://prnt.sc/" + random);

                // add user agent
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.addRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/95.0.4638.54 Safari/537.36");
                con.setReadTimeout(5000);
                con.setConnectTimeout(5000);

                // returns if IP address is blocked
                try {
                    con.getInputStream();
                } catch (IOException ex) {
                    if (ex.getMessage().contains("403") || ex.getMessage().contains("503")) {
                        shutdown();
                    }
                    return;
                }

                // setting up tidy
                Tidy tidy = new Tidy();
                tidy.setShowErrors(0);
                tidy.setShowWarnings(false);
                tidy.setQuiet(true);

                // grabs page elements
                InputStream input = con.getInputStream();
                Document document = tidy.parseDOM(input, null);
                NodeList imgs = document.getElementsByTagName("img");
                List<String> imageLinks = new ArrayList<>();

                // grabs all image links
                for (int i = 0; i < imgs.getLength(); i++) {
                    imageLinks.add(imgs.item(i).getAttributes().getNamedItem("src").getNodeValue());
                }

                // sorts through all found image links
                for (String link : imageLinks) {
                    if (link.contains("image.prntscr.com") && !link.contains("-") && !link.contains("_")) {
                        System.out.println("Image found: " + link);

                        // add user agent
                        url = new URL(link);
                        con = (HttpURLConnection) url.openConnection();
                        con.addRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/95.0.4638.54 Safari/537.36");
                        con.setReadTimeout(5000);
                        con.setConnectTimeout(5000);

                        // returns if any exceptions occur
                        try {
                            con.getInputStream();
                        } catch (Exception ex) {
                            return;
                        }

                        InputStream finalStream = con.getInputStream();

                        executorService.schedule(() -> {
                            try {
                                Files.copy(finalStream, savedFile.toPath());
                                System.out.println("Image saved: " + random + ".png");
                            } catch (IOException ignored) {
                            }
                        }, 0, TimeUnit.SECONDS);
                    }
                }

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }, delay, period, TimeUnit.MILLISECONDS);
    }

    public static String getRandomString() {
        String characters = "abcdefghijklmnopqrstuvwxyz1234567890";
        StringBuilder builder = new StringBuilder();
        Random random = new Random();
        int min = 6;
        int max = 6;
        int randomLength = random.nextInt(max + 1 - min) + min;

        while (builder.length() < randomLength) {
            int index = (int) (random.nextFloat() * characters.length());
            builder.append(characters.charAt(index));
        }

        return builder.toString();
    }
}
