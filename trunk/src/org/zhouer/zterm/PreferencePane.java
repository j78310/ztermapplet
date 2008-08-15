package org.zhouer.zterm;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeSelectionModel;

import org.zhouer.vt.Config;

/**
 * PreferencePane is an option pane which provides control items which enable
 * user to modify settings for ZTerm applet.
 * 
 * @author h45
 */
public class PreferencePane extends JOptionPane implements
		TreeSelectionListener {
	private static final long serialVersionUID = -1892496769315626958L;

	private ApperancePanel ap;
	private JTree categoryTree;
	private ConnectionPanel cp;
	private FontPanel fp;
	private GeneralPanel gp;
	private final JSplitPane jsp;

	private final Resource resource;
	private DefaultMutableTreeNode rootNode, generalNode, connectionNode,
			appearanceNode, fontNode;

	private final JPanel welcome;

	/**
	 * Constructor with no arguments
	 */
	public PreferencePane() {
		this.resource = Resource.getInstance();

		final JPanel panel = new JPanel();
		panel.setLayout(new GridLayout(1, 1));

		this.makeCategoryTree();
		this.welcome = new JPanel();
		this.welcome.setLayout(new BorderLayout());
		this.welcome
				.add(
						new JLabel(Messages
								.getString("Preference.Welcome_Label_Text")), BorderLayout.NORTH); //$NON-NLS-1$
		final JTextArea welcomeTextArea = new JTextArea(Messages
				.getString("Preference.Welcome_Description_Text")); //$NON-NLS-1$
		final JScrollPane welcomeScrollPane = new JScrollPane(welcomeTextArea);
		welcomeTextArea.setEditable(false);
		this.welcome.add(welcomeScrollPane, BorderLayout.CENTER);

		this.jsp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
				this.categoryTree, this.welcome);

		this.jsp.setOneTouchExpandable(true);
		this.jsp.setDividerLocation(120);

		this.gp = new GeneralPanel(this.resource);
		this.cp = new ConnectionPanel(this.resource);
		this.ap = new ApperancePanel(this.resource);
		this.fp = new FontPanel(this.resource);

		panel.add(this.jsp);

		this.setOptionType(JOptionPane.OK_CANCEL_OPTION);
		this.setMessage(panel);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.swing.JOptionPane#createDialog(java.awt.Component,
	 *      java.lang.String)
	 */
	@Override
	public JDialog createDialog(final Component component, final String title)
			throws HeadlessException {
		this.gp = new GeneralPanel(this.resource);
		this.cp = new ConnectionPanel(this.resource);
		this.ap = new ApperancePanel(this.resource);
		this.fp = new FontPanel(this.resource);
		return super.createDialog(component, title);
	}

	public void valueChanged(final TreeSelectionEvent tse) {

		final DefaultMutableTreeNode node = (DefaultMutableTreeNode) this.categoryTree
				.getLastSelectedPathComponent();

		if (node == null) {
			return;
		}

		if (node == this.generalNode) {
			this.jsp.setRightComponent(this.gp);
		} else if (node == this.connectionNode) {
			this.jsp.setRightComponent(this.cp);
		} else if (node == this.appearanceNode) {
			this.jsp.setRightComponent(this.ap);
		} else if (node == this.fontNode) {
			this.jsp.setRightComponent(this.fp);
		} else {
			this.jsp.setRightComponent(this.welcome);
		}
		this.jsp.setDividerLocation(120);
	}

	protected void submit() {
		this.resource.setValue(Resource.EXTERNAL_BROWSER, this.gp.browserField
				.getText());
		this.resource.setValue(Config.COPY_ON_SELECT,
				this.gp.copyOnSelectCheckBox.isSelected());
		this.resource.setValue(Config.CLEAR_AFTER_COPY,
				this.gp.clearAfterCopyCheckBox.isSelected());
		this.resource.setValue(Resource.REMOVE_MANUAL_DISCONNECT,
				this.gp.removeManualCheckBox.isSelected());
		this.resource.setValue(Config.AUTO_LINE_BREAK,
				this.gp.linebreakCheckBox.isSelected());
		this.resource.setValue(Config.AUTO_LINE_BREAK_LENGTH,
				this.gp.breaklengthModel.getValue().toString());
		this.resource.setValue(Resource.USE_CUSTOM_BELL,
				this.gp.customBellCheckBox.isSelected());
		this.resource.setValue(Resource.CUSTOM_BELL_PATH, this.gp.bellPathField
				.getText());

		this.resource.setValue(Resource.AUTO_RECONNECT,
				this.cp.autoReconnectCheckBox.isSelected());
		this.resource.setValue(Resource.AUTO_RECONNECT_TIME,
				this.cp.reconnectTimeModel.getValue().toString());
		this.resource.setValue(Resource.AUTO_RECONNECT_INTERVAL,
				this.cp.reconnectIntervalModel.getValue().toString());

		this.resource.setValue(Resource.ANTI_IDLE, this.cp.antiIdleCheckBox
				.isSelected());
		this.resource.setValue(Resource.ANTI_IDLE_INTERVAL,
				this.cp.antiIdleModel.getValue().toString());

		// chitsaou.070726: 防閒置字串
		this.resource.setValue(Resource.ANTI_IDLE_STRING,
				this.cp.antiIdleStringField.getText());

		this.resource.setValue(Resource.SYSTEM_LOOK_FEEL,
				this.ap.systemLookFeelCheckBox.isSelected());
		this.resource.setValue(Resource.SHOW_TOOLBAR,
				this.ap.showToolbarCheckBox.isSelected());
		this.resource.setValue(Config.CURSOR_BLINK, this.ap.cursorBlinkCheckBox
				.isSelected());
		this.resource.setValue(Resource.GEOMETRY_WIDTH, this.ap.widthModel
				.getValue().toString());
		this.resource.setValue(Resource.GEOMETRY_HEIGHT, this.ap.heightModel
				.getValue().toString());
		this.resource.setValue(Config.TERMINAL_SCROLLS, this.ap.scrollModel
				.getValue().toString());
		this.resource.setValue(Config.TERMINAL_COLUMNS,
				this.ap.terminalColumnsModel.getValue().toString());
		this.resource.setValue(Config.TERMINAL_ROWS, this.ap.terminalRowsModel
				.getValue().toString());

		// chitsaou.070726: 分頁編號
		// chitsaou.070726: 顯示捲軸
		this.resource.setValue(Resource.TAB_NUMBER, this.ap.tabNumberCheckBox
				.isSelected());
		this.resource.setValue(Resource.SHOW_SCROLL_BAR,
				this.ap.showScrollBarCheckBox.isSelected());

		this.resource.setValue(Config.FONT_FAMILY, this.fp.familyCombo
				.getSelectedItem().toString());
		this.resource.setValue(Config.FONT_SIZE, this.fp.sizeModel.getValue()
				.toString());
		this.resource
				.setValue(Config.FONT_BOLD, this.fp.boldCheck.isSelected());
		this.resource.setValue(Config.FONT_ITALY, this.fp.italyCheck
				.isSelected());
		this.resource.setValue(Config.FONT_ANTIALIAS, this.fp.aaCheck
				.isSelected());
		this.resource.setValue(Config.FONT_VERTICLAL_GAP,
				this.fp.fontVerticalGapModel.getValue().toString());
		this.resource.setValue(Config.FONT_HORIZONTAL_GAP,
				this.fp.fontHorizontalGapModel.getValue().toString());
		this.resource.setValue(Config.FONT_DESCENT_ADJUST,
				this.fp.fontDescentAdjustModel.getValue().toString());

		// 將修改寫回設定檔
		this.resource.writeFile();

		Model.getInstance().updateLookAndFeel();
		Model.getInstance().updateBounds();
		Model.getInstance().updateToolbar();
		Model.getInstance().updateSize();
		Model.getInstance().updateAntiIdleTime();
	}

	private void makeCategoryTree() {
		this.rootNode = new DefaultMutableTreeNode(Messages
				.getString("Preference.Tree_RootNode_Text")); //$NON-NLS-1$
		this.generalNode = new DefaultMutableTreeNode(Messages
				.getString("Preference.Tree_GeneralNode_Text")); //$NON-NLS-1$
		this.connectionNode = new DefaultMutableTreeNode(Messages
				.getString("Preference.Tree_ConnectionNode_Text")); //$NON-NLS-1$
		this.appearanceNode = new DefaultMutableTreeNode(Messages
				.getString("Preference.Tree_AppearanceNode_Text")); //$NON-NLS-1$
		this.fontNode = new DefaultMutableTreeNode(Messages
				.getString("Preference.Tree_FontNode_Text")); //$NON-NLS-1$

		this.rootNode.add(this.generalNode);
		this.rootNode.add(this.connectionNode);
		this.rootNode.add(this.appearanceNode);
		this.rootNode.add(this.fontNode);

		this.categoryTree = new JTree(this.rootNode);
		this.categoryTree.getSelectionModel().setSelectionMode(
				TreeSelectionModel.SINGLE_TREE_SELECTION);
		this.categoryTree.addTreeSelectionListener(this);
	}
}

