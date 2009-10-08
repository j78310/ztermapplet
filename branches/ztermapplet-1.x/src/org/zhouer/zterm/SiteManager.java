package org.zhouer.zterm;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.zhouer.protocol.Protocol;

/**
 * SiteManager is an option pane for users to modify their favorite site.
 * 
 * @author h45
 */
public class SiteManager extends JOptionPane {
	
	private class ParameterPanel extends JPanel implements ActionListener, KeyListener {
		private static final long serialVersionUID = -3479511727147739169L;

		private final JLabel autoConnectLabel;

		private final String[] emulationList = { "vt100", "xterm", "xterm-color", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				"ansi" }; //$NON-NLS-1$

		private final JLabel encodingLabel, emulationLabel;

		private final String[] encodingList = { "Big5", "UTF-8", "GB2312" }; //$NON-NLS-1$ //$NON-NLS-2$

		// 名稱, 位置, 埠號, 通訊協定. 帳號提示, 帳號, 密碼提示, 密碼, 自動連線, 自動登入
		private final JLabel nameLabel, hostLabel, portLabel, protocolLabel,
				aliasLabel;
		private final SiteManager parent;
		private final ButtonGroup protocolGroup;

		final JCheckBox autoConnectCheckBox;

		final JComboBox emulationCombo;
		final JComboBox encodingCombo;

		// private JCheckBox autoLoginCheckBox;
		// private JLabel autoLoginLabel;
		// private JLabel userPromptLabel, userLabel, passPromptLabel, passLabel;
		// private JTextField userPromptField, userField, passPromptField,
		// passField;

		final JTextField hostField, portField, aliasField;

		final JTextField nameField;
		final JRadioButton sshButton;

		JRadioButton telnetButton;

