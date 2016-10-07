package de.uni.freiburg.iig.telematik.sewol.context.constraint;

import java.awt.BorderLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import de.invation.code.toval.constraint.AbstractConstraint;
import de.invation.code.toval.constraint.graphic.ConstraintDialog;
import de.invation.code.toval.graphic.dialog.AbstractDialog;
import de.invation.code.toval.graphic.dialog.ExceptionDialog;
import de.invation.code.toval.graphic.dialog.ValueChooserDialog;
import de.invation.code.toval.graphic.renderer.AlternatingRowColorListCellRenderer;
import de.invation.code.toval.validate.CompatibilityException;
import de.invation.code.toval.validate.ParameterException;
import java.awt.Dimension;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

public class RoutingConstraintsDialog extends AbstractDialog<ConstraintContext> {

    private static final long serialVersionUID = 9017322681121907900L;

    private static final Dimension MINIMUM_SIZE_LEFT_PANEL = new Dimension(160, 360);
    private static final Dimension MINIMUM_SIZE_RIGHT_PANEL = new Dimension(240, 360);
    private static final Dimension PREFERRED_SIZE = new Dimension(450, 360);
    
    private JList listActivities = null;
    private JList listConstraints = null;

    private final DefaultListModel modelListActivities = new DefaultListModel();
    private final DefaultListModel modelListConstraints = new DefaultListModel();

    private JButton btnAddConstraint = null;
    private JButton btnEditConstraint = null;
    private JButton btnRemoveConstraint = null;

    private final List<AbstractConstraint<?>> constraints = new ArrayList<>();

    public RoutingConstraintsDialog(Window owner, ConstraintContext context) {
        super(owner, ButtonPanelLayout.CENTERED);
        setDialogObject(context);
        setIncludeCancelButton(false);
    }

    @Override
    protected void addComponents() {
        JPanel leftPanel = new JPanel(new BorderLayout());
        JLabel lblActivities = new JLabel("Activities:");
        leftPanel.add(lblActivities, BorderLayout.PAGE_START);
        JScrollPane activitiesScrollPane = new JScrollPane(getListActivities());
        activitiesScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        activitiesScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        leftPanel.add(activitiesScrollPane, BorderLayout.CENTER);
        leftPanel.setMinimumSize(MINIMUM_SIZE_LEFT_PANEL);
        
        JPanel rightPanel = new JPanel(new BorderLayout());
        JLabel lblConstraints = new JLabel("Constraints:");
        rightPanel.add(lblConstraints, BorderLayout.PAGE_START);
        JScrollPane constraintsScrollPane = new JScrollPane(getListConstraints());
        constraintsScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        constraintsScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        rightPanel.add(constraintsScrollPane, BorderLayout.CENTER);
        rightPanel.setMinimumSize(MINIMUM_SIZE_RIGHT_PANEL);
        JPanel buttons = new JPanel();
        BoxLayout layout = new BoxLayout(buttons, BoxLayout.LINE_AXIS);
        buttons.setLayout(layout);
        buttons.add(Box.createHorizontalGlue());
        buttons.add(getButtonAddConstraint());
        buttons.add(getButtonEditConstraint());
        buttons.add(getButtonRemoveConstraint());
        buttons.add(Box.createHorizontalGlue());
        rightPanel.add(buttons, BorderLayout.PAGE_END);
        updateListConstraints();
        
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        splitPane.setDividerLocation(0.6);
        splitPane.setBorder(BorderFactory.createEmptyBorder());
        
        mainPanel().setLayout(new BorderLayout());
        mainPanel().add(splitPane, BorderLayout.CENTER);
    }
    
    @Override
    public Dimension getPreferredSize(){
        return PREFERRED_SIZE;
    }
        
