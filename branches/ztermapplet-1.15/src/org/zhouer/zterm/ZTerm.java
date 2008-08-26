package org.zhouer.zterm;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.Locale;
import java.util.Vector;

import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JApplet;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.text.JTextComponent;

import org.zhouer.vt.Config;

/**
 * ZTerm is view for Java applet, and also entrance of program.
 * 
 * @author h45
 */
public class ZTerm extends JApplet {
	private static final long serialVersionUID = 6304594468121008572L;

	protected BufferedImage bi;
	protected JMenuItem big5Item, utf8Item;
	// 連線工具列
	protected JToolBar connectionToolbar;
	protected JButton copyButton, colorCopyButton, pasteButton,
			colorPasteButton;
	protected JMenuItem copyItem, pasteItem, colorCopyItem, colorPasteItem;
	protected JMenuItem[] favoriteItems;
	

	protected JButton openButton, closeButton, reopenButton;
	protected JMenuItem openItem, closeItem, reopenItem;

	protected JMenuItem popupCopyItem, popupPasteItem, popupColorCopyItem,
			popupColorPasteItem, popupCopyLinkItem;
	// popup 選單
	protected JPopupMenu popupMenu;
	protected JMenuItem preferenceItem, siteManagerItem, showToolbarItem;

	protected JComboBox siteField;
	protected DefaultComboBoxModel siteModel;

	protected JTextComponent siteText;

	// 分頁
	protected JTabbedPane tabbedPane;

	protected JButton telnetButton, sshButton;

	// 分頁 icon
	protected final ImageIcon tryingIcon, connectedIcon, closedIcon, bellIcon;
	protected JMenuItem usageItem, faqItem, aboutItem;
	private final ActionHandler actionController;
	private final ChangeHandler changeController;
	private final ComponentHandler componentController;

	private JMenu connectMenu, editMenu, toolsMenu, helpMenu;

	private JMenu encodingMenu;
	
	private JMenu languageMenu;
	
	private JMenu viewMenu;
	
	private JMenu historyMenu;
	
	protected JMenuItem[] languageItems;

	private final KeyHandler keyController;

	private final KeyEventHandler keyEventController;

	// 標準選單
	private JMenuBar menuBar;

	private final Model model;

	private final Resource resource;

	private final Sessions sessions;

	/**
	 * Constructor with no arguments
	 */
	public ZTerm() {
		this.sessions = Sessions.getInstance(); // 各個連線
		this.resource = Resource.getInstance(); // 各種設定
		
		// 設定語系
		Locale.setDefault(this.resource.getLocale());

		// 設定 Look and Feel
		this.updateLookAndFeel();

		// 初始化各種 icon
		this.tryingIcon = new ImageIcon(ZTerm.class.getResource(Messages
				.getString("ZTerm.Trying_Icon_File"))); //$NON-NLS-1$
		this.connectedIcon = new ImageIcon(ZTerm.class.getResource(Messages
				.getString("ZTerm.Connected_Icon_File"))); //$NON-NLS-1$
		this.closedIcon = new ImageIcon(ZTerm.class.getResource(Messages
				.getString("ZTerm.Closed_Icon_File"))); //$NON-NLS-1$
		this.bellIcon = new ImageIcon(ZTerm.class.getResource(Messages
				.getString("ZTerm.Bell_Icon_File"))); //$NON-NLS-1$

		// 建立事件控制器
		this.actionController = new ActionHandler();
		this.changeController = new ChangeHandler();
		this.componentController = new ComponentHandler();
		this.keyEventController = new KeyEventHandler();
		this.keyController = new KeyHandler();

		// 建立系統核心
		this.model = Model.getInstance();
		this.model.setView(this);
		
		this.configMemberField();
	}
	
	private void makeLanguageMenu() {
		final int languageAmount = 2;
		languageItems = new JMenuItem[languageAmount];
		languageItems[0] = new JMenuItem(Messages.getString("ZTerm.Language_English_Item"));
		languageItems[0].setToolTipText(Messages.getString("ZTerm.Language_English_ToolTip"));
		languageItems[0].addActionListener(actionController);
		languageItems[1] = new JMenuItem(Messages.getString("ZTerm.Language_TraditionalChinese_Item"));
		languageItems[1].setToolTipText(Messages.getString("ZTerm.Language_TraditionalChinese_ToolTip"));
		languageItems[1].addActionListener(actionController);
		
		for (int i = 0; i < languageItems.length; i++) {
			languageMenu.add(languageItems[i]);
		}
	}

