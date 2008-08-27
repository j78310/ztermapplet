package org.zhouer.zterm;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.Locale;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.JApplet;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;

import org.zhouer.utils.InternationalMessages;
import org.zhouer.vt.Config;

/**
 * ZTerm is view for Java applet, and also entrance of program.
 * 
 * @author h45
 */
public class ZTerm extends JApplet {
	private static final long serialVersionUID = 6304594468121008572L;

	protected BufferedImage terminalImage;
	protected JMenuItem big5Item, utf8Item;
	protected JMenuItem copyItem, pasteItem, colorCopyItem, colorPasteItem;
	protected JMenuItem[] favoriteItems;
	
	protected JMenuItem openItem, closeItem, reopenItem;

	protected JMenuItem popupCopyItem, popupPasteItem, popupColorCopyItem,
			popupColorPasteItem, popupCopyLinkItem;
	// popup 選單
	protected JPopupMenu popupMenu;
	protected JMenuItem preferenceItem, siteManagerItem;

	// 分頁
	protected JTabbedPane tabbedPane;

	// 分頁 icon
	protected final ImageIcon tryingIcon, connectedIcon, closedIcon, bellIcon;
	protected JMenuItem usageItem, faqItem, aboutItem;
	private final ActionHandler actionController;
	private final ChangeHandler changeController;
	private final ComponentHandler componentController;
	private final MouseHandler mouseController;

	private JMenu fileMenu, editMenu, viewMenu, toolsMenu, helpMenu;

	private JMenu encodingMenu;
	
	private JMenu languageMenu;
	
	private JMenu historyMenu;
	
	protected JMenuItem[] languageItems;

	private final KeyEventHandler keyEventController;

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

		// 初始化各種 icon
		this.tryingIcon = new ImageIcon(ClassLoader.getSystemResource(InternationalMessages
				.getString("ZTerm.Trying_Icon_File"))); //$NON-NLS-1$
		this.connectedIcon = new ImageIcon(ClassLoader.getSystemResource(InternationalMessages
				.getString("ZTerm.Connected_Icon_File"))); //$NON-NLS-1$
		this.closedIcon = new ImageIcon(ClassLoader.getSystemResource(InternationalMessages
				.getString("ZTerm.Closed_Icon_File"))); //$NON-NLS-1$
		this.bellIcon = new ImageIcon(ClassLoader.getSystemResource(InternationalMessages
				.getString("ZTerm.Bell_Icon_File"))); //$NON-NLS-1$

		// 建立事件控制器
		this.actionController = new ActionHandler();
		this.changeController = new ChangeHandler();
		this.componentController = new ComponentHandler();
		this.keyEventController = new KeyEventHandler();
		mouseController = new MouseHandler();

		// 建立系統核心
		this.model = Model.getInstance();
		this.model.setView(this);
		