    private JButton getButtonRemoveConstraint(){
        if(btnRemoveConstraint == null){
            btnRemoveConstraint = new JButton("Remove");
            btnRemoveConstraint.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    String activity = listActivities.getSelectedValue().toString();
                    if (listConstraints.getSelectedValue() != null) {
                        try {
                            getDialogObject().removeRoutingConstraint(activity, constraints.get(listConstraints.getSelectedIndex()));
                        } catch (ParameterException e1) {
                            JOptionPane.showMessageDialog(RoutingConstraintsDialog.this, "Cannot remove constraint from context.", "Internal Error", JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                        updateListConstraints();
                    }
                }
            });
        }
        return btnRemoveConstraint;
    }
        
    private JButton getButtonEditConstraint(){
        if(btnEditConstraint == null){
            btnEditConstraint = new JButton("Edit");
            btnEditConstraint.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (listConstraints.getSelectedValue() != null) {
                        try{
                        ConstraintDialog.showDialog(RoutingConstraintsDialog.this, constraints.get(listConstraints.getSelectedIndex()));
                        updateListConstraints();
                        } catch(Exception ex){
                            ExceptionDialog.showException(RoutingConstraintsDialog.this, "Internal Exception", ex, true);
                        }
                    }
                }
            });
        }
        return btnEditConstraint;
    }

    private JButton getButtonAddConstraint(){
        if(btnAddConstraint == null){
            btnAddConstraint = new JButton("Add");
            btnAddConstraint.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    String activity = listActivities.getSelectedValue().toString();
                    Set<String> activityAttributes = null;
                    try {
                        activityAttributes = getDialogObject().getDataUsageFor(activity).keySet();
                        if (activityAttributes.isEmpty()) {
                            JOptionPane.showMessageDialog(RoutingConstraintsDialog.this, "Cannot add constraints to activities without data usage.", "Missing data usage", JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                    } catch (ParameterException ex) {
                        JOptionPane.showMessageDialog(RoutingConstraintsDialog.this, "Cannot extract data usage information for activity \"" + activity + "\".", "Internal Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    List<String> chosenAttributes = null;
                    try {
                        chosenAttributes = ValueChooserDialog.showDialog(RoutingConstraintsDialog.this, "Choose attribute for new constraint", activityAttributes);
                    } catch (Exception e2) {
                        JOptionPane.showMessageDialog(RoutingConstraintsDialog.this, "<html>Cannot launch value chooser dialog dialog.<br>Reason: " + e2.getMessage() + "</html>", "Internal Exception", JOptionPane.ERROR_MESSAGE);
                    }
                    if (chosenAttributes != null && !chosenAttributes.isEmpty()) {
                        AbstractConstraint<?> newConstraint = null;
                        try{
                            newConstraint = ConstraintDialog.showDialog(RoutingConstraintsDialog.this, chosenAttributes.get(0));
                        } catch(Exception ex){
                            ExceptionDialog.showException(RoutingConstraintsDialog.this, "Internal Exception", ex, true);
                            return;
                        }
                        if (newConstraint != null) {
                            try {
                                getDialogObject().addRoutingConstraint(activity, newConstraint);
                                updateListConstraints();
                            } catch (CompatibilityException e1) {
                                JOptionPane.showMessageDialog(RoutingConstraintsDialog.this, e1.getMessage(), "Incompatible routing constraint", JOptionPane.ERROR_MESSAGE);
                            } catch (ParameterException e1) {
                                JOptionPane.showMessageDialog(RoutingConstraintsDialog.this, e1.getMessage(), "Invalid Argument", JOptionPane.ERROR_MESSAGE);
                            }
                        }
                    }

                }
            });
        }
        return btnAddConstraint;
    }

    private JList getListActivities() {
        if (listActivities == null) {
            listActivities = new JList(modelListActivities);
            listActivities.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            listActivities.setCellRenderer(new AlternatingRowColorListCellRenderer());
            listActivities.setFixedCellHeight(20);
            listActivities.setVisibleRowCount(10);
            listActivities.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            listActivities.setBorder(null);

            for (String activity : getDialogObject().getActivities()) {
                modelListActivities.addElement(activity);
            }

            listActivities.setSelectedIndex(0);

            listActivities.addListSelectionListener(new ListSelectionListener() {
                @Override
                public void valueChanged(ListSelectionEvent e) {
                    if ((e.getValueIsAdjusting() == false) && (listActivities.getSelectedValue() != null)) {
                        updateListConstraints();
                    }
                }
            }
            );
        }
        return listActivities;
    }

    private JList getListConstraints() {
        if (listConstraints == null) {
            listConstraints = new JList(modelListConstraints);
            listConstraints.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            listConstraints.setCellRenderer(new AlternatingRowColorListCellRenderer());
            listConstraints.setFixedCellHeight(20);
            listConstraints.setVisibleRowCount(10);
            listConstraints.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            listConstraints.setBorder(null);
        }
        return listConstraints;
    }

    private void updateListConstraints() {
        constraints.clear();
        modelListConstraints.clear();
        String activity = listActivities.getSelectedValue().toString();

        if (activity != null) {
            if (getDialogObject().hasRoutingConstraints(activity)) {
                constraints.addAll(getDialogObject().getRoutingConstraints(activity));
            }
        }

        for (AbstractConstraint<?> constraint : constraints) {
            modelListConstraints.addElement(constraint.toString());
        }
        if (!modelListConstraints.isEmpty()) {
            listConstraints.setSelectedIndex(0);
        }
    }
    
    @Override
    protected void setTitle() {
       setTitle("Set Activity Data Usage");
    }

    public static boolean showDialog(Window owner, ConstraintContext context) throws Exception {
        RoutingConstraintsDialog dialog = new RoutingConstraintsDialog(owner, context);
        dialog.setUpGUI();
        return dialog.getDialogObject() != null;
    }

}
