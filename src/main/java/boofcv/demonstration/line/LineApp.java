package boofcv.demonstration.line;

import java.io.*;
import java.util.List;
import java.util.concurrent.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.alg.feature.detect.edge.GGradientToEdgeFeatures;
import boofcv.alg.feature.detect.line.*;
import boofcv.alg.feature.detect.line.gridline.*;
import boofcv.alg.filter.binary.GThresholdImageOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.factory.filter.derivative.FactoryDerivative;
import boofcv.gui.binary.VisualizeBinaryData;
import boofcv.helper.*;
import boofcv.helper.visualize.*;
import boofcv.helper.visualize.control.*;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.feature.MatrixOfList;
import boofcv.struct.image.*;
import georegression.fitting.line.ModelManagerLinePolar2D_F32;
import georegression.struct.line.*;
import org.ddogleg.fitting.modelset.*;
import org.ddogleg.fitting.modelset.ransac.Ransac;

/**
 * Tracks moving objects and its size for motion detection
 *
 * @author Thomas Hartwig
 */
public class LineApp {

	private final BoofCvDevPanel dev;
	private final ControlDouble edgeThreshold;
	private final ControlInteger regionSize;
	private final ControlInteger maxLines;
	private final ControlInteger mergeAngle;
	private final ControlInteger mergeDistance;
	private final ControlList<PathLabel> paths;

	Class imageType = GrayF32.class;
	Class derivType = GrayF32.class;


	private BufferedImage workImage;
	private BufferedImage workImageGray;
	private List<LineSegment2D_F32> linesFound;

	private LineApp() {
		edgeThreshold = new ControlDouble("EdgeThreshold", 30, 0, 255);
		regionSize = new ControlInteger("RegionSize", 4, 1, 24);
		maxLines = new ControlInteger("MaxLines", 10, 1, 100);
		mergeAngle = new ControlInteger("MergeAngle", 4, 0, 90);
		mergeDistance = new ControlInteger("MergeDistance", 5, 1, 100);
		paths = new ControlList<>("Inputs",
				new PathLabel("1", "/home/th/dev/ai/uvisContainer/work/Tensor/20160701/20160701081953016-000000000000000000_frontbild.jpg"),
				new PathLabel("2", "/home/th/dev/ai/uvisContainer/work/Tensor/20160701/20160701084916585-000000000000000000_frontbild.jpg"),
				new PathLabel("3", "/home/th/dev/ai/uvisContainer/work/Tensor/20160701/20160701100302637-000000000000000000_frontbild.jpg"),
				new PathLabel("4", "/home/th/dev/ai/uvisContainer/work/Tensor/20160701/20160701131947938-000000000000000000_frontbild.jpg")
		);
		dev = new BoofCvDevPanel(control -> changeInput(), true, mergeDistance, mergeAngle, edgeThreshold, regionSize, maxLines, paths);
		SwingUtilities.invokeLater(this::changeInput);
	}

	private void renderFeatures(BufferedImage orig, double trackerFPS) {
		if (workImage != null) {
			Graphics2D g2 = workImage.createGraphics();
			final int hx = orig.getHeight();
			final int wx = orig.getWidth();
			g2.drawImage(orig, 0,0, wx, hx, null);
			if (linesFound != null) {
				g2.setColor(Color.RED);
				for (LineSegment2D_F32 f32 : linesFound) {
					g2.draw(HelperGeometry.convertToLine(f32));
				}
			}
			g2.setColor(Color.WHITE);
			g2.fillRect(5,15,140,20);
			g2.setColor(Color.BLACK);
			g2.drawString(String.format("Tracker FPS: %3.1f",trackerFPS),10,30);
		}
	}

	private final static ExecutorService service = Executors.newSingleThreadExecutor();
	private long timeGlobal = 0;