class ApperancePanel extends JPanel {
	private static final long serialVersionUID = -2051345281384271839L;

	public JLabel scrollLabel, terminalRowsLabel, terminalColumnsLabel;

	public SpinnerNumberModel scrollModel, terminalRowsModel,
			terminalColumnsModel;
	public JSpinner scrollSpinner, terminalRowsSpinner, terminalColumnsSpinner;

	public JCheckBox systemLookFeelCheckBox, showToolbarCheckBox,
			cursorBlinkCheckBox;
	public JLabel systemLookFeelLabel, showToolbarLabel, cursorBlinkLabel;

	public JCheckBox tabNumberCheckBox, showScrollBarCheckBox;
	// chitsaou.070726: 分頁編號
	// chitsaou.070726: 顯示捲軸
	public JLabel tabNumberLabel, showScrollBarLabel;
	public JLabel widthLabel, heightLabel;

	public SpinnerNumberModel widthModel, heightModel;
	public JSpinner widthSpinner, heightSpinner;
	private final Resource resource;

	public ApperancePanel(final Resource r) {
		super();
		this.resource = r;

		this.systemLookFeelLabel = new JLabel(Messages
				.getString("Preference.SystemLookFeel_Label_Text")); //$NON-NLS-1$
		this.systemLookFeelCheckBox = new JCheckBox();
		this.systemLookFeelCheckBox.setSelected(this.resource
				.getBooleanValue(Resource.SYSTEM_LOOK_FEEL));

		this.systemLookFeelCheckBox.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent actionEvent) {
				Resource.getInstance()
						.setValue(
								Resource.SYSTEM_LOOK_FEEL,
								ApperancePanel.this.systemLookFeelCheckBox
										.isSelected());
				Model.getInstance().updateLookAndFeel();
				new Thread() {
					@Override
					public void run() {
						final int option = JOptionPane
								.showConfirmDialog(
										ApperancePanel.this,
										Messages
												.getString("PreferencePane.ConfirmLookFeel_Text"), Messages.getString("PreferencePane.ConfirmLookFeel_Title"), JOptionPane.YES_NO_OPTION); //$NON-NLS-1$ //$NON-NLS-2$
						if (option == JOptionPane.NO_OPTION) {
							ApperancePanel.this.systemLookFeelCheckBox
									.setSelected(!ApperancePanel.this.systemLookFeelCheckBox
											.isSelected());
							Resource.getInstance().setValue(
									Resource.SYSTEM_LOOK_FEEL,
									ApperancePanel.this.systemLookFeelCheckBox
											.isSelected());
							Model.getInstance().updateLookAndFeel();
						}
					}
				}.start();
			}
		});

		this.showToolbarLabel = new JLabel(Messages
				.getString("Preference.ShowToolbar_Label_Text")); //$NON-NLS-1$
		this.showToolbarCheckBox = new JCheckBox();
		this.showToolbarCheckBox.setSelected(this.resource
				.getBooleanValue(Resource.SHOW_TOOLBAR));

		this.cursorBlinkLabel = new JLabel(Messages
				.getString("Preference.CursorBlink_Label_Text")); //$NON-NLS-1$
		this.cursorBlinkCheckBox = new JCheckBox();
		this.cursorBlinkCheckBox.setSelected(this.resource
				.getBooleanValue(Config.CURSOR_BLINK));

		this.widthLabel = new JLabel(Messages
				.getString("Preference.WindowWidth_Label_Text")); //$NON-NLS-1$
		this.widthModel = new SpinnerNumberModel(this.resource
				.getIntValue(Resource.GEOMETRY_WIDTH), 0, 4096, 1);
		this.widthSpinner = new JSpinner(this.widthModel);

		this.heightLabel = new JLabel(Messages
				.getString("Preference.WindowHeight_Label_Text")); //$NON-NLS-1$
		this.heightModel = new SpinnerNumberModel(this.resource
				.getIntValue(Resource.GEOMETRY_HEIGHT), 0, 4096, 1);
		this.heightSpinner = new JSpinner(this.heightModel);

		this.scrollLabel = new JLabel(Messages
				.getString("Preference.Scroll_Label_Text")); //$NON-NLS-1$
		this.scrollModel = new SpinnerNumberModel(this.resource
				.getIntValue(Config.TERMINAL_SCROLLS), 0, 10000, 1);
		this.scrollSpinner = new JSpinner(this.scrollModel);

		this.terminalColumnsLabel = new JLabel(Messages
				.getString("Preference.TerminalColumns_Label_Text")); //$NON-NLS-1$
		this.terminalColumnsModel = new SpinnerNumberModel(this.resource
				.getIntValue(Config.TERMINAL_COLUMNS), 80, 200, 1);
		this.terminalColumnsSpinner = new JSpinner(this.terminalColumnsModel);
		this.terminalColumnsSpinner.setEnabled(false);

		this.terminalRowsLabel = new JLabel(Messages
				.getString("Preference.TerminalRows_Label_Text")); //$NON-NLS-1$
		this.terminalRowsModel = new SpinnerNumberModel(this.resource
				.getIntValue(Config.TERMINAL_ROWS), 24, 200, 1);
		this.terminalRowsSpinner = new JSpinner(this.terminalRowsModel);
		this.terminalRowsSpinner.setEnabled(false);

		// chitsaou.070726: 分頁編號
		this.tabNumberLabel = new JLabel(Messages
				.getString("Preference.TabNumber_Label_Text")); //$NON-NLS-1$
		this.tabNumberCheckBox = new JCheckBox();
		this.tabNumberCheckBox.setSelected(this.resource
				.getBooleanValue(Resource.TAB_NUMBER));

		// chitsaou.070726: 顯示捲軸
		this.showScrollBarLabel = new JLabel(Messages
				.getString("Preference.ShowScrollBar_Label_Text")); //$NON-NLS-1$
		this.showScrollBarCheckBox = new JCheckBox();
		this.showScrollBarCheckBox.setSelected(this.resource
				.getBooleanValue(Resource.SHOW_SCROLL_BAR));

		this.setLayout(new GridBagLayout());
		final GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.WEST;

		c.gridx = 0;
		c.gridy = 0;
		this.add(this.systemLookFeelLabel, c);
		c.gridx = 1;
		this.add(this.systemLookFeelCheckBox, c);

		c.gridx = 0;
		c.gridy = 1;
		this.add(this.showToolbarLabel, c);
		c.gridx = 1;
		this.add(this.showToolbarCheckBox, c);

		c.gridx = 0;
		c.gridy = 2;
		this.add(this.cursorBlinkLabel, c);
		c.gridx = 1;
		this.add(this.cursorBlinkCheckBox, c);

		c.gridx = 0;
		c.gridy = 3;
		this.add(this.widthLabel, c);
		c.gridx = 1;
		this.add(this.widthSpinner, c);

		c.gridx = 0;
		c.gridy = 4;
		this.add(this.heightLabel, c);
		c.gridx = 1;
		this.add(this.heightSpinner, c);

		c.gridx = 0;
		c.gridy = 5;
		this.add(this.scrollLabel, c);
		c.gridx = 1;
		this.add(this.scrollSpinner, c);

		c.gridx = 0;
		c.gridy = 6;
		this.add(this.terminalColumnsLabel, c);
		c.gridx = 1;
		this.add(this.terminalColumnsSpinner, c);

		c.gridx = 0;
		c.gridy = 7;
		this.add(this.terminalRowsLabel, c);
		c.gridx = 1;
		this.add(this.terminalRowsSpinner, c);

		// chitsaou.070726: 分頁編號
		c.gridx = 0;
		c.gridy = 8;
		this.add(this.tabNumberLabel, c);
		c.gridx = 1;
		this.add(this.tabNumberCheckBox, c);

		// chitsaou.070726: 顯示捲軸
		c.gridx = 0;
		c.gridy = 9;
		this.add(this.showScrollBarLabel, c);
		c.gridx = 1;
		this.add(this.showScrollBarCheckBox, c);
	}
}

