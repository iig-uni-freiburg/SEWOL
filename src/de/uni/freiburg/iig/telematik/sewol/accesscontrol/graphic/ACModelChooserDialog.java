package de.uni.freiburg.iig.telematik.sewol.accesscontrol.graphic;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SpringLayout;

import de.invation.code.toval.graphic.component.EnumComboBox;
import de.invation.code.toval.graphic.dialog.AbstractDialog;
import de.invation.code.toval.graphic.renderer.AlternatingRowColorListCellRenderer;
import de.invation.code.toval.graphic.util.SpringUtilities;
import de.invation.code.toval.misc.soabase.SOABase;
import de.invation.code.toval.validate.Validate;
import de.uni.freiburg.iig.telematik.sewol.accesscontrol.AbstractACModel;
import de.uni.freiburg.iig.telematik.sewol.accesscontrol.acl.ACLModel;
import de.uni.freiburg.iig.telematik.sewol.accesscontrol.properties.ACModelType;

@SuppressWarnings("rawtypes")
public class ACModelChooserDialog extends AbstractDialog {

	private static final long serialVersionUID = 3658346248655306168L;
	private static final Dimension PREFERRED_SIZE = new Dimension(400,400);
	
	private EnumComboBox<ACModelType> comboModelType;
	private JComboBox comboModel;
	private DefaultComboBoxModel comboModelModel = new DefaultComboBoxModel();
	private Set<AbstractACModel> acModels;
	protected Collection<SOABase> contexts;
	private JButton btnAddModel;
	private JButton btnEditModel;
	private JTextArea areaPreview;

	public ACModelChooserDialog(Window owner, Collection<? extends AbstractACModel> acModels) {
		super(owner);
		Validate.notNull(acModels);
		Validate.noNullElements(acModels);
		this.acModels = new HashSet<>(acModels);
		contexts = new HashSet<>();
		for(AbstractACModel acModel: acModels){
			contexts.add(acModel.getContext());
		}
		setPreferredSize(PREFERRED_SIZE);
	}
	
	public ACModelChooserDialog(Window owner, Collection<? extends AbstractACModel> acModels, Collection<? extends SOABase> contexts) {
		this(owner, acModels);
		Validate.notNull(contexts);
		Validate.notEmpty(contexts);
		Validate.noNullElements(contexts);
		this.contexts = new HashSet<>(contexts);
	}

