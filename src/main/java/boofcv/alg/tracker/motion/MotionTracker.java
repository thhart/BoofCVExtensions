package boofcv.alg.tracker.motion;
import java.util.*;
import java.util.List;
import java.awt.*;
import boofcv.abst.motion.ConfigMotionSize;
import boofcv.alg.feature.detect.edge.*;
import boofcv.alg.filter.blur.BlurImageOps;
import boofcv.alg.misc.PixelMath;
import boofcv.factory.feature.detect.edge.FactoryEdgeDetectors;
import boofcv.helper.HelperConvert;
import boofcv.helper.HelperGeometry;
import boofcv.struct.image.*;
import georegression.geometry.UtilPolygons2D_I32;
import georegression.struct.shapes.Rectangle2D_I32;
/**
 This is a simple motion tracker which tries to recognize object size. Following algorithm is used:
 1. Take two input images and blur these.
 2. Create a diff of the inputs.
 3. Extract canny of the diff.
 4. Try to cluster boundaries of differences to check for singular objects.
It is recommended to make sure to use a time shift of two images for at least 100 ms to make sure the differences are
 visible in the image.
TODO: "Simple" light change is still a problem, when a camera switches to infrared mode for instance, this might
 be circumstanced by thresholding the motion time wise, so if the motion is persistent over a minimum time period like
 more than 1 second or for a minimum of frames
@author Thomas Hartwig
 @date 20160812
 */
public class MotionTracker {
   public ConfigMotionSize config = new ConfigMotionSize();
   public GrayU8 difference;
   public GrayU8 binary;
   public List<EdgeContour> contours;
   public final List<georegression.struct.shapes.Rectangle2D_I32> rectangles = new ArrayList<>();
public boolean isMotionDetected() {
      return motionDetected;
   }
public boolean motionDetected = false;
public MotionTracker() {
      this(new ConfigMotionSize());
   }
public MotionTracker(ConfigMotionSize config) {
      this.config = config;
   }
public void process(GrayU8 framePrevious, GrayU8 frameNext) {
      motionDetected = false;
      final GrayU8 g1 = frameNext.createSameShape();
      final GrayU8 g2 = frameNext.createSameShape();
      difference = frameNext.createSameShape();
      binary = frameNext.createSameShape();
      BlurImageOps.gaussian(framePrevious, g1, config.blurSigma, config.blurRadius, null);
      BlurImageOps.gaussian(frameNext, g2, config.blurSigma, config.blurRadius, null);
      PixelMath.diffAbs(g1, g2, difference);
      final CannyEdge<GrayU8, GrayS16> canny = FactoryEdgeDetectors.canny(config.cannyRadius, true, false, GrayU8.class, GrayS16.class);
      canny.process(difference, config.cannyMin, config.cannyMax, binary);
      contours = canny.getContours();
      rectangles.clear();
      if (contours != null) {
         for (EdgeContour contour : contours) {
            final List<EdgeSegment> ex = contour.segments;
            for (EdgeSegment segment : ex) {
               final Rectangle2D_I32 rectangle = new Rectangle2D_I32();
               UtilPolygons2D_I32.bounding(segment.points, rectangle);
               rectangles.add(rectangle);
            }
         }
      }
      double maxW = 0, maxH = 0;
for(Rectangle rec : HelperGeometry.clusterRectangles2D(HelperConvert.rectangles2D(rectangles), 2)) {
         maxW = Math.max(maxW, rec.width);
         maxH = Math.max(maxH, rec.height);
      }
if((maxW > 0 && maxW > (framePrevious.getWidth() * (config.percentage/100d))) || (maxH > 0 && maxH > (framePrevious.getHeight() * (config.percentage/100d)))) {
         motionDetected = true;
      }
   }
}
