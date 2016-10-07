package de.uni.freiburg.iig.telematik.sewol.accesscontrol.rbac.lattice.graphic;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import de.invation.code.toval.graphic.dialog.AbstractDialog;
import de.invation.code.toval.graphic.dialog.ValueChooserDialog;
import de.invation.code.toval.misc.ArrayUtils;
import de.invation.code.toval.misc.soabase.SOABase;
import de.invation.code.toval.validate.ParameterException;
import de.invation.code.toval.validate.ParameterException.ErrorCode;
import de.invation.code.toval.validate.Validate;
import de.uni.freiburg.iig.telematik.sewol.accesscontrol.rbac.RBACModel;
import de.uni.freiburg.iig.telematik.sewol.accesscontrol.rbac.lattice.RoleLattice;

public class RoleMembershipDialog extends AbstractDialog {

	private static final long serialVersionUID = 9017322681121907900L;
	
	private JList subjectList;
	private JList roleList;
	private DefaultListModel roleListModel = new DefaultListModel();
	
	
	private JPanel leftPanel;
	private JPanel rightPanel;
	
	private JButton btnAddDataUsage = null;
	private JButton btnRemoveDataUsage = null;
	
	public RoleMembershipDialog(Window owner, RBACModel rbacModel) throws Exception {
		super(owner, ButtonPanelLayout.CENTERED);
		Validate.notNull(rbacModel);
		if(rbacModel.getRoles().isEmpty()){
			throw new ParameterException(ErrorCode.EMPTY, "RBAC model does not contain any roles");
		}
		setDialogObject(rbacModel);
		setOKButtonText("Done");
		setIncludeCancelButton(false);
		setPreferredSize(new Dimension(400,300));
	}
	
	@Override
	protected void setTitle() {
		setTitle("Edit role membership");
	}
	
