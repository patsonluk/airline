package com.patson.util;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import javax.imageio.ImageIO;

import com.google.common.io.ByteStreams;

public class LogoGenerator {
	public static final int TEMPLATE_COUNT = 59;
	private static final Random random = new Random();

	public static byte[] generateLogo(int patternIndex, int color1Rgb, int color2Rgb) throws IOException {
		String fileName = "/logo/p" + Math.abs(patternIndex % TEMPLATE_COUNT) + ".bmp";
		BufferedImage image = ImageIO.read(LogoGenerator.class.getResourceAsStream(fileName));

		
		for (int yPixel = 0; yPixel < image.getHeight(); yPixel++) {
			for (int xPixel = 0; xPixel < image.getWidth(); xPixel++) {
				int color = image.getRGB(xPixel, yPixel);
				double alpha = ((double)new Color(color).getRed()) / 255;
				Color color1 = new Color(color1Rgb);
				Color color2 = new Color(color2Rgb);
				Color finalColor = new Color((int)(color2.getRed() * alpha + color1.getRed() * (1 - alpha)),
						(int)(color2.getGreen() * alpha + color1.getGreen() * (1 - alpha)),
						(int)(color2.getBlue() * alpha + color1.getBlue() * (1 - alpha)));
				
				
				image.setRGB(xPixel, yPixel, finalColor.getRGB());
				
			}
		}

		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ImageIO.write(image, "png", baos);
		baos.flush();
		return baos.toByteArray();
	}
	
	/**
	 * 
	 * @return a map of template index with the corresponding byte bmp image
	 */
	public static Map<Integer, byte[]> getTemplates() {
		Map<Integer, byte[]> templates = new HashMap<Integer, byte[]>();
		for (int i = 0 ; i < TEMPLATE_COUNT; i ++) {
			String fileName = "/logo/p" + i + ".bmp";

			try (InputStream in = LogoGenerator.class.getResourceAsStream(fileName)) {
				templates.put(i, ByteStreams.toByteArray(in));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return templates;
	}

	public static byte[] generateRandomLogo() throws IOException {
		Color color1;
		
		color1 = new Color(random.nextInt(80), random.nextInt(80), random.nextInt(80)); //darker color
		
		
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
		if (isAccent) {
			return 180 + random.nextInt(76);
		} else {
			return 40 + random.nextInt(76);
		}
 	}
}
