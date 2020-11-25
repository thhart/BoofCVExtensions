package boofcv.helper.visualize.control;
/**
 Created by th on 29.07.16.
 */
@SuppressWarnings("unused")
public class ControlFloat extends ControlNumber<Float> {
   public ControlFloat(String name, float value, float min, float max, double step) {
      super(name, value, min, max, false, step);
   }
public Float getValue() {
      return model.getNumber().floatValue();
   }
}
