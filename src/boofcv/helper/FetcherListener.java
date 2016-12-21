package boofcv.helper;

import java.awt.image.BufferedImage;
import boofcv.struct.image.GrayU8;

/**
 Created by th on 01.10.16.
 */
public interface FetcherListener {
   void updateImage(GrayU8 grayU8, BufferedImage image);
}
