package boofcv.helper.visualize.control;
import java.text.NumberFormat;
import java.util.Hashtable;
import java.awt.*;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.*;

/**
 Created by th on 28.07.16.
 */
abstract class ControlNumber<V> extends Control<V> {
	private JPanel panel;
	private JSpinner spinner;
	private JToggleButton toggleButton;
	private JSlider slider;
	private JPanel panelCards;
	final SpinnerNumberModel model;
	private final NumberFormat format = NumberFormat.getNumberInstance();
	private final Hashtable<Integer, JLabel> labelTable = new Hashtable<>();
	private final double multiplier;

	ControlNumber(final String name, double value, double min, double max, boolean integer, double step) {
		super(name);
		multiplier = integer ? 1 : 1000;
		format.setMaximumFractionDigits(integer ? 0 : 2);
		format.setMinimumFractionDigits(integer ? 0 : 2);
		model = new SpinnerNumberModel(value, min, max, integer ? Math.max(1, (int)step) : step);
		SwingUtilities.invokeLater(() -> {
			spinner.setModel(model);
			spinner.addChangeListener(change -> {
				if (!toggleButton.isSelected()) {
					controlListener.fireControlUpdated(ControlNumber.this);
					slider.setValue((int)(model.getNumber().intValue() * multiplier));
				}
				updateTitle();
			});
			slider.setMaximum((int)(max * multiplier));
			slider.setMinimum((int)(min * multiplier));
			slider.setValue((int)(value * multiplier));
			slider.setSnapToTicks(true);
			slider.setPaintTrack(true);
			slider.setPaintLabels(true);
			labelTable.put(slider.getMinimum(), new JLabel(format.format(min)));
			labelTable.put(slider.getMaximum(), new JLabel(format.format(max)));
			updateTitle();
			updateLabels();
			slider.addChangeListener(new ChangeListener() {
				public void stateChanged(ChangeEvent e) {
					if (toggleButton.isSelected()) {
						model.setValue(slider.getValue() / multiplier);
						controlListener.fireControlUpdated(ControlNumber.this);
					}
					updateTitle();
				}
			});
		});
		toggleButton.addActionListener(e -> showCard());
		showCard();
	}

	private void updateTitle() {
		SwingUtilities.invokeLater(() -> {
			((TitledBorder)panel.getBorder()).setTitle(name + ": " + format.format(getValue()));
			panel.repaint();
		});
	}

	private void updateLabels() {
		SwingUtilities.invokeLater(() -> {
			slider.setLabelTable(labelTable);
			slider.repaint();
		});
	}

	private void showCard() {
		SwingUtilities.invokeLater(() -> {
			if (toggleButton.isSelected()) {
				((CardLayout)panelCards.getLayout()).show(panelCards, "Slider");
			} else {
				((CardLayout)panelCards.getLayout()).show(panelCards, "Spinner");
			}
		});
	}

	public Component getComponent() {
		return panel;
	}
}
