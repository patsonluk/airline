package com.patson.util;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Random;

import javax.imageio.ImageIO;

public class LogoGenerator {
	private static final int patternCount = 18;
	private static final Random random = new Random();

	public static byte[] generateLogo(int patternIndex, int color1, int color2) throws IOException {
		String fileName = "/logo/p" + Math.abs(patternIndex % patternCount) + ".bmp";
		BufferedImage image = ImageIO.read(LogoGenerator.class.getResourceAsStream(fileName));

		int[][] array2D = new int[image.getWidth()][image.getHeight()];

		for (int yPixel = 0; yPixel < image.getHeight(); yPixel++) {
			for (int xPixel = 0; xPixel < image.getWidth(); xPixel++) {
				int color = image.getRGB(xPixel, yPixel);
				if (color == Color.BLACK.getRGB()) {
					array2D[xPixel][yPixel] = 1;
					System.out.print("X");

					image.setRGB(xPixel, yPixel, color1);
				} else {
					array2D[xPixel][yPixel] = 0; // ?
					System.out.print(" ");
					image.setRGB(xPixel, yPixel, color2);
				}
			}
			System.out.println();
		}

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ImageIO.write(image, "png", baos);
		baos.flush();
		return baos.toByteArray();
	}

	public static byte[] generateRandomLogo() throws IOException {
		Color color1 = new Color(random.nextInt(80), random.nextInt(80), random.nextInt(80));
		Color color2 = new Color(180 + random.nextInt(76), 180 + random.nextInt(76), 180 + random.nextInt(76));
		return generateLogo(random.nextInt(), color1.getRGB(), color2.getRGB());
	}
}
