package boofcv.helper;

import java.io.*;
import java.net.URL;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

/**
 Created by th on 11.08.16.
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class HelperIo {
   public static ByteArrayOutputStream readDataFromUrl(URL url) throws IOException {
      ByteArrayOutputStream bis = new ByteArrayOutputStream();
      InputStream is = null;
      try {
         is = url.openStream();
         byte[] bytebuff = new byte[4096];
         int n;
         while ((n = is.read(bytebuff)) > 0) {
            bis.write(bytebuff, 0, n);
         }
      } finally {
         try {
            if (is != null) {
               is.close();
            }
         } catch (Exception ignored) {
         }
      }
      return bis;
   }

   public static BufferedImage readImageFromUrl(URL url) throws IOException {
      return ImageIO.read(new ByteArrayInputStream(readDataFromUrl(url).toByteArray()));
   }
}
