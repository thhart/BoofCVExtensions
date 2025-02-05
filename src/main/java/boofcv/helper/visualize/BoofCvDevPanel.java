package boofcv.helper.visualize;

import boofcv.core.image.FactoryGImageMultiBand;
import boofcv.core.image.GImageMultiBand;
import boofcv.gui.image.VisualizeImageData;
import boofcv.helper.HelperConvert;
import boofcv.helper.visualize.control.*;
import boofcv.helper.visualize.control.Control.ControlListener;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.image.*;
import com.itth.optimizerhelper.Variable;
import org.apache.logging.log4j.LogManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.List;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings({"unused", "WeakerAccess"})
public class BoofCvDevPanel implements ControlListener {
	protected final static org.apache.logging.log4j.Logger logger = LogManager.getLogger(BoofCvDevPanel.class);

	public static String KEY_RESULT = "Result";
	public static String KEY_SOURCE = "Source";
	public static String REFERENCE_DEFAULT = "_DEFAULT_";
	private final Map<String, ImageContainer> containers = new ConcurrentHashMap<>();
	private final List<ControlListener> listeners = new ArrayList<>();
	private final List<PointListener> pointListeners = new ArrayList<>();
	private final String referenceSelected = REFERENCE_DEFAULT;
	private final List<ShapePaintable> shapePaintableList = new ArrayList<>();
	private final List<TextPaintable> textPaintableList = new ArrayList<>();

	public JPanel getContentPane() {
		return contentPane;
	}

	private JPanel contentPane;
	private Control[] controls;
	private JLabel labelColor;
	private JLabel labelMouse;
	private JLabel labelStatus;
	private JPanel panelAll;
	private JPanel panelControl;
	private JPanel panelImage;
	private JPanel panelThumbs;
	private Point pointClicked;
	private JProgressBar progressBar;
	private JScrollPane scrollPane;
	private double sx;
	private double sy;
	private JTabbedPane tabbedPane;

	public BoofCvDevPanel(Map<String, Control<?>> controls) {
		this(null, controls);
	}

	public BoofCvDevPanel(ControlListener listener, Class<?> clazz) throws IllegalAccessException {
		List<Control<?>> cx = new ArrayList<>();
		Field[] fields = clazz.getFields();
		Arrays.sort(fields, Comparator.comparing(Field::getName));
		for (Field field : fields) {
			try {
				if (field.isAnnotationPresent(Variable.class)) {
					Variable variable = field.getAnnotation(Variable.class);
					switch (variable.type()) {
						case Float ->
								cx.add(new ControlFloat(field.getName(), field.getFloat(null), (float)variable.min(), (float)variable.max(), variable.precision()));
						case Integer ->
								cx.add(new ControlInteger(field.getName(), field.getInt(null), variable.min(), variable.max(), variable.precision()));
						case Double ->
								cx.add(new ControlDouble(field.getName(), field.getDouble(null), variable.min(), variable.max(), variable.precision()));
						case Boolean -> cx.add(new ControlBoolean(field.getName(), field.getBoolean(null)));
					}
				}
			} catch (Exception e) {
				logger.error("could not initialize control for field: " + field.getName(), e);
			}
		}
		initialize(listener, false, cx.toArray(new Control[0]));
	}

	public BoofCvDevPanel(ControlListener listener, Object clazz) {
		List<Control<?>> cx = new ArrayList<>();
		Field[] fields = clazz.getClass().getFields();
		Arrays.sort(fields, Comparator.comparing(Field::getName));
		for (Field field : fields) {
			try {
				if (field.isAnnotationPresent(Variable.class)) {
					Variable variable = field.getAnnotation(Variable.class);
					switch (variable.type()) {
						case Float ->
								cx.add(new ControlFloat(field.getName(), field.getFloat(clazz), (float)variable.min(), (float)variable.max(), variable.precision()));
						case Integer ->
								cx.add(new ControlInteger(field.getName(), field.getInt(clazz), variable.min(), variable.max(), variable.precision()));
						case Double ->
								cx.add(new ControlDouble(field.getName(), field.getDouble(clazz), variable.min(), variable.max(), variable.precision()));
						case Boolean -> cx.add(new ControlBoolean(field.getName(), field.getBoolean(clazz)));
					}
				}
			} catch (Exception e) {
				logger.error("could not initialize control for field: " + field.getName(), e);
			}
		}
		initialize(listener, false, cx.toArray(new Control[0]));
	}

	public BoofCvDevPanel(Class<?> clazz) throws IllegalAccessException {
		this(null, clazz);
	}

	public BoofCvDevPanel(Object clazz) {
		this(null, clazz);
	}

