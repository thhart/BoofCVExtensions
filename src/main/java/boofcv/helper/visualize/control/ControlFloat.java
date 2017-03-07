package boofcv.helper.visualize.control;

/**
 Created by th on 29.07.16.
 */
@SuppressWarnings("unused")
public class ControlFloat extends ControlNumber<Float> {
   public ControlFloat(String name, float value, float min, float max) {
      super(name, value, min, max, false);
   }

   public Float getValue() {
      return model.getNumber().floatValue();
   }
}
