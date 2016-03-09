/*******************************************************************************
 * µ² - Micro-Agent Platform, core of the Otago Agent Platform (OPAL),
 * developed at the Information Science Department, 
 * University of Otago, Dunedin, New Zealand.
 * 
 * This file is part of the aforementioned software.
 * 
 * µ² is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * µ² is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with the Micro-Agents Framework.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * PlatformInspectorForm.java
 *
 * Created on 28/04/2011, 4:53:51 PM
 */

package org.nzdis.micro.inspector;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Font;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.Rectangle;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Document;
import javax.swing.text.Highlighter;
import javax.swing.text.JTextComponent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import org.nzdis.micro.Agent;
import org.nzdis.micro.AbstractAgent;
import org.nzdis.micro.MTConnector;
import org.nzdis.micro.Role;
import org.nzdis.micro.SystemAgentLoader;
import org.nzdis.micro.bootloader.MicroBootProperties;
import org.nzdis.micro.messaging.MTRuntime;
import org.nzdis.micro.messaging.processor.AbstractMicroFiber;
import org.nzdis.micro.util.StoppableRunnable;

/**
 * This class is the Gui for the PlatformInspector. Upon instantiation 
 * it registers a Systray icon, which opens the Inspector upon doubleclick.
 * 
 * @author <a href="cfrantz@infoscience.otago.ac.nz">Christopher Frantz</a>
 * @version $Revision: 1.0 $ $Date: 2011/09/07 00:00:00 $
 *
 */
public class PlatformInspectorGui extends javax.swing.JFrame {

    private static PlatformInspectorGui instance = null;
    DefaultMutableTreeNode root = null;
    public static final String systrayIconFileName = "SystrayLogo.png";
    public static final String treeBranchIconFileName = "Agent.png";
    public static final String treeLeafIconFileName = "leafIcon.png";
    private final static String ROOT_LABEL = "Platform";
    private static String TREE_ROOT_LABEL = ROOT_LABEL;
    private static final String PLATFORM_INSPECTOR_PREFIX = "PlatformInspector_";
    private static final String DETAIL_VIEW_LABEL_TEXT = "Inspecting ";
    private static final String COLLECTIVE_VIEW_TEXT = "Collective Agent View";
    private static final String DEFAULT_SEARCH_KEY_NAME = "Search";
    
    private static boolean debug = false;
    
    /** TreeSelectionListener (once initialized) for manual calls to valueChanged() method. */
    TreeSelectionListener treSelectionListener = null;
    
    /** temporary information on last chosen tree selection event (to manually call that if GUI is closed and reopened */
    TreeSelectionEvent oldTse = null;
	
    /** Creates new form PlatformInspectorForm */
    private PlatformInspectorGui() {
        if(instance == null){
            initComponents();
            tarDetails.setFont(new Font(Font.DIALOG, Font.PLAIN, 12));
            instance = this;
            setupSystray();
            if(readIcon(treeLeafIconFileName) != null){
                    leafIcon = readIcon(treeLeafIconFileName);
            }
            if(readIcon(treeBranchIconFileName) != null){
                    branchIcon = readIcon(treeBranchIconFileName);
            }
            txtMapEntryLineBreak.setText(String.valueOf(mapDecompositionThreshold));
            txtRefreshFrequency.setText(String.valueOf(delay));
            btnSearch.setToolTipText("Press button (or Enter key) to search and highlight the specified term in detail window.");
            btnReset.setToolTipText("Press button to reset (or ESC) to reset search term.");
            btnNext.setToolTipText("Press button to jump to the next occurrence of the search term in the detail window.");
        }
    }
    
    public static synchronized PlatformInspectorGui getInstance(){
    	if(instance == null){
    		instance = new PlatformInspectorGui();
    	}
    	return instance;
    }
    
    /**
     * Activates debug mode. Can be activated at runtime.
     * @param debugActivated De/Activates debug mode
     */
    public static void activateDebugMode(boolean debugActivated){
    	PlatformInspectorGui.debug = debugActivated;
    }
    
    /**
     * Sets number of entries upon which maps will be decomposed with line break for better 
     * readability in InspectorGui.  
     * @param decompositionThreshold Threshold to be set for map decomposition
     */
    public static void setMapDecompositionThreshold(int decompositionThreshold){
    	//instantiate if not yet done and set decomposition threshold
    	getInstance().mapDecompositionThreshold = decompositionThreshold;
    	//update UI
    	getInstance().txtMapEntryLineBreak.setText(String.valueOf(decompositionThreshold));
    }
    
    /**
     * Holds registered listeners (listening for selection of agent)
     */
    private ArrayList<PlatformInspectorListener> listeners = new ArrayList<PlatformInspectorListener>();
    
    /**
     * Registers a listener for agent selection activity in the PlatformInspectorGui.
     * @param listener
     */
    public void registerListener(PlatformInspectorListener listener){
    	if(!listeners.contains(listener)){
    		System.out.println(MTRuntime.getPlatformPrefix() + "Registered PlatformInspectorListener " + listener);
    		listeners.add(listener);
    	}
    }
    
    /**
     * Deregisters a listener for agent selection activity in the PlatformInspectorGui.
     * @param listener
     */
    public void deregisterListener(PlatformInspectorListener listener){
    	if(listeners.remove(listener)){
    		System.out.println(MTRuntime.getPlatformPrefix() + "Deregistered PlatformInspectorListener " + listener);
    	}
    }
    
    private void notifyListeners(String agentName){
    	for(int i=0; i<listeners.size(); i++){
    		listeners.get(i).agentSelected(agentName);
    	}
    }
    
    public static void highlightNode(String node){
    	getInstance().highlightElement(node);
    }
    
    private void highlightElement(String node){
    	setVisible(true);
        Enumeration enumeration = root.children();
        Object child = null;
        //find child that matches passed parameter
        while(enumeration.hasMoreElements()){
            child = enumeration.nextElement();
            if(child.toString().equals(node)){
            	break;
            }
        }
        //expand tree and scroll to node
        TreeNode[] nodes = ((DefaultTreeModel) treAgentRegistry.getModel()).getPathToRoot((TreeNode) child);
        TreePath agentPath = new TreePath(nodes);
        TreePath roleLeafPath = expandNode(agentPath);
        treAgentRegistry.scrollPathToVisible(roleLeafPath);
        treAgentRegistry.setSelectionPath(roleLeafPath);
    }
    
    /**
     * Expands a given treepath and returns its last child's treepath (if existing).
     * @param parent
     * @return
     */
    private TreePath expandNode(TreePath parent) {
        TreeNode node = (TreeNode)parent.getLastPathComponent();
        TreePath path = null;
        if (node.getChildCount() >= 0) {
            Enumeration e = node.children();
            while(e.hasMoreElements()) {
                TreeNode n = (TreeNode)e.nextElement();
                path = parent.pathByAddingChild(n);
                expandNode(path);
            }
        }
        treAgentRegistry.expandPath(parent);
        return path;
    }
    
