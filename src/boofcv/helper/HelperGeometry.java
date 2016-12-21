package boofcv.helper;

import java.util.*;
import java.util.List;
import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Line2D.Double;
import boofcv.helper.HelperConvert;
import georegression.struct.line.LineSegment2D_F32;
import georegression.struct.shapes.Rectangle2D_I32;

/**
 Created by th on 30.07.16.
 */
public class HelperGeometry {
   public static List<Rectangle> clusterRectangles2D(List<Rectangle> recs, final int distance) {
      final List<Rectangle> toRemove = new ArrayList<>();
      final ArrayList<Rectangle> cluster = new ArrayList<>();
      for (Rectangle rec1 : recs) {
         if(toRemove.contains(rec1)) continue;
         cluster.add(rec1);
         for (Rectangle rec2 : recs) {
            if(rec1 != rec2 && findClosest(rec1, rec2) < distance) {
               toRemove.add(rec2);
               rec1.setBounds(rec1.union(rec2));
            }
         }
      }
      return toRemove.size() > 0 ? clusterRectangles2D(cluster, distance) : cluster;
   }

   public static List<Rectangle2D_I32> clusterRectangles(List<Rectangle2D_I32> recs, final int distance) {
      return HelperConvert.rectangles(clusterRectangles2D(HelperConvert.rectangles2D(recs), distance));
   }

   public static Line2D convertToLine(LineSegment2D_F32 f32) {
      return new Double(f32.getA().getX(), f32.getA().getY(), f32.getB().getX(), f32.getB().getY());
   }

   public static double findClosest(Rectangle rec1, Rectangle rec2) {
      double x1, x2, y1, y2;
      double w, h;
      if (rec1.x > rec2.x) {
         x1 = rec2.x; w = rec2.width; x2 = rec1.x;
      } else {
         x1 = rec1.x; w = rec1.width; x2 = rec2.x;
      }
      if (rec1.y > rec2.y) {
         y1 = rec2.y; h = rec2.height; y2 = rec1.y;
      } else {
         y1 = rec1.y; h = rec1.height; y2 = rec2.y;
      }
      double a = Math.max(0, x2 - x1 - w);
      double b = Math.max(0, y2 - y1 - h);
      return Math.sqrt(a*a+b*b);
   }
}

