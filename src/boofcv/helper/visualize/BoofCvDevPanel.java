package boofcv.helper.visualize;

import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import javax.swing.*;
import boofcv.gui.image.VisualizeImageData;
import boofcv.helper.HelperConvert;
import boofcv.helper.visualize.control.Control;
import boofcv.helper.visualize.control.Control.ControlListener;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.image.*;

@SuppressWarnings("unused")
public class BoofCvDevPanel extends JDialog implements ControlListener {
   private final Control[] controls;
   private JPanel contentPane;
   private JPanel panelControl;
   private JPanel panelThumbs;
   private JPanel panelImage;
   private JProgressBar progressBar;
   private JPanel panelAll;
   private JScrollPane scrollPane;
   private JTabbedPane tabbedPane;
   public static String KEY_RESULT = "Result";
   public static String KEY_SOURCE = "Source";
   public static String REFERENCE_DEFAULT = "_DEFAULT_";
   private final Map<String, ImageContainer> containers = new ConcurrentHashMap<>();
   private String referenceSelected = REFERENCE_DEFAULT;
   private final List<ControlListener> listeners = new ArrayList<>();
   private Point pointClicked;

   private final List<Roi> rois = new ArrayList<>();

   public void addRoi(Shape shape) {
      rois.add(new Roi());
   }

   public void addRoi(Shape shape, Color color) {
      Roi roi = new Roi();
      roi.color = color;
      rois.add(roi);
   }

   public ImageBase getImage(String reference, String key) {
      ImageContainer container = containers.get(reference);
      if(container != null) {
         return container.images.get(key);
      }
      return null;
   }

   private static class Roi {
      private Color color = Color.RED;
      private float transparency = 1.0f;
      private Shape shape = new Rectangle();

   }

