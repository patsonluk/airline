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
		Color color1;
		
		if (random.nextBoolean()) {
			color1 = new Color(random.nextInt(80), random.nextInt(80), random.nextInt(80)); //darker color
		} else {
			color1 = new Color(176 + random.nextInt(80), 176 + random.nextInt(80), 176 + random.nextInt(80)); //brighter color
		}
		
		
		byte accentValue = (byte) (1 + random.nextInt(7));//byte mask RGB, + 1 to make sure there's at least always have one accent color
		
		byte redAccentMask = 0x4;
		byte greenAccentMask = 0x2;
		byte blueAccentMask = 0x1;
		
		Color color2 = new Color(getColorValue(isAccentColor(accentValue, redAccentMask)),
								 getColorValue(isAccentColor(accentValue, greenAccentMask)),
								 getColorValue(isAccentColor(accentValue, blueAccentMask)));
				                             
		return generateLogo(random.nextInt(), color1.getRGB(), color2.getRGB());
	}
	
	private static boolean isAccentColor(byte accentValue, byte mask) {
		return (accentValue & mask) == mask;
	}
	
	private static int getColorValue(boolean isAccent) {
		System.out.println(isAccent);
		if (isAccent) {
			return 180 + random.nextInt(76);
		} else {
			return 40 + random.nextInt(76);
		}
 	}
}