		public ParameterPanel(final SiteManager p) {
			super();
			this.parent = p;
			this.setLayout(new GridBagLayout());
			final GridBagConstraints c = new GridBagConstraints();

			this.nameLabel = new JLabel(Messages.getString("SiteManager.Name")); //$NON-NLS-1$
			this.hostLabel = new JLabel(Messages.getString("SiteManager.Host")); //$NON-NLS-1$
			this.portLabel = new JLabel(Messages.getString("SiteManager.Port")); //$NON-NLS-1$
			this.aliasLabel = new JLabel(Messages.getString("SiteManager.Alias")); //$NON-NLS-1$
			this.protocolLabel = new JLabel(Messages
					.getString("SiteManager.Protocal")); //$NON-NLS-1$
			this.encodingLabel = new JLabel(Messages
					.getString("SiteManager.Encoding")); //$NON-NLS-1$
			this.emulationLabel = new JLabel(Messages
					.getString("SiteManager.Emulation")); //$NON-NLS-1$
			this.autoConnectLabel = new JLabel(Messages
					.getString("SiteManager.AutoConnect")); //$NON-NLS-1$

			// autoLoginLabel = new JLabel("自動登入");
			// userPromptLabel = new JLabel("帳號提示");
			// userLabel = new JLabel("帳號");
			// passPromptLabel = new JLabel("密碼提示");
			// passLabel = new JLabel("密碼");

			this.nameField = new JTextField(15);
			this.nameField.addKeyListener(this);
			this.hostField = new JTextField(15);
			this.hostField.addKeyListener(this);
			this.portField = new JTextField(15);
			this.portField.addKeyListener(this);
			this.aliasField = new JTextField(15);
			this.aliasField.addKeyListener(this);

			this.telnetButton = new JRadioButton(Messages
					.getString("SiteManager.TelnetButtonText")); //$NON-NLS-1$
			this.telnetButton.addActionListener(this);
			this.sshButton = new JRadioButton(Messages
					.getString("SiteManager.SSHButtonText")); //$NON-NLS-1$
			this.sshButton.addActionListener(this);

			this.protocolGroup = new ButtonGroup();
			this.protocolGroup.add(this.telnetButton);
			this.protocolGroup.add(this.sshButton);

			this.encodingCombo = new JComboBox(this.encodingList);
			this.encodingCombo.addActionListener(this);

			this.emulationCombo = new JComboBox(this.emulationList);
			this.emulationCombo.addActionListener(this);

			this.autoConnectCheckBox = new JCheckBox();
			this.autoConnectCheckBox.addActionListener(this);

			// autoLoginCheckBox = new JCheckBox();
			// userPromptField = new JTextField( 15 );
			// userField = new JTextField( 15 );
			// passPromptField = new JTextField( 15 );
			// passField = new JTextField( 15 );

			c.ipadx = c.ipady = 3;

			c.gridx = 0;
			c.gridy = 0;
			c.gridwidth = 1;
			this.add(this.nameLabel, c);
			c.gridx = 1;
			c.gridwidth = 2;
			this.add(this.nameField, c);

			c.gridx = 0;
			c.gridy = 1;
			c.gridwidth = 1;
			this.add(this.hostLabel, c);
			c.gridx = 1;
			c.gridwidth = 2;
			this.add(this.hostField, c);

			c.gridx = 0;
			c.gridy = 2;
			c.gridwidth = 1;
			this.add(this.portLabel, c);
			c.gridx = 1;
			c.gridwidth = 2;
			this.add(this.portField, c);

			c.gridx = 0;
			c.gridy = 3;
			c.gridwidth = 1;
			this.add(this.aliasLabel, c);
			c.gridx = 1;
			c.gridwidth = 2;
			this.add(this.aliasField, c);

			c.gridx = 0;
			c.gridy = 4;
			c.gridwidth = 1;
			this.add(this.protocolLabel, c);
			c.gridx = 1;
			this.add(this.telnetButton, c);
			c.gridx = 2;
			this.add(this.sshButton, c);

			c.gridx = 0;
			c.gridy = 5;
			c.gridwidth = 1;
			this.add(this.encodingLabel, c);
			c.gridx = 1;
			this.add(this.encodingCombo, c);

			c.gridx = 0;
			c.gridy = 6;
			c.gridwidth = 1;
			this.add(this.emulationLabel, c);
			c.gridx = 1;
			this.add(this.emulationCombo, c);

			c.gridx = 0;
			c.gridy = 7;
			c.gridwidth = 1;
			this.add(this.autoConnectLabel, c);
			c.gridx = 1;
			this.add(this.autoConnectCheckBox, c);

			/*
			 * c.gridx = 0; c.gridy = 3; add( userPromptLabel, c ); c.gridx = 1;
			 * add( userPromptField, c );
			 * 
			 * c.gridx = 0; c.gridy = 4; add( userLabel, c ); c.gridx = 1; add(
			 * userField, c );
			 * 
			 * c.gridx = 0; c.gridy = 5; add( passPromptLabel, c ); c.gridx = 1;
			 * add( passPromptField, c );
			 * 
			 * c.gridx = 0; c.gridy = 6; add( passLabel, c ); c.gridx = 1; add(
			 * passField, c );
			 * 
			 * c.gridx = 0; c.gridy = 8; add( autoLoginLabel, c ); c.gridx = 1; add(
			 * autoLoginCheckBox, c );
			 */
			c.gridx = 0;
			c.gridy = 9;
			c.gridx = 1;
		}
		
		public void keyReleased(KeyEvent e) {
			update();
		}

		public void actionPerformed(final ActionEvent ae) {
			if (ae.getSource() == this.telnetButton) {
				this.portField.setText(Integer.toString(23));
			} else if (ae.getSource() == this.sshButton) {
				this.portField.setText(Integer.toString(22));
			}
		}
		
		private void update() {
			final Site s = new Site();
			s.name = nameField.getText();
			s.host = hostField.getText();
			s.port = Integer.parseInt(portField.getText());
			s.alias = aliasField.getText();

			if (telnetButton.isSelected()) {
				s.protocol = Protocol.TELNET;
			} else if (sshButton.isSelected()) {
				s.protocol = Protocol.SSH;
			}

			s.encoding = encodingCombo.getSelectedItem().toString();
			s.emulation = emulationCombo.getSelectedItem().toString();

			s.autoconnect = autoConnectCheckBox.isSelected();
			// s.autologin = autoLoginCheckBox.isSelected();
			parent.updateFavorite(s);
		}

