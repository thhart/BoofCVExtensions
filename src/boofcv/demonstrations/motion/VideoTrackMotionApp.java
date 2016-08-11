package boofcv.demonstrations.motion;

import java.io.*;
import java.lang.ref.WeakReference;
import java.net.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.awt.*;
import java.awt.image.BufferedImage;
import boofcv.abst.motion.ConfigMotionSize;
import boofcv.alg.tracker.motion.MotionTracker;
import boofcv.factory.motion.FactoryMotionTracker;
import boofcv.helper.*;
import boofcv.helper.visualize.*;
import boofcv.helper.visualize.control.*;
import boofcv.io.*;
import boofcv.io.image.*;
import boofcv.io.wrapper.DefaultMediaManager;
import boofcv.io.wrapper.images.*;
import boofcv.struct.image.*;

/**
 * Tracks moving objects and its size for motion detection
 *
 * @author Thomas Hartwig
 */
public class VideoTrackMotionApp {

	private final DevPanel dev;
	private final ControlDouble tMin;
	private final ControlDouble tMax;
	private final ControlInteger blur;
	private final ControlDouble sigma;
	private final ControlInteger radius;
	private final ControlInteger percentage;

	private BufferedImage workImage;
	private GrayU8 framePrevious;
	private GrayU8 frameNext;
	private final Map<String, Control> controlMap = new ConcurrentHashMap<>();

	private static List<PathLabel> inputs;
	private MotionTracker motionTracker;

	private VideoTrackMotionApp() {
		tMin = new ControlDouble("Tmin", 2, 0, 255);
		controlMap.put("Tmin", tMin);
		tMax = new ControlDouble("Tmax", 10, 0, 255);
		controlMap.put("Tmax", tMax);
		blur = new ControlInteger("BlurRadius", 4, 1, 24);
		controlMap.put("BlurRadius", blur);
		sigma = new ControlDouble("BlurSigma", -1, -1, 1);
		controlMap.put("BlurSigma", sigma);
		radius = new ControlInteger("Radius", 4, 1, 12);
		controlMap.put("Radius", radius);
		percentage = new ControlInteger("Percentage/Size", 25, 1, 100);
		controlMap.put("Percentage/Size", percentage);
		dev = new DevPanel(control -> {}, controlMap);
		scheduledFetcher = new ScheduledFetcher();
		Executors.newCachedThreadPool().submit(scheduledFetcher);
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

	private class ScheduledFetcher implements Runnable {
		public void setActive(boolean active) {
			this.active = active;
		}

		private boolean active = true;
		private boolean changePending = false;

		public String getSource() {
			return source;
		}

		public void setSource(String source) {
			this.source = source;
			this.changePending = true;
		}

		private String source = "";

		private final WeakReference<Runnable> reference;

		public ScheduledFetcher() {
			reference = new WeakReference<>(this);
		}

		public void run() {
			while(reference.get() != null) {
				try {
					Thread.sleep(1000);
					if (active && source != null && source.length() > 0) {
						InputStream stream = null;
						try {
							final SimpleImageSequence<ImageInterleaved> sequence;
							long waitMax = System.currentTimeMillis();
							if(source.startsWith("http")) {
								if (source.endsWith("mjpeg")) {
									final URL urlx = new URL(source);
									stream = urlx.openStream();
									sequence = new MjpegStreamSequence<>(stream, ImageType.il(3, ImageDataType.U8));
								} else {
									sequence = new UrlImageSequence(source);
								}
								waitMax += 1000;
							} else {
								MediaManager media = DefaultMediaManager.INSTANCE;
								sequence = media.openVideo(source, ImageType.il(3, ImageDataType.U8));
							}
							do {
								while(sequence.hasNext()) {
                           workImage = sequence.getGuiImage();
                           updateAlg(HelperConvert.convertToGray((InterleavedU8)sequence.next()), sequence.getGuiImage());
                        }
							} while (waitMax > System.currentTimeMillis());
						} finally {
							if (stream != null) {
								stream.close();
							}
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

	}

	final ScheduledFetcher scheduledFetcher;

	public void changeInput(int index) {
		scheduledFetcher.setSource(inputs.get(index).getPath());
		scheduledFetcher.setActive(true);
		frameNext = null; framePrevious = null;
	}


	protected void updateAlg(GrayU8 frame, BufferedImage buffImage) {
		long timerMeasure = System.currentTimeMillis();
		//this.frameLive = frame;
		this.frameNext = frame.clone();
		dev.updateImage(DevPanel.KEY_SOURCE, frame);
		if (System.currentTimeMillis() - timer > 500 || this.framePrevious == null) {
			timer = System.currentTimeMillis();
			this.framePrevious = frame.clone();
		}
		if(frameNext != null && framePrevious != null) {
			motionTracker = FactoryMotionTracker.motionTracker(new ConfigMotionSize(blur.getValue(), sigma.getValue(), radius.getValue(), tMin.getValue().floatValue(), tMax.getValue().floatValue(), percentage.getValue()));
			motionTracker.process(framePrevious, frameNext);
			dev.updateImage("Difference", motionTracker.difference);
			dev.updateImage("Contour", motionTracker.binary);
		}
		renderFeatures(buffImage, 1000/(System.currentTimeMillis() - timerMeasure));
		if (workImage != null) {
			dev.updateImage(DevPanel.KEY_RESULT, workImage);
		}
	}

	public static void main( String args[] ) {
		final VideoTrackMotionApp app = new VideoTrackMotionApp();
		inputs = new ArrayList<>();
		inputs.add(new PathLabel("Ball", "thhart/video/motion/Ball.mjpeg"));
		inputs.add(new PathLabel("Street", "thhart/video/motion/street_intersection.mp4"));
		inputs.add(new PathLabel("Camera", "http://192.168.188.200/snap.jpg"));
		app.changeInput(2);
	}

	private class UrlImageSequence implements SimpleImageSequence<ImageInterleaved> {
		private final URL url;
		private final int width, height;
		private InterleavedU8 frame;
		private BufferedImage guiImage;
		private AtomicInteger integer = new AtomicInteger(0);

		public UrlImageSequence(String url) throws IOException {
			this.url = new URL(url);
			final BufferedImage image = fetch();
			this.width = image.getWidth();
			this.height = image.getHeight();
		}

		private BufferedImage fetch() throws IOException {
			guiImage = HelperIo.readImageFromUrl(this.url);
			frame = new InterleavedU8(guiImage.getWidth(), guiImage.getHeight(), 3);
			ConvertBufferedImage.convertFrom(guiImage, frame, true);
			return guiImage;
		}

		public int getNextWidth() {
			return width;
		}

		public int getNextHeight() {
			return height;
		}

		public boolean hasNext() {
			return true;
		}

		public ImageInterleaved<InterleavedU8> next() {
			try {
				fetch();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return frame;
		}

		public BufferedImage getGuiImage() {
			return guiImage;
		}

		public void close() {

		}

		public int getFrameNumber() {
			return integer.getAndIncrement();
		}

		public void setLoop(boolean loop) {

		}

		public ImageType<ImageInterleaved> getImageType() {
			return ImageType.il(3, ImageDataType.U8);
		}

		public void reset() {

		}
	}
}