	public BoofCvDevPanel(Control<?>... controls) {
		this(null, false, controls);
	}

	public BoofCvDevPanel(ControlListener listener, Map<String, Control<?>> map) {
		this(listener, false, map.values().toArray(new Control[0]));
	}

	public BoofCvDevPanel(ControlListener listeners, boolean show, Control<?>... controls) {
		initialize(listeners, show, controls);
	}

	public void addListener(ControlListener listener) {
		if (listener != null) this.listeners.add(listener);
		new SwingWorker<Void, Void>() {
			@Override
			protected Void doInBackground() {
				listener.fireControlUpdated(controls);
				return null;
			}

			protected void done() {
			}
		}.execute();

	}

	public synchronized void addMouseListener(MouseListener l) {
		panelImage.addMouseListener(l);
	}

	public void addMouseMotionListener(final MouseMotionListener motionAdapter) {
		panelImage.addMouseMotionListener(motionAdapter);
	}

	public void addPointListener(PointListener pointListener) {
		pointListeners.add(pointListener);
	}

	public void addShape(Shape shape) {
		synchronized (shapePaintableList) {
			shapePaintableList.add(new ShapePaintable(shape));
		}
		contentPane.repaint();
	}

	public void addShape(Color color, Shape... shapes) {
		synchronized (shapePaintableList) {
			for (Shape shape : shapes) {
				ShapePaintable shapePaintable = new ShapePaintable(shape);
				shapePaintable.color = color;
				this.shapePaintableList.add(shapePaintable);
			}
		}
		contentPane.repaint();
	}

	public void addText(String format, Point point) {
		synchronized (textPaintableList) {
			TextPaintable e = new TextPaintable(format);
			e.point = point;
			textPaintableList.add(e);
		}
		contentPane.repaint();
	}

	public Integer asInt(Object name) {
		return (Integer)asValue(name);
	}

	public void clearLists() {
		synchronized (textPaintableList) {
			synchronized (shapePaintableList) {
				textPaintableList.clear();
				shapePaintableList.clear();
			}
		}
		contentPane.repaint();
	}

	public void fireControlUpdated(Control... controls) {
		for (ControlListener listener : listeners) {
			new SwingWorker<Void, Void>() {
				@Override
				protected Void doInBackground() throws Exception {
					try {
						listener.fireControlUpdated(controls);
					} catch (Exception e) {
						e.printStackTrace(); // TODO: handle somehow
					}
					return null;
				}

				protected void done() {
				}
			}.execute();
		}
	}

	private ImageContainer getContainerSelected() {
		return containers.get(referenceSelected);
	}

	public Control[] getControls() {
		return controls;
	}

	public ImageBase getImage(String reference, String key) {
		ImageContainer container = containers.get(reference);
		if (container != null) {
			return container.images.get(key);
		}
		return null;
	}

	public double getScaleX() {
		return sx;
	}

	public double getScaleY() {
		return sy;
	}

	public Object asValue(Object name) {
		if (name instanceof Control) return ((Control)name).getValue();
		for (Control control : controls) {
			if (control.getName().equals(name)) return control.getValue();
		}
		return null;
	}

	public Boolean asBool(Object name) {
		return (Boolean)asValue(name);
	}

	public Double asDouble(Object name) {
		return (Double)asValue(name);
	}

	public Float asFloat(Object name) {
		return (Float)asValue(name);
	}

	private void initScale(ImageBase image) {
		if (image != null) {
			sx = (double)panelImage.getWidth() / image.getWidth();
			sy = (double)panelImage.getHeight() / image.getHeight();
		}
	}

