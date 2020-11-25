package boofcv.helper.fetcher;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.atomic.AtomicInteger;
import java.awt.image.BufferedImage;
import boofcv.helper.HelperIo;
import boofcv.io.image.*;
import boofcv.struct.image.*;

/**
 Created by th on 01.10.16.
 */
public class UrlImageSequence implements SimpleImageSequence {
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

   public void close() {
   }

   private BufferedImage fetch() throws IOException {
      guiImage = HelperIo.readImageFromUrl(this.url);
      frame = new InterleavedU8(guiImage.getWidth(), guiImage.getHeight(), 3);
      ConvertBufferedImage.convertFrom(guiImage, frame, true);
      return guiImage;
   }

   public int getFrameNumber() {
      return integer.getAndIncrement();
   }

   public BufferedImage getGuiImage() {
      return guiImage;
   }

   public int getHeight() {
      return frame.height;
   }

   public ImageBase getImage() {
      return null;
   }

   public ImageType<ImageInterleaved> getImageType() {
      return ImageType.il(3, ImageDataType.U8);
   }

   public int getNextHeight() {
      return height;
   }

   public int getNextWidth() {
      return width;
   }

   public int getWidth() {
      return frame.width;
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

   public void reset() {
   }

   public void setLoop(boolean loop) {
   }
}
