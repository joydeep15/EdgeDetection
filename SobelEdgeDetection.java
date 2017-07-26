
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import java.awt.image.WritableRaster;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.highgui.Highgui;
import org.opencv.objdetect.CascadeClassifier;

public class SobelEdgeDetection {

	static String base = "swift";
	static int num = 0, order = 3;
	static float threshold =20;
	static BufferedImage intermediate;
	
	
	
	public static void thresholdImage(BufferedImage image,float[][] magValue) throws IOException {
	    BufferedImage result = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
	    result.getGraphics().drawImage(image, 0, 0, null);
	    
	    WritableRaster raster = result.getRaster();
	    
	    float[] histo = new float[image.getWidth()*image.getHeight()];
	     int top=0;
	    int[] pixels = new int[image.getWidth()];
	    for (int y = 0; y < image.getHeight(); y++) {
	        raster.getPixels(0, y, image.getWidth(), 1, pixels);   
	        for (int i = 0; i < pixels.length; i++) {
	            if (pixels[i] ==255){
	            	histo[top++]= magValue[i][y];
	            	System.out.println(magValue[i][y]);
	            }
	        }    
	    }
	    System.out.println(top);
	}
	
	public static BufferedImage convertPaintedImage(BufferedImage image) throws IOException {
		BufferedImage result = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
		result.getGraphics().drawImage(image, 0, 0, null);
		
		WritableRaster raster = result.getRaster();
		int[] pixels = new int[image.getWidth()];
		for (int y = 0; y < image.getHeight(); y++) {
			raster.getPixels(0, y, image.getWidth(), 1, pixels);
			for (int i = 0; i < pixels.length; i++) {
				if (pixels[i] == 0)
					pixels[i] = 255;
				else
					pixels[i] = 0;
			}
			raster.setPixels(0, y, image.getWidth(), 1, pixels);
		}
		File f = new File("F:\\cop_" + base + ".jpg");
		if (!ImageIO.write(result, "JPEG", f)) {
			throw new RuntimeException("Unexpected error writing image");
		}
		return result;
	}
	
	
	public static void constructIntermediate( float[][] magValue, boolean flag) throws IOException {

		WritableRaster raster = intermediate.getRaster();
		int[] pixels = new int[intermediate.getWidth()];
		
		for (int y = 0; y < intermediate.getHeight(); y++) {

			raster.getPixels(0, y, intermediate.getWidth(), 1, pixels);

			for (int i = 0; i < pixels.length; i++) {
				pixels[i]=(int)Math.min(255f,(1.25f* Math.abs(magValue[i][y] ))); 
				
			}
			 raster.setPixels(0, y, intermediate.getWidth(), 1, pixels);

		}

		if (flag) {
			File f = new File("F:\\intermediate_sobel_x_" + base + num + ".jpg");
			if (!ImageIO.write(intermediate, "JPEG", f)) {
				throw new RuntimeException("Unexpected error writing image");
			}
		} else {
			File f = new File("F:\\intermediate_sobel_y_" + base + num + ".jpg");
			if (!ImageIO.write(intermediate, "JPEG", f)) {
				throw new RuntimeException("Unexpected error writing image");
			}
		}
	}

