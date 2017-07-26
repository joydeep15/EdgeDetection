
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.awt.image.WritableRaster;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.highgui.Highgui;
import org.opencv.objdetect.CascadeClassifier;

public class CannyEdgeDetection {
	static String base = "beckham";
	private final static float GAUSSIAN_CUT_OFF = 0.005f;
	static int order = 7;
	static float sigma = 2.0f;
	static float lowThreshold;

	static float lowMultiplier = 15;
	static float highMultiplier = 18f;

	static float angle = 22.5f;

	static int smoothingOrder = 5;
	static float smoothingSigma = 3f;
	static float highThreshold;
	private static float[][] xKernel;
	private static float[][] yKernel;
	private static float[][] xMag;
	private static float[][] yMag;
	private static float[][] mag;
	static boolean flag = true;
	static int width;
	static int height;
	static BufferedImage result, intermediate;
	static float[][] direction;
	static boolean[][] flagged;
	static float max;
	static int num = 0;

	public CannyEdgeDetection(){
		xKernel = new float[order][order];
		yKernel = new float[order][order];
		xMag = new float[width][height];
		yMag = new float[width][height];
		mag = new float[width][height];
	
		result = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
	}

	public static class pair {
		int first;
		int second;

	}

	public static float smoothingValue(float x, float y, float smoothingSigma) {

		float raised = -(x * x + y * y) / (2 * (smoothingSigma * smoothingSigma));
		raised = (float) Math.exp(raised);

		raised *= 1 / (2 * Math.PI * smoothingSigma * smoothingSigma);
		return raised;

	}

	public static float[] getSmoothingKernel() {

		float[] kernel = new float[smoothingOrder * smoothingOrder];
		float sum = 0f;
		for (int i = 0, k = 0; i < smoothingOrder; i++) {
			for (int j = 0; j < smoothingOrder; j++) {
				float x = i - (smoothingOrder / 2);
				float y = j - (smoothingOrder / 2);
				kernel[k] = smoothingValue(x, y, smoothingSigma);
				sum += kernel[k++];

			}
		}
		sum = 1 / sum;
		for (int i = 0, k = 0; i < smoothingOrder; i++) {
			for (int j = 0; j < smoothingOrder; j++) {
				kernel[k] *= sum;
				System.out.print(kernel[k++] + "      ");
			}
			System.out.println("");
		}
		return kernel;

	}

	public static BufferedImage gaussianSmoothing(BufferedImage img) throws IOException {

		float[] gKernel = getSmoothingKernel();
		Kernel gMatrix = new Kernel(smoothingOrder, smoothingOrder, gKernel);
		ConvolveOp convolve = new ConvolveOp(gMatrix);
		img = convolve.filter(img, null);

		return img;
	}

	static boolean checkDirection(float pixel, float test) {

		float lower = (float) (pixel - Math.toRadians(angle));
		float upper = (float) (pixel + Math.toRadians(angle));

		return test <= upper ? (test >= lower ? true : false) : false;
	}

	static boolean checkBounds(int i, int j, int order) {

		if (i < 0 || j < 0)
			return false;

		if (i >= width || j >= height)
			return false;

		return true;

	}

	public static void alter(float[][] resultant, int i, int j, int x, int y, float max) {

		if (checkBounds(x, y, order))
			if (checkDirection(direction[i][j], direction[x][y])) {
				// same direction
				if (resultant[i][j] >= resultant[x][y]) {
					flagged[x][y] = true;
					flagged[i][j] = false;
				}

				else if (resultant[x][y] > max) {
					max = (int) resultant[x][y];
				}
			}

	}