	/**
	 * Update favorite menu with resource.
	 */
	public void updateFavoriteMenu() {
		final Vector<Site> favorites = this.resource.getFavorites();
		this.favoriteItems = new JMenuItem[favorites.size()];

		this.historyMenu.removeAll();

		// 顯示目前我的最愛內容
		for (int i = 0; i < favorites.size(); i++) {
			final Site fa = favorites.elementAt(i);
			this.favoriteItems[i] = new JMenuItem(fa.name);
			this.favoriteItems[i].setToolTipText(fa.host + ":" + fa.port); //$NON-NLS-1$
			this.favoriteItems[i].addActionListener(this.actionController);
			this.historyMenu.add(this.favoriteItems[i]);
		}
	}

	/**
	 * Update look and feel with resource.
	 */
	public void updateLookAndFeel() {
		try {
			if (this.resource.getBooleanValue(Resource.SYSTEM_LOOK_FEEL)) {
				UIManager.setLookAndFeel(UIManager
						.getSystemLookAndFeelClassName());
			} else {
				UIManager.setLookAndFeel(UIManager
						.getCrossPlatformLookAndFeelClassName());
			}
		} catch (final Exception e) {
			e.printStackTrace();
		} finally {
			SwingUtilities.updateComponentTreeUI(this);
		}
	}

	/**
	 * Update screen size of sessions with the dimension of view.
	 */
	public void updateSize() {
		Session session;

		// 產生跟主視窗一樣大的 image
		this.bi = new BufferedImage(this.getWidth(), this.getHeight(),
				BufferedImage.TYPE_INT_RGB);

		// 視窗大小調整時同步更新每個 session 的大小
		for (int i = 0; i < this.sessions.size(); i++) {
			session = this.sessions.elementAt(i);
			session.validate();
			session.updateImage(this.bi);
			session.updateSize();
		}
	}

	/**
	 * Update the icon on tab page of which session be modified with newer
	 * state.
	 * 
	 * @param state
	 *            newer state of session
	 * @param session
	 *            target session to be modified the icon
	 */
	public void updateTabState(final int state, final Session session) {
		int index;
		ImageIcon imageIcon;

		switch (state) {
		case Session.STATE_TRYING:
			imageIcon = this.tryingIcon;
			break;
		case Session.STATE_CONNECTED:
			imageIcon = this.connectedIcon;
			break;
		case Session.STATE_CLOSED:
			imageIcon = this.closedIcon;
			break;
		case Session.STATE_ALERT:
			imageIcon = this.bellIcon;
			break;
		default:
			imageIcon = null;
		}

		index = this.tabbedPane.indexOfComponent(session);
		if (index != -1) {
			this.tabbedPane.setIconAt(index, imageIcon);
		}
	}

	/**
	 * Update tool bar with resource, hiding or showing it.
	 * 
	 * @param isShowToolbar true, show tool bar; false, hide tool bar.
	 */
	public void updateToolbar(final boolean isShowToolbar) {

		this.showToolbarItem
				.setText(isShowToolbar ? Messages
						.getString("ZTerm.ToggleToolbar_MenuItem_Hide_Text") : Messages.getString("ZTerm.ToggleToolbar_MenuItem_Show_Text")); //$NON-NLS-1$ //$NON-NLS-2$

		this.connectionToolbar.setVisible(isShowToolbar);
		this.validate();
	}

	/**
	 * 預先讀取 font metrics 以加快未來開啟連線視窗的速度
	 */
	private void cacheFont() {
		final String family = this.resource.getStringValue(Config.FONT_FAMILY);
		final Font font = new Font(family, Font.PLAIN, 0);
		// 這個動作很慢
		this.getFontMetrics(font);
	}