		public void updateParameter(final Site s) {
			this.nameField.setText(s.name);
			this.hostField.setText(s.host);
			this.portField.setText(Integer.toString(s.port));
			this.aliasField.setText(s.alias);

			if (s.protocol.equalsIgnoreCase(Protocol.TELNET)) {
				this.telnetButton.setSelected(true);
			} else if (s.protocol.equalsIgnoreCase(Protocol.SSH)) {
				this.sshButton.setSelected(true);
			}

			this.encodingCombo.setSelectedItem(s.encoding);
			this.emulationCombo.setSelectedItem(s.emulation);

			this.autoConnectCheckBox.setSelected(s.autoconnect);
		}

		public void keyPressed(KeyEvent e) {
			// Do nothing
		}

		public void keyTyped(KeyEvent e) {
			// Do nothing
		}
	}
	
	private class SitePanel extends JPanel implements ActionListener, ListSelectionListener {
		private static final long serialVersionUID = 6399807179665067907L;

		private JButton addButton, removeButton, upButton, downButton;
		private final Vector<Site> favorites;

		private JPanel modifyPanel;
		private final SiteManager parent;

		private JList siteList;
		private DefaultListModel siteListModel;

		public SitePanel(final SiteManager siteManager, final Vector<Site> favorite) {
			super();

			this.parent = siteManager;
			this.favorites = favorite;

			this.makeList();
			this.makeModify();

			this.setLayout(new BorderLayout());
			this.add(new JScrollPane(this.siteList), BorderLayout.CENTER);
			this.add(this.modifyPanel, BorderLayout.SOUTH);
		}

		public void actionPerformed(final ActionEvent ae) {
			if (ae.getSource() == this.addButton) {
				this.siteListModel.addElement("新站台"); //$NON-NLS-1$
				this.favorites
						.add(new Site("新站台", "hostname", 23, Protocol.TELNET)); //$NON-NLS-1$ //$NON-NLS-2$
				this.siteList.setSelectedIndex(this.siteListModel.getSize() - 1);
			} else if (ae.getSource() == this.removeButton) {
				final int i = this.siteList.getSelectedIndex();
				if (i != -1) {
					this.siteListModel.removeElementAt(i);
					this.favorites.removeElementAt(i);
					this.siteList.setSelectedIndex(i - 1);
				}
			} else if (ae.getSource() == this.upButton) {
				final int i = this.siteList.getSelectedIndex();
				if (i > 0) {
					final Site tmp;

					tmp = this.favorites.elementAt(i);
					this.favorites.removeElementAt(i);
					this.favorites.insertElementAt(tmp, i - 1);

					final Object tmp2;
					tmp2 = this.siteListModel.elementAt(i);
					this.siteListModel.removeElementAt(i);
					this.siteListModel.insertElementAt(tmp2, i - 1);

					this.siteList.setSelectedIndex(i - 1);
				}
			} else if (ae.getSource() == this.downButton) {
				final int i = this.siteList.getSelectedIndex();
				if (i < this.siteListModel.size() - 1) {
					final Site tmp;

					tmp = this.favorites.elementAt(i);
					this.favorites.removeElementAt(i);
					this.favorites.insertElementAt(tmp, i + 1);

					final Object tmp2;
					tmp2 = this.siteListModel.elementAt(i);
					this.siteListModel.removeElementAt(i);
					this.siteListModel.insertElementAt(tmp2, i + 1);

					this.siteList.setSelectedIndex(i + 1);
				}
			}
		}

		public void updateFavorite(final Site f) {
			final int index = this.siteList.getSelectedIndex();
			if (index != -1) {
				this.siteListModel.setElementAt(f.name, index);
				this.favorites.setElementAt(f, index);
			}
		}

		public void valueChanged(final ListSelectionEvent lse) {
			final int index = this.siteList.getSelectedIndex();
			if (index != -1) {
				this.parent.updateParameter(this.favorites.elementAt(index));
			}
		}

