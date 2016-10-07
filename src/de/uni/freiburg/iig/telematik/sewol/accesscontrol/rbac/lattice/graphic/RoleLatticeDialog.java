package de.uni.freiburg.iig.telematik.sewol.accesscontrol.rbac.lattice.graphic;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.LineBorder;

import de.invation.code.toval.graphic.dialog.AbstractEditCreateDialog;
import de.invation.code.toval.graphic.dialog.ValueEditingDialog;
import de.invation.code.toval.graphic.renderer.AlternatingRowColorListCellRenderer;
import de.invation.code.toval.validate.CompatibilityException;
import de.invation.code.toval.validate.ParameterException;
import de.invation.code.toval.validate.Validate;
import de.uni.freiburg.iig.telematik.sewol.accesscontrol.rbac.lattice.RoleLattice;
import edu.uci.ics.jung.graph.DirectedSparseGraph;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.decorators.ToStringLabeller;
import edu.uci.ics.jung.visualization.renderers.Renderer.VertexLabel.Position;

public class RoleLatticeDialog extends AbstractEditCreateDialog<RoleLattice> implements EdgeAddedListener {

    private static final long serialVersionUID = -5216821409053567193L;

    public static final int PREFERRED_BUTTON_WIDTH = 160;
    public static final int PREFERRED_BUTTON_HEIGHT = JButton.HEIGHT;

    private JPanel controlPanel;

    private JButton btnAddRoles;
    private JButton btnClearRoles;
    private JButton btnAddEdges;
    private JButton btnMoveNodes;

    private DirectedSparseGraph<String, String> graph;
    private RoleGraphLayout layout;
    private VisualizationViewer<String, String> vv;
    private RoleGraphMouse gm;

    private JList roleList;
    private DefaultListModel roleListModel;

    public RoleLatticeDialog(Window owner, Collection<String> roles) throws Exception {
        super(owner, roles);
    }

