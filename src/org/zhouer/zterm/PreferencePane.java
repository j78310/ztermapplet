package org.zhouer.zterm;

import java.awt.BorderLayout;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
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
 * @author Chin-Chang Yang
 */
public class PreferencePane extends JOptionPane implements
		TreeSelectionListener {
	private static final long serialVersionUID = -1892496769315626958L;

	protected ApperancePanel apperancePanel;
	private JTree categoryTree;
	private final ConnectionPanel connectionPanel;
	private final FontPanel fontPanel;
	private final GeneralPanel generalPanel;
	private final JSplitPane splitPanel;

	private final Resource resource;
	private DefaultMutableTreeNode rootNode, generalNode, connectionNode,
			appearanceNode, fontNode;

	private final JPanel welcomePanel;
	
	private final JLabel welcomeLabel;
	
	private final JTextArea welcomeTextArea;
	
	private final JScrollPane welcomeScrollPane;

	/**
	 * Constructor with no arguments
	 */
	public PreferencePane() {
		resource = Resource.getInstance();

		final JPanel panel = new JPanel();
		panel.setLayout(new GridLayout(1, 1));

		makeCategoryTree();
		welcomeLabel = new JLabel(Messages
				.getString("Preference.Welcome_Label_Text"));
		welcomePanel = new JPanel();
		welcomePanel.setLayout(new BorderLayout());
		welcomePanel.add(welcomeLabel, BorderLayout.NORTH); //$NON-NLS-1$
		welcomeTextArea = new JTextArea(Messages
				.getString("Preference.Welcome_Description_Text")); //$NON-NLS-1$
		welcomeScrollPane = new JScrollPane(welcomeTextArea);
		welcomeTextArea.setEditable(false);
		welcomePanel.add(welcomeScrollPane, BorderLayout.CENTER);

		splitPanel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, categoryTree, welcomePanel);

		splitPanel.setOneTouchExpandable(true);
		splitPanel.setDividerLocation(120);

		generalPanel = new GeneralPanel(resource);
		connectionPanel = new ConnectionPanel(resource);
		apperancePanel = new ApperancePanel(resource);
		fontPanel = new FontPanel(resource);

		panel.add(splitPanel);

		setOptionType(JOptionPane.OK_CANCEL_OPTION);
		setMessage(panel);
	}

	public void valueChanged(final TreeSelectionEvent tse) {

		final DefaultMutableTreeNode node = (DefaultMutableTreeNode) categoryTree
				.getLastSelectedPathComponent();

		if (node == null) {
			return;
		}

		if (node == generalNode) {
			splitPanel.setRightComponent(generalPanel);
		} else if (node == connectionNode) {
			splitPanel.setRightComponent(connectionPanel);
		} else if (node == appearanceNode) {
			splitPanel.setRightComponent(apperancePanel);
		} else if (node == fontNode) {
			splitPanel.setRightComponent(fontPanel);
		} else {
			splitPanel.setRightComponent(welcomePanel);
		}
		splitPanel.setDividerLocation(120);
	}
	
	public void refreshText() {
		refreshTreeNodeText();
		refreshWelcomeText();
		apperancePanel.refreshText();
		connectionPanel.refreshText();
		fontPanel.refreshText();
		generalPanel.refreshText();
	}
	
	/**
	 * Refresh the text in the tree node.
	 */
	private void refreshTreeNodeText() {
		rootNode.setUserObject(Messages
				.getString("Preference.Tree_RootNode_Text")); //$NON-NLS-1$
		generalNode.setUserObject(Messages
				.getString("Preference.Tree_GeneralNode_Text")); //$NON-NLS-1$
		connectionNode.setUserObject(Messages
				.getString("Preference.Tree_ConnectionNode_Text")); //$NON-NLS-1$
		appearanceNode.setUserObject(Messages
				.getString("Preference.Tree_AppearanceNode_Text")); //$NON-NLS-1$
		fontNode.setUserObject(Messages
				.getString("Preference.Tree_FontNode_Text")); //$NON-NLS-1$
	}

	private void refreshWelcomeText() {
		welcomeLabel.setText(Messages
				.getString("Preference.Welcome_Label_Text"));
		welcomeTextArea.setText(Messages
				.getString("Preference.Welcome_Description_Text"));
	}
	
	public void reloadSettings() {
		resource.readRcFile();
		generalPanel.browserField.setText(resource.getStringValue(Resource.EXTERNAL_BROWSER));
		generalPanel.copyOnSelectCheckBox.setSelected(resource.getBooleanValue(Config.COPY_ON_SELECT));
		generalPanel.clearAfterCopyCheckBox.setSelected(resource.getBooleanValue(Config.CLEAR_AFTER_COPY));
		generalPanel.removeManualCheckBox.setSelected(resource.getBooleanValue(Resource.REMOVE_MANUAL_DISCONNECT));
		generalPanel.linebreakCheckBox.setSelected(resource.getBooleanValue(Config.AUTO_LINE_BREAK));
		generalPanel.breaklengthModel.setValue(resource.getIntValue(Config.AUTO_LINE_BREAK_LENGTH));
		generalPanel.customBellCheckBox.setSelected(resource.getBooleanValue(Resource.USE_CUSTOM_BELL));
		generalPanel.bellPathField.setText(resource.getStringValue(Resource.CUSTOM_BELL_PATH));
		generalPanel.settingFileField.setText(resource.getStringValue(Resource.RESOURCE_LOCATION));
		connectionPanel.autoReconnectCheckBox.setSelected(resource.getBooleanValue(Resource.AUTO_RECONNECT));
		connectionPanel.reconnectTimeModel.setValue(resource.getIntValue(Resource.AUTO_RECONNECT_TIME));
		connectionPanel.reconnectIntervalModel.setValue(resource.getIntValue(Resource.AUTO_RECONNECT_INTERVAL));
		connectionPanel.antiIdleCheckBox.setSelected(resource.getBooleanValue(Resource.ANTI_IDLE));
		connectionPanel.antiIdleModel.setValue(resource.getIntValue(Resource.ANTI_IDLE_INTERVAL));
		connectionPanel.antiIdleStringField.setText(resource.getStringValue(Resource.ANTI_IDLE_STRING));
		apperancePanel.systemLookFeelCheckBox.setSelected(resource.getBooleanValue(Resource.SYSTEM_LOOK_FEEL));
		apperancePanel.showToolbarCheckBox.setSelected(resource.getBooleanValue(Resource.SHOW_TOOLBAR));
		apperancePanel.cursorBlinkCheckBox.setSelected(resource.getBooleanValue(Config.CURSOR_BLINK));
		apperancePanel.widthModel.setValue(resource.getIntValue(Resource.GEOMETRY_WIDTH));
		apperancePanel.heightModel.setValue(resource.getIntValue(Resource.GEOMETRY_HEIGHT));
		apperancePanel.scrollModel.setValue(resource.getIntValue(Config.TERMINAL_SCROLLS));
		apperancePanel.terminalColumnsModel.setValue(resource.getIntValue(Config.TERMINAL_COLUMNS));
		apperancePanel.terminalRowsModel.setValue(resource.getIntValue(Config.TERMINAL_ROWS));
		apperancePanel.tabNumberCheckBox.setSelected(resource.getBooleanValue(Resource.TAB_NUMBER));
		apperancePanel.showScrollBarCheckBox.setSelected(resource.getBooleanValue(Resource.SHOW_SCROLL_BAR));
		fontPanel.familyCombo.setSelectedItem(resource.getStringValue(Config.FONT_FAMILY));
		fontPanel.sizeModel.setValue(resource.getIntValue(Config.FONT_SIZE));
		fontPanel.boldCheck.setSelected(resource.getBooleanValue(Config.FONT_BOLD));
		fontPanel.italyCheck.setSelected(resource.getBooleanValue(Config.FONT_ITALY));
		fontPanel.aaCheck.setSelected(resource.getBooleanValue(Config.FONT_ANTIALIAS));
		fontPanel.fontVerticalGapModel.setValue(resource.getIntValue(Config.FONT_VERTICLAL_GAP));
		fontPanel.fontHorizontalGapModel.setValue(resource.getIntValue(Config.FONT_HORIZONTAL_GAP));
		fontPanel.fontDescentAdjustModel.setValue(resource.getIntValue(Config.FONT_DESCENT_ADJUST));
	}

	protected void submit() {
		resource.setValue(Resource.EXTERNAL_BROWSER, generalPanel.browserField.getText());
		resource.setValue(Config.COPY_ON_SELECT, generalPanel.copyOnSelectCheckBox
				.isSelected());
		resource.setValue(Config.CLEAR_AFTER_COPY, generalPanel.clearAfterCopyCheckBox
				.isSelected());
		resource.setValue(Resource.REMOVE_MANUAL_DISCONNECT,
				generalPanel.removeManualCheckBox.isSelected());
		resource.setValue(Config.AUTO_LINE_BREAK, generalPanel.linebreakCheckBox
				.isSelected());
		resource.setValue(Config.AUTO_LINE_BREAK_LENGTH, generalPanel.breaklengthModel
				.getValue().toString());
		resource.setValue(Resource.USE_CUSTOM_BELL, generalPanel.customBellCheckBox
				.isSelected());
		resource
				.setValue(Resource.CUSTOM_BELL_PATH, generalPanel.bellPathField.getText());
		resource.setValue(Resource.RESOURCE_LOCATION, generalPanel.settingFileField.getText());

		resource.setValue(Resource.AUTO_RECONNECT, connectionPanel.autoReconnectCheckBox
				.isSelected());
		resource.setValue(Resource.AUTO_RECONNECT_TIME, connectionPanel.reconnectTimeModel
				.getValue().toString());
		resource.setValue(Resource.AUTO_RECONNECT_INTERVAL,
				connectionPanel.reconnectIntervalModel.getValue().toString());

		resource.setValue(Resource.ANTI_IDLE, connectionPanel.antiIdleCheckBox.isSelected());
		resource.setValue(Resource.ANTI_IDLE_INTERVAL, connectionPanel.antiIdleModel
				.getValue().toString());

		// chitsaou.070726: 防閒置字串
		resource.setValue(Resource.ANTI_IDLE_STRING, connectionPanel.antiIdleStringField
				.getText());

		resource.setValue(Resource.SYSTEM_LOOK_FEEL, apperancePanel.systemLookFeelCheckBox
				.isSelected());
		resource.setValue(Resource.SHOW_TOOLBAR, apperancePanel.showToolbarCheckBox
				.isSelected());
		resource.setValue(Config.CURSOR_BLINK, apperancePanel.cursorBlinkCheckBox
				.isSelected());
		resource.setValue(Resource.GEOMETRY_WIDTH, apperancePanel.widthModel.getValue()
				.toString());
		resource.setValue(Resource.GEOMETRY_HEIGHT, apperancePanel.heightModel.getValue()
				.toString());
		resource.setValue(Config.TERMINAL_SCROLLS, apperancePanel.scrollModel.getValue()
				.toString());
		resource.setValue(Config.TERMINAL_COLUMNS, apperancePanel.terminalColumnsModel
				.getValue().toString());
		resource.setValue(Config.TERMINAL_ROWS, apperancePanel.terminalRowsModel.getValue()
				.toString());

		// chitsaou.070726: 分頁編號
		// chitsaou.070726: 顯示捲軸
		resource.setValue(Resource.TAB_NUMBER, apperancePanel.tabNumberCheckBox
				.isSelected());
		resource.setValue(Resource.SHOW_SCROLL_BAR, apperancePanel.showScrollBarCheckBox
				.isSelected());

		resource.setValue(Config.FONT_FAMILY, fontPanel.familyCombo.getSelectedItem()
				.toString());
		resource.setValue(Config.FONT_SIZE, fontPanel.sizeModel.getValue().toString());
		resource.setValue(Config.FONT_BOLD, fontPanel.boldCheck.isSelected());
		resource.setValue(Config.FONT_ITALY, fontPanel.italyCheck.isSelected());
		resource.setValue(Config.FONT_ANTIALIAS, fontPanel.aaCheck.isSelected());
		resource.setValue(Config.FONT_VERTICLAL_GAP, fontPanel.fontVerticalGapModel
				.getValue().toString());
		resource.setValue(Config.FONT_HORIZONTAL_GAP, fontPanel.fontHorizontalGapModel
				.getValue().toString());
		resource.setValue(Config.FONT_DESCENT_ADJUST, fontPanel.fontDescentAdjustModel
				.getValue().toString());

		// 將修改寫回設定檔
		resource.writeRcFile();

		Model.getInstance().updateLookAndFeel();
		Model.getInstance().updateBounds();
		Model.getInstance().updateToolbar(apperancePanel.showToolbarCheckBox.isSelected());
		Model.getInstance().updateSize();
		Model.getInstance().updateAntiIdleTime();
	}

	private void makeCategoryTree() {
		rootNode = new DefaultMutableTreeNode(Messages
				.getString("Preference.Tree_RootNode_Text")); //$NON-NLS-1$
		generalNode = new DefaultMutableTreeNode(Messages
				.getString("Preference.Tree_GeneralNode_Text")); //$NON-NLS-1$
		connectionNode = new DefaultMutableTreeNode(Messages
				.getString("Preference.Tree_ConnectionNode_Text")); //$NON-NLS-1$
		appearanceNode = new DefaultMutableTreeNode(Messages
				.getString("Preference.Tree_AppearanceNode_Text")); //$NON-NLS-1$
		fontNode = new DefaultMutableTreeNode(Messages
				.getString("Preference.Tree_FontNode_Text")); //$NON-NLS-1$

		rootNode.add(generalNode);
		rootNode.add(connectionNode);
		rootNode.add(appearanceNode);
		rootNode.add(fontNode);

		categoryTree = new JTree(rootNode);
		categoryTree.getSelectionModel().setSelectionMode(
				TreeSelectionModel.SINGLE_TREE_SELECTION);
		categoryTree.addTreeSelectionListener(this);
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
		resource = r;

		systemLookFeelLabel = new JLabel(Messages
				.getString("Preference.SystemLookFeel_Label_Text")); //$NON-NLS-1$
		systemLookFeelCheckBox = new JCheckBox();
		systemLookFeelCheckBox.setSelected(resource
				.getBooleanValue(Resource.SYSTEM_LOOK_FEEL));

		systemLookFeelCheckBox.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent actionEvent) {
				Resource.getInstance().setValue(Resource.SYSTEM_LOOK_FEEL,
						systemLookFeelCheckBox.isSelected());
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
							systemLookFeelCheckBox
									.setSelected(!systemLookFeelCheckBox
											.isSelected());
							Resource.getInstance().setValue(
									Resource.SYSTEM_LOOK_FEEL,
									systemLookFeelCheckBox.isSelected());
							Model.getInstance().updateLookAndFeel();
						}
					}
				}.start();
			}
		});

		showToolbarLabel = new JLabel(Messages
				.getString("Preference.ShowToolbar_Label_Text")); //$NON-NLS-1$
		showToolbarCheckBox = new JCheckBox();
		showToolbarCheckBox.setSelected(resource
				.getBooleanValue(Resource.SHOW_TOOLBAR));

		cursorBlinkLabel = new JLabel(Messages
				.getString("Preference.CursorBlink_Label_Text")); //$NON-NLS-1$
		cursorBlinkCheckBox = new JCheckBox();
		cursorBlinkCheckBox.setSelected(resource
				.getBooleanValue(Config.CURSOR_BLINK));

		widthLabel = new JLabel(Messages
				.getString("Preference.WindowWidth_Label_Text")); //$NON-NLS-1$
		widthModel = new SpinnerNumberModel(resource
				.getIntValue(Resource.GEOMETRY_WIDTH), 0, 4096, 1);
		widthSpinner = new JSpinner(widthModel);

		heightLabel = new JLabel(Messages
				.getString("Preference.WindowHeight_Label_Text")); //$NON-NLS-1$
		heightModel = new SpinnerNumberModel(resource
				.getIntValue(Resource.GEOMETRY_HEIGHT), 0, 4096, 1);
		heightSpinner = new JSpinner(heightModel);

		scrollLabel = new JLabel(Messages
				.getString("Preference.Scroll_Label_Text")); //$NON-NLS-1$
		scrollModel = new SpinnerNumberModel(resource
				.getIntValue(Config.TERMINAL_SCROLLS), 0, 10000, 1);
		scrollSpinner = new JSpinner(scrollModel);

		terminalColumnsLabel = new JLabel(Messages
				.getString("Preference.TerminalColumns_Label_Text")); //$NON-NLS-1$
		terminalColumnsModel = new SpinnerNumberModel(resource
				.getIntValue(Config.TERMINAL_COLUMNS), 80, 200, 1);
		terminalColumnsSpinner = new JSpinner(terminalColumnsModel);
		terminalColumnsSpinner.setEnabled(false);

		terminalRowsLabel = new JLabel(Messages
				.getString("Preference.TerminalRows_Label_Text")); //$NON-NLS-1$
		terminalRowsModel = new SpinnerNumberModel(resource
				.getIntValue(Config.TERMINAL_ROWS), 24, 200, 1);
		terminalRowsSpinner = new JSpinner(terminalRowsModel);
		terminalRowsSpinner.setEnabled(false);

		// chitsaou.070726: 分頁編號
		tabNumberLabel = new JLabel(Messages
				.getString("Preference.TabNumber_Label_Text")); //$NON-NLS-1$
		tabNumberCheckBox = new JCheckBox();
		tabNumberCheckBox.setSelected(resource
				.getBooleanValue(Resource.TAB_NUMBER));

		// chitsaou.070726: 顯示捲軸
		showScrollBarLabel = new JLabel(Messages
				.getString("Preference.ShowScrollBar_Label_Text")); //$NON-NLS-1$
		showScrollBarCheckBox = new JCheckBox();
		showScrollBarCheckBox.setSelected(resource
				.getBooleanValue(Resource.SHOW_SCROLL_BAR));

		setLayout(new GridBagLayout());
		final GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.WEST;

		c.gridx = 0;
		c.gridy = 0;
		this.add(systemLookFeelLabel, c);
		c.gridx = 1;
		this.add(systemLookFeelCheckBox, c);

		c.gridx = 0;
		c.gridy = 1;
		this.add(showToolbarLabel, c);
		c.gridx = 1;
		this.add(showToolbarCheckBox, c);

		c.gridx = 0;
		c.gridy = 2;
		this.add(cursorBlinkLabel, c);
		c.gridx = 1;
		this.add(cursorBlinkCheckBox, c);

		c.gridx = 0;
		c.gridy = 3;
		this.add(widthLabel, c);
		c.gridx = 1;
		this.add(widthSpinner, c);

		c.gridx = 0;
		c.gridy = 4;
		this.add(heightLabel, c);
		c.gridx = 1;
		this.add(heightSpinner, c);

		c.gridx = 0;
		c.gridy = 5;
		this.add(scrollLabel, c);
		c.gridx = 1;
		this.add(scrollSpinner, c);

		c.gridx = 0;
		c.gridy = 6;
		this.add(terminalColumnsLabel, c);
		c.gridx = 1;
		this.add(terminalColumnsSpinner, c);

		c.gridx = 0;
		c.gridy = 7;
		this.add(terminalRowsLabel, c);
		c.gridx = 1;
		this.add(terminalRowsSpinner, c);

		// chitsaou.070726: 分頁編號
		c.gridx = 0;
		c.gridy = 8;
		this.add(tabNumberLabel, c);
		c.gridx = 1;
		this.add(tabNumberCheckBox, c);

		// chitsaou.070726: 顯示捲軸
		c.gridx = 0;
		c.gridy = 9;
		this.add(showScrollBarLabel, c);
		c.gridx = 1;
		this.add(showScrollBarCheckBox, c);
	}

	/**
	 * Refresh text in appearance panel because the locale might be modified.
	 */
	public void refreshText() {
		systemLookFeelLabel.setText(Messages
				.getString("Preference.SystemLookFeel_Label_Text")); //$NON-NLS-1$

		showToolbarLabel.setText(Messages
				.getString("Preference.ShowToolbar_Label_Text")); //$NON-NLS-1$
		cursorBlinkLabel.setText(Messages
				.getString("Preference.CursorBlink_Label_Text")); //$NON-NLS-1$
		widthLabel.setText(Messages
				.getString("Preference.WindowWidth_Label_Text")); //$NON-NLS-1$

		heightLabel.setText(Messages
				.getString("Preference.WindowHeight_Label_Text")); //$NON-NLS-1$
		scrollLabel.setText(Messages
				.getString("Preference.Scroll_Label_Text")); //$NON-NLS-1$
		terminalColumnsLabel.setText(Messages
				.getString("Preference.TerminalColumns_Label_Text")); //$NON-NLS-1$
		terminalRowsLabel.setText(Messages
				.getString("Preference.TerminalRows_Label_Text")); //$NON-NLS-1$
		tabNumberLabel.setText(Messages
				.getString("Preference.TabNumber_Label_Text")); //$NON-NLS-1$
		showScrollBarLabel.setText(Messages
				.getString("Preference.ShowScrollBar_Label_Text")); //$NON-NLS-1$
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
		resource = r;

		final boolean autoReconnect = resource
				.getBooleanValue(Resource.AUTO_RECONNECT);
		autoReconnectLabel = new JLabel(Messages
				.getString("Preference.AutoReconnect_Label_Text")); //$NON-NLS-1$
		autoReconnectCheckBox = new JCheckBox();
		autoReconnectCheckBox.setSelected(autoReconnect);
		autoReconnectCheckBox.addActionListener(this);

		reconnectTimeLabel = new JLabel(Messages
				.getString("Preference.ReconnectTime_Label_Text")); //$NON-NLS-1$
		reconnectTimeModel = new SpinnerNumberModel(resource
				.getIntValue(Resource.AUTO_RECONNECT_TIME), 0, 3600, 1);
		reconnectTimeSpinner = new JSpinner(reconnectTimeModel);
		reconnectTimeSpinner.setEnabled(autoReconnect);

		reconnectIntervalLabel = new JLabel(Messages
				.getString("Preference.ReconnectInterval_Label_Text")); //$NON-NLS-1$
		reconnectIntervalModel = new SpinnerNumberModel(resource
				.getIntValue(Resource.AUTO_RECONNECT_INTERVAL), 0, 60000, 1);
		reconnectIntervalSpinner = new JSpinner(reconnectIntervalModel);
		reconnectIntervalSpinner.setEnabled(autoReconnect);

		antiIdleLabel = new JLabel(Messages
				.getString("Preference.AntiIdle_Label_Text")); //$NON-NLS-1$
		antiIdleCheckBox = new JCheckBox();
		antiIdleCheckBox.setSelected(resource
				.getBooleanValue(Resource.ANTI_IDLE));
		antiIdleCheckBox.addActionListener(this);

		antiIdleTimeLabel = new JLabel(Messages
				.getString("Preference.AntiIdleTime_Label_Text")); //$NON-NLS-1$
		antiIdleModel = new SpinnerNumberModel(resource
				.getIntValue(Resource.ANTI_IDLE_INTERVAL), 0, 3600, 1);
		antiIdleTimeSpinner = new JSpinner(antiIdleModel);
		antiIdleTimeSpinner.setEnabled(resource
				.getBooleanValue(Resource.ANTI_IDLE));

		// chitsaou.070726: 防閒置字串
		antiIdleStringLabel = new JLabel(Messages
				.getString("Preference.AntiIdleString_Label_Text")); //$NON-NLS-1$
		antiIdleStringField = new JTextField(resource
				.getStringValue(Resource.ANTI_IDLE_STRING), 15);

		setLayout(new GridBagLayout());
		final GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.WEST;

		c.gridx = 0;
		c.gridy = 0;
		this.add(autoReconnectLabel, c);
		c.gridx = 1;
		this.add(autoReconnectCheckBox, c);

		c.gridx = 0;
		c.gridy = 1;
		this.add(reconnectTimeLabel, c);
		c.gridx = 1;
		this.add(reconnectTimeSpinner, c);

		c.gridx = 0;
		c.gridy = 2;
		this.add(reconnectIntervalLabel, c);
		c.gridx = 1;
		this.add(reconnectIntervalSpinner, c);

		c.gridx = 0;
		c.gridy = 3;
		this.add(antiIdleLabel, c);
		c.gridx = 1;
		this.add(antiIdleCheckBox, c);

		c.gridx = 0;
		c.gridy = 4;
		this.add(antiIdleTimeLabel, c);
		c.gridx = 1;
		this.add(antiIdleTimeSpinner, c);

		// chitsaou.070726: 防閒置字串
		c.gridx = 0;
		c.gridy = 5;
		this.add(antiIdleStringLabel, c);
		c.gridx = 1;
		this.add(antiIdleStringField, c);
	}

	/**
	 * Refresh the text in the connection panel because the locale might be modified.
	 */
	public void refreshText() {
		autoReconnectLabel.setText(Messages
				.getString("Preference.AutoReconnect_Label_Text")); //$NON-NLS-1$
		reconnectTimeLabel.setText(Messages
				.getString("Preference.ReconnectTime_Label_Text")); //$NON-NLS-1$
		reconnectIntervalLabel.setText(Messages
				.getString("Preference.ReconnectInterval_Label_Text")); //$NON-NLS-1$
		antiIdleLabel.setText(Messages
				.getString("Preference.AntiIdle_Label_Text")); //$NON-NLS-1$
		antiIdleTimeLabel.setText(Messages
				.getString("Preference.AntiIdleTime_Label_Text")); //$NON-NLS-1$
		antiIdleStringLabel.setText(Messages
				.getString("Preference.AntiIdleString_Label_Text")); //$NON-NLS-1$
	}

	public void actionPerformed(final ActionEvent ae) {
		if (ae.getSource() == autoReconnectCheckBox) {
			reconnectTimeSpinner.setEnabled(autoReconnectCheckBox.isSelected());
			reconnectIntervalSpinner.setEnabled(autoReconnectCheckBox
					.isSelected());
		} else if (ae.getSource() == antiIdleCheckBox) {
			antiIdleTimeSpinner.setEnabled(antiIdleCheckBox.isSelected());
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
		resource = r;

		familyLabel = new JLabel(Messages
				.getString("Preference.FontFamily_Label_Text")); //$NON-NLS-1$
		final GraphicsEnvironment ge = GraphicsEnvironment
				.getLocalGraphicsEnvironment();
		final String[] familyList = ge.getAvailableFontFamilyNames();
		familyCombo = new JComboBox(familyList);
		familyCombo
				.setSelectedItem(resource.getStringValue(Config.FONT_FAMILY));

		sizeLabel = new JLabel(Messages
				.getString("Preference.FontSize_Label_Text")); //$NON-NLS-1$
		sizeModel = new SpinnerNumberModel(resource
				.getIntValue(Config.FONT_SIZE), 0, 64, 1);
		sizeSpinner = new JSpinner(sizeModel);

		boldLabel = new JLabel(Messages
				.getString("Preference.FontBold_Label_Text")); //$NON-NLS-1$
		boldCheck = new JCheckBox();
		boldCheck.setSelected(resource.getBooleanValue(Config.FONT_BOLD));

		italyLabel = new JLabel(Messages
				.getString("Preference.FontItaly_Label_Text")); //$NON-NLS-1$
		italyCheck = new JCheckBox();
		italyCheck.setSelected(resource.getBooleanValue(Config.FONT_ITALY));

		aaLabel = new JLabel(Messages
				.getString("Preference.FontAntiAliasing_Label_Text")); //$NON-NLS-1$
		aaCheck = new JCheckBox();
		aaCheck.setSelected(resource.getBooleanValue(Config.FONT_ANTIALIAS));

		fontVerticalGapLabel = new JLabel(Messages
				.getString("Preference.FontVerticalGap_Label_Text")); //$NON-NLS-1$
		fontVerticalGapModel = new SpinnerNumberModel(resource
				.getIntValue(Config.FONT_VERTICLAL_GAP), -10, 10, 1);
		fontVerticalGapSpinner = new JSpinner(fontVerticalGapModel);

		fontHorizontalGapLabel = new JLabel(Messages
				.getString("Preference.FontHorizontalGap_Label_Text")); //$NON-NLS-1$
		fontHorizontalGapModel = new SpinnerNumberModel(resource
				.getIntValue(Config.FONT_HORIZONTAL_GAP), -10, 10, 1);
		fontHorizontalGapSpinner = new JSpinner(fontHorizontalGapModel);

		fontDescentAdjustLabel = new JLabel(Messages
				.getString("Preference.FontDescentAdjust_Label_Text")); //$NON-NLS-1$
		fontDescentAdjustModel = new SpinnerNumberModel(resource
				.getIntValue(Config.FONT_DESCENT_ADJUST), -10, 10, 1);
		fontDescentAdjustSpinner = new JSpinner(fontDescentAdjustModel);

		setLayout(new GridBagLayout());
		final GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.WEST;

		c.gridx = 0;
		c.gridy = 0;
		this.add(familyLabel, c);
		c.gridx = 1;
		this.add(familyCombo, c);

		c.gridx = 0;
		c.gridy = 1;
		this.add(sizeLabel, c);
		c.gridx = 1;
		this.add(sizeSpinner, c);

		c.gridx = 0;
		c.gridy = 2;
		this.add(boldLabel, c);
		c.gridx = 1;
		this.add(boldCheck, c);

		c.gridx = 0;
		c.gridy = 3;
		this.add(italyLabel, c);
		c.gridx = 1;
		this.add(italyCheck, c);

		c.gridx = 0;
		c.gridy = 4;
		this.add(aaLabel, c);
		c.gridx = 1;
		this.add(aaCheck, c);

		c.gridx = 0;
		c.gridy = 5;
		this.add(fontVerticalGapLabel, c);
		c.gridx = 1;
		this.add(fontVerticalGapSpinner, c);

		c.gridx = 0;
		c.gridy = 6;
		this.add(fontHorizontalGapLabel, c);
		c.gridx = 1;
		this.add(fontHorizontalGapSpinner, c);

		c.gridx = 0;
		c.gridy = 7;
		this.add(fontDescentAdjustLabel, c);
		c.gridx = 1;
		this.add(fontDescentAdjustSpinner, c);
	}

	/**
	 * Refresh the text in the font panel because the locale might be changed.
	 */
	public void refreshText() {
		familyLabel.setText(Messages
				.getString("Preference.FontFamily_Label_Text")); //$NON-NLS-1$
		sizeLabel.setText(Messages
				.getString("Preference.FontSize_Label_Text")); //$NON-NLS-1$
		boldLabel.setText(Messages
				.getString("Preference.FontBold_Label_Text")); //$NON-NLS-1$
		italyLabel.setText(Messages
				.getString("Preference.FontItaly_Label_Text")); //$NON-NLS-1$
		aaLabel.setText(Messages
				.getString("Preference.FontAntiAliasing_Label_Text")); //$NON-NLS-1$
		fontVerticalGapLabel.setText(Messages
				.getString("Preference.FontVerticalGap_Label_Text")); //$NON-NLS-1$
		fontHorizontalGapLabel.setText(Messages
				.getString("Preference.FontHorizontalGap_Label_Text")); //$NON-NLS-1$		
		fontDescentAdjustLabel.setText(Messages
				.getString("Preference.FontDescentAdjust_Label_Text")); //$NON-NLS-1$
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
	
	// Spot the location of file for settings
	protected JLabel settingFileLabel;
	
	// Indicate the location of file for settings 
	protected JTextField settingFileField;
	
	// Choose the location of file for settings
	protected JButton settingFileButton;

	private StringBuffer bellPathParentDirectory = new StringBuffer("");
	private StringBuffer settingFileParentDirectory = new StringBuffer("");;
	private final Resource resource;

	public GeneralPanel(final Resource r) {
		super();
		resource = r;
		
		settingFileLabel = new JLabel(Messages
				.getString("Preference.SettingFileLocation_Label"));
		settingFileField = new JTextField(resource.getStringValue(Resource.RESOURCE_LOCATION));
		settingFileButton = new JButton("...");
		settingFileButton.addActionListener(this);

		browserLabel = new JLabel(Messages
				.getString("Preference.BrowserCommand_Label_Text")); //$NON-NLS-1$
		browserField = new JTextField(resource
				.getStringValue(Resource.EXTERNAL_BROWSER), 20);

		copyOnSelectLabel = new JLabel(Messages
				.getString("Preference.CopyOnSelect_Label_Text")); //$NON-NLS-1$
		copyOnSelectCheckBox = new JCheckBox();
		copyOnSelectCheckBox.setSelected(resource
				.getBooleanValue(Config.COPY_ON_SELECT));

		clearAfterCopyLabel = new JLabel(Messages
				.getString("Preference.ClearAfterCopy_Label_Text")); //$NON-NLS-1$
		clearAfterCopyCheckBox = new JCheckBox();
		clearAfterCopyCheckBox.setSelected(resource
				.getBooleanValue(Config.CLEAR_AFTER_COPY));

		removeManualLabel = new JLabel(Messages
				.getString("Preference.RemoveManual_Label_Text")); //$NON-NLS-1$
		removeManualCheckBox = new JCheckBox();
		removeManualCheckBox.setSelected(resource
				.getBooleanValue(Resource.REMOVE_MANUAL_DISCONNECT));

		linebreakLabel = new JLabel(Messages
				.getString("Preference.LineBreak_Label_Text")); //$NON-NLS-1$
		linebreakCheckBox = new JCheckBox();
		linebreakCheckBox.setSelected(resource
				.getBooleanValue(Config.AUTO_LINE_BREAK));
		linebreakCheckBox.addActionListener(this);

		breaklengthLabel = new JLabel(Messages
				.getString("Preference.BreakLength_Label_Text")); //$NON-NLS-1$
		breaklengthModel = new SpinnerNumberModel(resource
				.getIntValue(Config.AUTO_LINE_BREAK_LENGTH), 1, 512, 1);
		breaklengthSpinner = new JSpinner(breaklengthModel);
		breaklengthSpinner.setEnabled(resource
				.getBooleanValue(Config.AUTO_LINE_BREAK));

		customBellLabel = new JLabel(Messages
				.getString("Preference.CustomBell_Label_Text")); //$NON-NLS-1$
		customBellCheckBox = new JCheckBox();
		customBellCheckBox.setSelected(resource
				.getBooleanValue(Resource.USE_CUSTOM_BELL));
		customBellCheckBox.addActionListener(this);

		bellPathLabel = new JLabel(Messages
				.getString("Preference.BellPath_Label_Text")); //$NON-NLS-1$
		bellPathField = new JTextField(resource
				.getStringValue(Resource.CUSTOM_BELL_PATH), 8);
		bellPathField.setEnabled(resource
				.getBooleanValue(Resource.USE_CUSTOM_BELL));
		bellPathButton = new JButton(Messages
				.getString("Preference.BellPath_Button_Text")); //$NON-NLS-1$
		bellPathButton.setEnabled(resource
				.getBooleanValue(Resource.USE_CUSTOM_BELL));
		bellPathButton.addActionListener(this);

		setLayout(new GridBagLayout());
		final GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.WEST;

		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 3;
		this.add(browserLabel, c);

		c.gridx = 0;
		c.gridy = 1;
		c.gridwidth = 3;
		this.add(browserField, c);

		c.gridwidth = 1;

		c.gridx = 0;
		c.gridy = 2;
		this.add(copyOnSelectLabel, c);
		c.gridx = 1;
		this.add(copyOnSelectCheckBox, c);

		c.gridx = 0;
		c.gridy = 3;
		this.add(clearAfterCopyLabel, c);
		c.gridx = 1;
		this.add(clearAfterCopyCheckBox, c);

		c.gridx = 0;
		c.gridy = 4;
		this.add(removeManualLabel, c);
		c.gridx = 1;
		this.add(removeManualCheckBox, c);

		c.gridx = 0;
		c.gridy = 5;
		this.add(linebreakLabel, c);
		c.gridx = 1;
		this.add(linebreakCheckBox, c);

		c.gridx = 0;
		c.gridy = 6;
		this.add(breaklengthLabel, c);
		c.gridx = 1;
		this.add(breaklengthSpinner, c);

		c.gridx = 0;
		c.gridy = 7;
		this.add(customBellLabel, c);
		c.gridx = 1;
		this.add(customBellCheckBox, c);

		c.gridx = 0;
		c.gridy = 8;
		this.add(bellPathLabel, c);
		c.gridx = 1;
		this.add(bellPathField, c);
		c.gridx = 2;
		this.add(bellPathButton, c);
		
		c.gridx = 0;
		c.gridy = 9;
		this.add(settingFileLabel, c);
		c.gridx = 1;
		this.add(settingFileField, c);
		c.gridx = 2;
		this.add(settingFileButton, c);
	}

	/**
	 * Refresh the text
	 */
	public void refreshText() {
		settingFileLabel.setText(Messages
				.getString("Preference.SettingFileLocation_Label"));
		browserLabel.setText(Messages
				.getString("Preference.BrowserCommand_Label_Text")); //$NON-NLS-1$
		copyOnSelectLabel.setText(Messages
				.getString("Preference.CopyOnSelect_Label_Text")); //$NON-NLS-1$
		clearAfterCopyLabel.setText(Messages
				.getString("Preference.ClearAfterCopy_Label_Text")); //$NON-NLS-1$
		removeManualLabel.setText(Messages
				.getString("Preference.RemoveManual_Label_Text")); //$NON-NLS-1$
		linebreakLabel.setText(Messages
				.getString("Preference.LineBreak_Label_Text")); //$NON-NLS-1$
		breaklengthLabel.setText(Messages
				.getString("Preference.BreakLength_Label_Text")); //$NON-NLS-1$
		customBellLabel.setText(Messages
				.getString("Preference.CustomBell_Label_Text")); //$NON-NLS-1$
		bellPathLabel.setText(Messages
				.getString("Preference.BellPath_Label_Text")); //$NON-NLS-1$
		bellPathButton.setText(Messages
				.getString("Preference.BellPath_Button_Text")); //$NON-NLS-1$
	}

	public void actionPerformed(final ActionEvent event) {
		if (event.getSource() == linebreakCheckBox) {
			breaklengthSpinner.setEnabled(linebreakCheckBox.isSelected());
		} else if (event.getSource() == customBellCheckBox) {
			bellPathField.setEnabled(customBellCheckBox.isSelected());
			bellPathButton.setEnabled(customBellCheckBox.isSelected());
		} else if (event.getSource() == bellPathButton) {
			askFileLocationAndUpdateIt(bellPathField, bellPathParentDirectory);
		} else if (event.getSource() == settingFileButton) {
			askFileLocationAndUpdateIt(settingFileField, settingFileParentDirectory);
			resource.setResourceLocation(settingFileField.getText());
			Model.getInstance().updatePreferencePane();
			Model.getInstance().refreshMessages();
		}
	}
	
	private void askFileLocationAndUpdateIt(
			final JTextField pathField, 
			final StringBuffer parentDirectory) {		

		final JFileChooser fileChooser = createFileChooser(parentDirectory.toString());
		final int response = fileChooser.showOpenDialog(this);

		if (response == JFileChooser.APPROVE_OPTION) {
			UpdatePathField(fileChooser, pathField, parentDirectory);
		}
	}
	
	private void UpdatePathField(
			final JFileChooser fileChooser, 
			final JTextField pathField, 
			final StringBuffer parentDirectory) {

		final File selectedFile = fileChooser.getSelectedFile();
		pathField.setText(selectedFile.getAbsolutePath());
		parentDirectory.delete(0, parentDirectory.length());
		parentDirectory.append(selectedFile.getParent());
	}
	
	private JFileChooser createFileChooser(final String parentDirectory) {
		JFileChooser fileChooser;
		
		if ("".equals(parentDirectory)) {
			fileChooser = new JFileChooser();
		} else {
			fileChooser = new JFileChooser(parentDirectory.toString());
		}
		
		return fileChooser;
	}
}