	private void initialize(ControlListener listener, boolean show, Control<?>... controls) {
		if (listener != null) this.listeners.add(listener);
		this.controls = controls;

		final Box box = Box.createVerticalBox();
		panelControl.add(box, BorderLayout.NORTH);
		SwingUtilities.invokeLater(() -> {
			for (Control<?> control : controls) {
				control.setControlListener(this);
				box.add(control.getComponent());
			}
		});
		SwingUtilities.invokeLater(() -> {
			panelImage.add(new Painter());
			panelThumbs.add(new PainterThumbnail());
			if (show) {
				showOnScreen();
			}
		});
		tabbedPane.addChangeListener(e -> SwingUtilities.invokeLater(() -> {
			scrollPane.getParent().removeAll();
			((JComponent)tabbedPane.getSelectedComponent()).add(scrollPane);
			tabbedPane.updateUI();
		}));
		panelImage.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				pointClicked = e.getPoint();
			}
		});
		panelImage.addMouseMotionListener(new MouseMotionAdapter() {
			public void mouseDragged(MouseEvent e) {
				scrollTo(pointClicked, e.getPoint());
			}

			public void mouseMoved(MouseEvent e) {
				int x = (int)Math.round(e.getPoint().x / sx);
				int y = (int)Math.round(e.getPoint().y / sy);
				SwingUtilities.invokeLater(() -> labelMouse.setText(" " + x + "x" + y + " "));
				SwingUtilities.invokeLater(() -> {
					ImageContainer imageContainer = getContainerSelected();
					if (imageContainer != null) {
						String key = tabbedPane.getSelectedComponent().getName();
						final ImageBase image = imageContainer.get(key);
						if (image != null && image.width > 0 && image.height > 0)	 {
							initScale(image);
							final int width = image.width;
							final int height = image.height;
							GImageMultiBand wrap = FactoryGImageMultiBand.wrap(image);
							float[] pixels = new float[image.getImageType().getNumBands()];
							wrap.get(x, y, pixels);
							for (PointListener pointListener : pointListeners) {
								pointListener.pointMovedTo(x, y, (int)pixels[0],
										(int)pixels[pixels.length > 1 ? 1 : 0], (int)pixels[pixels.length > 1 ? 2 : 0]);
							}
						}
					}
				});
			}
		});
		panelImage.addMouseWheelListener(e -> SwingUtilities.invokeLater(() -> {
			Dimension size = panelImage.getSize();
			int amount = -e.getUnitsToScroll() * 4;
			int w = size.width + amount;
			int h = size.height + Math.round(amount * ((float)panelImage.getHeight() / panelImage.getWidth()));
			panelImage.setPreferredSize(new Dimension(w, h));
			panelImage.setSize(new Dimension(w, h));
			Rectangle rec = new Rectangle(0, 0, w, h);
			//scrollTo(new Point((int) rec.getCenterX(), (int) rec.getCenterY()),new Point(-e.getPoint().x, -e.getPoint().y));
		}));
	}

	private void onCancel() {
		// add your code here if necessary
		if(frame != null) frame.dispose();
	}

	public void populateTo(Class<?> clazz) throws Exception {
		for (Field field : clazz.getFields()) {
			for (Control control : controls) {
				if (control.getName().equals(field.getName())) {
					field.set(null, control.getValue());
				}
			}
		}
	}

	public void populateTo(Object clazz) throws Exception {
		for (Field field : clazz.getClass().getFields()) {
			for (Control control : controls) {
				if (control.getName().equals(field.getName())) {
					field.set(clazz, control.getValue());
				}
			}
		}
	}

	public void repaint() {
		contentPane.repaint();
	}

	private void scrollTo(final Point point, final Point pointBase) {
		SwingUtilities.invokeLater(() -> {
			int deltaX = point.x - pointBase.x;
			int deltaY = point.y - pointBase.y;
			Rectangle view = scrollPane.getViewport().getViewRect();
			view.x += deltaX;
			view.y += deltaY;
			panelImage.scrollRectToVisible(view);
		});
	}

	public void setBusy(boolean status) {
		progressBar.setIndeterminate(status);
	}

	public void setStatus(String msg) {
		SwingUtilities.invokeLater(() -> {
			labelStatus.setText(msg);
		});
	}


	private volatile JFrame frame;

	public BoofCvDevPanel showOnScreen() {
		createFrame();
		SwingUtilities.invokeLater(() -> frame.setVisible(true));
		return this;
	}

	public JFrame createFrame() {
		if(frame == null) {
			synchronized (this) {
				if (frame == null) {
					frame = new JFrame();
					SwingUtilities.invokeLater(() -> {
						// call onCancel() when cross is clicked
			frame.setContentPane(contentPane);
						frame.setTitle("BoofCV Panel");
						frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
			WeakReference<BoofCvDevPanel> weakReference = new WeakReference<>(this);
						frame.addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent e) {
					if (weakReference.get() != null) {
						weakReference.get().onCancel();
					}
				}
			});
			contentPane.registerKeyboardAction(e -> {
				if(weakReference.get() != null)
					weakReference.get().onCancel();
			}, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);


						frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
						frame.pack();
						frame.setSize(1920, 1280);
					});
			}
		}
		}
		return frame;
	}

	public void updateImage(String key, ImageBase image) {
		updateImage(REFERENCE_DEFAULT, key, image);
	}

	public void updateImage(String reference, String key, ImageBase image) {
		synchronized (containers) {
			ImageContainer container = containers.computeIfAbsent(reference, k -> new ImageContainer(reference));
			if (!container.containsKey(key) && !key.equals(KEY_RESULT)) {
				JPanel panel = new JPanel(new BorderLayout());
				panel.setName(key);
				SwingUtilities.invokeLater(() -> {
					tabbedPane.addTab(key, panel);
				});
			}
			container.images.put(key, image);
			SwingUtilities.invokeLater(() -> panelImage.repaint());
			SwingUtilities.invokeLater(() -> panelThumbs.repaint());
		}
	}

	public void updateImage(BufferedImage image) {
		updateImage(KEY_RESULT, image);
	}

	public void updateImage(ImageBase image) {
		updateImage(KEY_RESULT, image);
	}

	public void updateImage(String key, BufferedImage image) {
		if (image.getSampleModel().getNumBands() > 1) {
			final Planar<GrayU8> dst = new Planar<>(GrayU8.class, image.getWidth(), image.getHeight(), image.getSampleModel().getNumBands());
			ConvertBufferedImage.convertFromPlanar(image, dst, true, GrayU8.class);
			updateImage(key, dst);
		} else {
			final GrayU8 dst = new GrayU8(image.getWidth(), image.getHeight());
			ConvertBufferedImage.convertFrom(image, dst);
			updateImage(key, dst);
		}
	}

	public void visualizeImage(String keyResult, ImageGray<?> output) {
		updateImage(keyResult, VisualizeImageData.grayMagnitude(output, null, 1.0));
	}

	public interface PointListener {
		void pointMovedTo(int x, int y, int r, int g, int b);
	}

	private static class ShapePaintable {
		private final Shape shape;
		private final float transparency = 1.0f;
		private Color color = Color.RED;

		public ShapePaintable(Shape shape) {
			this.shape = shape;
		}
	}

	private static class TextPaintable {
		private final Color color = Color.WHITE;
		private final String text;
		private final float transparency = 1.0f;
		public Point point;

		public TextPaintable(String text) {
			this.text = text;
		}
	}

	private class ImageContainer {
		private final Map<String, ImageBase> images = new ConcurrentHashMap<>();
		private final String reference;

		ImageContainer(String reference) {
			this.reference = reference;
		}

		boolean containsKey(String key) {
			return images.containsKey(key);
		}

		public ImageBase get(String key) {
			return images.get(key);
		}

		public int hashCode() {
			return reference.hashCode();
		}

		Iterable<? extends String> keySet() {
			return images.keySet();
		}
	}

	private class Painter extends JPanel {
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			ImageContainer imageContainer = getContainerSelected();
			if (imageContainer != null) {
				String key = tabbedPane.getSelectedComponent().getName();
				ImageBase image = imageContainer.get(key);
				if (image != null) {
					initScale(image);
					final int width = image.width;
					final int height = image.height;
					if(image.getImageType().getFamily() == ImageType.Family.PLANAR && image.getImageType().getNumBands() == 4) {
						Planar<GrayU8> planar = new Planar<>(GrayU8.class, width, height, 3);
						for (int i = 0; i < 3; i++) {
							planar.setBand(i, ((Planar<GrayU8>)image).getBand(i));
						}
						image = planar;
					}
					g.drawImage(ConvertBufferedImage.convertTo(image, null, true), 0, 0, getWidth(), getHeight(), null);
					Graphics2D g2d = (Graphics2D)g;
					g2d.setTransform(AffineTransform.getScaleInstance(sx, sy));
					synchronized (shapePaintableList) {
						for (ShapePaintable sx : shapePaintableList) {
							g2d.setColor(sx.color);
							g2d.draw(sx.shape);
						}
					}
					synchronized (textPaintableList) {
						for (TextPaintable tx : textPaintableList) {
							g2d.setColor(tx.color);
							g2d.drawString(tx.text, tx.point.x, tx.point.y);
						}
					}
				}
			}
		}
	}

	private class PainterThumbnail extends JPanel {
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			final int height = getHeight() - 26;
			int pos = 0;
			ImageContainer imageContainer = getContainerSelected();
			if (imageContainer != null) {
				for (String key : imageContainer.keySet()) {
					final ImageBase image = imageContainer.get(key);
					int ph = (image.height < height) ? image.height : height;
					int pw = (int)((double)ph / image.height * image.width);
          try {
            g.drawImage(HelperConvert.convertToBufferedGray(image), pos, 24, pw, ph, null);
          } catch (Exception e) {
						logger.warn("could not convert image to buffered image: " + e);
          }
          g.setColor(Color.WHITE);
					g.fillRect(pos, 0, pw, 24);
					g.setColor(Color.DARK_GRAY);
					g.drawRect(pos, 24, pw, pw);
					g.setColor(Color.BLACK);
					g.drawString(key, pos + 1, 18);
					pos += pw;
				}
			}
		}
	}
}
