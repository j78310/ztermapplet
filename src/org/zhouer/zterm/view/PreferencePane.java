package org.zhouer.zterm.view;

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

import org.zhouer.utils.InternationalMessages;
import org.zhouer.vt.Config;
import org.zhouer.zterm.model.Model;
import org.zhouer.zterm.model.Resource;

/**
 * PreferencePane is an option pane which provides control items which enable
 * user to modify settings for ZTerm applet.
 * 
 * @author h45
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
		welcomeLabel = new JLabel(InternationalMessages
				.getString("Preference.Welcome_Label_Text"));
		welcomePanel = new JPanel();
		welcomePanel.setLayout(new BorderLayout());
		welcomePanel.add(welcomeLabel, BorderLayout.NORTH); //$NON-NLS-1$
		welcomeTextArea = new JTextArea(InternationalMessages
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

	public void submit() {
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

		resource.setValue(Config.CURSOR_BLINK, apperancePanel.cursorBlinkCheckBox
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
		resource.writeFile();

		Model.getInstance().updateSize();
		Model.getInstance().updateAntiIdleTime();
	}

	private void makeCategoryTree() {
		rootNode = new DefaultMutableTreeNode(InternationalMessages
				.getString("Preference.Tree_RootNode_Text")); //$NON-NLS-1$
		generalNode = new DefaultMutableTreeNode(InternationalMessages
				.getString("Preference.Tree_GeneralNode_Text")); //$NON-NLS-1$
		connectionNode = new DefaultMutableTreeNode(InternationalMessages
				.getString("Preference.Tree_ConnectionNode_Text")); //$NON-NLS-1$
		appearanceNode = new DefaultMutableTreeNode(InternationalMessages
				.getString("Preference.Tree_AppearanceNode_Text")); //$NON-NLS-1$
		fontNode = new DefaultMutableTreeNode(InternationalMessages
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

	public JCheckBox cursorBlinkCheckBox;
	public JLabel cursorBlinkLabel;

	private final Resource resource;

	public ApperancePanel(final Resource r) {
		super();
		resource = r;

		cursorBlinkLabel = new JLabel(InternationalMessages
				.getString("Preference.CursorBlink_Label_Text")); //$NON-NLS-1$
		cursorBlinkCheckBox = new JCheckBox();
		cursorBlinkCheckBox.setSelected(resource
				.getBooleanValue(Config.CURSOR_BLINK));

		setLayout(new GridBagLayout());
		final GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.WEST;

		c.gridx = 0;
		c.gridy = 0;
		this.add(cursorBlinkLabel, c);
		c.gridx = 1;
		this.add(cursorBlinkCheckBox, c);
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
		autoReconnectLabel = new JLabel(InternationalMessages
				.getString("Preference.AutoReconnect_Label_Text")); //$NON-NLS-1$
		autoReconnectCheckBox = new JCheckBox();
		autoReconnectCheckBox.setSelected(autoReconnect);
		autoReconnectCheckBox.addActionListener(this);

		reconnectTimeLabel = new JLabel(InternationalMessages
				.getString("Preference.ReconnectTime_Label_Text")); //$NON-NLS-1$
		reconnectTimeModel = new SpinnerNumberModel(resource
				.getIntValue(Resource.AUTO_RECONNECT_TIME), 0, 3600, 1);
		reconnectTimeSpinner = new JSpinner(reconnectTimeModel);
		reconnectTimeSpinner.setEnabled(autoReconnect);

		reconnectIntervalLabel = new JLabel(InternationalMessages
				.getString("Preference.ReconnectInterval_Label_Text")); //$NON-NLS-1$
		reconnectIntervalModel = new SpinnerNumberModel(resource
				.getIntValue(Resource.AUTO_RECONNECT_INTERVAL), 0, 60000, 1);
		reconnectIntervalSpinner = new JSpinner(reconnectIntervalModel);
		reconnectIntervalSpinner.setEnabled(autoReconnect);

		antiIdleLabel = new JLabel(InternationalMessages
				.getString("Preference.AntiIdle_Label_Text")); //$NON-NLS-1$
		antiIdleCheckBox = new JCheckBox();
		antiIdleCheckBox.setSelected(resource
				.getBooleanValue(Resource.ANTI_IDLE));
		antiIdleCheckBox.addActionListener(this);

		antiIdleTimeLabel = new JLabel(InternationalMessages
				.getString("Preference.AntiIdleTime_Label_Text")); //$NON-NLS-1$
		antiIdleModel = new SpinnerNumberModel(resource
				.getIntValue(Resource.ANTI_IDLE_INTERVAL), 0, 3600, 1);
		antiIdleTimeSpinner = new JSpinner(antiIdleModel);
		antiIdleTimeSpinner.setEnabled(resource
				.getBooleanValue(Resource.ANTI_IDLE));

		// chitsaou.070726: 防閒置字串
		antiIdleStringLabel = new JLabel(InternationalMessages
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

		familyLabel = new JLabel(InternationalMessages
				.getString("Preference.FontFamily_Label_Text")); //$NON-NLS-1$
		final GraphicsEnvironment ge = GraphicsEnvironment
				.getLocalGraphicsEnvironment();
		final String[] familyList = ge.getAvailableFontFamilyNames();
		familyCombo = new JComboBox(familyList);
		familyCombo
				.setSelectedItem(resource.getStringValue(Config.FONT_FAMILY));

		sizeLabel = new JLabel(InternationalMessages
				.getString("Preference.FontSize_Label_Text")); //$NON-NLS-1$
		sizeModel = new SpinnerNumberModel(resource
				.getIntValue(Config.FONT_SIZE), 0, 64, 1);
		sizeSpinner = new JSpinner(sizeModel);

		boldLabel = new JLabel(InternationalMessages
				.getString("Preference.FontBold_Label_Text")); //$NON-NLS-1$
		boldCheck = new JCheckBox();
		boldCheck.setSelected(resource.getBooleanValue(Config.FONT_BOLD));

		italyLabel = new JLabel(InternationalMessages
				.getString("Preference.FontItaly_Label_Text")); //$NON-NLS-1$
		italyCheck = new JCheckBox();
		italyCheck.setSelected(resource.getBooleanValue(Config.FONT_ITALY));

		aaLabel = new JLabel(InternationalMessages
				.getString("Preference.FontAntiAliasing_Label_Text")); //$NON-NLS-1$
		aaCheck = new JCheckBox();
		aaCheck.setSelected(resource.getBooleanValue(Config.FONT_ANTIALIAS));

		fontVerticalGapLabel = new JLabel(InternationalMessages
				.getString("Preference.FontVerticalGap_Label_Text")); //$NON-NLS-1$
		fontVerticalGapModel = new SpinnerNumberModel(resource
				.getIntValue(Config.FONT_VERTICLAL_GAP), -10, 10, 1);
		fontVerticalGapSpinner = new JSpinner(fontVerticalGapModel);

		fontHorizontalGapLabel = new JLabel(InternationalMessages
				.getString("Preference.FontHorizontalGap_Label_Text")); //$NON-NLS-1$
		fontHorizontalGapModel = new SpinnerNumberModel(resource
				.getIntValue(Config.FONT_HORIZONTAL_GAP), -10, 10, 1);
		fontHorizontalGapSpinner = new JSpinner(fontHorizontalGapModel);

		fontDescentAdjustLabel = new JLabel(InternationalMessages
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
		resource = r;

		browserLabel = new JLabel(InternationalMessages
				.getString("Preference.BrowserCommand_Label_Text")); //$NON-NLS-1$
		browserField = new JTextField(resource
				.getStringValue(Resource.EXTERNAL_BROWSER), 20);

		copyOnSelectLabel = new JLabel(InternationalMessages
				.getString("Preference.CopyOnSelect_Label_Text")); //$NON-NLS-1$
		copyOnSelectCheckBox = new JCheckBox();
		copyOnSelectCheckBox.setSelected(resource
				.getBooleanValue(Config.COPY_ON_SELECT));

		clearAfterCopyLabel = new JLabel(InternationalMessages
				.getString("Preference.ClearAfterCopy_Label_Text")); //$NON-NLS-1$
		clearAfterCopyCheckBox = new JCheckBox();
		clearAfterCopyCheckBox.setSelected(resource
				.getBooleanValue(Config.CLEAR_AFTER_COPY));

		removeManualLabel = new JLabel(InternationalMessages
				.getString("Preference.RemoveManual_Label_Text")); //$NON-NLS-1$
		removeManualCheckBox = new JCheckBox();
		removeManualCheckBox.setSelected(resource
				.getBooleanValue(Resource.REMOVE_MANUAL_DISCONNECT));

		linebreakLabel = new JLabel(InternationalMessages
				.getString("Preference.LineBreak_Label_Text")); //$NON-NLS-1$
		linebreakCheckBox = new JCheckBox();
		linebreakCheckBox.setSelected(resource
				.getBooleanValue(Config.AUTO_LINE_BREAK));
		linebreakCheckBox.addActionListener(this);

		breaklengthLabel = new JLabel(InternationalMessages
				.getString("Preference.BreakLength_Label_Text")); //$NON-NLS-1$
		breaklengthModel = new SpinnerNumberModel(resource
				.getIntValue(Config.AUTO_LINE_BREAK_LENGTH), 1, 512, 1);
		breaklengthSpinner = new JSpinner(breaklengthModel);
		breaklengthSpinner.setEnabled(resource
				.getBooleanValue(Config.AUTO_LINE_BREAK));

		customBellLabel = new JLabel(InternationalMessages
				.getString("Preference.CustomBell_Label_Text")); //$NON-NLS-1$
		customBellCheckBox = new JCheckBox();
		customBellCheckBox.setSelected(resource
				.getBooleanValue(Resource.USE_CUSTOM_BELL));
		customBellCheckBox.addActionListener(this);

		bellPathLabel = new JLabel(InternationalMessages
				.getString("Preference.BellPath_Label_Text")); //$NON-NLS-1$
		bellPathField = new JTextField(resource
				.getStringValue(Resource.CUSTOM_BELL_PATH), 8);
		bellPathField.setEnabled(resource
				.getBooleanValue(Resource.USE_CUSTOM_BELL));
		bellPathButton = new JButton(InternationalMessages
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
	}

	public void actionPerformed(final ActionEvent ae) {
		if (ae.getSource() == linebreakCheckBox) {
			breaklengthSpinner.setEnabled(linebreakCheckBox.isSelected());
		} else if (ae.getSource() == customBellCheckBox) {
			bellPathField.setEnabled(customBellCheckBox.isSelected());
			bellPathButton.setEnabled(customBellCheckBox.isSelected());
		} else if (ae.getSource() == bellPathButton) {

			if (parentDirectory != null) {
				jfc = new JFileChooser(parentDirectory);
			} else {
				jfc = new JFileChooser();
			}

			if (jfc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
				selectedFile = jfc.getSelectedFile();
				parentDirectory = selectedFile.getParent();
				bellPathField.setText(selectedFile.getAbsolutePath());
			}
		}
	}
}