	public static float[][] supress(float[][] x, float[][] y, float[][] resultant) {

		direction = new float[width][height];
		flagged = new boolean[width][height];

		for (int i = 0; i < width; i++) {
			for (int j = 0; j < height; j++) {
				flagged[i][j] = false;
			}
		}

		for (int i = 0; i < width; i++)
			for (int j = 0; j < height; j++) {
				double temp = (double) y[i][j] / x[i][j];
				direction[i][j] = (float) Math.atan(temp);
			}

		for (int i = 0; i < width; i++) {
			for (int j = 0; j < height; j++) {

				if (resultant[i][j] == 0)
					continue;

				pair up = new pair();
				up.first = i - 1;
				up.second = j;

				pair down = new pair();
				down.first = i + 1;
				down.second = j;

				pair left = new pair();
				left.first = i;
				left.second = j - 1;

				pair right = new pair();
				right.first = i;
				right.second = j + 1;

				pair topRight = new pair();
				topRight.first = i - 1;
				topRight.second = j + 1;

				pair topLeft = new pair();
				topLeft.first = i - 1;
				topLeft.second = j - 1;

				pair bottomLeft = new pair();
				bottomLeft.first = i + 1;
				bottomLeft.second = j - 1;

				pair bottomRight = new pair();
				bottomRight.first = i + 1;
				bottomRight.second = j + 1;

				max = resultant[i][j];

				alter(resultant, i, j, up.first, up.second, max);
				alter(resultant, i, j, down.first, down.second, max);
				alter(resultant, i, j, left.first, left.second, max);
				alter(resultant, i, j, right.first, right.second, max);
				alter(resultant, i, j, topLeft.first, topLeft.second, max);
				alter(resultant, i, j, topRight.first, topRight.second, max);
				alter(resultant, i, j, bottomLeft.first, bottomLeft.second, max);
				alter(resultant, i, j, bottomRight.first, bottomRight.second, max);

				if (resultant[i][j] < max)
					flagged[i][j] = true;
			}
		}

		for (int i = 0; i < width; i++)
			for (int j = 0; j < height; j++)
				if (flagged[i][j] == true)
					resultant[i][j] = 0f;

		return resultant;

	}

	private static void resultant(float[][] mag, float[][] xmag, float[][] ymag) {

		for (int i = 0; i < width; i++) {
			for (int j = 0; j < height; j++) {
				mag[i][j] = (float) Math.sqrt(xmag[i][j] * xmag[i][j] + ymag[i][j] * ymag[i][j]);
			}
		}
	}

	public static void convol(float[][] mag, BufferedImage img, float[][] kernel) {
		int width = img.getWidth();
		int height = img.getHeight();
		int k = order / 2;
		float sum;
		for (int x = k; x < width - k; x++) {
			for (int y = k; y < height - k; y++) {
				sum = 0;
				for (int i = -k; i <= k; i++) {
					for (int j = -k; j <= k; j++) {
						sum += ((img.getRGB(x - i, y - j) & 0xFF) * kernel[i + k][j + k]);
					}
				}
				if (Math.abs(sum) < GAUSSIAN_CUT_OFF)
					sum = 0;
				mag[x][y] = sum;
			}
		}
	}

	public static float kernelValue(float x, float y, float sigma) {

		float raised = -(x * x + y * y) / (2 * (sigma * sigma));
		raised = (float) Math.exp(raised);

		raised *= ((-x) / (sigma * sigma));
		return raised;

	}

	public static float[][] transpose(float[][] xkernel) {

		float[][] kernel = new float[order][order];
		for (int i = 0; i < order; i++) {
			for (int j = 0; j < order; j++) {
				kernel[i][j] = xkernel[j][i];

			}

		}

		return kernel;

	}

	public static void follow(int i, int j) {

		for (int k = -1; k <= 1; k++) {
			for (int l = -1; l <= 1; l++) {
				if (checkBounds(i + k, j + l, order) && mag[i + k][j + l] > lowThreshold
						&& mag[i + k][j + l] < highThreshold) {
					result.setRGB(i + k, j + l, Color.white.getRGB());
					mag[i + k][j + l] = highThreshold + 1;
					follow(i + k, j + k);
				}
			}
		}

	}

