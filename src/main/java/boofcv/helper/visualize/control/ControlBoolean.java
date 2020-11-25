package boofcv.helper.visualize.control;
import java.awt.*;
import javax.swing.*;
import javax.swing.border.TitledBorder;

/**
 Created by th on 28.07.16.
 */
public class ControlBoolean extends Control<Boolean> {
   private JCheckBox valueCheckBox;
   private JPanel panel;

   public ControlBoolean(final String name, boolean status) {
      super(name);
      SwingUtilities.invokeLater(() -> ((TitledBorder)panel.getBorder()).setTitle(name));
      SwingUtilities.invokeLater(() -> {
         valueCheckBox.setSelected(status);
         valueCheckBox.addActionListener(event -> controlListener.fireControlUpdated(ControlBoolean.this));
      });
   }

   public Boolean getValue() {
      return valueCheckBox.isSelected();
   }

   public Component getComponent() {
      return panel;
   }
}