class ConnectionPanel extends JPanel implements ActionListener {
	private static final long serialVersionUID = 5706390056762240339L;

	public JCheckBox antiIdleCheckBox;

	// chitsaou.070726: 防閒置字串
	public JLabel antiIdleLabel, antiIdleTimeLabel, antiIdleStringLabel;
	public SpinnerNumberModel antiIdleModel;
	public JTextField antiIdleStringField;
	public JSpinner antiIdleTimeSpinner;

	public JCheckBox autoReconnectCheckBox;
	public JLabel autoReconnectLabel, reconnectTimeLabel,
			reconnectIntervalLabel;
	public SpinnerNumberModel reconnectTimeModel, reconnectIntervalModel;
	public JSpinner reconnectTimeSpinner, reconnectIntervalSpinner;
	private final Resource resource;

	public ConnectionPanel(final Resource r) {
		super();
		this.resource = r;

		final boolean autoReconnect = this.resource
				.getBooleanValue(Resource.AUTO_RECONNECT);
		this.autoReconnectLabel = new JLabel(Messages
				.getString("Preference.AutoReconnect_Label_Text")); //$NON-NLS-1$
		this.autoReconnectCheckBox = new JCheckBox();
		this.autoReconnectCheckBox.setSelected(autoReconnect);
		this.autoReconnectCheckBox.addActionListener(this);

		this.reconnectTimeLabel = new JLabel(Messages
				.getString("Preference.ReconnectTime_Label_Text")); //$NON-NLS-1$
		this.reconnectTimeModel = new SpinnerNumberModel(this.resource
				.getIntValue(Resource.AUTO_RECONNECT_TIME), 0, 3600, 1);
		this.reconnectTimeSpinner = new JSpinner(this.reconnectTimeModel);
		this.reconnectTimeSpinner.setEnabled(autoReconnect);

		this.reconnectIntervalLabel = new JLabel(Messages
				.getString("Preference.ReconnectInterval_Label_Text")); //$NON-NLS-1$
		this.reconnectIntervalModel = new SpinnerNumberModel(this.resource
				.getIntValue(Resource.AUTO_RECONNECT_INTERVAL), 0, 60000, 1);
		this.reconnectIntervalSpinner = new JSpinner(
				this.reconnectIntervalModel);
		this.reconnectIntervalSpinner.setEnabled(autoReconnect);

		this.antiIdleLabel = new JLabel(Messages
				.getString("Preference.AntiIdle_Label_Text")); //$NON-NLS-1$
		this.antiIdleCheckBox = new JCheckBox();
		this.antiIdleCheckBox.setSelected(this.resource
				.getBooleanValue(Resource.ANTI_IDLE));
		this.antiIdleCheckBox.addActionListener(this);

		this.antiIdleTimeLabel = new JLabel(Messages
				.getString("Preference.AntiIdleTime_Label_Text")); //$NON-NLS-1$
		this.antiIdleModel = new SpinnerNumberModel(this.resource
				.getIntValue(Resource.ANTI_IDLE_INTERVAL), 0, 3600, 1);
		this.antiIdleTimeSpinner = new JSpinner(this.antiIdleModel);
		this.antiIdleTimeSpinner.setEnabled(this.resource
				.getBooleanValue(Resource.ANTI_IDLE));

		// chitsaou.070726: 防閒置字串
		this.antiIdleStringLabel = new JLabel(Messages
				.getString("Preference.AntiIdleString_Label_Text")); //$NON-NLS-1$
		this.antiIdleStringField = new JTextField(this.resource
				.getStringValue(Resource.ANTI_IDLE_STRING), 15);

		this.setLayout(new GridBagLayout());
		final GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.WEST;

		c.gridx = 0;
		c.gridy = 0;
		this.add(this.autoReconnectLabel, c);
		c.gridx = 1;
		this.add(this.autoReconnectCheckBox, c);

		c.gridx = 0;
		c.gridy = 1;
		this.add(this.reconnectTimeLabel, c);
		c.gridx = 1;
		this.add(this.reconnectTimeSpinner, c);

		c.gridx = 0;
		c.gridy = 2;
		this.add(this.reconnectIntervalLabel, c);
		c.gridx = 1;
		this.add(this.reconnectIntervalSpinner, c);

		c.gridx = 0;
		c.gridy = 3;
		this.add(this.antiIdleLabel, c);
		c.gridx = 1;
		this.add(this.antiIdleCheckBox, c);

		c.gridx = 0;
		c.gridy = 4;
		this.add(this.antiIdleTimeLabel, c);
		c.gridx = 1;
		this.add(this.antiIdleTimeSpinner, c);

		// chitsaou.070726: 防閒置字串
		c.gridx = 0;
		c.gridy = 5;
		this.add(this.antiIdleStringLabel, c);
		c.gridx = 1;
		this.add(this.antiIdleStringField, c);
	}