	public static BufferedImage hysteresis(float[][] mag, float low, float high) {
		// BufferedImage result = new BufferedImage(width, height,
		// BufferedImage.TYPE_BYTE_GRAY);
		// result.getGraphics().drawImage(img, 0, 0, null);

		for (int i = 0; i < width; i++) {
			for (int j = 0; j < height; j++) {
				if (mag[i][j] <= low) {
					result.setRGB(i, j, Color.black.getRGB());
					mag[i][j] = 0;
				} else if (mag[i][j] >= high) {
					result.setRGB(i, j, Color.white.getRGB());
				}
			}
		}

		for (int i = 1; i < width - 1; i++) {
			for (int j = 1; j < height - 1; j++) {
				if (mag[i][j] >= high) {
					for (int k = -1; k <= 1; k++) {
						for (int l = -1; l <= 1; l++) {
							if (mag[i + k][j + l] > lowThreshold && mag[i + k][j + l] < highThreshold) {
								result.setRGB(i + k, j + l, Color.white.getRGB());
								mag[i + k][j + l] = high + 1;
								follow(i + k, j + k);
							}
						}
					}
				}
			}
		}

		for (int i = 1; i < width - 1; i++) {
			for (int j = 1; j < height - 1; j++) {
				if (mag[i][j] >= low && mag[i][j] < high) {
					result.setRGB(i, j, Color.black.getRGB());
				}
			}
		}

		return result;
	}

	public static float[][] getXKernel() {

		float[][] kernel = new float[order][order]; ///// doubt

		for (int i = 0; i < order; i++) {
			for (int j = 0; j < order; j++) {
				float x = i - (order / 2);
				float y = j - (order / 2);
				kernel[i][j] = kernelValue(x, y, sigma);
			}
		}

		float sum = 0;
		for (int i = 0; i < order / 2; i++)
			for (int j = 0; j < order; j++)
				sum += kernel[i][j];
		sum = (1 / sum);
		for (int i = 0; i < order; i++) {
			for (int j = 0; j < order; j++) {
				kernel[i][j] *= sum;

			}
		}

		return kernel;

	}

	public static float autoThresholdMedian(BufferedImage image) throws IOException {
		float median = -1;

		BufferedImage result = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
		// ImageIO.write(result, "JPG", new
		// File("F://Programs//graymaybe.jpg"));
		result.getGraphics().drawImage(image, 0, 0, null);
		//ImageIO.write(result, "JPG", new File("F://Programs//graymaybe.jpg"));
		WritableRaster raster = result.getRaster(); ////// doubt
		int[] freq = new int[256];
		int[] cum = new int[256];
		int sum = 0;
		int[] pixels = new int[image.getWidth()];
		for (int y = 0; y < image.getHeight(); y++) {

			raster.getPixels(0, y, image.getWidth(), 1, pixels);

			for (int i = 0; i < pixels.length; i++) {
				freq[pixels[i]]++;
			}
			// raster.setPixels(0, y, image.getWidth(), 1, pixels);

		}
		for (int i = 0; i < 256; i++) {
			sum += freq[i];
			cum[i] = sum;
		}
		// System.out.println(sum);
		sum /= 2; ///// doubt
		for (int i = 0; i < 256; i++) {
			if (sum < cum[i]) {
				median = i;
				break;
			}
		}
		System.out.println(median);
		//return median;
		 return 1;

	}

	public static void constructIntermediate( float[][] magValue, boolean flag) throws IOException {

		WritableRaster raster = intermediate.getRaster();
		int[] pixels = new int[intermediate.getWidth()];
		
		for (int y = 0; y < intermediate.getHeight(); y++) {

			raster.getPixels(0, y, intermediate.getWidth(), 1, pixels);

			for (int i = 0; i < pixels.length; i++) {
				pixels[i]=(int)Math.min(255f,1.25f* Math.abs(magValue[i][y] )); 
				
			}
			 raster.setPixels(0, y, intermediate.getWidth(), 1, pixels);

		}

		if (flag) {
			File f = new File("F:\\intermediate_x_" + base + num + ".jpg");
			if (!ImageIO.write(intermediate, "JPEG", f)) {
				throw new RuntimeException("Unexpected error writing image");
			}
		} else {
			File f = new File("F:\\intermediate_y_" + base + num + ".jpg");
			if (!ImageIO.write(intermediate, "JPEG", f)) {
				throw new RuntimeException("Unexpected error writing image");
			}
		}
	}