    @Override
    public void setVisible(boolean visible){
    	super.setVisible(visible);
    	if(visible){
    		fillTextArea();
    	}
    }
    
    public void setRepeatedRefresh(boolean refresh){
    	this.continuousAgentRegistryUpdate = refresh;
    }
    
    
    private BufferedImage readImageFromProjectFolder(String iconFileName){
    	BufferedImage img = null;
		try {
		    img = ImageIO.read(new File(System.getProperty("user.dir") + "/" + iconFileName));
		} catch (IOException e) {
		}
		return img;
    }
    
    private Image toImage(BufferedImage bufferedImage) {
        return Toolkit.getDefaultToolkit().createImage(bufferedImage.getSource());
    }
    
    private Image readImage(String iconFileName){
    	//first look same package as this class
    	if(this.getClass().getResource(iconFileName) != null){
    		return new ImageIcon(this.getClass().getResource(iconFileName)).getImage();
    	} else {
    		//try project root folder
    		if(readImageFromProjectFolder(iconFileName) != null){    		
    			return toImage(readImageFromProjectFolder(iconFileName));
    		} else {
    			//dummy image
    			System.err.println(new StringBuffer("Icon image file '")
    				.append(iconFileName)
    				.append("' for Platform Inspector could not be found, using dummy images for icons."));
    			return toImage(new BufferedImage(16, 16, BufferedImage.TYPE_BYTE_GRAY));
    		}
    	}
    }
    
    private ImageIcon readIcon(String iconFileName){
    	//first look same package as this class
    	if(this.getClass().getResource(iconFileName) != null){
    		return new ImageIcon(this.getClass().getResource(iconFileName));
    	}
    	return null;
    }
    
    private void setupSystray(){
    	if(SystemTray.isSupported()){
    		
    		final TrayIcon trayIcon = new TrayIcon(readImage(systrayIconFileName), "Micro-Agent Platform Inspector");
    		
    		ActionListener openInspectorWindow = new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent arg0) {
					if(instance.isVisible()){
						instance.setVisible(false);
					} else {
						instance.setVisible(true);
                                                //call previously selected agent if window had been closed in between
                                                if(oldTse != null && treSelectionListener != null){
                                                    treSelectionListener.valueChanged(oldTse);
                                                }
						//instance.setExtendedState(instance.getExtendedState() | JFrame.MAXIMIZED_BOTH);
					}
				}
			};
    		
			trayIcon.addActionListener(openInspectorWindow);

    		final SystemTray tray = SystemTray.getSystemTray();
			
			PopupMenu popup = new PopupMenu();

