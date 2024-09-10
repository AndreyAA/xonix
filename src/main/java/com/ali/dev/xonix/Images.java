package com.ali.dev.xonix;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class Images {
    public static final Image bonusLife = loadImage("resources/images/hart20.png");
    public static final Image speedUp = loadImage("resources/images/speedup20.png");
    public static final Image freeze = loadImage("resources/images/freeze20.png");

    private static BufferedImage loadImage(String image) {
        try {
            return ImageIO.read(new File(image));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
