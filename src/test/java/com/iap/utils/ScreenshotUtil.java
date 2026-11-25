package com.iap.utils;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

public final class ScreenshotUtil {

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private ScreenshotUtil() {
    }

    /**
     * Takes a screenshot for a report step: scaled to at most 900 px wide and saved as JPEG, so a report with
     * many steps stays small. Returns it Base64-encoded, or null if the screenshot could not be taken.
     */
    public static String thumbnailBase64(WebDriver driver) {
        try {
            byte[] png = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
            BufferedImage source = ImageIO.read(new ByteArrayInputStream(png));
            int width = Math.min(900, source.getWidth());
            int height = Math.max(1, source.getHeight() * width / source.getWidth());
            BufferedImage rgb = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = rgb.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(source, 0, 0, width, height, null);
            g.dispose();

            ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
            ImageWriteParam params = writer.getDefaultWriteParam();
            params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            params.setCompressionQuality(0.7f);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (ImageOutputStream ios = ImageIO.createImageOutputStream(out)) {
                writer.setOutput(ios);
                writer.write(null, new IIOImage(rgb, null, null), params);
            } finally {
                writer.dispose();
            }
            return Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (Exception e) {
            System.err.println("Could not capture step screenshot: " + e.getMessage());
            return null;
        }
    }

    /**
     * Saves a PNG of the current browser view to target/screenshots/&lt;className&gt;/ and returns its path,
     * or null if the screenshot could not be taken.
     */
    public static Path capture(WebDriver driver, String className, String testName, String status) {
        try {
            byte[] png = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
            Path dir = Paths.get("target", "screenshots", className);
            Files.createDirectories(dir);
            Path target = dir.resolve(testName + "_" + status + "_" + LocalDateTime.now().format(STAMP) + ".png");
            Files.write(target, png);
            return target;
        } catch (Exception e) {
            System.err.println("Could not capture screenshot: " + e.getMessage());
            return null;
        }
    }
}
