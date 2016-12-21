package boofcv.demonstration.line;

import java.io.*;
import java.util.List;
import java.util.concurrent.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import boofcv.abst.feature.detect.line.DetectLineHoughPolar;
import boofcv.alg.filter.blur.GBlurImageOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.factory.feature.detect.line.*;
import boofcv.gui.binary.VisualizeBinaryData;
import boofcv.gui.image.*;
import boofcv.helper.*;
import boofcv.helper.visualize.DevPanel;
import boofcv.helper.visualize.control.*;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.image.*;
import georegression.struct.line.*;

/**
 * Tracks moving objects and its size for motion detection
 *
 * @author Thomas Hartwig
 */
public class LinePolarApp {

   private final DevPanel dev;
   private final ControlDouble edgeThreshold;
   private final ControlInteger regionSize;
   private final ControlInteger maxLines;
   private final ControlInteger mergeAngle;
   private final ControlInteger mergeDistance;
   private final ControlList<PathLabel> paths;

   Class imageType = GrayU8.class;
   Class derivType = GrayU8.class;


   private BufferedImage workImage;
   private BufferedImage workImageGray;
   private List<LineSegment2D_F32> linesFound;

   private LinePolarApp() {
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
      dev = new DevPanel(control -> changeInput(), true, mergeDistance, mergeAngle, edgeThreshold, regionSize, maxLines, paths);
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
      service.submit((Callable<Void>)() -> {
         try {
            try {
               if (timeLocal == timeGlobal) {
                  workImage = ImageIO.read(new FileInputStream(paths.getValue().getPath()));
                  workImageGray = HelperConvert.convertToBufferedGray(ConvertBufferedImage.extractInterleavedU8(workImage));
                  dev.setBusy(true);
                  updateAlgPolar();
                  dev.setBusy(false);
               }
            } catch (IOException e) {
               e.printStackTrace();
            }
         } catch (Throwable throwable) {
            throwable.printStackTrace();
         }
         return null;
      });

   }


   private void updateAlgPolar() {
      long timerMeasure = System.currentTimeMillis();
      BufferedImage image = workImageGray;
      GrayU8 input = (GrayU8) GeneralizedImageOps.createSingleBand(imageType, image.getWidth(), image.getHeight());
      GrayU8 blur = (GrayU8) GeneralizedImageOps.createSingleBand(imageType, image.getWidth(), image.getHeight());

      ConvertBufferedImage.convertFromSingle(image, input, imageType);
      GBlurImageOps.gaussian(input, blur, -1, 2, null);

      DetectLineHoughPolar<GrayU8,GrayU8> alg =  FactoryDetectLineAlgs.houghPolar(
            new ConfigHoughPolar(5, 10, 2, Math.PI / 180, 25, 10), imageType, derivType);

      List<LineParametric2D_F32> lines = alg.detect(blur);

      BufferedImage renderedTran = VisualizeImageData.grayMagnitude(alg.getTransform().getTransform(),null,-1);
      BufferedImage renderedBinary = VisualizeBinaryData.renderBinary(alg.getBinary(), false, null);

      this.linesFound.clear();
      for (LineParametric2D_F32 line : lines) {
         final LineSegment2D_F32 e = new LineSegment2D_F32();
         linesFound.add(e);
      }

      dev.updateImage("Magnitude", renderedTran);
      dev.updateImage("Binary", renderedBinary);
      dev.updateImage(DevPanel.KEY_SOURCE, workImage);
      renderFeatures(workImage, 1000/(System.currentTimeMillis() - timerMeasure));
      if (workImage != null) {
         dev.updateImage(DevPanel.KEY_RESULT, workImage);
      }

   }

   public static void main( String args[] ) {
      new LinePolarApp();
   }

}
