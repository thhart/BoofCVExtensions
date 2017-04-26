package boofcv.helper.visualize;

import java.lang.annotation.*;

/**
 Created by th on 19.04.17.
 */
@Target(value = ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Variable {
   public double min() default Double.MIN_VALUE;
   public double max() default Double.MAX_VALUE;
   public double precision() default 1D;
   public TYPE type() default TYPE.Integer;
   public boolean optimize() default true;

   enum TYPE {
      Float, Integer, Double, Boolean
   }
}