	public void actionPerformed(final ActionEvent ae) {
		if (ae.getSource() == this.autoReconnectCheckBox) {
			this.reconnectTimeSpinner.setEnabled(this.autoReconnectCheckBox
					.isSelected());
			this.reconnectIntervalSpinner.setEnabled(this.autoReconnectCheckBox
					.isSelected());
		} else if (ae.getSource() == this.antiIdleCheckBox) {
			this.antiIdleTimeSpinner.setEnabled(this.antiIdleCheckBox
					.isSelected());
		}
	}
}

class FontPanel extends JPanel {
	private static final long serialVersionUID = 1511310874988772350L;

	public JCheckBox boldCheck, italyCheck, aaCheck;

	public JLabel boldLabel, italyLabel, aaLabel;
	public JComboBox familyCombo;

	public JLabel familyLabel;
	public JLabel fontVerticalGapLabel, fontHorizontalGapLabel,
			fontDescentAdjustLabel;
	public SpinnerNumberModel fontVerticalGapModel, fontHorizontalGapModel,
			fontDescentAdjustModel;

	public JSpinner fontVerticalGapSpinner, fontHorizontalGapSpinner,
			fontDescentAdjustSpinner;
	public JLabel sizeLabel;

	public SpinnerNumberModel sizeModel;
	public JSpinner sizeSpinner;
	private final Resource resource;