	@Override
	protected void addComponents() throws Exception {
		mainPanel().setLayout(new BorderLayout());
		
		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, getLeftPanel(), getRightPanel());
		splitPane.setBorder(BorderFactory.createEmptyBorder());
		mainPanel().add(splitPane, BorderLayout.CENTER);
	}
	
	private JPanel getLeftPanel(){
		if(leftPanel == null){
			leftPanel = new JPanel(new BorderLayout());
			leftPanel.add(new JLabel("Subjects"), BorderLayout.PAGE_START);
			JScrollPane scrollPane = new JScrollPane();
			scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
			leftPanel.add(scrollPane, BorderLayout.CENTER);
			scrollPane.setViewportView(getSubjectList());
			scrollPane.setMinimumSize(new Dimension(150, 300));
			scrollPane.setBorder(BorderFactory.createEmptyBorder());
		}
		return leftPanel;
	}
	
	private JPanel getRightPanel(){
		if(rightPanel == null){
			rightPanel = new JPanel(new BorderLayout());
			rightPanel.add(new JLabel("Assigned roles:"), BorderLayout.PAGE_START);
			JScrollPane scrollPane = new JScrollPane();
			scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
			rightPanel.add(scrollPane, BorderLayout.CENTER);
			scrollPane.setViewportView(getRoleList());
			scrollPane.setMinimumSize(new Dimension(150, 300));
			scrollPane.setBorder(BorderFactory.createEmptyBorder());
			
			JPanel buttons = new JPanel();
			BoxLayout layout = new BoxLayout(buttons, BoxLayout.LINE_AXIS);
			buttons.setLayout(layout);
			buttons.add(getAddDataUsageButton());
			buttons.add(getRemoveDataUsageButton());
			buttons.add(Box.createHorizontalGlue());
			rightPanel.add(buttons, BorderLayout.PAGE_END);
		}
		return rightPanel;
	}

	private JButton getAddDataUsageButton(){
		if(btnAddDataUsage == null){
			btnAddDataUsage = new JButton("Add");
			btnAddDataUsage.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					List<String> roles = null;
					try {
						roles = ValueChooserDialog.showDialog(RoleMembershipDialog.this, "Add new role membership", getDialogObject().getRoles(), ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
					} catch (Exception e2) {
						JOptionPane.showMessageDialog(RoleMembershipDialog.this, "<html>Cannot launch value chooser dialog<br>Reason: " + e2.getMessage() + "</html>", "Internal Exception", JOptionPane.ERROR_MESSAGE);
					}
					if(roles != null && !roles.isEmpty()){
						try {
							getDialogObject().addRoleMembership(subjectList.getSelectedValue().toString(), roles);
						} catch (Exception e1) {
							JOptionPane.showMessageDialog(RoleMembershipDialog.this, "Cannot add role membership:\n"+e1.getMessage(), "Internal Error", JOptionPane.ERROR_MESSAGE);
						}
						updateRoleList();
					}
				}
			});
		}
		return btnAddDataUsage;
	}
	
	private JButton getRemoveDataUsageButton(){
		if(btnRemoveDataUsage == null){
			btnRemoveDataUsage = new JButton("Remove");
			btnRemoveDataUsage.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if(roleList.getSelectedValues().length > 0){
						removeRoleMembership(ArrayUtils.toStringList(roleList.getSelectedValues()));
					}
				}
			});
		}
		return btnRemoveDataUsage;
	}
	
	@Override
	public RBACModel getDialogObject() {
		return (RBACModel) super.getDialogObject();
	}
	
	private void removeRoleMembership(Collection<String> roles){
		try {
			getDialogObject().removeRoleMembership(subjectList.getSelectedValue().toString(), roles);
		} catch (Exception e) {
			JOptionPane.showMessageDialog(RoleMembershipDialog.this, "Cannot remove role membership:\n"+e.getMessage(), "Internal Error", JOptionPane.ERROR_MESSAGE);
		}
		updateRoleList();
	}
	
	private JList getSubjectList(){
		if(subjectList == null){
			subjectList = new JList();
			subjectList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			subjectList.setFixedCellHeight(20);
			subjectList.setVisibleRowCount(10);
			subjectList.setBorder(null);
			
			DefaultListModel listModel = new DefaultListModel();
			for(String activity: getDialogObject().getContext().getSubjects())
				listModel.addElement(activity);
			subjectList.setModel(listModel);
			
			subjectList.setSelectedIndex(0);
			
			subjectList.addListSelectionListener(
	        		new ListSelectionListener(){
	        			public void valueChanged(ListSelectionEvent e) {
	        			    if ((e.getValueIsAdjusting() == false) && (subjectList.getSelectedValue() != null)) {
	        			    	updateRoleList();
	        			    }
	        			}
	        		}
	        );
		}
		return subjectList;
	}
	
	private JList getRoleList(){
		if(roleList == null){
			roleList = new JList(roleListModel);
			roleList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
			roleList.setFixedCellHeight(20);
			roleList.setVisibleRowCount(10);
			roleList.setBorder(null);
			
			roleList.setSelectedIndex(0);
			
			roleList.addKeyListener(new KeyListener() {
				
				@Override
				public void keyTyped(KeyEvent e) {}
				
				@Override
				public void keyReleased(KeyEvent e) {
					if(e.getKeyCode() == KeyEvent.VK_DELETE || e.getKeyCode() == KeyEvent.VK_BACK_SPACE){
						if(roleList.getSelectedValues().length > 0){
							removeRoleMembership(ArrayUtils.toStringList(roleList.getSelectedValues()));
						}
					}
				}
				
				@Override
				public void keyPressed(KeyEvent e) {}
			});
		}
		return roleList;
	}
	
	private void updateRoleList(){
		if(subjectList.getSelectedValue() != null){
			roleListModel.clear();
			try {
				for(String role: getDialogObject().getRolesFor(subjectList.getSelectedValue().toString(), false)){
					roleListModel.addElement(role);
				}
			} catch (Exception e) {
				JOptionPane.showMessageDialog(RoleMembershipDialog.this, "Cannot extract roles:\n"+e.getMessage(), "Internal Error", JOptionPane.ERROR_MESSAGE);
			}
		}
	}
	
	@Override
	protected void cancelProcedure(){
		dispose();
	}
	
	public static void showDialog(Window owner, RBACModel rbacModel) throws Exception{
		RoleMembershipDialog dialog = new RoleMembershipDialog(owner, rbacModel);
		dialog.setUpGUI();
		dialog.getDialogObject();
	}


	public static void main(String[] args) throws Exception {
		SOABase context = new SOABase("c1");
		context.setSubjects(Arrays.asList("U1","U2","U3","U4","U5","U6","U7","U8","U9","U10","U11","U12","U13","U14"));
		context.setActivities(Arrays.asList("T1","T2","T3","T4","T5"));
		
		RoleLattice l = new RoleLattice(Arrays.asList("role0", "role1", "role2", "role3"));
		l.addRelation("role0", "role1");
		l.addRelation("role0", "role2");
		l.addRelation("role1", "role3");
		l.addRelation("role2", "role3");
		RBACModel rbac = new RBACModel("rbac1", context, l);
		
		
		rbac.setRoleMembership("role0", Arrays.asList("U8"));
		rbac.setRoleMembership("role1", Arrays.asList("U1","U3"));
		rbac.setRoleMembership("role2", Arrays.asList("U5","U7"));
		rbac.setRoleMembership("role3", Arrays.asList("U3","U4"));
		System.out.println("roles for user U1: " + rbac.getRolesFor("U1", true) + "(with rights propagation)");
		
		rbac.setActivityPermission("role0", new HashSet<String>(Arrays.asList("T4")));
		rbac.setActivityPermission("role1", new HashSet<String>(Arrays.asList("T2")));
		rbac.setActivityPermission("role2", new HashSet<String>(Arrays.asList("T3")));
		rbac.setActivityPermission("role3", new HashSet<String>(Arrays.asList("T1","T5")));
		
		RoleMembershipDialog.showDialog(null, rbac);
		System.out.println(rbac);
	}
}
