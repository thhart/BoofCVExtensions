package boofcv.helper.fetcher;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.awt.image.BufferedImage;
import boofcv.helper.*;
import boofcv.io.MediaManager;
import boofcv.io.image.SimpleImageSequence;
import boofcv.io.wrapper.DefaultMediaManager;
import boofcv.io.wrapper.images.MjpegStreamSequence;
import boofcv.struct.image.*;
/**
 Created by th on 01.10.16.
 */
public class ScheduledFetcher implements Runnable {
   private final FetcherListener fetcherListener;
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
public ScheduledFetcher(FetcherListener fetcherListener) {
      this.fetcherListener = fetcherListener;
      reference = new WeakReference<>(this); //FIXME: check self reference
   }
public void run() {
      while (reference.get() != null) {
         try {
            Thread.sleep(1000);
            if (active && source != null && source.length() > 0) {
               InputStream stream = null;
               try {
                  changePending = false;
                  SimpleImageSequence sequence = null;
                  long waitMax = System.currentTimeMillis();
                  if (source.startsWith("http")) {
                     if (source.endsWith("mjpeg")) {
                        final URL urlx = new URL(source);
                        stream = urlx.openStream();
                        sequence = new MjpegStreamSequence(stream, ImageType.il(3, ImageDataType.U8));
                     } else {
                        sequence = new UrlImageSequence(source);
                     }
                     waitMax += 1000;
                  } else {
                     MediaManager media = DefaultMediaManager.INSTANCE;
                     sequence = media.openVideo(source, sequence.getImageType());
                  }
                  do {
                     while (sequence.hasNext() && !changePending) {
                        fetcherListener.updateImage(HelperConvert.convertToGray((InterleavedU8)sequence.next()), (BufferedImage)sequence.getGuiImage());
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
