package com.ali.dev.xonix;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

public class Images {
    public static final Image bonusLife = loadImage("images/hart20.png");
    public static final Image speedUp = loadImage("images/speedup20.png");
    public static final Image freeze = loadImage("images/freeze20.png");
    public static final Image bomb = loadImage("images/bomb20.png");
    public static final Image splash = loadImage("images/xonix.png");

    private static BufferedImage loadImage(String image) {
        try {
            // Get the ClassLoader
            ClassLoader classLoader = Images.class.getClassLoader();

            // Get the resource as an InputStream
            InputStream inputStream = classLoader.getResourceAsStream(image);

            if (inputStream == null) {
                throw new IllegalArgumentException("Image not found: " + image);
            }

            // Read the image from the InputStream
            return ImageIO.read(inputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