	private void configMemberField() {
		this.resource.getBooleanValue(Resource.SHOW_TOOLBAR);

		// 設定主畫面 Layout
		this.getContentPane().setLayout(new BorderLayout());

		this.makeMenu();
		makePopupMenu();
		this.makeTabbedPane();
		this.makeToolbar();
		this.makeLanguageMenu();
		
		// 設定視窗位置、大小
		this.model.updateBounds();
		// 設定好視窗大小後才知道 image 大小
		this.bi = new BufferedImage(this.getWidth(), this.getHeight(),
				BufferedImage.TYPE_INT_RGB);

		// 更新畫面上的文字
		updateText();
		
		this.setVisible(true);

		// 設定事件控制器的目標主題
		this.actionController.setView(this);
		this.actionController.setModel(this.model);
		this.changeController.setView(this);
		this.changeController.setModel(this.model);
		this.keyController.setView(this);
		this.keyController.setModel(this.model);
		this.keyEventController.setView(this);
		this.keyEventController.setModel(this.model);
		this.componentController.setView(this);
		this.componentController.setModel(this.model);

		// 設定系統核心的目標介面
		this.model.setView(this);

		// 接收視窗大小改變的事件
		this.addComponentListener(this.componentController);

		// 攔截鍵盤 event 以處理快速鍵
		KeyboardFocusManager.getCurrentKeyboardFocusManager()
				.addKeyEventDispatcher(this.keyEventController);

		// 在程式啟動時就先讀一次字型，讓使用者開第一個連線視窗時不會感覺太慢。
		this.cacheFont();

		// 自動連線
		this.model.autoconnect();
	}

	// 建立主選單
	private void makeMenu() {
		this.menuBar = new JMenuBar();
		this.connectMenu = new JMenu();
		this.languageMenu = new JMenu();
		this.editMenu = new JMenu();
		this.viewMenu = new JMenu();
		this.historyMenu = new JMenu();
		this.toolsMenu = new JMenu();
		this.helpMenu = new JMenu();
		this.encodingMenu = new JMenu();
		this.openItem = new JMenuItem();
		this.closeItem = new JMenuItem();
		this.reopenItem = new JMenuItem();
		this.copyItem = new JMenuItem();
		this.pasteItem = new JMenuItem();
		this.colorCopyItem = new JMenuItem();
		this.colorPasteItem = new JMenuItem();
		this.preferenceItem = new JMenuItem();
		this.siteManagerItem = new JMenuItem();
		this.showToolbarItem = new JMenuItem();
		this.usageItem = new JMenuItem();
		this.faqItem = new JMenuItem();
		this.aboutItem = new JMenuItem();
		this.big5Item = new JMenuItem();
		this.utf8Item = new JMenuItem();
		
		this.connectMenu.setMnemonic(KeyEvent.VK_F);
		this.editMenu.setMnemonic(KeyEvent.VK_E);
		viewMenu.setMnemonic(KeyEvent.VK_V);
		historyMenu.setMnemonic(KeyEvent.VK_Y);
		this.toolsMenu.setMnemonic(KeyEvent.VK_T);
		this.helpMenu.setMnemonic(KeyEvent.VK_H);
		
		this.openItem.addActionListener(this.actionController);
		this.closeItem.addActionListener(this.actionController);
		this.reopenItem.addActionListener(this.actionController);
		this.copyItem.addActionListener(this.actionController);
		this.pasteItem.addActionListener(this.actionController);
		this.colorCopyItem.addActionListener(this.actionController);
		this.colorPasteItem.addActionListener(this.actionController);
		this.preferenceItem.addActionListener(this.actionController);
		this.siteManagerItem.addActionListener(this.actionController);
		this.showToolbarItem.addActionListener(this.actionController);
		this.usageItem.addActionListener(this.actionController);
		this.faqItem.addActionListener(this.actionController);
		this.aboutItem.addActionListener(this.actionController);
		this.big5Item.addActionListener(this.actionController);
		this.utf8Item.addActionListener(this.actionController);
		
		this.menuBar.add(this.connectMenu);
		this.menuBar.add(this.editMenu);
		this.menuBar.add(this.viewMenu);
		menuBar.add(historyMenu);
		this.menuBar.add(this.toolsMenu);
		this.menuBar.add(this.helpMenu);

		this.connectMenu.add(this.openItem);
		this.connectMenu.add(this.reopenItem);
		this.connectMenu.add(this.closeItem);

		this.updateFavoriteMenu();

		this.encodingMenu.add(this.big5Item);
		this.encodingMenu.add(this.utf8Item);

		this.editMenu.add(this.copyItem);
		this.editMenu.add(this.pasteItem);
		this.editMenu.add(this.colorCopyItem);
		this.editMenu.add(this.colorPasteItem);
		
		this.viewMenu.add(this.showToolbarItem);
		this.viewMenu.add(this.encodingMenu);
		this.viewMenu.add(this.languageMenu);

		this.toolsMenu.add(this.preferenceItem);
		this.toolsMenu.add(this.siteManagerItem);

		this.helpMenu.add(this.usageItem);
		this.helpMenu.add(this.faqItem);
		this.helpMenu.add(this.aboutItem);

		this.setJMenuBar(this.menuBar);
	}
	