	public FontPanel(final Resource r) {
		super();
		this.resource = r;

		this.familyLabel = new JLabel(Messages
				.getString("Preference.FontFamily_Label_Text")); //$NON-NLS-1$
		final GraphicsEnvironment ge = GraphicsEnvironment
				.getLocalGraphicsEnvironment();
		final String[] familyList = ge.getAvailableFontFamilyNames();
		this.familyCombo = new JComboBox(familyList);
		this.familyCombo.setSelectedItem(this.resource
				.getStringValue(Config.FONT_FAMILY));

		this.sizeLabel = new JLabel(Messages
				.getString("Preference.FontSize_Label_Text")); //$NON-NLS-1$
		this.sizeModel = new SpinnerNumberModel(this.resource
				.getIntValue(Config.FONT_SIZE), 0, 64, 1);
		this.sizeSpinner = new JSpinner(this.sizeModel);

		this.boldLabel = new JLabel(Messages
				.getString("Preference.FontBold_Label_Text")); //$NON-NLS-1$
		this.boldCheck = new JCheckBox();
		this.boldCheck.setSelected(this.resource
				.getBooleanValue(Config.FONT_BOLD));

		this.italyLabel = new JLabel(Messages
				.getString("Preference.FontItaly_Label_Text")); //$NON-NLS-1$
		this.italyCheck = new JCheckBox();
		this.italyCheck.setSelected(this.resource
				.getBooleanValue(Config.FONT_ITALY));

		this.aaLabel = new JLabel(Messages
				.getString("Preference.FontAntiAliasing_Label_Text")); //$NON-NLS-1$
		this.aaCheck = new JCheckBox();
		this.aaCheck.setSelected(this.resource
				.getBooleanValue(Config.FONT_ANTIALIAS));

		this.fontVerticalGapLabel = new JLabel(Messages
				.getString("Preference.FontVerticalGap_Label_Text")); //$NON-NLS-1$
		this.fontVerticalGapModel = new SpinnerNumberModel(this.resource
				.getIntValue(Config.FONT_VERTICLAL_GAP), -10, 10, 1);
		this.fontVerticalGapSpinner = new JSpinner(this.fontVerticalGapModel);

		this.fontHorizontalGapLabel = new JLabel(Messages
				.getString("Preference.FontHorizontalGap_Label_Text")); //$NON-NLS-1$
		this.fontHorizontalGapModel = new SpinnerNumberModel(this.resource
				.getIntValue(Config.FONT_HORIZONTAL_GAP), -10, 10, 1);
		this.fontHorizontalGapSpinner = new JSpinner(
				this.fontHorizontalGapModel);

		this.fontDescentAdjustLabel = new JLabel(Messages
				.getString("Preference.FontDescentAdjust_Label_Text")); //$NON-NLS-1$
		this.fontDescentAdjustModel = new SpinnerNumberModel(this.resource
				.getIntValue(Config.FONT_DESCENT_ADJUST), -10, 10, 1);
		this.fontDescentAdjustSpinner = new JSpinner(
				this.fontDescentAdjustModel);

		this.setLayout(new GridBagLayout());
		final GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.WEST;

		c.gridx = 0;
		c.gridy = 0;
		this.add(this.familyLabel, c);
		c.gridx = 1;
		this.add(this.familyCombo, c);

		c.gridx = 0;
		c.gridy = 1;
		this.add(this.sizeLabel, c);
		c.gridx = 1;
		this.add(this.sizeSpinner, c);

		c.gridx = 0;
		c.gridy = 2;
		this.add(this.boldLabel, c);
		c.gridx = 1;
		this.add(this.boldCheck, c);

		c.gridx = 0;
		c.gridy = 3;
		this.add(this.italyLabel, c);
		c.gridx = 1;
		this.add(this.italyCheck, c);

		c.gridx = 0;
		c.gridy = 4;
		this.add(this.aaLabel, c);
		c.gridx = 1;
		this.add(this.aaCheck, c);

		c.gridx = 0;
		c.gridy = 5;
		this.add(this.fontVerticalGapLabel, c);
		c.gridx = 1;
		this.add(this.fontVerticalGapSpinner, c);

		c.gridx = 0;
		c.gridy = 6;
		this.add(this.fontHorizontalGapLabel, c);
		c.gridx = 1;
		this.add(this.fontHorizontalGapSpinner, c);

		c.gridx = 0;
		c.gridy = 7;
		this.add(this.fontDescentAdjustLabel, c);
		c.gridx = 1;
		this.add(this.fontDescentAdjustSpinner, c);
	}
}

