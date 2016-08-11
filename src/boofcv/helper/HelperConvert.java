package boofcv.helper;

import java.util.*;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import boofcv.core.image.ConvertImage;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.image.*;
import georegression.struct.shapes.Rectangle2D_I32;

/**
 Created by th on 28.07.16.
 */
public class HelperConvert {
   public static GrayU8 convertToGray(InterleavedU8 image) {
      return ConvertImage.average(image, new GrayU8(image.width, image.height));
   }

   public static BufferedImage convertToBufferedGray(ImageBase image) {
      if (image instanceof GrayU8) {
         return ConvertBufferedImage.convertTo((GrayU8) image, null);
      }
      if (image instanceof GrayF32) {
         return ConvertBufferedImage.convertTo((GrayF32) image, null);
      }
      if (image instanceof GrayI16) {
         return ConvertBufferedImage.convertTo((GrayI16) image, null);
      }
      final BufferedImage ix = new BufferedImage(320, 200, BufferedImage.TYPE_BYTE_GRAY);
      ix.getGraphics().drawString("Image type not supported: " + image.getClass().getSimpleName(), 5, 24);
      return ix;
   }

   public static List<Rectangle> rectangles2D(List<Rectangle2D_I32> list) {
      final ArrayList<Rectangle> recs = new ArrayList<>();
      for (Rectangle2D_I32 i32 : list) {
         recs.add(new Rectangle(i32.x0, i32.y0, i32.getWidth(), i32.getHeight()));
      }
      return recs;
   }

   public static List<Rectangle2D_I32> rectangles(List<Rectangle> list) {
      final ArrayList<Rectangle2D_I32> recs = new ArrayList<>();
      for (Rectangle i32 : list) {
         recs.add(new Rectangle2D_I32(i32.x, i32.y, i32.x + i32.width, i32.y + i32.height));
      }
      return recs;
   }

   public static GrayU8 convertToBufferedGray(BufferedImage read) {
      return ConvertBufferedImage.extractGrayU8(read);
   }
}