	private void makePopupMenu() {
		this.popupMenu = new JPopupMenu();

		this.popupCopyItem = new JMenuItem();
		this.popupPasteItem = new JMenuItem();
		this.popupColorCopyItem = new JMenuItem();
		this.popupColorPasteItem = new JMenuItem();
		this.popupCopyLinkItem = new JMenuItem();
		
		this.popupCopyItem.addActionListener(this.actionController);
		this.popupPasteItem.addActionListener(this.actionController);
		this.popupColorCopyItem.addActionListener(this.actionController);
		this.popupColorPasteItem.addActionListener(this.actionController);
		this.popupCopyLinkItem.addActionListener(this.actionController);
		this.popupCopyLinkItem.setEnabled(false);

		this.popupMenu.add(this.popupCopyItem);
		this.popupMenu.add(this.popupPasteItem);
		this.popupMenu.add(this.popupColorCopyItem);
		this.popupMenu.add(this.popupColorPasteItem);
		this.popupMenu.add(this.popupCopyLinkItem);
	}

	private void makeTabbedPane() {
		// tab 擺在上面，太多 tab 時使用捲頁的顯示方式
		this.tabbedPane = new JTabbedPane(SwingConstants.TOP,
				JTabbedPane.SCROLL_TAB_LAYOUT);
		this.tabbedPane.addChangeListener(this.changeController);
		this.getContentPane().add(this.tabbedPane);
	}

	private void makeToolbar() {
		final boolean isShowToolbar = this.resource
				.getBooleanValue(Resource.SHOW_TOOLBAR);
		
		this.connectionToolbar = new JToolBar();
		this.connectionToolbar.setVisible(isShowToolbar);
		this.connectionToolbar.setRollover(true);

		this.closeButton = new JButton();
		this.closeButton.setFocusable(false);
		this.closeButton.addActionListener(this.actionController);

		this.reopenButton = new JButton();
		this.reopenButton.setFocusable(false);
		this.reopenButton.addActionListener(this.actionController);

		this.copyButton = new JButton();
		this.copyButton.setFocusable(false);
		this.copyButton.addActionListener(this.actionController);

		this.pasteButton = new JButton();
		this.pasteButton.setFocusable(false);
		this.pasteButton.addActionListener(this.actionController);

		this.colorCopyButton = new JButton();
		this.colorCopyButton.setFocusable(false);
		this.colorCopyButton.addActionListener(this.actionController);

		this.colorPasteButton = new JButton();
		this.colorPasteButton.setFocusable(false);
		this.colorPasteButton.addActionListener(this.actionController);

		this.telnetButton = new JButton();
		this.telnetButton.setFocusable(false);
		this.telnetButton.addActionListener(this.actionController);

		this.sshButton = new JButton();
		this.sshButton.setFocusable(false);
		this.sshButton.addActionListener(this.actionController);

		this.siteModel = new DefaultComboBoxModel();
		this.siteField = new JComboBox(this.siteModel);
		this.siteField.setEditable(true);

		this.siteText = (JTextComponent) this.siteField.getEditor()
				.getEditorComponent();
		this.siteText.addKeyListener(this.keyController);

		this.openButton = new JButton();
		this.openButton.setFocusable(false);
		this.openButton.addActionListener(this.actionController);

		this.connectionToolbar.add(this.closeButton);
		this.connectionToolbar.add(this.reopenButton);

		this.connectionToolbar.add(new JToolBar.Separator());

		this.connectionToolbar.add(this.copyButton);
		this.connectionToolbar.add(this.pasteButton);
		this.connectionToolbar.add(this.colorCopyButton);
		this.connectionToolbar.add(this.colorPasteButton);

		this.connectionToolbar.add(new JToolBar.Separator());

		this.connectionToolbar.add(this.telnetButton);
		this.connectionToolbar.add(this.sshButton);

		this.connectionToolbar.add(new JToolBar.Separator());

		this.connectionToolbar.add(this.siteField);

		this.connectionToolbar.add(new JToolBar.Separator());

		this.connectionToolbar.add(this.openButton);

		this.getContentPane().add(this.connectionToolbar, BorderLayout.NORTH);
	}
	
