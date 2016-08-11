package boofcv.helper.visualize.control;

import java.util.logging.Logger;
import java.awt.Component;

/**
 Created by th on 28.07.16.
 */
public abstract class Control<V> {
// ------------------------------ FIELDS ------------------------------
protected final static Logger logger = Logger.getLogger("Control");
   final String name;

   public ControlListener getControlListener() {
      return controlListener;
   }

   public void setControlListener(ControlListener controlListener) {
      this.controlListener = controlListener;
   }

   protected ControlListener controlListener = control ->
         logger.warning("ControlManager not set, controls will not send its values.");

// --------------------------- CONSTRUCTORS ---------------------------

   protected Control(String name) {this.name = name;}

// -------------------------- OTHER METHODS --------------------------

   public abstract V getValue();
   public abstract Component getComponent();

   public String getName() {
      return name;
   }

   // -------------------------- INNER CLASSES --------------------------

   public interface ControlListener {
      void fireControlUpdated(Control control);
   }
}
