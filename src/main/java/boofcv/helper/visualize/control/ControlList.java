package boofcv.helper.visualize.control;
import java.awt.*;
import javax.swing.*;
import javax.swing.border.TitledBorder;

/**
 Created by th on 28.07.16.
 */
public class ControlList<T> extends Control<T> {
   private final DefaultComboBoxModel<T> model;
   private JPanel panel;
   private JComboBox<T> comboBox;

   //private JLabel label;
   public ControlList(final String name, T... list) {
      super(name);
      //SwingUtilities.invokeLater(() -> label.setText(name + ": "));
      SwingUtilities.invokeLater(() -> comboBox.addActionListener(event -> controlListener.fireControlUpdated(ControlList.this)));
      model = new DefaultComboBoxModel<>();
      for (T t : list) {
         model.addElement(t);
      }
      comboBox.setModel(model);
      SwingUtilities.invokeLater(() -> {
         ((TitledBorder)panel.getBorder()).setTitle(name);
         panel.repaint();
      });
   }

   public T getValue() {
      return (T)model.getSelectedItem();
   }

   public Component getComponent() {
      return panel;
   }
}