	public void updateText() {
		final boolean isShowToolbar = this.resource
		.getBooleanValue(Resource.SHOW_TOOLBAR);
		
		this.connectMenu.setText(Messages
				.getString("ZTerm.Connect_Menu_Text")); //$NON-NLS-1$
		this.connectMenu.setToolTipText(Messages
				.getString("ZTerm.Connect_Menu_ToolTip")); //$NON-NLS-1$
		
		this.languageMenu.setText("Language");
		this.languageMenu.setToolTipText("Change your language");
		
		historyMenu.setText(Messages
				.getString("ZTerm.Site_Menu_Text"));

		this.viewMenu.setText(Messages.getString("ZTerm.View_Menu_Text")); //$NON-NLS-1$
		this.viewMenu.setToolTipText(Messages
				.getString("ZTerm.View_Menu_ToolTip")); //$NON-NLS-1$
		
		historyMenu.setText(Messages.getString("ZTerm.History_Menu_Text")); //$NON-NLS-1$
		historyMenu.setToolTipText(Messages
				.getString("ZTerm.History_Menu_ToolTip")); //$NON-NLS-1$

		this.editMenu.setText(Messages.getString("ZTerm.Edit_Menu_Text")); //$NON-NLS-1$
		this.editMenu.setToolTipText(Messages
				.getString("ZTerm.Edit_Menu_ToolTip")); //$NON-NLS-1$

		this.toolsMenu.setText(Messages
				.getString("ZTerm.Option_Menu_Text")); //$NON-NLS-1$
		this.toolsMenu.setToolTipText(Messages
				.getString("ZTerm.Option_Menu_ToolTip")); //$NON-NLS-1$

		this.helpMenu.setText(Messages.getString("ZTerm.Help_Menu_Text")); //$NON-NLS-1$
		this.helpMenu.setToolTipText(Messages
				.getString("ZTerm.Help_Menu_ToolTip")); //$NON-NLS-1$

		this.encodingMenu.setText(Messages
				.getString("ZTerm.Encoding_Menu_Text")); //$NON-NLS-1$

		this.openItem.setText(Messages
				.getString("ZTerm.Open_MenuItem_Text")); //$NON-NLS-1$
		this.openItem.setToolTipText(Messages
				.getString("ZTerm.Open_MenuItem_ToolTip")); //$NON-NLS-1$

		this.closeItem.setText(Messages
				.getString("ZTerm.Close_MenuItem_Text")); //$NON-NLS-1$
		this.closeItem.setToolTipText(Messages
				.getString("ZTerm.Close_MenuItem_ToolTip")); //$NON-NLS-1$

		this.reopenItem.setText(Messages
				.getString("ZTerm.Reopen_Item_Text")); //$NON-NLS-1$
		this.reopenItem.setToolTipText(Messages
				.getString("ZTerm.Reopen_Item_ToolTip")); //$NON-NLS-1$

		this.copyItem.setText(Messages
				.getString("ZTerm.Copy_MenuItem_Text")); //$NON-NLS-1$
		this.copyItem.setToolTipText(Messages
				.getString("ZTerm.Copy_MenuItem_ToolTip")); //$NON-NLS-1$

		this.pasteItem.setText(Messages
				.getString("ZTerm.Paste_MenuItem_Text")); //$NON-NLS-1$
		this.pasteItem.setToolTipText(Messages
				.getString("ZTerm.Paste_MenuItem_ToolTip")); //$NON-NLS-1$

		this.colorCopyItem.setText(Messages
				.getString("ZTerm.ColorCopy_MenuItem_Text")); //$NON-NLS-1$
		colorCopyItem.setToolTipText(Messages.getString("ZTerm.ColorCopy_MenuItem__ToolTip")); //$NON-NLS-1$

		this.colorPasteItem.setText(Messages
				.getString("ZTerm.ColorPaste_MenuItem_Text")); //$NON-NLS-1$
		colorPasteItem.setToolTipText(Messages.getString("ZTerm.ColorPaste_MenuItem__ToolTip")); //$NON-NLS-1$

		this.preferenceItem.setText(Messages
				.getString("ZTerm.Preference_MenuItem_Text")); //$NON-NLS-1$
		this.preferenceItem.setToolTipText(Messages
				.getString("ZTerm.Preference_MenuItem_ToolTip")); //$NON-NLS-1$

		this.siteManagerItem.setText(Messages
				.getString("ZTerm.SiteManager_MenuItem_Text")); //$NON-NLS-1$
		this.siteManagerItem.setToolTipText(Messages
				.getString("ZTerm.SiteManager_MenuItem_ToolTip")); //$NON-NLS-1$

		this.showToolbarItem.setText(
				isShowToolbar ? Messages
						.getString("ZTerm.ToggleToolbar_MenuItem_Hide_Text") : Messages.getString("ZTerm.ToggleToolbar_MenuItem_Show_Text")); //$NON-NLS-1$ //$NON-NLS-2$

		this.usageItem.setText(Messages
				.getString("ZTerm.Usage_MenuItem_Text")); //$NON-NLS-1$

		this.faqItem.setText(Messages
				.getString("ZTerm.FAQ_MenuItem_Text")); //$NON-NLS-1$

		this.aboutItem.setText(Messages
				.getString("ZTerm.About_MenuItem_Text")); //$NON-NLS-1$

		this.big5Item.setText(Messages
				.getString("ZTerm.Big5_MenuItem_Text")); //$NON-NLS-1$

		this.utf8Item.setText(Messages
				.getString("ZTerm.UTF8_MenuItem_Text")); //$NON-NLS-1$
		
		this.popupCopyLinkItem.setText(Messages
				.getString("ZTerm.Popup_CopyLink_MenuItem_Text")); //$NON-NLS-1$
		
		this.popupCopyItem.setText(Messages
				.getString("ZTerm.Copy_MenuItem_Text")); //$NON-NLS-1$
		this.popupCopyItem.setToolTipText(Messages
				.getString("ZTerm.Copy_MenuItem_ToolTip")); //$NON-NLS-1$

		this.popupPasteItem.setText(Messages
				.getString("ZTerm.Paste_MenuItem_Text")); //$NON-NLS-1$
		
		this.popupPasteItem.setToolTipText(Messages
				.getString("ZTerm.Paste_MenuItem_ToolTip")); //$NON-NLS-1$

		this.popupColorCopyItem.setText(Messages
				.getString("ZTerm.ColorCopy_MenuItem_Text")); //$NON-NLS-1$
		popupColorCopyItem.setToolTipText(Messages.getString("ZTerm.ColorCopy_MenuItem__ToolTip")); //$NON-NLS-1$

		this.popupColorPasteItem.setText(Messages
				.getString("ZTerm.ColorPaste_MenuItem_Text")); //$NON-NLS-1$
		popupColorPasteItem.setToolTipText(Messages.getString("ZTerm.ColorPaste_MenuItem__ToolTip")); //$NON-NLS-1$

		
		
		this.closeButton.setText(Messages
				.getString("ZTerm.Close_Button_Text")); //$NON-NLS-1$
		this.closeButton.setToolTipText(Messages
				.getString("ZTerm.Close_Button_ToolTip")); //$NON-NLS-1$

		this.reopenButton.setText(Messages
				.getString("ZTerm.Reopen_Button_Text")); //$NON-NLS-1$
		this.reopenButton.setToolTipText(Messages
				.getString("ZTerm.Reopen_Button_ToolTip")); //$NON-NLS-1$

		this.copyButton.setText(Messages
				.getString("ZTerm.Copy_Button_Text")); //$NON-NLS-1$
		this.copyButton.setToolTipText(Messages
				.getString("ZTerm.Copy_Button_ToolTip")); //$NON-NLS-1$

		this.pasteButton.setText(Messages
				.getString("ZTerm.Paste_Button_Text")); //$NON-NLS-1$
		this.pasteButton.setToolTipText(Messages
				.getString("ZTerm.Paste_Button_ToolTip")); //$NON-NLS-1$

		this.colorCopyButton.setText(Messages
				.getString("ZTerm.ColorCopy_Button_Text")); //$NON-NLS-1$

		this.colorPasteButton.setText(Messages
				.getString("ZTerm.ColorPaste_Button_Text")); //$NON-NLS-1$

		this.telnetButton.setText(Messages
				.getString("ZTerm.Telnet_Button_Text")); //$NON-NLS-1$
		this.telnetButton.setToolTipText(Messages
				.getString("ZTerm.Telnet_Button_ToolTip")); //$NON-NLS-1$

		this.sshButton.setText(Messages
				.getString("ZTerm.SSH_Button_Text")); //$NON-NLS-1$
		this.sshButton.setToolTipText(Messages
				.getString("ZTerm.SSH_Button_ToolTip")); //$NON-NLS-1$
		
		this.siteField.setToolTipText(Messages
				.getString("ZTerm.Site_ComboBox_ToolTip")); //$NON-NLS-1$

		this.openButton .setText(Messages
				.getString("ZTerm.Open_Button_Text")); //$NON-NLS-1$
	}
}
