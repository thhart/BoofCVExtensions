package boofcv.demonstration.motion;
import java.io.IOException;
import java.text.*;
import java.util.Map;
import java.util.concurrent.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.swing.SwingUtilities;
import boofcv.abst.motion.ConfigMotionSize;
import boofcv.alg.tracker.motion.MotionTracker;
import boofcv.factory.motion.FactoryMotionTracker;
import boofcv.gui.binary.VisualizeBinaryData;
import boofcv.helper.*;
import boofcv.helper.fetcher.ScheduledFetcher;
import boofcv.helper.visualize.BoofCvDevPanel;
import boofcv.helper.visualize.control.*;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.GrayU8;
/**
 * Tracks moving objects and its size for motion detection
 *
 * @author Thomas Hartwig
 */
public class VideoTrackMotionApp implements FetcherListener {
private final BoofCvDevPanel dev;
	private final ControlDouble tMin;
	private final ControlDouble tMax;
	private final ControlInteger blur;
	private final ControlDouble sigma;
	private final ControlInteger radius;
	private final ControlInteger percentage;
	private final ControlList<PathLabel> paths;
private BufferedImage workImage;
	private GrayU8 framePrevious;
	private GrayU8 frameNext;
	private final Map<String, Control> controlMap = new ConcurrentHashMap<>();
private MotionTracker motionTracker;
	private final long timerSave = 0;
	private long counter = 0;
	private final NumberFormat format = NumberFormat.getNumberInstance();
private VideoTrackMotionApp() {
		format.setMinimumIntegerDigits(4);
		format.setGroupingUsed(false);
		tMin = new ControlDouble("Tmin", 2, 0, 255, 0.1);
		controlMap.put("Tmin", tMin);
		tMax = new ControlDouble("Tmax", 10, 0, 255, 0.1);
		controlMap.put("Tmax", tMax);
		blur = new ControlInteger("BlurRadius", 4, 1, 24, 1);
		controlMap.put("BlurRadius", blur);
		sigma = new ControlDouble("BlurSigma", -1, -1, 1, 0.1);
		controlMap.put("BlurSigma", sigma);
		radius = new ControlInteger("Radius", 4, 1, 12, 1);
		controlMap.put("Radius", radius);
		percentage = new ControlInteger("Percentage/Size", 25, 1, 100, 1);
		controlMap.put("Percentage/Size", percentage);
		paths = new ControlList<>("Inputs",
				new PathLabel("Ball", "thhart/video/motion/Ball.mjpeg"),
				new PathLabel("Street", "thhart/video/motion/street_intersection.mp4"),
				new PathLabel("Camera1", "http://192.168.188.42/snapshot.cgi?user=admin"),
				new PathLabel("Camera2", "http://192.168.188.200/snap.jpg")
		);
		controlMap.put("Paths", paths);
		dev = new BoofCvDevPanel(controls -> { changeInput(); }, controlMap);
		scheduledFetcher = new ScheduledFetcher(this);
		Executors.newCachedThreadPool().submit(scheduledFetcher);
		SwingUtilities.invokeLater(() -> changeInput());
	}
private void renderFeatures(BufferedImage orig, double trackerFPS) {
		if (workImage != null) {
			Graphics2D g2 = workImage.createGraphics();
			final int hx = orig.getHeight();
			final int wx = orig.getWidth();
			g2.drawImage(orig, 0,0, wx, hx, null);
			if (motionTracker != null) {
				for(Rectangle rec : HelperGeometry.clusterRectangles2D(HelperConvert.rectangles2D(motionTracker.rectangles), 2)) {
               g2.setColor(Color.WHITE);
               g2.draw(rec);
            }
				g2.setColor(Color.WHITE);
				g2.fillRect(5,15,140,20);
				g2.setColor(Color.BLACK);
				g2.drawString(String.format("Tracker FPS: %3.1f",trackerFPS),10,30);
			}
			if(motionTracker != null && motionTracker.isMotionDetected()) {
				g2.setColor(Color.RED);
				g2.fillOval(workImage.getWidth() - 25, 5, 20, 20);
			}
		}
	}
private long timer = System.currentTimeMillis();
private final ScheduledFetcher scheduledFetcher;
private void changeInput() {
		scheduledFetcher.setSource(paths.getValue().getPath());
		scheduledFetcher.setActive(true);
		motionTracker = FactoryMotionTracker.motionTracker(new ConfigMotionSize(blur.getValue(), sigma.getValue(), radius.getValue(), tMin.getValue().floatValue(), tMax.getValue().floatValue(), percentage.getValue()));
		frameNext = null; framePrevious = null;
	}
public void updateImage(GrayU8 grayU8, BufferedImage image) {
		workImage = image;
		updateAlg(grayU8, workImage);
	}
private void updateAlg(GrayU8 frame, BufferedImage buffImage) {
		long timerMeasure = System.currentTimeMillis();
		//this.frameLive = frame;
		this.frameNext = frame.clone();
		dev.updateImage(BoofCvDevPanel.KEY_SOURCE, frame);
		if (System.currentTimeMillis() - timer > 500 || this.framePrevious == null) {
			timer = System.currentTimeMillis();
			this.framePrevious = frame.clone();
		}
		if(frameNext != null && framePrevious != null) {
			try {
				UtilImageIO.savePGM(frameNext, "/tmp/motion" + format.format(1000 % counter++) + ".pgm");
			} catch (IOException e) {
				e.printStackTrace();
			}
			motionTracker.process(framePrevious, frameNext);
			GrayU8 difference = motionTracker.difference;
			dev.updateImage("Difference", difference);
			dev.updateImage("Contour", VisualizeBinaryData.renderBinary(motionTracker.binary, false, null));
			dev.updateImage("Lines", VisualizeBinaryData.renderContours(motionTracker.contours, null, difference.width, difference.height, null));
		}
		renderFeatures(buffImage, 1000/(System.currentTimeMillis() - timerMeasure));
		if (workImage != null) {
			dev.updateImage(BoofCvDevPanel.KEY_RESULT, workImage);
		}
	}
public static void main( String[] args) {
		final VideoTrackMotionApp app = new VideoTrackMotionApp();
	}
}
