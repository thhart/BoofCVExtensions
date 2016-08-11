package boofcv.helper.visualize;

import java.awt.Rectangle;
import org.junit.Assert;

/**
 Created by th on 31.07.16.
 */
public class HelperGeometryTest {
   @org.junit.Test
   public void findClosest() throws Exception {
      Rectangle rec1 = new Rectangle(10,10,10,10);
      Rectangle rec2 = new Rectangle(10,10,5,5);
      Assert.assertEquals(0, HelperGeometry.findClosest(rec1, rec2), 0);
      rec2.setLocation(22,10);
      Assert.assertEquals(2, HelperGeometry.findClosest(rec1, rec2), 0);
      rec2.setLocation(10,22);
      Assert.assertEquals(2, HelperGeometry.findClosest(rec1, rec2), 0);
      rec2.setLocation(4,10);
      Assert.assertEquals(1, HelperGeometry.findClosest(rec1, rec2), 0);
   }

}