		private void makeList() {
			final Iterator<Site> iter = this.favorites.iterator();
			this.siteListModel = new DefaultListModel();

			while (iter.hasNext()) {
				this.siteListModel.addElement(iter.next().name);
			}

			this.siteList = new JList(this.siteListModel);
			this.siteList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			this.siteList.addListSelectionListener(this);
			this.siteList.setSelectedIndex(0);
		}

		private void makeModify() {
			this.modifyPanel = new JPanel();
			this.modifyPanel.setLayout(new GridLayout(0, 2, 3, 3));

			this.addButton = new JButton(Messages
					.getString("SiteManager.AddButtonText")); //$NON-NLS-1$
			this.addButton.addActionListener(this);
			this.removeButton = new JButton(Messages
					.getString("SiteManager.RemoveButtonText")); //$NON-NLS-1$
			this.removeButton.addActionListener(this);
			this.upButton = new JButton(Messages
					.getString("SiteManager.UpButtonText")); //$NON-NLS-1$
			this.upButton.addActionListener(this);
			this.downButton = new JButton(Messages
					.getString("SiteManager.DownButtonText")); //$NON-NLS-1$
			this.downButton.addActionListener(this);

			this.modifyPanel.add(this.addButton);
			this.modifyPanel.add(this.removeButton);
			this.modifyPanel.add(this.upButton);
			this.modifyPanel.add(this.downButton);
		}
	}
	
	private static final long serialVersionUID = 3644901803388220764L;

	private final Vector<Site> favorites;
	private final JSplitPane jsp;

	private final ParameterPanel parameterPanel;

	private final Resource resource;

	private final SitePanel sitePanel;

	/**
	 * Constructor with no arguments
	 */
	public SiteManager() {
		this.resource = Resource.getInstance();
		this.favorites = this.resource.getFavorites();

		this.parameterPanel = new ParameterPanel(this);
		this.sitePanel = new SitePanel(this, this.favorites);

		this.jsp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, this.sitePanel,
				this.parameterPanel);

		final JPanel panel = new JPanel(new GridLayout(1, 1));
		panel.add(this.jsp);
		this.setMessage(panel);

		this.jsp.setDividerLocation(160);
		this.setOptionType(JOptionPane.OK_CANCEL_OPTION);
		
		init();
	}
	
	private void init() {
		if (sitePanel.siteListModel.getSize() == 0) {
			sitePanel.siteListModel.addElement("新站台"); //$NON-NLS-1$
			sitePanel.favorites
					.add(new Site("新站台", "hostname", 23, Protocol.TELNET)); //$NON-NLS-1$ //$NON-NLS-2$
			sitePanel.siteList.setSelectedIndex(sitePanel.siteListModel.getSize() - 1);
		}
	}

	/**
	 * Ask site panel to update favorite sites.
	 * 
	 * @param site
	 *            newer site to be updated
	 */
	public void updateFavorite(final Site site) {
		this.sitePanel.updateFavorite(site);
	}

	/**
	 * Ask parameter panel to update parameters with selected site.
	 * 
	 * @param site
	 *            newer site to be updated
	 */
	public void updateParameter(final Site site) {
		this.parameterPanel.updateParameter(site);
	}

	protected void submit() {
		// 將修改更新後寫回設定檔
		final Site s = new Site();
		s.name = this.parameterPanel.nameField.getText();
		s.host = this.parameterPanel.hostField.getText();
		s.port = Integer.parseInt(this.parameterPanel.portField.getText());
		s.alias = this.parameterPanel.aliasField.getText();

		if (this.parameterPanel.telnetButton.isSelected()) {
			s.protocol = Protocol.TELNET;
		} else if (this.parameterPanel.sshButton.isSelected()) {
			s.protocol = Protocol.SSH;
		}

		s.encoding = this.parameterPanel.encodingCombo.getSelectedItem()
				.toString();
		s.emulation = this.parameterPanel.emulationCombo.getSelectedItem()
				.toString();

		s.autoconnect = this.parameterPanel.autoConnectCheckBox.isSelected();
		// s.autologin = autoLoginCheckBox.isSelected();
		this.updateFavorite(s);
		this.resource.setFavorites(this.favorites);
		this.resource.writeRcFile();
		Model.getInstance().updateFavoriteMenu();
	}
}