class GeneralPanel extends JPanel implements ActionListener {
	private static final long serialVersionUID = 290521402254313069L;

	public JButton bellPathButton;

	public JTextField bellPathField;
	public JLabel bellPathLabel;

	public JLabel breaklengthLabel;
	public SpinnerNumberModel breaklengthModel;

	public JSpinner breaklengthSpinner;
	public JTextField browserField;
	public JLabel browserLabel;

	public JCheckBox copyOnSelectCheckBox, clearAfterCopyCheckBox,
			removeManualCheckBox, linebreakCheckBox;
	public JLabel copyOnSelectLabel, clearAfterCopyLabel, removeManualLabel,
			linebreakLabel;

	public JCheckBox customBellCheckBox;
	public JLabel customBellLabel;
	private JFileChooser jfc;

	private String parentDirectory;
	private final Resource resource;
	private File selectedFile;

	public GeneralPanel(final Resource r) {
		super();
		this.resource = r;

		this.browserLabel = new JLabel(Messages
				.getString("Preference.BrowserCommand_Label_Text")); //$NON-NLS-1$
		this.browserField = new JTextField(this.resource
				.getStringValue(Resource.EXTERNAL_BROWSER), 20);

		this.copyOnSelectLabel = new JLabel(Messages
				.getString("Preference.CopyOnSelect_Label_Text")); //$NON-NLS-1$
		this.copyOnSelectCheckBox = new JCheckBox();
		this.copyOnSelectCheckBox.setSelected(this.resource
				.getBooleanValue(Config.COPY_ON_SELECT));

		this.clearAfterCopyLabel = new JLabel(Messages
				.getString("Preference.ClearAfterCopy_Label_Text")); //$NON-NLS-1$
		this.clearAfterCopyCheckBox = new JCheckBox();
		this.clearAfterCopyCheckBox.setSelected(this.resource
				.getBooleanValue(Config.CLEAR_AFTER_COPY));

		this.removeManualLabel = new JLabel(Messages
				.getString("Preference.RemoveManual_Label_Text")); //$NON-NLS-1$
		this.removeManualCheckBox = new JCheckBox();
		this.removeManualCheckBox.setSelected(this.resource
				.getBooleanValue(Resource.REMOVE_MANUAL_DISCONNECT));

		this.linebreakLabel = new JLabel(Messages
				.getString("Preference.LineBreak_Label_Text")); //$NON-NLS-1$
		this.linebreakCheckBox = new JCheckBox();
		this.linebreakCheckBox.setSelected(this.resource
				.getBooleanValue(Config.AUTO_LINE_BREAK));
		this.linebreakCheckBox.addActionListener(this);

		this.breaklengthLabel = new JLabel(Messages
				.getString("Preference.BreakLength_Label_Text")); //$NON-NLS-1$
		this.breaklengthModel = new SpinnerNumberModel(this.resource
				.getIntValue(Config.AUTO_LINE_BREAK_LENGTH), 1, 512, 1);
		this.breaklengthSpinner = new JSpinner(this.breaklengthModel);
		this.breaklengthSpinner.setEnabled(this.resource
				.getBooleanValue(Config.AUTO_LINE_BREAK));

		this.customBellLabel = new JLabel(Messages
				.getString("Preference.CustomBell_Label_Text")); //$NON-NLS-1$
		this.customBellCheckBox = new JCheckBox();
		this.customBellCheckBox.setSelected(this.resource
				.getBooleanValue(Resource.USE_CUSTOM_BELL));
		this.customBellCheckBox.addActionListener(this);

		this.bellPathLabel = new JLabel(Messages
				.getString("Preference.BellPath_Label_Text")); //$NON-NLS-1$
		this.bellPathField = new JTextField(this.resource
				.getStringValue(Resource.CUSTOM_BELL_PATH), 8);
		this.bellPathField.setEnabled(this.resource
				.getBooleanValue(Resource.USE_CUSTOM_BELL));
		this.bellPathButton = new JButton(Messages
				.getString("Preference.BellPath_Button_Text")); //$NON-NLS-1$
		this.bellPathButton.setEnabled(this.resource
				.getBooleanValue(Resource.USE_CUSTOM_BELL));
		this.bellPathButton.addActionListener(this);

		this.setLayout(new GridBagLayout());
		final GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.WEST;

		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 3;
		this.add(this.browserLabel, c);

		c.gridx = 0;
		c.gridy = 1;
		c.gridwidth = 3;
		this.add(this.browserField, c);

		c.gridwidth = 1;

		c.gridx = 0;
		c.gridy = 2;
		this.add(this.copyOnSelectLabel, c);
		c.gridx = 1;
		this.add(this.copyOnSelectCheckBox, c);

		c.gridx = 0;
		c.gridy = 3;
		this.add(this.clearAfterCopyLabel, c);
		c.gridx = 1;
		this.add(this.clearAfterCopyCheckBox, c);

		c.gridx = 0;
		c.gridy = 4;
		this.add(this.removeManualLabel, c);
		c.gridx = 1;
		this.add(this.removeManualCheckBox, c);

		c.gridx = 0;
		c.gridy = 5;
		this.add(this.linebreakLabel, c);
		c.gridx = 1;
		this.add(this.linebreakCheckBox, c);

		c.gridx = 0;
		c.gridy = 6;
		this.add(this.breaklengthLabel, c);
		c.gridx = 1;
		this.add(this.breaklengthSpinner, c);

		c.gridx = 0;
		c.gridy = 7;
		this.add(this.customBellLabel, c);
		c.gridx = 1;
		this.add(this.customBellCheckBox, c);

		c.gridx = 0;
		c.gridy = 8;
		this.add(this.bellPathLabel, c);
		c.gridx = 1;
		this.add(this.bellPathField, c);
		c.gridx = 2;
		this.add(this.bellPathButton, c);
	}

	public void actionPerformed(final ActionEvent ae) {
		if (ae.getSource() == this.linebreakCheckBox) {
			this.breaklengthSpinner.setEnabled(this.linebreakCheckBox
					.isSelected());
		} else if (ae.getSource() == this.customBellCheckBox) {
			this.bellPathField.setEnabled(this.customBellCheckBox.isSelected());
			this.bellPathButton
					.setEnabled(this.customBellCheckBox.isSelected());
		} else if (ae.getSource() == this.bellPathButton) {

			if (this.parentDirectory != null) {
				this.jfc = new JFileChooser(this.parentDirectory);
			} else {
				this.jfc = new JFileChooser();
			}

			if (this.jfc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
				this.selectedFile = this.jfc.getSelectedFile();
				this.parentDirectory = this.selectedFile.getParent();
				this.bellPathField.setText(this.selectedFile.getAbsolutePath());
			}
		}
	}
}