   public BoofCvDevPanel(ControlListener listeners, boolean show, Control... controls) {
      setContentPane(contentPane);
      setModal(false);
      if(listeners != null) this.listeners.add(listeners);
      // call onCancel() when cross is clicked
      setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
      addWindowListener(new WindowAdapter() {
         public void windowClosing(WindowEvent e) {
            onCancel();
         }
      });
      this.controls = controls;
      // call onCancel() on ESCAPE
      contentPane.registerKeyboardAction(e -> onCancel(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
      final Box box = Box.createVerticalBox();
      panelControl.add(box, BorderLayout.NORTH);
      for (Control control : controls) {
         control.setControlListener(this);
         box.add(control.getComponent());
      }
      setSize(1920, 1024);
      SwingUtilities.invokeLater(() -> {
         panelImage.add(new Painter());
         panelThumbs.add(new PainterThumbnail());
         if (show) {
            setVisible(true);
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
      });
      panelImage.addMouseWheelListener(e -> SwingUtilities.invokeLater(() -> {
         Dimension size = panelImage.getSize();
         int amount = -e.getUnitsToScroll() * 4;
         int x = size.width + amount;
         int y = size.height + Math.round(amount * ((float)panelImage.getHeight() / panelImage.getWidth()));
         panelImage.setPreferredSize(new Dimension(x, y));
         panelImage.setSize(new Dimension(x, y));
         //Rectangle rec = panelSource.getBounds();
         //scrollTo(new Point((int) rec.getCenterX(), (int) rec.getCenterY()), new Point(e.getPoint().x, e.getPoint().y));
      }));
   }

   public void addMouseMotionListener(final MouseMotionListener motionAdapter) {
      panelImage.addMouseMotionListener(motionAdapter);
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

   public BoofCvDevPanel(Map<String, Control> controls) {
      this(null, controls);
   }

   public void addListener(ControlListener listener) {
      if(listener != null) this.listeners.add(listener);
   }

   public void showOnScreen() {
      SwingUtilities.invokeLater(() -> setVisible(true));
   }

   public BoofCvDevPanel(ControlListener listeners, Map<String, Control> map) {
      this(listeners, true, map.values().toArray(new Control[map.size()]));
   }

   public Object getValue(Object name) {
      if (name instanceof Control) return ((Control)name).getValue();
      for (Control control : controls) {
         if (control.getName().equals(name)) return control.getValue();
      }
      return null;
   }

   public Double getValueAsDouble(Object name) {
      return (Double)getValue(name);
   }

   public Integer getValueAsInt(Object name) {
      return (Integer)getValue(name);
   }

   public Boolean getValueAsBoolean(Object name) {
      return (Boolean)getValue(name);
   }

   public void updateImage(String key, BufferedImage image) {
      if (image.getSampleModel().getNumBands() > 1) {
         final InterleavedU8 dst = new InterleavedU8(image.getWidth(), image.getHeight(), 3);
         ConvertBufferedImage.convertFromInterleaved(image, dst, true);
         updateImage(key, dst);
      } else {
         final GrayU8 dst = new GrayU8(image.getWidth(), image.getHeight());
         ConvertBufferedImage.convertFrom(image, dst);
         updateImage(key, dst);
      }
   }

   public void setBusy(boolean status) {
      progressBar.setIndeterminate(status);
   }

   public void updateImage(String key, ImageBase image) {
      updateImage(REFERENCE_DEFAULT, key, image);
   }

   public void updateImage(String reference, String key, ImageBase image) {
      ImageContainer container = containers.computeIfAbsent(reference, k -> new ImageContainer(reference));
      if(! container.containsKey(key) && ! key.equals(KEY_RESULT)) {
         JPanel panel = new JPanel(new BorderLayout());
         panel.setName(key);
         tabbedPane.addTab(key, panel);
      }
      container.images.put(key, image);
      SwingUtilities.invokeLater(() -> panelImage.repaint());
      SwingUtilities.invokeLater(() -> panelThumbs.repaint());
   }

   private void onCancel() {
      // add your code here if necessary
      dispose();
   }

   public void fireControlUpdated(Control control) {
      for (ControlListener listener : listeners) {
         new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
               try {
                  listener.fireControlUpdated(control);
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

   public void visualizeImage(String keyResult, ImageGray<?> output) {
      updateImage(keyResult, VisualizeImageData.grayMagnitudeTemp(output, null, 1.0));
   }

   private class Painter extends JPanel {
      protected void paintComponent(Graphics g) {
         super.paintComponent(g);
         ImageContainer imageContainer = containers.get(referenceSelected);
         if (imageContainer != null) {
            String key = tabbedPane.getSelectedComponent().getName();
            final ImageBase image = imageContainer.get(key);
            if (image != null) {
               final int width = image.width;
               final int height = image.height;
               g.drawImage(ConvertBufferedImage.convertTo(image, null, true), 0, 0, getWidth(), getHeight(), null);
            }
         }
      }
   }


   private class ImageContainer {
      private String reference;
      private final Map<String, ImageBase> images = new ConcurrentHashMap<>();

      ImageContainer(String reference) {
         this.reference = reference;
      }

      public int hashCode() {
         return reference.hashCode();
      }

      boolean containsKey(String key) {
         return images.containsKey(key);
      }

      public ImageBase get(String key) {
         return images.get(key);
      }

      Iterable<? extends String> keySet() {
         return images.keySet();
      }
   }

   private class PainterThumbnail extends JPanel {
      protected void paintComponent(Graphics g) {
         super.paintComponent(g);
         final int height = getHeight() - 26;
         int pos = 0;
         ImageContainer imageContainer = containers.get(referenceSelected);
         if (imageContainer != null) {
            for (String key : imageContainer.keySet()) {
                  final ImageBase image = imageContainer.get(key);
                  int ph = (image.height < height) ? image.height : height;
                  int pw = (int)((double)ph / image.height * image.width);
                  g.drawImage(HelperConvert.convertToBufferedGray(image), pos, 24, pw, ph, null);
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