		this.configMemberField();
	}
	
	private void makeLanguageMenu() {
		final int languageAmount = 2;
		languageItems = new JMenuItem[languageAmount];
		languageItems[0] = new JMenuItem(InternationalMessages.getString("ZTerm.Language_English_Item"));
		languageItems[0].setToolTipText(InternationalMessages.getString("ZTerm.Language_English_ToolTip"));
		languageItems[0].addActionListener(actionController);
		languageItems[1] = new JMenuItem(InternationalMessages.getString("ZTerm.Language_TraditionalChinese_Item"));
		languageItems[1].setToolTipText(InternationalMessages.getString("ZTerm.Language_TraditionalChinese_ToolTip"));
		languageItems[1].addActionListener(actionController);
		
		for (int i = 0; i < languageItems.length; i++) {
			languageMenu.add(languageItems[i]);
		}
	}

	/**
	 * Update favorite menu with resource.
	 */
	public void updateFavoriteMenu() {
		final Vector favorites = this.resource.getFavorites();
		this.favoriteItems = new JMenuItem[favorites.size()];

		this.historyMenu.removeAll();

		// 顯示目前我的最愛內容
		for (int i = 0; i < favorites.size(); i++) {
			final Site fa = (Site) favorites.elementAt(i);
			this.favoriteItems[i] = new JMenuItem(fa.name);
			this.favoriteItems[i].setToolTipText(fa.host + ":" + fa.port); //$NON-NLS-1$
			this.favoriteItems[i].addActionListener(this.actionController);
			this.historyMenu.add(this.favoriteItems[i]);
		}
	}

	/**
	 * Update screen size of sessions with the dimension of view.
	 */
	public void updateSize() {
		SessionPane session;

		// 產生跟主視窗一樣大的 image
		this.terminalImage = new BufferedImage(this.getWidth(), this.getHeight(),
				BufferedImage.TYPE_INT_RGB);

		// 視窗大小調整時同步更新每個 session 的大小
		for (int i = 0; i < this.sessions.size(); i++) {
			session = (SessionPane) this.sessions.elementAt(i);
			session.validate();
			session.updateImage(this.terminalImage);
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
	public void updateTabState(final int state, final SessionPane session) {
		int index;
		ImageIcon imageIcon;

		switch (state) {
		case SessionPane.STATE_TRYING:
			imageIcon = this.tryingIcon;
			break;
		case SessionPane.STATE_CONNECTED:
			imageIcon = this.connectedIcon;
			break;
		case SessionPane.STATE_CLOSED:
			imageIcon = this.closedIcon;
			break;
		case SessionPane.STATE_ALERT:
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
	 * 預先讀取 font metrics 以加快未來開啟連線視窗的速度
	 */
	private void cacheFont() {
		final String family = this.resource.getStringValue(Config.FONT_FAMILY);
		final Font font = new Font(family, Font.PLAIN, 0);
		// 這個動作很慢
		this.getFontMetrics(font);
	}

	private void configMemberField() {

		// 設定主畫面 Layout
		this.getContentPane().setLayout(new BorderLayout());

		this.makeMenu();
		makePopupMenu();
		this.makeTabbedPane();
		this.makeLanguageMenu();
		
		// 設定視窗位置、大小
		this.model.updateBounds();
		// 設定好視窗大小後才知道 image 大小
		this.terminalImage = new BufferedImage(this.getWidth(), this.getHeight(),
				BufferedImage.TYPE_INT_RGB);

		// 更新畫面上的文字
		updateText();
		
		this.setVisible(true);

		// 設定事件控制器的目標主題
		this.actionController.setView(this);
		this.actionController.setModel(this.model);
		this.changeController.setView(this);
		this.changeController.setModel(this.model);
		this.keyEventController.setView(this);
		this.keyEventController.setModel(this.model);
		this.componentController.setView(this);
		this.componentController.setModel(this.model);
		mouseController.setView(this);
		mouseController.setModel(model);

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
		this.fileMenu = new JMenu();
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
		this.usageItem = new JMenuItem();
		this.faqItem = new JMenuItem();
		this.aboutItem = new JMenuItem();
		this.big5Item = new JMenuItem();
		this.utf8Item = new JMenuItem();
		
		this.fileMenu.setMnemonic(KeyEvent.VK_F);
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
		this.usageItem.addActionListener(this.actionController);
		this.faqItem.addActionListener(this.actionController);
		this.aboutItem.addActionListener(this.actionController);
		this.big5Item.addActionListener(this.actionController);
		this.utf8Item.addActionListener(this.actionController);
		
		this.fileMenu.add(this.openItem);
		this.fileMenu.add(this.reopenItem);
		this.fileMenu.add(this.closeItem);

		this.updateFavoriteMenu();

		this.encodingMenu.add(this.big5Item);
		this.encodingMenu.add(this.utf8Item);

		this.editMenu.add(this.copyItem);
		this.editMenu.add(this.pasteItem);
		this.editMenu.add(this.colorCopyItem);
		this.editMenu.add(this.colorPasteItem);
		
		this.viewMenu.add(this.encodingMenu);
		this.viewMenu.add(this.languageMenu);

		this.toolsMenu.add(this.preferenceItem);
		this.toolsMenu.add(this.siteManagerItem);

		this.helpMenu.add(this.usageItem);
		this.helpMenu.add(this.faqItem);
		this.helpMenu.add(this.aboutItem);
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

		this.popupMenu.add(fileMenu);
		this.popupMenu.add(editMenu);
		this.popupMenu.add(viewMenu);
		this.popupMenu.add(historyMenu);
		this.popupMenu.add(toolsMenu);
		popupMenu.add(helpMenu);
	}

	private void makeTabbedPane() {
		// tab 擺在上面，太多 tab 時使用捲頁的顯示方式
		this.tabbedPane = new JTabbedPane(SwingConstants.TOP,
				JTabbedPane.SCROLL_TAB_LAYOUT);
		this.tabbedPane.addChangeListener(this.changeController);
		this.tabbedPane.addMouseListener(mouseController);
		this.getContentPane().add(this.tabbedPane, BorderLayout.CENTER);
	}
	
	public void updateText() {		
		this.fileMenu.setText(InternationalMessages
				.getString("ZTerm.Connect_Menu_Text")); //$NON-NLS-1$
		this.fileMenu.setToolTipText(InternationalMessages
				.getString("ZTerm.Connect_Menu_ToolTip")); //$NON-NLS-1$
		
		this.languageMenu.setText("Language");
		this.languageMenu.setToolTipText("Change your language");
		
		historyMenu.setText(InternationalMessages
				.getString("ZTerm.Site_Menu_Text"));

		this.viewMenu.setText(InternationalMessages.getString("ZTerm.View_Menu_Text")); //$NON-NLS-1$
		this.viewMenu.setToolTipText(InternationalMessages
				.getString("ZTerm.View_Menu_ToolTip")); //$NON-NLS-1$
		
		historyMenu.setText(InternationalMessages.getString("ZTerm.History_Menu_Text")); //$NON-NLS-1$
		historyMenu.setToolTipText(InternationalMessages
				.getString("ZTerm.History_Menu_ToolTip")); //$NON-NLS-1$

		this.editMenu.setText(InternationalMessages.getString("ZTerm.Edit_Menu_Text")); //$NON-NLS-1$
		this.editMenu.setToolTipText(InternationalMessages
				.getString("ZTerm.Edit_Menu_ToolTip")); //$NON-NLS-1$

		this.toolsMenu.setText(InternationalMessages
				.getString("ZTerm.Option_Menu_Text")); //$NON-NLS-1$
		this.toolsMenu.setToolTipText(InternationalMessages
				.getString("ZTerm.Option_Menu_ToolTip")); //$NON-NLS-1$

		this.helpMenu.setText(InternationalMessages.getString("ZTerm.Help_Menu_Text")); //$NON-NLS-1$
		this.helpMenu.setToolTipText(InternationalMessages
				.getString("ZTerm.Help_Menu_ToolTip")); //$NON-NLS-1$

		this.encodingMenu.setText(InternationalMessages
				.getString("ZTerm.Encoding_Menu_Text")); //$NON-NLS-1$

		this.openItem.setText(InternationalMessages
				.getString("ZTerm.Open_MenuItem_Text")); //$NON-NLS-1$
		this.openItem.setToolTipText(InternationalMessages
				.getString("ZTerm.Open_MenuItem_ToolTip")); //$NON-NLS-1$

		this.closeItem.setText(InternationalMessages
				.getString("ZTerm.Close_MenuItem_Text")); //$NON-NLS-1$
		this.closeItem.setToolTipText(InternationalMessages
				.getString("ZTerm.Close_MenuItem_ToolTip")); //$NON-NLS-1$

		this.reopenItem.setText(InternationalMessages
				.getString("ZTerm.Reopen_Item_Text")); //$NON-NLS-1$
		this.reopenItem.setToolTipText(InternationalMessages
				.getString("ZTerm.Reopen_Item_ToolTip")); //$NON-NLS-1$

		this.copyItem.setText(InternationalMessages
				.getString("ZTerm.Copy_MenuItem_Text")); //$NON-NLS-1$
		this.copyItem.setToolTipText(InternationalMessages
				.getString("ZTerm.Copy_MenuItem_ToolTip")); //$NON-NLS-1$

		this.pasteItem.setText(InternationalMessages
				.getString("ZTerm.Paste_MenuItem_Text")); //$NON-NLS-1$
		this.pasteItem.setToolTipText(InternationalMessages
				.getString("ZTerm.Paste_MenuItem_ToolTip")); //$NON-NLS-1$

		this.colorCopyItem.setText(InternationalMessages
				.getString("ZTerm.ColorCopy_MenuItem_Text")); //$NON-NLS-1$
		colorCopyItem.setToolTipText(InternationalMessages.getString("ZTerm.ColorCopy_MenuItem__ToolTip")); //$NON-NLS-1$

		this.colorPasteItem.setText(InternationalMessages
				.getString("ZTerm.ColorPaste_MenuItem_Text")); //$NON-NLS-1$
		colorPasteItem.setToolTipText(InternationalMessages.getString("ZTerm.ColorPaste_MenuItem__ToolTip")); //$NON-NLS-1$

		this.preferenceItem.setText(InternationalMessages
				.getString("ZTerm.Preference_MenuItem_Text")); //$NON-NLS-1$
		this.preferenceItem.setToolTipText(InternationalMessages
				.getString("ZTerm.Preference_MenuItem_ToolTip")); //$NON-NLS-1$

		this.siteManagerItem.setText(InternationalMessages
				.getString("ZTerm.SiteManager_MenuItem_Text")); //$NON-NLS-1$
		this.siteManagerItem.setToolTipText(InternationalMessages
				.getString("ZTerm.SiteManager_MenuItem_ToolTip")); //$NON-NLS-1$

		this.usageItem.setText(InternationalMessages
				.getString("ZTerm.Usage_MenuItem_Text")); //$NON-NLS-1$

		this.faqItem.setText(InternationalMessages
				.getString("ZTerm.FAQ_MenuItem_Text")); //$NON-NLS-1$

		this.aboutItem.setText(InternationalMessages
				.getString("ZTerm.About_MenuItem_Text")); //$NON-NLS-1$

		this.big5Item.setText(InternationalMessages
				.getString("ZTerm.Big5_MenuItem_Text")); //$NON-NLS-1$

		this.utf8Item.setText(InternationalMessages
				.getString("ZTerm.UTF8_MenuItem_Text")); //$NON-NLS-1$
		
		this.popupCopyLinkItem.setText(InternationalMessages
				.getString("ZTerm.Popup_CopyLink_MenuItem_Text")); //$NON-NLS-1$
		
		this.popupCopyItem.setText(InternationalMessages
				.getString("ZTerm.Copy_MenuItem_Text")); //$NON-NLS-1$
		this.popupCopyItem.setToolTipText(InternationalMessages
				.getString("ZTerm.Copy_MenuItem_ToolTip")); //$NON-NLS-1$

		this.popupPasteItem.setText(InternationalMessages
				.getString("ZTerm.Paste_MenuItem_Text")); //$NON-NLS-1$
		
		this.popupPasteItem.setToolTipText(InternationalMessages
				.getString("ZTerm.Paste_MenuItem_ToolTip")); //$NON-NLS-1$

		this.popupColorCopyItem.setText(InternationalMessages
				.getString("ZTerm.ColorCopy_MenuItem_Text")); //$NON-NLS-1$
		popupColorCopyItem.setToolTipText(InternationalMessages.getString("ZTerm.ColorCopy_MenuItem__ToolTip")); //$NON-NLS-1$

		this.popupColorPasteItem.setText(InternationalMessages
				.getString("ZTerm.ColorPaste_MenuItem_Text")); //$NON-NLS-1$
		popupColorPasteItem.setToolTipText(InternationalMessages.getString("ZTerm.ColorPaste_MenuItem__ToolTip")); //$NON-NLS-1$
	}
}
