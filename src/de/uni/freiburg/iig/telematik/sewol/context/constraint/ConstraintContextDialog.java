package de.uni.freiburg.iig.telematik.sewol.context.constraint;

import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import de.uni.freiburg.iig.telematik.sewol.context.process.ProcessContextDialog;
import java.awt.Dimension;
import java.util.List;

public class ConstraintContextDialog extends ProcessContextDialog {

    private JButton btnSetConstraints;

    public ConstraintContextDialog(Window owner) throws Exception {
        super(owner);
    }

    public ConstraintContextDialog(Window owner, ConstraintContext context) throws Exception {
        super(owner, context);
    }

    @Override
    public ConstraintContext getDialogObject() {
        return (ConstraintContext) super.getDialogObject();
    }

    @Override
    protected List<JButton> getButtonsActivity() {
        List<JButton> activityButtons = super.getButtonsActivity();
        activityButtons.add(getButtonSetConstraints());
        return activityButtons;
    }

    private JButton getButtonSetConstraints() {
        if (btnSetConstraints == null) {
            btnSetConstraints = new JButton("Set constraints");
            btnSetConstraints.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if(!getDialogObject().containsActivities()){
                        JOptionPane.showMessageDialog(ConstraintContextDialog.this, "Context does not contain any " + getDialogObject().getActivityDescriptorPlural().toLowerCase(), "Incomplete Definition", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    if(!getDialogObject().containsAttributes()){
                        JOptionPane.showMessageDialog(ConstraintContextDialog.this, "Context does not contain any " + getDialogObject().getObjectDescriptorPlural().toLowerCase(), "Incomplete Definition", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    try {
                        RoutingConstraintsDialog.showDialog(ConstraintContextDialog.this, getDialogObject());
                    } catch (Exception e1) {
                        JOptionPane.showMessageDialog(ConstraintContextDialog.this, "Cannot launch data usage dialog.\nReason: " + e1.getMessage(), "Invalid Parameter", JOptionPane.ERROR_MESSAGE);
                    }
                }
            });
        }
        return btnSetConstraints;
    }
    
    @Override
    public Dimension getPreferredSize(){
        return new Dimension((int) super.getPreferredSize().getWidth(), (int) super.getPreferredSize().getHeight() + 40);
    }

    //------- STARTUP ---------------------------------------------------------------------------------------------------------------
    
    public static ConstraintContext showDialog(Window parentWindow) throws Exception {
        ConstraintContextDialog contextDialog = new ConstraintContextDialog(parentWindow);
        contextDialog.setUpGUI();
        return contextDialog.getDialogObject();
    }

    public static boolean showDialog(Window parentWindow, ConstraintContext context) throws Exception {
        ConstraintContextDialog contextDialog = new ConstraintContextDialog(parentWindow, context);
        contextDialog.setUpGUI();
        return contextDialog.getDialogObject() != null;
    }

    public static void main(String[] args) throws Exception {
        ConstraintContext c = new ConstraintContext("GerdContext");
        c.setActivities(Arrays.asList("act1", "act2"));
        c.setObjects(Arrays.asList("obj1", "obj2"));
        ConstraintContextDialog.showDialog(null, c);
        System.out.println(c);
    }
}