	@Override
	protected void addComponents() throws Exception {
		mainPanel().setLayout(new BorderLayout());
		
		JPanel topPanel = new JPanel();
		SpringLayout layout = new SpringLayout();
        topPanel.setLayout(layout);
        topPanel.add(new JLabel("Model type:"));
        comboModelType =  new EnumComboBox<ACModelType>(ACModelType.class);
        comboModelType.setSelectedItem(ACModelType.ACL);
        comboModelType.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				if(e.getStateChange() == ItemEvent.SELECTED) {
					updateModelCombo();
					updatePreview();
                }
			}
		});
        topPanel.add(comboModelType);
        topPanel.add(new JLabel("Chosen model:"));
        comboModel = new JComboBox(comboModelModel);
        comboModel.setRenderer(new ACModelCellRenderer());
        comboModel.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				if(e.getStateChange() == ItemEvent.SELECTED) {
					updatePreview();
                }
			}
		});
		topPanel.add(comboModel);
		SpringUtilities.makeCompactGrid(topPanel, 2, 2, 0, 0, 0, 0);
		mainPanel().add(topPanel, BorderLayout.PAGE_START);
		
		areaPreview = new JTextArea();
		mainPanel().add(new JScrollPane(areaPreview), BorderLayout.CENTER);
		
		mainPanel().add(getACButtonPanel(), BorderLayout.PAGE_END);
		
		updateModelCombo();
		updatePreview();
	}
	
	private JPanel getACButtonPanel() {
		JPanel panelButtons = new JPanel();
		BoxLayout l = new BoxLayout(panelButtons, BoxLayout.LINE_AXIS);
		panelButtons.setLayout(l);
		
		panelButtons.add(getButtonAddModel());
		panelButtons.add(getButtonEditModel());
		panelButtons.add(Box.createHorizontalGlue());
		return panelButtons;
	}

	@Override
	protected void setTitle() {
		setTitle("Choose Access control model");
	}

	@Override
	protected void okProcedure() {
		setDialogObject(getSelectedModel());
		super.okProcedure();
	}

	private JButton getButtonAddModel(){
		if(btnAddModel == null){
			btnAddModel = new JButton("Add");
			btnAddModel.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					AbstractACModel newModel = null;
					try{
						switch(comboModelType.getSelectedItem()){
						case ACL:
							newModel = ACModelDialog.showDialog(ACModelChooserDialog.this, "New ACL", ACModelType.ACL, contexts.iterator().next(), contexts);
							break;
						case RBAC:
							newModel = ACModelDialog.showDialog(ACModelChooserDialog.this, "New RBAC-Model", ACModelType.RBAC, contexts.iterator().next(), contexts);
							break;
						}
					} catch(Exception ex){
						JOptionPane.showMessageDialog(ACModelChooserDialog.this, "Cannot launch ACModelDialog: " + ex.getMessage(), "Internal Error", JOptionPane.ERROR_MESSAGE);
						return;
					}
					if(newModel == null)
						return;
					
					if(ensureValidModel(newModel)){
						addNewACModel(newModel);
					}
				}
			});
		}
		return btnAddModel;
	}
	
	private boolean ensureValidModel(AbstractACModel newModel) {
		boolean cont = true;
		while(cont){
			try{
				validateNewModel(newModel);
				cont = false;
			} catch(Exception ex){
				int result = JOptionPane.showConfirmDialog(ACModelChooserDialog.this, "Cannot add new AC model: " + ex.getMessage() + "\nEdit AC model?", "Invalid Parameter", JOptionPane.YES_NO_OPTION);
				if(result != JOptionPane.YES_OPTION){
					return false;
				}
				try {
					ACModelDialog.showDialog(ACModelChooserDialog.this, newModel, contexts);
				} catch (Exception e2) {
					JOptionPane.showMessageDialog(ACModelChooserDialog.this, "Cannot launch ACModelDialog: " + e2.getMessage(), "Internal Error", JOptionPane.ERROR_MESSAGE);
					return false;
				}
			}
		}
		return true;
	}
	
	protected void addNewACModel(AbstractACModel newModel) {
		acModels.add(newModel);
		updateModelCombo();
		comboModel.setSelectedItem(newModel);
		updatePreview();
	}
	
	private JButton getButtonEditModel(){
		if(btnEditModel == null){
			btnEditModel = new JButton("Edit");
			btnEditModel.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					try{
						ACModelDialog.showDialog(ACModelChooserDialog.this, getSelectedModel(), contexts);
					} catch(Exception ex){
						JOptionPane.showMessageDialog(ACModelChooserDialog.this, "Cannot launch ACModelDialog: " + ex.getMessage(), "Internal Error", JOptionPane.ERROR_MESSAGE);
						return;
					}
					if(ensureValidModel(getSelectedModel()))
						updatePreview();
				}
			});
		}
		return btnEditModel;
	}
	
	private AbstractACModel getSelectedModel(){
		Object selectedObject = comboModel.getSelectedItem();
		if(selectedObject == null)
			return null;
		return (AbstractACModel) selectedObject;
	}
	
	private void updateModelCombo() {
		comboModel.removeAllItems();
		for(AbstractACModel model: acModels){
			if(model.getType() == comboModelType.getSelectedItem()){
				comboModelModel.addElement(model);
			}
		}
		comboModel.setEnabled(comboModel.getItemCount() > 0);
	}
	
	private void updatePreview(){
		areaPreview.setText("");
		if(comboModel.getSelectedItem() != null){
			AbstractACModel selectedModel = (AbstractACModel) comboModel.getSelectedItem();
			areaPreview.setText(selectedModel.toString());
		}
	}
	
	protected void validateNewModel(AbstractACModel newModel) throws Exception {}
	
	@Override
	public AbstractACModel getDialogObject() {
		return (AbstractACModel) super.getDialogObject();
	}
	
	private class ACModelCellRenderer extends AlternatingRowColorListCellRenderer {

		private static final long serialVersionUID = 4697232137766666376L;

		@Override
		protected String getText(Object value) {
			return ((AbstractACModel) value).getName();
		}

		@Override
		protected String getTooltip(Object value) {
			return ((AbstractACModel) value).getName();
		}
		
	}

	public static AbstractACModel showDialog(Window owner, Collection<AbstractACModel> acModels) throws Exception{
		ACModelChooserDialog dialog = new ACModelChooserDialog(owner, acModels);
		dialog.setUpGUI();
		return dialog.getDialogObject();
	}
	
	public static AbstractACModel showDialog(Window owner, Collection<AbstractACModel> acModels, Collection<SOABase> contexts) throws Exception{
		ACModelChooserDialog dialog = new ACModelChooserDialog(owner, acModels, contexts);
		dialog.setUpGUI();
		return dialog.getDialogObject();
	}

	public static void main(String[] args) throws Exception{
		SOABase base1 = SOABase.createSOABase("base1", 10, 10, 10);
		ACLModel m1 = new ACLModel("m1", base1);
		ACLModel m2 = new ACLModel("m2", base1);
		List<AbstractACModel> list = new ArrayList<AbstractACModel>();
		list.add(m1);
		list.add(m2);
		ACModelChooserDialog.showDialog(null, list);
	}
}
