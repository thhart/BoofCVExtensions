package boofcv.helper;
/**
 Created by th on 01.10.16.
 */
public class PathLabel extends boofcv.io.PathLabel {
   public PathLabel(String label, String path) {
      super(label, path);
   }
   public String toString() {
      return label;
   }
}
