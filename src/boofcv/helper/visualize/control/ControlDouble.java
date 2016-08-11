package boofcv.helper.visualize.control;

/**
 Created by th on 29.07.16.
 */
public class ControlDouble extends ControlNumber<Double> {
   public ControlDouble(String name, double value, double min, double max) {
      super(name, value, min, max, false);
   }

   public Double getValue() {
      return model.getNumber().doubleValue();
   }
}