			ActionListener exitListener = new ActionListener(){

				@Override
				public void actionPerformed(ActionEvent arg0) {
					trayIcon.displayMessage("Micro-Agent Platform Inspector", 
							"Shutting down Inspector.", TrayIcon.MessageType.INFO);
					tray.remove(trayIcon);
					try {
						Thread.sleep(800);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				
			};
			MenuItem exitItem = new MenuItem("Exit");
			exitItem.addActionListener(exitListener);
			popup.add(exitItem);

			trayIcon.setPopupMenu(popup);
    		try {
				tray.add(trayIcon);
			} catch (AWTException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	}
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        tarDetails = new javax.swing.JTextArea();
        btnRefreshAgentRegistry = new javax.swing.JButton();
        jScrollPaneTree = new javax.swing.JScrollPane();
        treAgentRegistry = new javax.swing.JTree();
        lblAgentRegistry = new javax.swing.JLabel();
        lblDetails = new javax.swing.JLabel();
        chkRefreshAgentRegistry = new javax.swing.JCheckBox();
        txtRefreshFrequency = new javax.swing.JTextField();
        lblRefreshRate = new javax.swing.JLabel();
        chkRefreshDetailsView = new javax.swing.JCheckBox();
        btnRefreshDetailView = new javax.swing.JButton();
        txtMapEntryLineBreak = new javax.swing.JTextField();
        chkPrefixClassName = new javax.swing.JCheckBox();
        lblMapEntryLinebreak = new javax.swing.JLabel();
        txtSearch = new javax.swing.JTextField();
        btnSearch = new javax.swing.JButton();
        chkAutomaticHighlight = new javax.swing.JCheckBox();
        chkCaseSensitive = new javax.swing.JCheckBox();
        btnReset = new javax.swing.JButton();
        btnNext = new javax.swing.JButton();
        chkFollowOutputWithScrollbar = new javax.swing.JCheckBox();

        setTitle("Micro-Agent Platform Inspector");
        setPreferredSize(new java.awt.Dimension(1260, 550));
        addWindowFocusListener(new java.awt.event.WindowFocusListener() {
            public void windowGainedFocus(java.awt.event.WindowEvent evt) {
                formWindowGainedFocus(evt);
            }
            public void windowLostFocus(java.awt.event.WindowEvent evt) {
            }
        });
        addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentHidden(java.awt.event.ComponentEvent evt) {
                formComponentHidden(evt);
            }
        });
        addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                formKeyTyped(evt);
            }
        });

        tarDetails.setEditable(false);
        tarDetails.setColumns(20);
        tarDetails.setRows(5);
        jScrollPane1.setViewportView(tarDetails);

        btnRefreshAgentRegistry.setText("Refresh Agent Overview");
        btnRefreshAgentRegistry.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                btnRefreshAgentRegistryMouseClicked(evt);
            }
        });

        treAgentRegistry.setAutoscrolls(true);
        jScrollPaneTree.setViewportView(treAgentRegistry);

        lblAgentRegistry.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        lblAgentRegistry.setText("Registered Agents/Roles");

        lblDetails.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        lblDetails.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblDetails.setText("Inspected Internals");
        lblDetails.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);

        chkRefreshAgentRegistry.setSelected(true);
        chkRefreshAgentRegistry.setText("Refresh Agent Register automatically");
        chkRefreshAgentRegistry.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chkRefreshAgentRegistryActionPerformed(evt);
            }
        });

        txtRefreshFrequency.setText("1000");
        txtRefreshFrequency.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                txtRefreshFrequencyKeyReleased(evt);
            }
        });

        lblRefreshRate.setText("Refresh Rate (in ms)");

        chkRefreshDetailsView.setSelected(true);
        chkRefreshDetailsView.setText("Refresh Details View automatically");
        chkRefreshDetailsView.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chkRefreshDetailsViewActionPerformed(evt);
            }
        });

        btnRefreshDetailView.setText("Refresh Detail View");
        btnRefreshDetailView.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRefreshDetailViewActionPerformed(evt);
            }
        });

        txtMapEntryLineBreak.setText("6");
        txtMapEntryLineBreak.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                txtMapEntryLineBreakKeyReleased(evt);
            }
        });

        chkPrefixClassName.setSelected(true);
        chkPrefixClassName.setText("Prefix Field Names with Class Names");
        chkPrefixClassName.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chkPrefixClassNameActionPerformed(evt);
            }
        });

        lblMapEntryLinebreak.setText("<html>Number of map entries before<br>decomposition with line breaks<html>");

        txtSearch.setText("Specify search terms separated by whitespace or framed by quotation marks (e.g. \"agent 5\").");
        txtSearch.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                txtSearchMouseClicked(evt);
            }
        });
        txtSearch.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                txtSearchKeyPressed(evt);
            }
            public void keyTyped(java.awt.event.KeyEvent evt) {
                txtSearchKeyTyped(evt);
            }
        });

        btnSearch.setText("Search");
        btnSearch.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                btnSearchMouseClicked(evt);
            }
        });

        chkAutomaticHighlight.setText("Automatic highlighting");

        chkCaseSensitive.setText("Case-sensitive");

        btnReset.setText("Reset");
        btnReset.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                btnResetMouseClicked(evt);
            }
        });

        btnNext.setText("Jump to next");
        btnNext.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                btnNextMouseClicked(evt);
            }
        });

        chkFollowOutputWithScrollbar.setText("Follow output with scrollbar");
        chkFollowOutputWithScrollbar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chkFollowOutputWithScrollbarActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPaneTree, javax.swing.GroupLayout.PREFERRED_SIZE, 266, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(26, 26, 26)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(txtSearch, javax.swing.GroupLayout.PREFERRED_SIZE, 466, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addComponent(btnRefreshAgentRegistry, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(btnRefreshDetailView, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addGap(18, 18, 18)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(chkRefreshDetailsView)
                                    .addComponent(chkRefreshAgentRegistry))
                                .addGap(9, 9, 9)))
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(chkPrefixClassName)
                                .addGap(18, 18, 18)
                                .addComponent(txtRefreshFrequency, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(lblRefreshRate)
                                .addGap(0, 0, Short.MAX_VALUE))
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(btnSearch, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                        .addComponent(btnReset)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(btnNext))
                                    .addGroup(layout.createSequentialGroup()
                                        .addComponent(chkFollowOutputWithScrollbar)
                                        .addGap(0, 0, Short.MAX_VALUE)))
                                .addGap(18, 18, 18)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(layout.createSequentialGroup()
                                        .addComponent(chkAutomaticHighlight)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(chkCaseSensitive))
                                    .addGroup(layout.createSequentialGroup()
                                        .addComponent(txtMapEntryLineBreak, javax.swing.GroupLayout.PREFERRED_SIZE, 21, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(7, 7, 7)
                                        .addComponent(lblMapEntryLinebreak, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))))))
                .addGap(15, 15, 15))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGap(74, 74, 74)
                .addComponent(lblAgentRegistry, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lblDetails)
                .addGap(397, 397, 397))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGap(33, 33, 33)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblAgentRegistry)
                    .addComponent(lblDetails))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 349, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(txtSearch, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(btnSearch)
                            .addComponent(chkCaseSensitive)
                            .addComponent(chkAutomaticHighlight))
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addGap(3, 3, 3)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(btnReset)
                                    .addComponent(btnNext))
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(layout.createSequentialGroup()
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                            .addComponent(chkRefreshAgentRegistry)
                                            .addComponent(btnRefreshAgentRegistry)))
                                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(chkFollowOutputWithScrollbar))))
                            .addGroup(layout.createSequentialGroup()
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addComponent(lblMapEntryLinebreak, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(txtMapEntryLineBreak, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(chkRefreshDetailsView, javax.swing.GroupLayout.PREFERRED_SIZE, 27, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(chkPrefixClassName)
                                .addComponent(txtRefreshFrequency, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(lblRefreshRate))
                            .addComponent(btnRefreshDetailView)))
                    .addComponent(jScrollPaneTree, javax.swing.GroupLayout.DEFAULT_SIZE, 464, Short.MAX_VALUE))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnRefreshAgentRegistryMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnRefreshAgentRegistryMouseClicked
        fillTextArea();
    }//GEN-LAST:event_btnRefreshAgentRegistryMouseClicked

    private void chkPrefixClassNameActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chkPrefixClassNameActionPerformed
        if(chkPrefixClassName.isSelected()){
            InspectorAnnotationReflector.printClassName = true;
        } else {
            InspectorAnnotationReflector.printClassName = false;
        }
        manageUiThread(lastClassParameter, lastObjectParameter, lastObjectsParameter);
    }//GEN-LAST:event_chkPrefixClassNameActionPerformed

    private void chkRefreshDetailsViewActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chkRefreshDetailsViewActionPerformed
        if(chkRefreshDetailsView.isSelected()){
            continuousDetailViewUpdate = true;
            manageUiThread(lastClassParameter, lastObjectParameter, lastObjectsParameter);
        } else {
            continuousDetailViewUpdate = false;
            updateRunnable.stop();
        }
    }//GEN-LAST:event_chkRefreshDetailsViewActionPerformed

    private void btnRefreshDetailViewActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRefreshDetailViewActionPerformed
        manageUiThread(lastClassParameter, lastObjectParameter, lastObjectsParameter);
    }//GEN-LAST:event_btnRefreshDetailViewActionPerformed

    private void txtMapEntryLineBreakKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtMapEntryLineBreakKeyReleased
        if(!txtMapEntryLineBreak.getText().isEmpty()){
            try{
                mapDecompositionThreshold = Integer.parseInt(txtMapEntryLineBreak.getText());
                manageUiThread(lastClassParameter, lastObjectParameter, lastObjectsParameter);
            }catch(NumberFormatException ex){
                JOptionPane.showMessageDialog(this, "Invalid Input for map decomposition threshold (must be a number)!", "Please enter a valid number for the decomposition threshold!", JOptionPane.ERROR_MESSAGE);
            }
        }
    }//GEN-LAST:event_txtMapEntryLineBreakKeyReleased

    private void txtRefreshFrequencyKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtRefreshFrequencyKeyReleased
    	if(!txtRefreshFrequency.getText().isEmpty()){
	    	try{
	            delay = Integer.parseInt(txtRefreshFrequency.getText());
	            manageUiThread(lastClassParameter, lastObjectParameter, lastObjectsParameter);
	        }catch(NumberFormatException ex){
	            JOptionPane.showMessageDialog(this, "Invalid Input", "Please enter a valid number for the refresh rate!", JOptionPane.ERROR_MESSAGE);
	        }
    	}
    }//GEN-LAST:event_txtRefreshFrequencyKeyReleased

    private void formWindowGainedFocus(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowGainedFocus
        if(lastClassParameter != null || lastObjectParameter != null || lastObjectsParameter != null){
            manageUiThread(lastClassParameter, lastObjectParameter, lastObjectsParameter);
        }
    }//GEN-LAST:event_formWindowGainedFocus
    
    private void txtSearchKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtSearchKeyTyped
    	if(!txtSearch.getText().equals("")){
	    	if(chkAutomaticHighlight.isSelected()){
	        	highlightSearchTerms(false);
	        }
    	}
    	if(!btnSearch.getText().equals(DEFAULT_SEARCH_KEY_NAME)){
    		btnSearch.setText(DEFAULT_SEARCH_KEY_NAME);
    	}
    }//GEN-LAST:event_txtSearchKeyTyped

    private void btnSearchMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnSearchMouseClicked
        if(!txtSearch.getText().equals("")){
                oldSearchTerm = txtSearch.getText();
                //reset highlighting index (for working 'Next' button)
                highlightIndex = -1;
                //would be searched upon next thread update but still do explicitly if details are not continuously updated
                highlightSearchTerms(true);
        }
    }//GEN-LAST:event_btnSearchMouseClicked

    private void txtSearchMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_txtSearchMouseClicked
        if(oldSearchTerm == null){
        	//empty field when mouse clicked and no previous user search term
        	txtSearch.setText("");
        }
    }//GEN-LAST:event_txtSearchMouseClicked

    private void txtSearchKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtSearchKeyPressed
        //if enter is pressed
        if(evt.getKeyCode() == KeyEvent.VK_ENTER){
            if(!txtSearch.getText().isEmpty()){
                if(txtSearch.getText().equals(oldSearchTerm)){
                    //if nothing new entered, interprete Enter key as jump to next result entry (adjusting scrollpane)
                    btnNextMouseClicked(null);
                } else {
                    //treat as if Search button has been clicked
                    btnSearchMouseClicked(null);
                }
            }
        }
    }//GEN-LAST:event_txtSearchKeyPressed

    private void btnResetMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnResetMouseClicked
        txtSearch.setText("");
        txtSearchKeyTyped(null);
    }//GEN-LAST:event_btnResetMouseClicked

    private void btnNextMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnNextMouseClicked
        //System.out.println("Should jump to next result.");
        try {
            jumpToNextHighlight(tarDetails);
        } catch (BadLocationException ex) {
            JOptionPane.showMessageDialog(tarDetails, "Could not jump to next highlight as it points to an invalid position.", "Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }//GEN-LAST:event_btnNextMouseClicked

    private void formKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_formKeyTyped
        //reset search field (and search itself)
    	System.err.println("Key types");
    	if(evt.getKeyCode() == KeyEvent.VK_ESCAPE){
            btnResetMouseClicked(null);
        } else 
            //if enter is not typed while having search field in focus, interprete as jump.
            if(evt.getKeyCode() == KeyEvent.VK_ENTER){
				System.out.println("Enter key typed while in search field");
                if(!txtSearch.getText().isEmpty() && txtSearch.getText().equals(oldSearchTerm)){
                    //if nothing new entered, interprete Enter key as jump to next result entry (adjusting scrollpane)
                    btnNextMouseClicked(null);
                }
            } else if(!txtSearch.isFocusOwner()){
                //set focus to search text field
                txtSearch.requestFocusInWindow();
                System.err.println("Requested focus for search box");
                //type key again
                formKeyTyped(evt);
            }
    }//GEN-LAST:event_formKeyTyped

    private void formComponentHidden(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_formComponentHidden
        //notify all listeners so they can perform eventual cleanup activities to minimize waste of resources
        notifyListeners(null);
    }//GEN-LAST:event_formComponentHidden

    private void chkRefreshAgentRegistryActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chkRefreshAgentRegistryActionPerformed
        if(chkRefreshAgentRegistry.isSelected()){
            if(!directoryUpdateRunnable.isRunning()){
            	new Thread(directoryUpdateRunnable).start();
            }
        } else {
            directoryUpdateRunnable.stop();
        }
    }//GEN-LAST:event_chkRefreshAgentRegistryActionPerformed

    /** Update policy for scrollbar */
    Integer oldTextAreaUpdatePolicy = null;
    
    private void chkFollowOutputWithScrollbarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chkFollowOutputWithScrollbarActionPerformed
        DefaultCaret caret = (DefaultCaret)tarDetails.getCaret();
        if(chkFollowOutputWithScrollbar.isSelected()){    
            oldTextAreaUpdatePolicy = caret.getUpdatePolicy();
            caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        } else {
            caret.setUpdatePolicy(oldTextAreaUpdatePolicy);
        }
        
    }//GEN-LAST:event_chkFollowOutputWithScrollbarActionPerformed

    private ImageIcon leafIcon = null;
    private ImageIcon branchIcon = null;
    DefaultTreeCellRenderer renderer = null;
    
    private void fillTextArea(){
    	
    	if(root == null){
        	root = new DefaultMutableTreeNode(PlatformInspectorGui.ROOT_LABEL);
        	treAgentRegistry = new JTree(root);
        	jScrollPaneTree.setViewportView(treAgentRegistry);
        }
    	
    	if(MicroBootProperties.isPlatformInitialized()){
    		
    		agentRegisterCounter = MTConnector.getRegisteredAgents().size();
    		TREE_ROOT_LABEL = new StringBuffer(PlatformInspectorGui.ROOT_LABEL)
    		.append(" (").append(agentRegisterCounter).append(" agents)").toString();
    		root = new DefaultMutableTreeNode(TREE_ROOT_LABEL);

	        HashMap<Integer, HashSet<String>> sortMap = new HashMap<Integer, HashSet<String>>();
	        for(String agentName: MTConnector.getRegisteredAgents().keySet()){
	            int length = agentName.length();
	            if(!agentName.startsWith(MTRuntime.anonymousAgentPrefix)){
	            	length = 99;
	            }
	            if(!sortMap.containsKey(length)){
	                HashSet<String> internalMap = new HashSet<String>();
	                internalMap.add(agentName);
	                sortMap.put(length, internalMap);
	            } else {
	                sortMap.get(length).add(agentName);
	            }
	        }
	        List<Integer> keySet = new ArrayList<Integer>(sortMap.keySet());
	        if(keySet.size() > 1){
		        Collections.sort(keySet);
	        }
	        
	        for(int i=0; i<keySet.size(); i++){
	            ArrayList<String> list = new ArrayList<String>(sortMap.get(keySet.get(i)));
	            Collections.sort(list);
	            //add agents as nodes
	            for(int l=0; l<list.size(); l++){
	                DefaultMutableTreeNode agentNode = new DefaultMutableTreeNode(list.get(l));
	                root.add(agentNode);
	                AbstractMicroFiber fiber = MTConnector.getRegisteredAgent(list.get(l).toString());
	                if(fiber != null){
		                AbstractAgent agent = (AbstractAgent) fiber.getAgent();
		                if(agent != null){
			                Role[] roles = agent.getRoles();
			                //add roles to subnodes
			                for(int m=0; m<roles.length; m++){
			                    DefaultMutableTreeNode roleNode = new DefaultMutableTreeNode(roles[m].getRoleName());
			                    agentNode.add(roleNode);
			                }
		                }
	                }
	            }
	            if(collectiveViewActivated){
	            	DefaultMutableTreeNode collectiveNode = new DefaultMutableTreeNode(COLLECTIVE_VIEW_TEXT);
	            	root.add(collectiveNode);
	            }
	        }
	        treAgentRegistry = new JTree(root);
            treAgentRegistry.setPreferredSize(null);
            if(renderer == null){
            	renderer = new DefaultTreeCellRenderer();
            	if(leafIcon != null){
            		renderer.setLeafIcon(leafIcon);
            	}
            	if(branchIcon != null){
            		renderer.setOpenIcon(branchIcon);
            		renderer.setClosedIcon(branchIcon);
            	}
            	treAgentRegistry.setCellRenderer(renderer);
            } else {
            	treAgentRegistry.setCellRenderer(renderer);
            }
	        treAgentRegistry.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
	        //initialize tree selection listener
                treSelectionListener = new TreeSelectionListener() {
	
	            public void valueChanged(TreeSelectionEvent tse) {
                        //save tree selection event
                        oldTse = tse;
	            	//decomposing clicked node
	            	int start = tse.getPath().toString().lastIndexOf("[");
	            	int end = tse.getPath().toString().lastIndexOf("]");
	            	String nodeName = tse.getPath().toString().substring(start+1, end);

	            	if(nodeName.equals(TREE_ROOT_LABEL)){
	            		lblDetails.setText(DETAIL_VIEW_LABEL_TEXT + ROOT_LABEL);
	            		showContentForClass(MTConnector.class);
	            		notifyListeners(null);
	            	} else {
		            	if(tse.getNewLeadSelectionPath() != null){
			                Object clickedOn = tse.getNewLeadSelectionPath().getLastPathComponent();
			                Object parentOfClickedOne = null;
			                if(tse.getNewLeadSelectionPath().getParentPath() != null){
			                    parentOfClickedOne = tse.getNewLeadSelectionPath().getParentPath().getLastPathComponent();
			                }
			                if(collectiveViewActivated && clickedOn.toString().equals(COLLECTIVE_VIEW_TEXT)){
			                	//System.err.println("Multiple objects to be inspected");
			                	lblDetails.setText(DETAIL_VIEW_LABEL_TEXT + COLLECTIVE_VIEW_TEXT);
			                	ArrayList<Agent> agents = new ArrayList<Agent>();
			                	Agent[] agArr = SystemAgentLoader.getAgentsRecursive();
			                	for(int i=0; i<agArr.length; i++){
			                		agents.add(agArr[i]);
			                	}
			                	showContentForObjects((List)agents);
			                } else if(MTConnector.getAgentForName(clickedOn.toString()) != null){
			                    //must be an agent which has been clicked on --> show agent content in text area
			                	if(debug){
			                		System.out.println("Looking up agent " + clickedOn.toString() + " -> " + MTConnector.getAgentForName(clickedOn.toString()));
			                	}
			                	AbstractAgent inspectedAgent = MTConnector.getAgentForName(clickedOn.toString());
			                	lblDetails.setText(DETAIL_VIEW_LABEL_TEXT + inspectedAgent.getAgentName());
			                	showContentForObject(inspectedAgent);
			                	//notify eventual listeners
			                	notifyListeners(inspectedAgent.getAgentName());
			                } else {
			                    if(parentOfClickedOne != null){
			                    	AbstractAgent agent = MTConnector.getAgentForName(parentOfClickedOne.toString());
				                	if(agent == null){
				                		lblDetails.setText("Inspected Internals");
				                		//do nothing
				                	} else {
				                		Role inspectedRole = agent.getRole(clickedOn.toString());
				                		lblDetails.setText(DETAIL_VIEW_LABEL_TEXT + "Role '" 
				                				+ inspectedRole.getRoleName()
				                				+ "' on " + agent.getAgentName());
				                		showContentForObject(inspectedRole);
				                	}
			                    }
			                }
		            	}
	            	}
	            }
	        };
                treAgentRegistry.addTreeSelectionListener(treSelectionListener);
	        jScrollPaneTree.setViewportView(treAgentRegistry);
    	} else {
    		tarDetails.setText("Platform loaded: ");
    		tarDetails.append(String.valueOf(MicroBootProperties.isPlatformInitialized()));
    		tarDetails.append(InspectorAnnotationReflector.LINE_SEPARATOR);
    		tarDetails.append("Current configuration settings:");
    		tarDetails.append(InspectorAnnotationReflector.LINE_SEPARATOR);
    		tarDetails.append("-------------------------------------------------------");
    		tarDetails.append(InspectorAnnotationReflector.LINE_SEPARATOR);
    		for(Entry<Object, Object> entry: MicroBootProperties.bootProperties.entrySet()){	
    			tarDetails.append(entry.getKey().toString());
    			tarDetails.append(": ");
    			tarDetails.append(entry.getValue().toString());
    			tarDetails.append(InspectorAnnotationReflector.LINE_SEPARATOR);
    		}	
    	}
    	if(!directoryUpdateRunnable.isRunning() && chkRefreshAgentRegistry.isSelected()){
    		Thread directoryThread = new Thread(directoryUpdateRunnable);
    		directoryThread.setName(PLATFORM_INSPECTOR_PREFIX + "RegistryUpdateThread");
    		directoryThread.start();
    	}	
    }
    
    int agentRegisterCounter = 0;
    boolean continuousAgentRegistryUpdate = true;
    boolean continuousDetailViewUpdate = true;
    boolean collectiveViewActivated = true;
    
    StoppableRunnable<JFrame> directoryUpdateRunnable = new StoppableRunnable<JFrame>(this, "PlatformInspector Directory Update Thread") {
		
		@Override
		public void stoppableRun() {
			if(MicroBootProperties.isPlatformInitialized()){
				if(continuousAgentRegistryUpdate){
					if(getReferenceObject() != null && getReferenceObject().isVisible()){
						if(MTConnector.getRegisteredAgents().size() != agentRegisterCounter){
							tarDetails.setText("");
							fillTextArea();
						}
					} else {
						stop();
					}
				} else {
					if(agentRegisterCounter == 0){
						if(getReferenceObject() != null && getReferenceObject().isVisible()){
							tarDetails.setText("");
							fillTextArea();
						}
					}
					stop();
				}
			}
			try {
				Thread.sleep(delay);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	};
    
    private Class lastClassParameter = null;
    private Object lastObjectParameter = null;
    private List<Object> lastObjectsParameter = null;
    
    private void manageUiThread(Class classToInspect, Object objectToInspect, List<Object> objectsToInspect){
        lastClassParameter = classToInspect;
        lastObjectParameter = objectToInspect;
        lastObjectsParameter = objectsToInspect;
        if(debug){
        	System.out.println("Attempting to inspect " + (lastClassParameter == null ? "" : lastClassParameter.getSimpleName()) + (lastObjectParameter == null ? "" : lastObjectParameter) + (lastObjectsParameter == null ? "" : lastObjectsParameter));
        }
    	StoppableRunnable<JFrame> tempRunnable = createUiUpdateRunnable(classToInspect, objectToInspect, objectsToInspect, this);
    	
    	if(updateRunnable != null && updateRunnable.isRunning()){
    		updateRunnable.stop();
    	}
    	updateRunnable = tempRunnable;
        if(continuousDetailViewUpdate){
            Thread detailViewThread = new Thread(updateRunnable);
            detailViewThread.setName(PLATFORM_INSPECTOR_PREFIX + "DetailViewUpdateThread");
            detailViewThread.start();
        } else {
            updateRunnable.stoppableRun();
        }
    }

    public void showContentForClass(Class classToInspect){
    	manageUiThread(classToInspect, null, null);
    }
    
    public void showContentForObject(Object objectToInspect){
    	manageUiThread(null, objectToInspect, null);
    }
    
    public void showContentForObjects(List<Object> objectsToInspect){
    	manageUiThread(null, null, objectsToInspect);
    }
    
    //reference for runnable in order to stop it when obsolete
    StoppableRunnable<JFrame> updateRunnable = null;
    //delay between UI refresh
    int delay = 1000;
    //threshold of number of entries in order to decompose maps with line break
    int mapDecompositionThreshold = 6;

    private StoppableRunnable<JFrame> createUiUpdateRunnable(final Class classToInspect, final Object objectToInspect, final List<Object> objectsToInspectCollectively, final JFrame frame){
    	
    	StoppableRunnable<JFrame> runnable = new StoppableRunnable<JFrame>(frame, "PlatformInspector Details View Update Thread") {
			
    		StringBuffer lastBuffer = null;
    		
    		@Override
			public void stoppableRun() {
				
				if(getReferenceObject() != null && getReferenceObject().isVisible()){
					LinkedHashMap<String, Object> inspectionMap = new LinkedHashMap<String, Object>();

					try{
						if(classToInspect != null){
							inspectionMap = InspectorAnnotationReflector.inspect(classToInspect, InspectorAnnotationReflector.INSPECTION);
						} else {
							if(objectToInspect != null){
								inspectionMap = InspectorAnnotationReflector.inspect(objectToInspect, InspectorAnnotationReflector.INSPECTION);
							} else if(objectsToInspectCollectively != null){
								//inspect all roles for agent
								for(int i=0; i<objectsToInspectCollectively.size(); i++){ 
									Object tempObject = objectsToInspectCollectively.get(i);
									String agName = ((Agent)tempObject).getAgentName();
									Role[] roles = ((Agent)tempObject).getRoles(); 
									//inspect on agent level
									inspectionMap.put(agName, InspectorAnnotationReflector.inspect((Agent)tempObject, InspectorAnnotationReflector.COLLECTIVE_INSPECTION));
									//inspect on role level
									for(int j=0; j<roles.length; j++){
										inspectionMap.put(agName + " - Role " + roles[j], InspectorAnnotationReflector.inspect(roles[j], InspectorAnnotationReflector.COLLECTIVE_INSPECTION));
									}
								}
							} else {
								//Error: both class and object parameter are null.
								stop();
							}
						}
				    	StringBuffer buffer = new StringBuffer();
				    	try{
					    	for(Map.Entry<String, Object> entry: inspectionMap.entrySet()){
					    		buffer.append("--------------------------------------------------------")
					    			.append("---------------------------------------------------------------------------------------")
					    			.append(InspectorAnnotationReflector.LINE_SEPARATOR);
					    		
					    		buffer.append(entry.getKey()).append(": ");
					    		buffer = decomposeRecursively(entry.getValue(), buffer);
					    		buffer.append(InspectorAnnotationReflector.LINE_SEPARATOR);
					    	}
				    	} catch (ConcurrentModificationException e){
				    		//catching the case that there was a change to agent properties while inspecting them - do nothing
				    		if(debug){
				    			e.printStackTrace();
				    		}
				    	}
				    	if(buffer == null || buffer.length() == 0){
				    		stop();
				    	} else {
					    	//set collected reflection information to details text area
				    		tarDetails.setText(buffer.toString());
					    	//keep last output
				    		lastBuffer = buffer;
					    	highlightSearchTerms(false);
					    	try {
								Thread.sleep(delay);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
				    	}
					} catch (InvocationTargetException e){
						//agent has been deleted
						stop();
						tarDetails.setText(InspectorAnnotationReflector.LINE_SEPARATOR + "Agent cannot be further inspected. It may either not be existing anymore or an error has occured during inspection.");
						if(lastBuffer != null && lastBuffer.length() != 0){
							tarDetails.append(new StringBuffer(InspectorAnnotationReflector.LINE_SEPARATOR)
								.append(InspectorAnnotationReflector.LINE_SEPARATOR)
								.append("Last output:")
								.append(InspectorAnnotationReflector.LINE_SEPARATOR)
								.append(InspectorAnnotationReflector.LINE_SEPARATOR)
								.append(lastBuffer).toString());
						}
						if(debug){
							e.printStackTrace();
						}
					}
				} else {
					stop();
				}
			}
			
			private int decompositionLevel = 1;
			
			private synchronized StringBuffer decomposeRecursively(Object value, StringBuffer buffer){
				StringBuffer intBuffer = buffer;
				
				if(value != null && containsMap(value)){
					//System.out.println("Value is Map: " + value);
					boolean containsMap = false;
					int mapSize = 0;
					for(Object key: ((Map)value).keySet()){
						if(containsMap(((Map)value).get(key))){
							containsMap = true;
							break;
						}
					}
					
					//check for nested map
					mapSize = ((Map)value).size();
					if(!containsMap && mapSize < mapDecompositionThreshold && !containsCollection(value, true)){
						//if map on highest level (but no embedded map) --> just print
						if(mapSize > 0){
							intBuffer.append("(").append(mapSize).append(") ");
						}
						intBuffer.append(value);
					} else {
						//else (if embedded map, threshold exceeded, 
						//or (potentially large) collection contained as value) decompose into elements to make it more readable
						if(mapSize > 0){
							intBuffer.append(" (").append(mapSize).append(") ");
						}
						for(Object key: ((Map)value).keySet()){
							intBuffer.append(InspectorAnnotationReflector.LINE_SEPARATOR).append(calcStringPrefix()).append(" ").append(key);
							intBuffer.append(": ");
							decompositionLevel++;
							intBuffer = decomposeRecursively(((Map)value).get(key), intBuffer);
							decompositionLevel--;
						}
					}
				} else {
					if(value != null && value.getClass().isArray()){
						//System.out.println("Value is Array: " + value);
						//if value is array, decompose
						intBuffer.append("[");
						for(int i=0; i<Array.getLength(value); i++){
							if(Array.get(value, i) != null){
								intBuffer = decomposeRecursively(Array.get(value, i), intBuffer);
								//intBuffer.append(Array.get(value, i));
								intBuffer.append(", ");
							}
						}
						int lastIndex = intBuffer.lastIndexOf(", ");
						intBuffer.delete(lastIndex, lastIndex+2);
						intBuffer.append("]");
					} else if(value != null && containsCollection(value, true)) {
						//System.out.println("Value is Collection: " + value);
						intBuffer.append("(").append(((Collection)value).size()).append(") ");
						if(((Collection)value).size() > mapDecompositionThreshold){
							//eventually decompose big collections
							intBuffer.append("(").append(InspectorAnnotationReflector.LINE_SEPARATOR);
							for(Object element: ((Collection)value)){
								intBuffer = decomposeRecursively(element, intBuffer);
								intBuffer.append(InspectorAnnotationReflector.LINE_SEPARATOR);
							}
							intBuffer.append(")");
						} else {
							//just print collection 'as is' if small
							intBuffer.append(value);
						}
						
					} else {
						//System.out.println("Value is NOT array or collection: " + value);
						//else simply add to print buffer
						intBuffer.append(value);
					}
				}
				return intBuffer;
			}
			
			/**
			 * Tests if a given object is a map.
			 * @param object
			 * @return
			 */
			public boolean containsMap(Object object){
				boolean containsMap = false;
				if(object == null){
					return containsMap;
				}
				Class recursiveClass = object.getClass();
				while(recursiveClass != null && !containsMap){
					for(int i=0; i<recursiveClass.getInterfaces().length; i++){
						if(recursiveClass.getInterfaces()[i].equals(Map.class)){
							containsMap = true;
							break;
						}
					}
					if(!containsMap){
						recursiveClass = recursiveClass.getSuperclass();
					}
				}
				return containsMap;
			}
			
			public boolean containsCollection(Object object, boolean testForMap){
				if(object == null){
					return false;
				}
				boolean isMap = false;
				if(testForMap){
					Class[] objectInterfaces = object.getClass().getInterfaces();
					for(int i = 0; i < objectInterfaces.length; i++){
						if(objectInterfaces[i].equals(Map.class)){
							isMap = true;
							break;
						}
					}
				}
				if(isMap){
					return containsCollection(((Map)object).values(), false);
				} else {
					boolean containsCollection = false;
					Class recursiveClass = object.getClass();
					while(recursiveClass != null && !containsCollection){
						for(int i=0; i<recursiveClass.getInterfaces().length; i++){
							if(recursiveClass.getInterfaces()[i].equals(Collection.class)){
								containsCollection = true;
								break;
							}
						}
						if(!containsCollection){
							recursiveClass = recursiveClass.getSuperclass();
						}
					}
					return containsCollection;
				}
			}
			
			int previousLength = 0;
			
			private StringBuffer calcStringPrefix(){
				StringBuffer buf = new StringBuffer();
				int decomp = decompositionLevel;
				while(decomp > 0){
					buf.append("-");
					decomp--;
				}
				if(previousLength != 0 && previousLength > buf.length()){
					previousLength = buf.length();
					buf = new StringBuffer(InspectorAnnotationReflector.LINE_SEPARATOR).append(buf);
				} else {
					previousLength = buf.length();
				}
				return buf;
			}
			
		};
        return runnable;
    }
    
    //holds old search term in order to detect changes (relevant for automated highlighting by threads) - should be maintained if search term be considered current
    private String oldSearchTerm = null;
    
    /**
     * Highlights current search terms (in txtSearch textfield) 
     * @param override indicates if a new search is performed (using the search button) or simple thread update with old terms
     */
    protected void highlightSearchTerms(boolean override) {
        if(txtSearch.getText().isEmpty()){
            return;
        }
    	//update highlighting if search term had NOT BEEN changed and search had been pressed neither (simple automated thread update)
    	if(!override && !chkAutomaticHighlight.isSelected() && oldSearchTerm != null && oldSearchTerm.equals(txtSearch.getText())){
            highlight(tarDetails, txtSearch.getText());
            //scroll to last highlight (if next button had been used)
            jumpToUpdatedHighlightPosition(tarDetails);
            return;
    	}
    	//update highlighting if continuous highlighting is activated or search button has been pressed and term had indeed been changed, reset highlight index
        if((override || chkAutomaticHighlight.isSelected()) && oldSearchTerm != null && !oldSearchTerm.equals(txtSearch.getText())){
            highlight(tarDetails, txtSearch.getText());
            //reset highlighting as now NEW SEARCH term
            highlightIndex = -1;
            //jump to first entry
            try {
				jumpToNextHighlight(tarDetails);
			} catch (BadLocationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        } else if(oldSearchTerm != null && oldSearchTerm.equals(txtSearch.getText())) {
            //else update highlighting and go to old highlight as search term HAS NOT CHANGED - will only work if number of highlights hasn't changed
            highlight(tarDetails, txtSearch.getText());
            //scroll to last highlight (if next button had been used)
            jumpToUpdatedHighlightPosition(tarDetails);
        } else {
            //if old search term is null, no button has been pressed and no automatic highlighting is activated by the search term has changed
        }
    }

	//Highlighting-related source taken from: http://www.exampledepot.com/egs/javax.swing.text/style_HiliteWords.html
    // creates highlights around all occurrences of pattern in textComp
    public void highlight(JTextComponent textComp, String pattern) {
        // First remove all old highlights
        removeHighlights(textComp);

        try {
            Highlighter hilite = textComp.getHighlighter();
            Document doc = textComp.getDocument();
            String text = doc.getText(0, doc.getLength());

            //check if search is naive
            //check if uneven number of quotation marks, then fall back to individual tokens
            if(!chkCaseSensitive.isSelected()){
                    pattern = pattern.toLowerCase();
                    text = text.toLowerCase();
            }
            //check on comprehensive terms
            ArrayList<String> individualSearchTerms = new ArrayList<String>();
            ArrayList<String> quotationSearchTerms = new ArrayList<String>();
            String quotationMark = "\"";
            String whiteSpace = " ";
            StringTokenizer tok = new StringTokenizer(pattern, whiteSpace, true);
            //System.out.println("Tokens: " + tok.countTokens());
            while(tok.hasMoreElements()){
                    individualSearchTerms.add(tok.nextToken());
            }
            //System.out.println("Whitespace-separated tokens: " + individualSearchTerms.toString());
            int startIndex = 0;
            int stopIndex = 0;
            boolean started = false;
            for(int i=0; i<individualSearchTerms.size(); i++){
                    //indication of detected entering quotation mark 
                    boolean quotationMarkDetected = false;
                    if(individualSearchTerms.get(i).contains(quotationMark)){
                            //System.out.println("Contains: " + individualSearchTerms.get(i));
                            if(!started){
                                    started = true;
                                    startIndex = i;
                                    quotationMarkDetected = true;
                            }
                            //do this check instead of an else as the term might contain terminating quotation mark (i.e. two quotation marks) as well
                            //if term only contains ending tag
                            if((!quotationMarkDetected && started) 
                                            //if term contains both starting and ending tag
                                            || (individualSearchTerms.get(i).indexOf(quotationMark) != individualSearchTerms.get(i).lastIndexOf(quotationMark) && started)){
                                started = false;
                                //System.out.println("Stopped:");
                                stopIndex = i;
                                if(startIndex != stopIndex){
                                        String buildQuotTerm = "";
                                        for(int l=startIndex; l<stopIndex+1; l++){
                                                if(l == startIndex){
                                                        //start of quotation
                                                        buildQuotTerm += individualSearchTerms.get(l).substring(1);
                                                        //System.out.println("First term: " + buildQuotTerm);
                                                } else {
                                                        if(l == stopIndex){
                                                                //add last bit
                                                                int lastLength = individualSearchTerms.get(l).length();
                                                                buildQuotTerm += /*whiteSpace +*/ individualSearchTerms.get(l).substring(0, lastLength-1);
                                                                //System.out.println("Last term: " + buildQuotTerm);
                                                        } else {
                                                                //add medium terms that have been tokenized but do not contain quotation marks
                                                                buildQuotTerm += /*whiteSpace +*/ individualSearchTerms.get(l);
                                                                //System.out.println("Middle term: " + buildQuotTerm);
                                                        }
                                                }
                                        }
                                        quotationSearchTerms.add(buildQuotTerm);
                                        //System.out.println("QuotterM: " + quotationSearchTerms);
                                } else {
                                        //all in one token anyway
                                        int lastLength = individualSearchTerms.get(i).length();
                                        //System.out.println("Identified term: " + individualSearchTerms.get(i) + ": " + individualSearchTerms.get(i).substring(1, lastLength-1));
                                        quotationSearchTerms.add(individualSearchTerms.get(i).substring(1, lastLength-1));
                                }
                                    //remove from individual search list
	                            for(int k=startIndex; k<=stopIndex; k++){
	                                    //always remove at same position as with each remove everything shifts to the left
	                                    individualSearchTerms.remove(startIndex);
	                                    //reduce counter
	                                    i--;
	                            }
                            //System.out.println("Indivi after cleanupt: " + individualSearchTerms);
                            }
                    }
            }
            //remove quotation marks from individual terms that do not have whitespace
            /*for(int i=0; i<individualSearchTerms.size(); i++){
                    String term = individualSearchTerms.get(i);
                    if(term.startsWith(quotationMark) && term.endsWith(quotationMark)){
                            term = term.substring(1, term.length()-1);
                            individualSearchTerms.remove(i);
                            individualSearchTerms.add(i, term);
                    }
            }*/

            //clean up individual search terms from whitespaces
            for(int i=0; i<individualSearchTerms.size(); i++){
                    if(individualSearchTerms.get(i).equals(whiteSpace)){
                            individualSearchTerms.remove(i);
                            i--;
                    }
            }

            int highlights = 0;
            //System.out.println("Patterns before adding: " + individualSearchTerms);
            //add all search terms together
            individualSearchTerms.addAll(quotationSearchTerms);
            //System.out.println("Patterns after adding: " + individualSearchTerms);

            for(String term: individualSearchTerms){

                    pattern = term;
                    int pos = 0;
                    // Search for pattern
                    while ((pos = text.indexOf(pattern, pos)) >= 0) {
                        // Create highlighter using private painter and apply around pattern
                        hilite.addHighlight(pos, pos+pattern.length(), myHighlightPainter);
                        pos += pattern.length();
                        highlights++;
                    }
            }
            btnSearch.setText(DEFAULT_SEARCH_KEY_NAME + " (" + highlights + " matches)");
        } catch (BadLocationException e) {
        }
    }
    
    /* currently selected highlight */
    int highlightIndex = -1;
    /* number of highlights (approximation of remembering previous searches) */
    int numberOfHighlights = 0;
    
    /**
     * Jumps to next highlight or starts from the beginning if at last highlight.
     * @param textComp
     * @throws BadLocationException 
     */
    public void jumpToNextHighlight(JTextArea textComp) throws BadLocationException{
        Highlighter hilite = textComp.getHighlighter();
        Highlighter.Highlight[] hilites = hilite.getHighlights();
        //System.out.println("Number: " + hilites.length);
        //System.out.println("Current index: " + highlightIndex);
        //save number of current highlights
        numberOfHighlights = hilites.length;
        if(hilites.length != 0){
            if(hilites.length == highlightIndex + 1){
                highlightIndex = -1;
            }
            Highlighter.Highlight currentHighlight = hilites[highlightIndex + 1];
            jumpToHighlight(textComp, currentHighlight);
            highlightIndex++;
        }
    }
    
    /**
     * Jumps to updated position of previous highlight (if search 
     * result had not changed), else does nothing.
     * @param textComp 
     */
    private void jumpToUpdatedHighlightPosition(JTextArea textComp){
        Highlighter hilite = textComp.getHighlighter();
        Highlighter.Highlight[] hilites = hilite.getHighlights();
        if(hilites.length != numberOfHighlights){
            return;
        }
        if(highlightIndex != -1){
	        //if no changed number of highlights, update position.
	        jumpToHighlight(textComp, hilites[highlightIndex]);
        }
    }
    
    /**
     * Jumps to specified highlight (in specified text area).
     * @param textComp
     * @param highlight 
     */
    private void jumpToHighlight(JTextArea textComp, Highlighter.Highlight highlight){
        try {
            int startPos = highlight.getStartOffset();
            int endPos = highlight.getEndOffset();
            Rectangle rect = textComp.modelToView(startPos);
            textComp.scrollRectToVisible(rect);
            //System.out.println("Should be at pos." + startPos + ", " + endPos);
            textComp.select(startPos, endPos + 1);
        } catch (BadLocationException ex) {
            System.err.println("Could not jump to next highlighted result.");
        }
    }

    // Removes only our private highlights
    public void removeHighlights(JTextComponent textComp) {
        Highlighter hilite = textComp.getHighlighter();
        Highlighter.Highlight[] hilites = hilite.getHighlights();

        for (int i=0; i<hilites.length; i++) {
            if (hilites[i].getPainter() instanceof MyHighlightPainter) {
                hilite.removeHighlight(hilites[i]);
            }
        }
    }

    Color baseColor = Color.red;
    
    // An instance of the private subclass of the default highlight painter
    Highlighter.HighlightPainter myHighlightPainter = new MyHighlightPainter(new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), 50));

    // A private subclass of the default highlight painter
    class MyHighlightPainter extends DefaultHighlighter.DefaultHighlightPainter {
        public MyHighlightPainter(Color color) {
            super(color);
        }
    }

    /**
    * @param args the command line arguments
    */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
               new PlatformInspectorGui().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnNext;
    private javax.swing.JButton btnRefreshAgentRegistry;
    private javax.swing.JButton btnRefreshDetailView;
    private javax.swing.JButton btnReset;
    private javax.swing.JButton btnSearch;
    private javax.swing.JCheckBox chkAutomaticHighlight;
    private javax.swing.JCheckBox chkCaseSensitive;
    private javax.swing.JCheckBox chkFollowOutputWithScrollbar;
    private javax.swing.JCheckBox chkPrefixClassName;
    private javax.swing.JCheckBox chkRefreshAgentRegistry;
    private javax.swing.JCheckBox chkRefreshDetailsView;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPaneTree;
    private javax.swing.JLabel lblAgentRegistry;
    private javax.swing.JLabel lblDetails;
    private javax.swing.JLabel lblMapEntryLinebreak;
    private javax.swing.JLabel lblRefreshRate;
    private javax.swing.JTextArea tarDetails;
    private javax.swing.JTree treAgentRegistry;
    private javax.swing.JTextField txtMapEntryLineBreak;
    private javax.swing.JTextField txtRefreshFrequency;
    private javax.swing.JTextField txtSearch;
    // End of variables declaration//GEN-END:variables

}
