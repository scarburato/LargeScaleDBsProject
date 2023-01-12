package londonSafeTravel.client.gui;

import londonSafeTravel.schema.graph.Disruption;

import javax.swing.*;
import java.awt.event.*;

public class DisruptionDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JLabel disruptionID;
    private JFormattedTextField severityTextField1;
    private JButton buttonCancel;

    Disruption disruption;

    public DisruptionDialog(Disruption d) {
        disruption = d;

        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        disruptionID.setText(disruption.id);
        severityTextField1.setText(disruption.severity);

        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onOK();
            }
        });

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }

    private void onOK() {
        // add your code here
        dispose();
    }

}