	public static void convolution(float[][] mag, BufferedImage img, float[][] kernel) {
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
				if (Math.abs(sum) < 0.005f)
					sum = 0;
				mag[x][y] = sum;
			}
		}
	}


	public static BufferedImage getSobelOutput(BufferedImage img) throws IOException {
		BufferedImage result = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
		result.getGraphics().drawImage(img, 0, 0, null);

		float[][] xMag = new float[img.getWidth()][img.getHeight()];
		float[][] yMag = new float[img.getWidth()][img.getHeight()];
		float[][] resMag=  new float[img.getWidth()][img.getHeight()];
		
		float[][] x1 = { { -1, 0, 1 }, { -2, 0, 2 }, { -1, 0, 1 } };
		float[][] y1 = { { -1, -2, -1 }, { 0, 0, 0 }, { 1, 2, 1 } };
		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 3; j++) {
				x1[i][j] /= 4.0f;
				y1[i][j] /= 4.0f;
			}
		}
		intermediate = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
		convolution(xMag, img, x1);
		constructIntermediate(xMag, true);
		
		convolution(yMag, img, y1);
		constructIntermediate( yMag, false);

		for (int i = 0; i < img.getWidth(); i++) {
			for (int j = 0; j < img.getHeight(); j++) {
				float resMagn = resultantMag(xMag, yMag, i, j);
				resMag[i][j]=resMagn;
				if (resMagn > threshold) {
					result.setRGB(i, j, Color.white.getRGB());
				} else {
					result.setRGB(i, j, Color.black.getRGB());
				}
			}
		}

		BufferedImage src = ImageIO.read(new File("F:\\painted_" + base + "0.jpg"));
		
		src=convertPaintedImage(src);

	
		thresholdImage(src, resMag);
		
		
		return result;
	}

	// formula for making things work.
	private static float resultantMag(float[][] xmag, float[][] ymag, int i, int j) {

		float res = (float) Math.sqrt(xmag[i][j] * xmag[i][j] + ymag[i][j] * ymag[i][j]);

		return res;
	}
	
	


	public static void showPixels(BufferedImage image) throws IOException {
		BufferedImage result = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
		result.getGraphics().drawImage(image, 0, 0, null);
		WritableRaster raster = result.getRaster();
		int[] pixels = new int[image.getWidth()];
		for (int y = 0; y < image.getHeight(); y++) {
			raster.getPixels(0, y, image.getWidth(), 1, pixels);
			for (int i = 0; i < pixels.length; i++) {
				System.out.println(pixels[i]);
			}
			raster.setPixels(0, y, image.getWidth(), 1, pixels);
		}

	}


	public static float autoThresholdMedian(BufferedImage image) {
		float median = -1;

		BufferedImage result = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
		result.getGraphics().drawImage(image, 0, 0, null);
		WritableRaster raster = result.getRaster();
		int[] freq = new int[256];
		int[] cum = new int[256];
		int sum = 0;
		int[] pixels = new int[image.getWidth()];
		for (int y = 0; y < image.getHeight(); y++) {
			raster.getPixels(0, y, image.getWidth(), 1, pixels);
			for (int i = 0; i < pixels.length; i++) {
				freq[pixels[i]]++;
			}
			raster.setPixels(0, y, image.getWidth(), 1, pixels);
		}
		for (int i = 0; i < 256; i++) {
			sum += freq[i];
			cum[i] = sum;
		}
		// System.out.println(sum);
		sum /= 2;
		for (int i = 0; i < 256; i++) {
			if (sum < cum[i]) {
				median = i;
				break;
			}
		}
		System.out.println(median);
		return median;
	}

	public static void main(String args[]) throws IOException {

		try {
			System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
			System.out.println("\nRunning FaceDetector");

			CascadeClassifier faceDetector = new CascadeClassifier(("D:\\haarcascade_frontalface_alt.xml"));
			Mat image = Highgui.imread(("D:\\" + base + ".jpg"));
			BufferedImage src = ImageIO.read(new File("D:\\" + base + ".jpg"));

			MatOfRect faceDetections = new MatOfRect();
			faceDetector.detectMultiScale(image, faceDetections);
			BufferedImage dest;

			/*
			 * dest=ImageIO.read(new File("D:\\" + "pika.jpg"));
			 * 
			 * BufferedImage overImg = new BufferedImage(dest.getWidth(),
			 * dest.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
			 * overImg.getGraphics().drawImage(dest, 0, 0, null); threshold=
			 * autoThresholdMedian(overImg); System.out.println(threshold);
			 * threshold*=.035f; //showPixels(overImg);
			 * //overImg=gaussianSmoothing(overImg); BufferedImage result =
			 * ImageReader.overRide(overImg);
			 * 
			 * BufferedImage finalRes = new BufferedImage(dest.getWidth(),
			 * dest.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
			 * finalRes.getGraphics().drawImage(splitJoin(result), 0, 0, null);
			 * 
			 * 
			 * // writing
			 * 
			 * 
			 * JFrame jframe = new JFrame();
			 * jframe.getContentPane().setLayout(new FlowLayout());
			 * jframe.getContentPane().add(new JLabel(new ImageIcon(finalRes)));
			 * 
			 * jframe.pack(); jframe.setVisible(true);
			 * jframe.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			 * 
			 * File f = new File("D:\\sorp_" + base + num + ".jpg"); if
			 * (!ImageIO.write(finalRes, "JPEG", f)) { throw new
			 * RuntimeException("Unexpected error writing image"); }
			 * 
			 */

			System.out.println(String.format("Detected %s faces", faceDetections.toArray().length));
			for (Rect rect : faceDetections.toArray()) {
				Core.rectangle(image, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height),
						new Scalar(0, 255, 0));

				if (rect.y - (rect.height / 6) * 2 >= 0
						&& rect.y + rect.height + (rect.height / 6) * 2 <= src.getHeight()
						&& rect.x - (rect.width / 6) >= 0 && rect.x + rect.width + (rect.width / 6) <= src.getWidth()) {
					
					dest = src.getSubimage(rect.x - rect.width / 6, rect.y - (rect.height / 6) * 2,
							rect.width + rect.width / 3, rect.height + 2 * rect.height / 3);
				} else if (rect.y - rect.height / 6 >= 0 && rect.y + rect.height + rect.height / 6 <= src.getHeight()
						&& rect.x - (rect.width / 6) >= 0 && rect.x + rect.width + (rect.width / 6) <= src.getWidth()) {
					
					dest = src.getSubimage(rect.x - rect.width / 6, rect.y - rect.height / 6,
							rect.width + rect.width / 3, rect.height + rect.height / 3);
				}

				else if (rect.y - rect.height / 6 >= 0 && rect.y + rect.height + rect.height / 6 <= src.getHeight()) {
					
					dest = src.getSubimage(rect.x, rect.y - rect.height / 6, rect.width, rect.height + rect.height / 3);
				} else {
					
					dest = src.getSubimage(rect.x, rect.y, rect.width, rect.height);
				}

				File f = new File("D:\\corp_" + base + num + ".jpg");
				if (!ImageIO.write(dest, "JPEG", f)) {
					throw new RuntimeException("Unexpected error writing image");
				}

				BufferedImage overImg = new BufferedImage(dest.getWidth(), dest.getHeight(),
						BufferedImage.TYPE_BYTE_GRAY);
				overImg.getGraphics().drawImage(dest, 0, 0, null);
				System.out.println(threshold);

				BufferedImage result = getSobelOutput(overImg);


				JFrame jframe = new JFrame();
				jframe.getContentPane().setLayout(new FlowLayout());
				jframe.getContentPane().add(new JLabel(new ImageIcon(result)));
				jframe.pack();
				jframe.setVisible(true);
				jframe.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

				f = new File("D:\\sorp_" + base + num + ".jpg");
				if (!ImageIO.write(result, "JPEG", f)) {
					throw new RuntimeException("Unexpected error writing image");
				}
			}

		} catch (IOException exception) {
			exception.printStackTrace();
		}
	}
}