    public RoleLatticeDialog(Window owner, RoleLattice lattice) throws Exception {
        super(owner, lattice);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected RoleLattice newDialogObject(Object... parameters) {
        Validate.notNull(parameters);
        Validate.noNullElements(parameters);
        Collection<String> roles = null;
        try {
            roles = (Collection<String>) parameters[0];
        } catch (Exception e) {
            throw new ParameterException("Cannot extract roles from parameter list.\nReason: " + e.getMessage());
        }
        return new RoleLattice(roles);
    }

    protected void initialize() {
        roleListModel = new DefaultListModel();
        setupGraph();
    }

    @Override
    public RoleLattice getDialogObject() {
        return (RoleLattice) super.getDialogObject();
    }

    @Override
    protected void addComponents() throws Exception {
        mainPanel().setLayout(new BorderLayout(20, 0));
        mainPanel().add(getControlPanel(), BorderLayout.LINE_START);
        mainPanel().add(getGraphPanel(), BorderLayout.CENTER);
    }

    private JPanel getControlPanel() {
        if (controlPanel == null) {
            controlPanel = new JPanel();
            BoxLayout layout = new BoxLayout(controlPanel, BoxLayout.PAGE_AXIS);
            controlPanel.setLayout(layout);
            controlPanel.setPreferredSize(new Dimension(PREFERRED_BUTTON_WIDTH, 300));

            controlPanel.add(new JLabel("Roles:"));
            JScrollPane scrollPane = new JScrollPane();
            scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
            scrollPane.setViewportView(getRoleList());
            controlPanel.add(scrollPane);
            controlPanel.add(Box.createVerticalStrut(10));
            controlPanel.add(getButtonAddRoles());
            controlPanel.add(getButtonClearRoles());
            controlPanel.add(getButtonAddEdges());
            controlPanel.add(getButtonMoveNodes());
        }
        return controlPanel;
    }

    private JButton getButtonAddRoles() {
        if (btnAddRoles == null) {
            btnAddRoles = new JButton("Add roles");
            btnAddRoles.setMinimumSize(new Dimension(PREFERRED_BUTTON_WIDTH, PREFERRED_BUTTON_HEIGHT));
            btnAddRoles.setMaximumSize(new Dimension(PREFERRED_BUTTON_WIDTH, PREFERRED_BUTTON_HEIGHT));
            btnAddRoles.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    List<String> newRoles = null;
                    try {
                        newRoles = ValueEditingDialog.showDialog(RoleLatticeDialog.this, "Roles", String.class);
                    } catch (Exception e2) {
                        internalException("Cannot launch value chooser dialog dialog.", e2);
                    }
                    if (newRoles != null) {
                        if (getDialogObject() == null) {
                            setDialogObject(new RoleLattice(newRoles));
                        } else {
                            try {
                                getDialogObject().addRoles(newRoles);
                            } catch (Exception e1) {
                                exception("Error on adding role to lattice.", e1);
                            }
                        }
                        updateRoleList();
                        updateGraph();
                    }
                }
            });
        }
        return btnAddRoles;
    }

    private JButton getButtonClearRoles() {
        if (btnClearRoles == null) {
            btnClearRoles = new JButton("Clear roles");
            btnClearRoles.setMinimumSize(new Dimension(PREFERRED_BUTTON_WIDTH, PREFERRED_BUTTON_HEIGHT));
            btnClearRoles.setMaximumSize(new Dimension(PREFERRED_BUTTON_WIDTH, PREFERRED_BUTTON_HEIGHT));
            btnClearRoles.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    setDialogObject(null);
                    updateRoleList();
                    clearGraph();
                }
            });
        }
        return btnClearRoles;
    }

    private JButton getButtonAddEdges() {
        if (btnAddEdges == null) {
            btnAddEdges = new JButton("Add edges");
            btnAddEdges.setMinimumSize(new Dimension(PREFERRED_BUTTON_WIDTH, PREFERRED_BUTTON_HEIGHT));
            btnAddEdges.setMaximumSize(new Dimension(PREFERRED_BUTTON_WIDTH, PREFERRED_BUTTON_HEIGHT));
            btnAddEdges.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    gm.setEditingMode();
                }
            });
        }
        return btnAddEdges;
    }

    private JButton getButtonMoveNodes() {
        if (btnMoveNodes == null) {
            btnMoveNodes = new JButton("Move nodes");
            btnMoveNodes.setMinimumSize(new Dimension(PREFERRED_BUTTON_WIDTH, PREFERRED_BUTTON_HEIGHT));
            btnMoveNodes.setMaximumSize(new Dimension(PREFERRED_BUTTON_WIDTH, PREFERRED_BUTTON_HEIGHT));
            btnMoveNodes.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    gm.setPickingMode();
                }
            });
        }
        return btnMoveNodes;
    }

    private JComponent getGraphPanel() {
        if (vv == null) {
            layout = new RoleGraphLayout(graph);
            layout.setSize(new Dimension(300, 300));
            vv = new VisualizationViewer<String, String>(layout);
            vv.setBorder(new LineBorder(new Color(0, 0, 0)));
            vv.getRenderContext().setVertexFillPaintTransformer(new RoleVertexColorTransformer());
            vv.getRenderContext().setVertexLabelTransformer(new ToStringLabeller<String>());
            vv.getRenderContext().setVertexShapeTransformer(new RoleVertexSizeTransformer());
            vv.getRenderContext().setEdgeLabelTransformer(new RoleEdgeLabeller());
            vv.getRenderer().getVertexLabelRenderer().setPosition(Position.CNTR);
            vv.setPreferredSize(new Dimension(350, 350));
            gm = new RoleGraphMouse(vv.getRenderContext());
            vv.setGraphMouse(gm);
            gm.setPickingMode();
            gm.addEdgeAddedListener(this);

            vv.setFocusable(true);
            vv.addKeyListener(new GraphKeyListener());
        }
        return vv;
    }

    @Override
    protected void setTitle() {
        if (!editMode()) {
            setTitle("Create role lattice");
        } else {
            setTitle("Edit role lattice");
        }
    }

    @Override
    protected boolean validateAndSetFieldValues() throws Exception {
        if (getDialogObject() == null) {
            throw new ParameterException("Incomplete role lattice definition.");
        }
        return true;
    }

    private void setupGraph() throws CompatibilityException {
        graph = new DirectedSparseGraph<String, String>();
        if (getDialogObject() == null) {
            return;
        }
        for (String role : getDialogObject().getRoles()) {
            graph.addVertex(role);
        }
        for (String role : getDialogObject().getRoles()) {
            for (String dominatedRole : getDialogObject().getDominatedRolesFor(role, false)) {
                graph.addEdge(role + "-" + dominatedRole, dominatedRole, role);
            }
        }
    }

    private String getSourceFromEdgeName(String edgeName) {
        return edgeName.substring(0, edgeName.indexOf('-'));
    }

    private String getTargetFromEdgeName(String edgeName) {
        return edgeName.substring(edgeName.indexOf('-') + 1, edgeName.length());
    }

    private JList getRoleList() {
        if (roleList == null) {
            roleList = new JList(roleListModel);
            roleList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            roleList.setCellRenderer(new AlternatingRowColorListCellRenderer());
            roleList.setFixedCellHeight(20);
            roleList.setVisibleRowCount(10);
            roleList.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            roleList.setBorder(null);
            roleList.setMinimumSize(new Dimension(PREFERRED_BUTTON_WIDTH, 100));

            if (getDialogObject() != null) {
                for (String role : getDialogObject().getRoles()) {
                    roleListModel.addElement(role);
                }
            }

            roleList.setFocusable(true);

            roleList.addKeyListener(new KeyListener() {

                @Override
                public void keyTyped(KeyEvent e) {
                }

                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_DELETE || e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
                        if (roleList.getSelectedValues().length > 0) {
                            for (Object selectedRole : roleList.getSelectedValues()) {
                                roleListModel.removeElement(selectedRole);
                                graph.removeVertex((String) selectedRole);
                                try {
                                    getDialogObject().removeRole((String) selectedRole);
                                } catch (Exception e1) {
                                    exception("Error on removing role from lattice.", e1);
                                }
                            }
                            layout.reset();
                            vv.repaint();
                        }
                    }
                }

                @Override
                public void keyReleased(KeyEvent e) {
                }

            });
        }
        return roleList;
    }

    private void updateRoleList() {
        roleListModel.removeAllElements();
        if (getDialogObject() != null) {
            for (String role : getDialogObject().getRoles()) {
                roleListModel.addElement(role);
            }
        }
    }

    private void clearGraph() {
        Set<String> vertices = new HashSet<String>(graph.getVertices());
        for (String vertex : vertices) {
            graph.removeVertex(vertex);
        }
        layout.reset();
        vv.repaint();
    }

    private void updateGraph() {
        if (getDialogObject() != null) {
            for (String role : getDialogObject().getRoles()) {
                graph.addVertex(role);
            }
        }
        layout.reset();
        vv.repaint();
    }

    @Override
    public void edgeAdded(String sourceVertex, String targetVertex) {
        getDialogObject().addRelation(targetVertex, sourceVertex);
    }

    private class GraphKeyListener implements KeyListener {

        @Override
        public void keyTyped(KeyEvent e) {
        }

        @Override
        public void keyReleased(KeyEvent e) {
        }

        @Override
        public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_DELETE) {
                boolean graphChanges = false;
                Set<String> pickedEdges = vv.getPickedEdgeState().getPicked();
                if (!pickedEdges.isEmpty()) {
                    for (String pickedEdge : pickedEdges) {
                        getDialogObject().removeRelation(getTargetFromEdgeName(pickedEdge), getSourceFromEdgeName(pickedEdge));
                        graph.removeEdge(pickedEdge);
                    }
                    graphChanges = true;
                    vv.getPickedEdgeState().clear();
                } else {
                    Set<String> pickedVertices = vv.getPickedVertexState().getPicked();
                    if (!pickedVertices.isEmpty()) {
                        for (String pickedVertex : pickedVertices) {
                            // System.out.println("Remove role " + pickedVertex + " from roles " + lattice.getRoles());
                            getDialogObject().removeRole(pickedVertex);
                            graph.removeVertex(pickedVertex);
                        }
                        graphChanges = true;
                        updateRoleList();
                        vv.getPickedVertexState().clear();
                    }
                }
                if (graphChanges) {
                    layout.reset();
                    vv.repaint();
                }
            }
        }
    }

    public static RoleLattice showDialog(Window owner, Collection<String> roles) throws Exception {
        RoleLatticeDialog roleLatticeDialog = new RoleLatticeDialog(owner, roles);
        roleLatticeDialog.setUpGUI();
        return roleLatticeDialog.getDialogObject();
    }

    public static RoleLattice showDialog(Window owner, RoleLattice roleLattice) throws Exception {
        RoleLatticeDialog roleLatticeDialog = new RoleLatticeDialog(owner, roleLattice);
        roleLatticeDialog.setUpGUI();
        return roleLatticeDialog.getDialogObject();
    }

    @Override
    protected void prepareEditing() throws Exception {
    }

}
