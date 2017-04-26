package boofcv.helper.visualize.control;

import java.awt.Component;
import org.slf4j.*;

/**
 Created by th on 28.07.16.
 */
@SuppressWarnings("unused")
public abstract class Control<V> {
// ------------------------------ FIELDS ------------------------------
   protected final static Logger logger = LoggerFactory.getLogger(Control.class);

   final String name;

   public ControlListener getControlListener() {
      return controlListener;
   }

   public void setControlListener(ControlListener controlListener) {
      this.controlListener = controlListener;
   }

   ControlListener controlListener = controls ->
         logger.warn("ControlManager not set, controls will not send its values.");

// --------------------------- CONSTRUCTORS ---------------------------

   Control(String name) {this.name = name;}

// -------------------------- OTHER METHODS --------------------------

   public abstract V getValue();

   public boolean getAsBoolean() {
      return String.valueOf(getValue()).toLowerCase().matches("(true|1|on)");
   }

   public int getAsInteger() {
      return (int) Double.parseDouble(String.valueOf(getValue()));
   }

   public float getAsFloat() {
      return Float.parseFloat(String.valueOf(getValue()));
   }

   public double getAsDouble() {
      return Double.parseDouble(String.valueOf(getValue()));
   }

   public abstract Component getComponent();

   public String getName() {
      return name;
   }

   // -------------------------- INNER CLASSES --------------------------

   public interface ControlListener {
      void fireControlUpdated(Control... controls);
   }
}