	private void changeInput() {
		final long timeLocal;
		synchronized (service) {
			timeGlobal = System.currentTimeMillis();
			timeLocal = timeGlobal;
		}
		service.submit(new Callable<Void>() {
			public Void call() throws Exception {
				try {
						if (timeLocal == timeGlobal) {
							workImage = ImageIO.read(new FileInputStream(paths.getValue().getPath()));
							workImageGray = HelperConvert.convertToBufferedGray(ConvertBufferedImage.extractInterleavedU8(workImage));
							dev.setBusy(true);
							updateAlgRansac();
							dev.setBusy(false);
						}
				} catch (Throwable throwable) {
					throwable.printStackTrace();
				}
				return null;
			}
		});

	}


	private void updateAlgRansac() {
		long timerMeasure = System.currentTimeMillis();
		//this.frameLive = frame;
		BufferedImage image = workImageGray;
		ImageGray input = GeneralizedImageOps.createSingleBand(imageType, image.getWidth(), image.getHeight());
		ImageGray derivX = GeneralizedImageOps.createSingleBand(derivType, image.getWidth(), image.getHeight());
		ImageGray derivY = GeneralizedImageOps.createSingleBand(derivType, image.getWidth(), image.getHeight());
		GrayF32 edgeIntensity =  new GrayF32(input.width,input.height);
		GrayF32 suppressed =  new GrayF32(input.width,input.height);
		GrayF32 orientation =  new GrayF32(input.width,input.height);
		GrayS8 direction = new GrayS8(input.width,input.height);
		GrayU8 detected = new GrayU8(input.width,input.height);

		GridLineModelDistance distance = new GridLineModelDistance((float)(Math.PI*0.75));
		GridLineModelFitter fitter = new GridLineModelFitter((float)(Math.PI*0.75));
		ModelManager<LinePolar2D_F32> manager = new ModelManagerLinePolar2D_F32();

		ModelMatcher<LinePolar2D_F32, Edgel> matcher = new Ransac<>(0L, manager, fitter, distance, 25, 1);

		final ImageGradient gradient = FactoryDerivative.sobel(imageType, derivType);

		System.out.println("Image width "+input.width+" height "+input.height);

		ConvertBufferedImage.convertFromSingle(image, input, imageType);
		gradient.process(input, derivX, derivY);
		GGradientToEdgeFeatures.intensityAbs(derivX, derivY, edgeIntensity);

		// non-max suppression on the lines
		//		GGradientToEdgeFeatures.direction(derivX,derivY,orientation);
		//		GradientToEdgeFeatures.discretizeDirection4(orientation,direction);
		//		GradientToEdgeFeatures.nonMaxSuppression4(edgeIntensity,direction,suppressed);

		GThresholdImageOps.threshold(edgeIntensity,detected,edgeThreshold.getValue(),false);

		GridRansacLineDetector<GrayF32> alg = new ImplGridRansacLineDetector_F32(regionSize.getValue(), maxLines.getValue(),matcher);
		alg.process((GrayF32) derivX, (GrayF32) derivY, detected);
		MatrixOfList<LineSegment2D_F32> gridLine = alg.getFoundLines();

		//ConnectLinesGrid connect = new ConnectLinesGrid(Math.PI*0.01,1,8);
		//		connect.process(gridLine);
		//		LineImageOps.pruneClutteredGrids(gridLine,3);
		linesFound = gridLine.createSingleList();
		System.out.println("size = "+ linesFound.size());
		LineImageOps.mergeSimilar(linesFound, (float) Math.toRadians(mergeAngle.getValue()), mergeDistance.getValue());
		//		LineImageOps.pruneSmall(found,40);
		System.out.println("after size = "+ linesFound.size());

		BufferedImage renderedBinary = VisualizeBinaryData.renderBinary(detected, false, null);
		dev.updateImage("Binary", renderedBinary);
		dev.updateImage(BoofCvDevPanel.KEY_SOURCE, workImage);
		renderFeatures(workImage, 1000/(System.currentTimeMillis() - timerMeasure));
		if (workImage != null) {
			dev.updateImage(BoofCvDevPanel.KEY_RESULT, workImage);
		}
	}


	public static void main( String args[] ) {
		new LineApp();
	}

}
