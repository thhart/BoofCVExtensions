package boofcv.helper.visualize.control;
/**
 Created by th on 29.07.16.
 */
public class ControlInteger extends ControlNumber<Integer> {
   public ControlInteger(String name, double value, double min, double max, double step) {
      super(name, value, min, max, true, step);
   }
public Integer getValue() {
      return model.getNumber().intValue();
   }
}
