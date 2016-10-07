package de.uni.freiburg.iig.telematik.sewol.context.process;

import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import de.invation.code.toval.misc.soabase.SOABaseDialog;
import de.invation.code.toval.validate.ParameterException;
import de.uni.freiburg.iig.telematik.sewol.accesscontrol.AbstractACModel;
import de.uni.freiburg.iig.telematik.sewol.accesscontrol.graphic.ACModelDialog;
import de.uni.freiburg.iig.telematik.sewol.context.du.DataUsageDialog;
import java.awt.Dimension;
import java.util.List;
import javax.swing.JComponent;

public class ProcessContextDialog extends SOABaseDialog {

    private static final long serialVersionUID = 1356633338243797124L;
    
    public static final Dimension PREFERRED_DIALOG_SIZE = new Dimension(499, 400);

    private JButton btnSetACModel;
    private JButton btnSetDataUsage;

    public ProcessContextDialog(Window owner) throws Exception {
        super(owner);
    }

    public ProcessContextDialog(Window owner, ProcessContext context) throws Exception {
        super(owner, context);
    }

    @Override
    public ProcessContext getDialogObject() {
        return (ProcessContext) super.getDialogObject();
    }

    @Override
    protected List<JButton> getButtonsActivity() {
        List<JButton> activityButtons = super.getButtonsActivity();
        activityButtons.add(getButtonSetDataUsage());
        return activityButtons;
    }
    
    @Override
    public Dimension getPreferredSize(){
        return PREFERRED_DIALOG_SIZE;
    }

    private JButton getButtonSetDataUsage() {
        if (btnSetDataUsage == null) {
            btnSetDataUsage = new JButton("Edit data usage");
            btnSetDataUsage.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    try {
                        DataUsageDialog.showDialog(ProcessContextDialog.this, getDialogObject());
                    } catch (Exception e1) {
                        JOptionPane.showMessageDialog(ProcessContextDialog.this, "Cannot launch data usage dialog.\nReason: " + e1.getMessage(), "Invalid Parameter", JOptionPane.ERROR_MESSAGE);
                    }
                }
            });
        }
        return btnSetDataUsage;
    }

    @Override
    protected JComponent getCustomComponent() {
        return getACModelPanel();
    }
    

    private JPanel getACModelPanel() {
        JPanel acModelPanel = new JPanel();
        BoxLayout layout = new BoxLayout(acModelPanel, BoxLayout.LINE_AXIS);
        acModelPanel.setLayout(layout);

        acModelPanel.add(new JLabel("Access Control Model:"));
        JTextField txtACModelName = new JTextField();
        txtACModelName.setEditable(false);
        txtACModelName.setColumns(10);
        acModelPanel.add(txtACModelName);
        acModelPanel.add(getSetACModelButton());
        acModelPanel.add(Box.createGlue());
        return acModelPanel;
    }

    private JButton getSetACModelButton() {
        if (btnSetACModel == null) {
            btnSetACModel = new JButton("Set/Edit");
            btnSetACModel.setEnabled(false);
            btnSetACModel.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    try {
                        AbstractACModel<?> acModel = getDialogObject().getACModel();
                        if (acModel != null) {
                            try {
                                ACModelDialog.showDialog(ProcessContextDialog.this, acModel);
                            } catch (Exception e1) {
                                JOptionPane.showMessageDialog(ProcessContextDialog.this, "Cannot launch access control model dialog.\nReason:" + e1.getMessage(), "Invalid Parameter", JOptionPane.ERROR_MESSAGE);
                            }
                        } else {
							// ask for model type | provide list with possible acModels?
                            //						-> new constructor with acModels parameter
                            // Set acmodel of context
                        }
                    } catch (ParameterException e1) {
                        JOptionPane.showMessageDialog(ProcessContextDialog.this, e1.getMessage(), "Cannot set access control model.", JOptionPane.ERROR_MESSAGE);
                    }
                }
            });
            btnSetACModel.setBounds(342, 371, 98, 29);
        }
        return btnSetACModel;
    }

	//------- STARTUP ---------------------------------------------------------------------------------------------------------------
    public static ProcessContext showDialog(Window parentWindow) throws Exception {
        ProcessContextDialog contextDialog = new ProcessContextDialog(parentWindow);
        contextDialog.setUpGUI();
        return contextDialog.getDialogObject();
    }

    public static boolean showDialog(Window parentWindow, ProcessContext context) throws Exception {
        ProcessContextDialog contextDialog = new ProcessContextDialog(parentWindow, context);
        contextDialog.setUpGUI();
        return contextDialog.getDialogObject() != null;
    }

    public static void main(String[] args) throws Exception {
        ProcessContext c = new ProcessContext("GerdContext");
        c.setActivities(Arrays.asList("act1", "act2"));
        ProcessContextDialog.showDialog(null, c);
        System.out.println(c);
    }
}
