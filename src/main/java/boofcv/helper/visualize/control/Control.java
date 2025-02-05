package boofcv.helper.visualize.control;

import boofcv.helper.visualize.BoofCvDevPanel;
import org.apache.logging.log4j.LogManager;

import java.awt.*;

/**
 Created by th on 28.07.16.
 */
@SuppressWarnings("unused")
public abstract class Control<V> {
	protected final static org.apache.logging.log4j.Logger logger = LogManager.getLogger(BoofCvDevPanel.class);
	final String name;
	ControlListener controlListener = controls ->
			logger.warn("ControlManager not set, controls will not send its values.");

	Control(String name) {this.name = name;}

	public boolean getAsBoolean() {
		return String.valueOf(getValue()).toLowerCase().matches("(true|1|on)");
	}

	public double getAsDouble() {
		return Double.parseDouble(String.valueOf(getValue()));
	}

	public float getAsFloat() {
		return Float.parseFloat(String.valueOf(getValue()));
	}

	public int getAsInteger() {
		return (int)Double.parseDouble(String.valueOf(getValue()));
	}

	public abstract Component getComponent();

	public ControlListener getControlListener() {
		return controlListener;
	}

	public void setControlListener(ControlListener controlListener) {
		this.controlListener = controlListener;
	}

	public String getName() {
		return name;
	}

	public abstract V getValue();

	public interface ControlListener {
		void fireControlUpdated(Control<?>... controls);
	}
}