	public static void main(String[] args) throws IOException {

		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		System.out.println("\nRunning FaceDetector");

		CascadeClassifier faceDetector = new CascadeClassifier(("D:\\haarcascade_frontalface_alt.xml"));
		Mat image = Highgui.imread(("D:\\" + base + ".jpg"));
		BufferedImage src = ImageIO.read(new File("D:\\" + base + ".jpg"));

		MatOfRect faceDetections = new MatOfRect();
		faceDetector.detectMultiScale(image, faceDetections);
		BufferedImage dest;

		System.out.println(String.format("Detected %s faces", faceDetections.toArray().length));
		for (Rect rect : faceDetections.toArray()) {
			if (rect.y - (rect.height / 6) * 2 >= 0 && rect.y + rect.height + (rect.height / 6) * 2 <= src.getHeight()
					&& rect.x - (rect.width / 6) >= 0 && rect.x + rect.width + (rect.width / 6) <= src.getWidth()) {
				// System.out.println(flag);
				dest = src.getSubimage(rect.x - rect.width / 6, rect.y - (rect.height / 6) * 2,
						rect.width + rect.width / 3, rect.height + 2 * rect.height / 3);
			} else if (rect.y - rect.height / 6 >= 0 && rect.y + rect.height + rect.height / 6 <= src.getHeight()
					&& rect.x - (rect.width / 6) >= 0 && rect.x + rect.width + (rect.width / 6) <= src.getWidth()) {
				// System.out.println(flag);
				dest = src.getSubimage(rect.x - rect.width / 6, rect.y - rect.height / 6, rect.width + rect.width / 3,
						rect.height + rect.height / 3);
			}

			else if (rect.y - rect.height / 6 >= 0 && rect.y + rect.height + rect.height / 6 <= src.getHeight()) {
				dest = src.getSubimage(rect.x, rect.y - rect.height / 6, rect.width, rect.height + rect.height / 3);
			} else {
				dest = src.getSubimage(rect.x, rect.y, rect.width, rect.height);
			}
			File f = new File("F:\\corp_" + base + num + ".jpg");
			if (!ImageIO.write(dest, "JPEG", f)) {
				throw new RuntimeException("Unexpected error writing image");
			}

			float median = autoThresholdMedian(dest);
			width = dest.getWidth();
			height = dest.getHeight();
			new CannyEdgeDetection();
			yKernel = getXKernel();
			xKernel = transpose(yKernel);

			lowThreshold = median * lowMultiplier;
			highThreshold = median * highMultiplier;
			BufferedImage overImg = new BufferedImage(dest.getWidth(), dest.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
			overImg.getGraphics().drawImage(dest, 0, 0, null);

			intermediate = new BufferedImage(dest.getWidth(), dest.getHeight(), BufferedImage.TYPE_BYTE_GRAY);

			overImg = gaussianSmoothing(overImg);
			convol(xMag, overImg, xKernel);
			constructIntermediate(xMag, true);

			convol(yMag, overImg, yKernel);
			constructIntermediate( yMag, false);

			resultant(mag, xMag, yMag);
			mag = supress(xMag, yMag, mag);

			BufferedImage finalRes = hysteresis(mag, lowThreshold, highThreshold);

			// writing
			f = new File("F:\\zzz_" + base + num + ".jpg");
			if (!ImageIO.write(finalRes, "JPEG", f)) {
				throw new RuntimeException("Unexpected error writing image");
			}

			JFrame jframe = new JFrame();
			jframe.getContentPane().setLayout(new FlowLayout());
			jframe.getContentPane().add(new JLabel(new ImageIcon(finalRes)));

			jframe.pack();
			jframe.setVisible(true);
			jframe.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

			num++;
		}
	}

	
}
