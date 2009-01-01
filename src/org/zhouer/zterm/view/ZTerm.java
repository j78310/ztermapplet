package org.zhouer.zterm.view;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.Locale;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.JApplet;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import org.zhouer.utils.InternationalMessages;
import org.zhouer.vt.Config;
import org.zhouer.zterm.model.Model;
import org.zhouer.zterm.model.Resource;
import org.zhouer.zterm.model.SessionPool;
import org.zhouer.zterm.model.Site;

/**
 * ZTerm is view for Java applet, and also entrance of program.
 * 
 * @author Chin-Chang Yang
 */
public class ZTerm extends JApplet {

	private static final long	serialVersionUID	= 1L;

	protected BufferedImage			terminalImage;
	protected JMenuItem				big5Item, utf8Item, copyItem, pasteItem,
			colorCopyItem, colorPasteItem, openItem, closeItem, reopenItem,
			popupCopyItem, popupPasteItem, popupColorCopyItem,
			popupColorPasteItem, popupCopyLinkItem, popupCloseItem, preferenceItem,
			siteManagerItem, usageItem, faqItem, aboutItem, hideMenuBarItem,
			showMenuBarItem;
	protected JMenuItem[]			favoriteItems, languageItems;

	// popup 選單
	protected JPopupMenu			popupMenu;

	// 分頁
	protected JTabbedPane			tabbedPane;

	// 分頁 icon
	protected final ImageIcon		tryingIcon, connectedIcon, closedIcon,
			bellIcon;

	private final ActionHandler		actionController	= new ActionHandler();
	private final ChangeHandler		changeController	= new ChangeHandler();
	private final ComponentHandler	componentController	= new ComponentHandler();
	private final MouseHandler		mouseController		= new MouseHandler();
	private final KeyEventHandler	keyEventController	= new KeyEventHandler();

	private JMenu					fileMenu, editMenu, viewMenu, toolsMenu,
			helpMenu, encodingMenu, languageMenu, historyMenu;

	private JMenuBar				menuBar;
	private final Model				model;
	private final Resource			resource;
	private final SessionPool			sessions;

	/**
	 * Constructor with no arguments
	 */
	public ZTerm() {
		sessions = SessionPool.getInstance(); // 各個連線
		resource = Resource.getInstance(); // 各種設定

		// 設定語系
		Locale.setDefault(resource.getLocale());

		// 初始化各種 icon
		tryingIcon = new ImageIcon(getClass().getResource(
			"/res/icon/trying.png"));
		connectedIcon = new ImageIcon(getClass().getResource(
			"/res/icon/connected.png"));
		closedIcon = new ImageIcon(getClass().getResource(
			"/res/icon/closed.png"));
		bellIcon = new ImageIcon(getClass().getResource("/res/icon/bell.png"));

		// 建立系統核心
		model = Model.getInstance();
		model.setView(this);

		configMemberField();
	}

	/**
	 * Change current session showed on the screen.
	 * 
	 * @param index
	 *            the index of session to be showed on the screen.
	 */
	public void changeSession(final int index) {
		if ((0 <= index) && (index < tabbedPane.getTabCount())) {

			// 取得焦點的工作
			final Runnable sessionFocuser = new Runnable() {
				public void run() {
					final SessionPane session = (SessionPane) sessions.get(index);
					session.requestFocusInWindow();
				}
			};

			// 切換分頁的工作 （包含取得焦點的工作）
			final Runnable tabbedSwitcher = new Runnable() {
				public void run() {
					tabbedPane.setSelectedIndex(index);
					SwingUtilities.invokeLater(sessionFocuser);
				}
			};

			// 啟動切換分頁的工作
			SwingUtilities.invokeLater(tabbedSwitcher);
		}
	}

	/**
	 * Getter of the current session which is able to be viewed
	 * 
	 * @return the current session
	 */
	public SessionPane getCurrentSession() {
		final SessionPane session = (SessionPane) tabbedPane.getSelectedComponent();

		return session;
	}

	/**
	 * Close current tab which is showing on the screen.
	 */
	public void closeCurrentTab() {
		final SessionPane session = getCurrentSession();

		if (session != null) {

			// 連線中則詢問是否要斷線
			if (!session.isClosed()) {
				if (model.showConfirm(
					InternationalMessages.getString("ZTerm.Message_Confirm_Close"), InternationalMessages.getString("ZTerm.Title_Confirm_Close"), JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) { //$NON-NLS-1$ //$NON-NLS-2$
					return;
				}

				// 通知 session 要中斷連線了
				session.close(false);

				if (!resource.getBooleanValue(Resource.REMOVE_MANUAL_DISCONNECT)) {
					return;
				}
			}

			// 通知 session 要被移除了
			session.remove();

			tabbedPane.remove(session);
			model.getSessions().remove(session);

			// 刪除分頁會影響分頁編號
			model.updateTabTitle();

			// 讓現在被選取的分頁取得 focus.
			model.updateTab();
		}
	}

	/**
	 * Update favorite menu with resource.
	 */
	public void updateFavoriteMenu() {
		final Vector favorites = resource.getFavorites();
		favoriteItems = new JMenuItem[favorites.size()];

		historyMenu.removeAll();

		// 顯示目前我的最愛內容
		for (int i = 0; i < favorites.size(); i++) {
			final Site fa = (Site) favorites.elementAt(i);
			favoriteItems[i] = new JMenuItem(fa.getName());
			favoriteItems[i].setToolTipText(fa.getHost() + ":" + fa.getPort()); //$NON-NLS-1$
			favoriteItems[i].addActionListener(actionController);
			historyMenu.add(favoriteItems[i]);
		}
	}

	/**
	 * Connect to the site at certain index of tab page.
	 * 
	 * @param site
	 *            site to be connected.
	 * @param index
	 *            the index of tab page corresponding to this site.
	 */
	public void connect(final Site site, final int index) {
		final SessionPane session = new SessionPane(site, terminalImage);

		// index 為連線後放在第幾個分頁，若為 -1 表開新分頁。
		if (index == -1) {
			model.getSessions().add(session);

			// 一開始預設 icon 是連線中斷
			final ImageIcon icon = closedIcon;

			tabbedPane.addTab(site.getName(), icon, session, site.getHost());
			tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 1);
		} else {
			model.getSessions().setElementAt(session, index);
			tabbedPane.setComponentAt(index, session);
		}

		// 每個 session 都是一個 thread, 解決主程式被 block 住的問題。
		new Thread(session).start();
	}

	/**
	 * Check if the session is selected on tab page.
	 * 
	 * @param session
	 * @return true if the session is selected on tab page; false, otherwise.
	 */
	public boolean isTabForeground(final SessionPane session) {
		return (tabbedPane.indexOfComponent(session) == tabbedPane.getSelectedIndex());
	}

	/**
	 * Reopen session which is currently disconnected.
	 * 
	 * @param session
	 *            session to be reopened.
	 */
	public void reopenSession(final SessionPane session) {
		if (session != null) {
			// 若連線中則開新分頁，已斷線則重連。
			if (session.isClosed()) {
				this.connect(session.getSite(),
					tabbedPane.indexOfComponent(session));
			} else {
				// FIXME magic number -1
				this.connect(session.getSite(), -1);
			}
		}
	}

	/**
	 * Show pop-up menu at current position.
	 * 
	 * @param x
	 *            the position x which located mouse.
	 * @param y
	 *            the position y which located mouse.
	 * @param link
	 *            the link which is selected.
	 */
	public void showPopup(final int x, final int y, final String link) {
		final Point viewLocation = getLocationOnScreen();

		popupCopyLinkItem.setEnabled(link != null);
		model.setCopiedLink(link);

		// 傳進來的是滑鼠相對於視窗左上角的座標，減去主視窗相對於螢幕左上角的座標，可得滑鼠相對於主視窗的座標。
		popupMenu.show(this, x - viewLocation.x, y - viewLocation.y);
	}

	/**
	 * Update tab title to each tab page.
	 */
	public void updateTabTitle() {
		for (int i = 0; i < tabbedPane.getTabCount(); i++) {
			final SessionPane session = (SessionPane) model.getSessions().elementAt(
				i);

			tabbedPane.setTitleAt(i, session.getSite().getName());
		}
	}

	/**
	 * Update screen size of sessions with the dimension of view.
	 */
	public void updateSize() {
		SessionPane session;

		// 產生跟主視窗一樣大的 image
		terminalImage = new BufferedImage(getWidth(), getHeight(),
			BufferedImage.TYPE_INT_RGB);

		// 視窗大小調整時同步更新每個 session 的大小
		for (int i = 0; i < sessions.size(); i++) {
			session = (SessionPane) sessions.elementAt(i);
			session.validate();
			session.updateImage(terminalImage);
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
			imageIcon = tryingIcon;
			break;
		case SessionPane.STATE_CONNECTED:
			imageIcon = connectedIcon;
			break;
		case SessionPane.STATE_CLOSED:
			imageIcon = closedIcon;
			break;
		case SessionPane.STATE_ALERT:
			imageIcon = bellIcon;
			break;
		default:
			imageIcon = null;
		}

		index = tabbedPane.indexOfComponent(session);
		if (index != -1) {
			tabbedPane.setIconAt(index, imageIcon);
		}
	}

	public void updateText() {
		fileMenu.setText(InternationalMessages.getString("ZTerm.Connect_Menu_Text")); //$NON-NLS-1$
		languageMenu.setText("Language");
		languageMenu.setToolTipText("Change your language");
		historyMenu.setText(InternationalMessages.getString("ZTerm.Site_Menu_Text"));
		viewMenu.setText(InternationalMessages.getString("ZTerm.View_Menu_Text")); //$NON-NLS-1$
		historyMenu.setText(InternationalMessages.getString("ZTerm.History_Menu_Text")); //$NON-NLS-1$
		editMenu.setText(InternationalMessages.getString("ZTerm.Edit_Menu_Text")); //$NON-NLS-1$
		toolsMenu.setText(InternationalMessages.getString("ZTerm.Option_Menu_Text")); //$NON-NLS-1$
		helpMenu.setText(InternationalMessages.getString("ZTerm.Help_Menu_Text")); //$NON-NLS-1$
		encodingMenu.setText(InternationalMessages.getString("ZTerm.Encoding_Menu_Text")); //$NON-NLS-1$
		openItem.setText(InternationalMessages.getString("ZTerm.Open_MenuItem_Text")); //$NON-NLS-1$
		closeItem.setText(InternationalMessages.getString("ZTerm.Close_MenuItem_Text")); //$NON-NLS-1$
		reopenItem.setText(InternationalMessages.getString("ZTerm.Reopen_Item_Text")); //$NON-NLS-1$
		copyItem.setText(InternationalMessages.getString("ZTerm.Copy_MenuItem_Text")); //$NON-NLS-1$
		pasteItem.setText(InternationalMessages.getString("ZTerm.Paste_MenuItem_Text")); //$NON-NLS-1$
		colorCopyItem.setText(InternationalMessages.getString("ZTerm.ColorCopy_MenuItem_Text")); //$NON-NLS-1$
		colorPasteItem.setText(InternationalMessages.getString("ZTerm.ColorPaste_MenuItem_Text")); //$NON-NLS-1$
		preferenceItem.setText(InternationalMessages.getString("ZTerm.Preference_MenuItem_Text")); //$NON-NLS-1$
		siteManagerItem.setText(InternationalMessages.getString("ZTerm.SiteManager_MenuItem_Text")); //$NON-NLS-1$
		usageItem.setText(InternationalMessages.getString("ZTerm.Usage_MenuItem_Text")); //$NON-NLS-1$
		faqItem.setText(InternationalMessages.getString("ZTerm.FAQ_MenuItem_Text")); //$NON-NLS-1$
		aboutItem.setText(InternationalMessages.getString("ZTerm.About_MenuItem_Text")); //$NON-NLS-1$
		big5Item.setText(InternationalMessages.getString("ZTerm.Big5_MenuItem_Text")); //$NON-NLS-1$
		utf8Item.setText(InternationalMessages.getString("ZTerm.UTF8_MenuItem_Text")); //$NON-NLS-1$
		hideMenuBarItem.setText(InternationalMessages.getString("ZTerm.HideMenuBar_MenuItem_Text")); //$NON-NLS-1$
		showMenuBarItem.setText(InternationalMessages.getString("ZTerm.ShowMenuBar_MenuItem_Text")); //$NON-NLS-1$
		popupCopyLinkItem.setText(InternationalMessages.getString("ZTerm.Popup_CopyLink_MenuItem_Text")); //$NON-NLS-1$
		popupCopyItem.setText(InternationalMessages.getString("ZTerm.Copy_MenuItem_Text")); //$NON-NLS-1$
		popupPasteItem.setText(InternationalMessages.getString("ZTerm.Paste_MenuItem_Text")); //$NON-NLS-1$
		popupColorCopyItem.setText(InternationalMessages.getString("ZTerm.ColorCopy_MenuItem_Text")); //$NON-NLS-1$
		popupColorPasteItem.setText(InternationalMessages.getString("ZTerm.ColorPaste_MenuItem_Text")); //$NON-NLS-1$
		popupCloseItem.setText(InternationalMessages.getString("ZTerm.Close_MenuItem_Text"));
	}

	/**
	 * 預先讀取 font metrics 以加快未來開啟連線視窗的速度
	 */
	private void cacheFont() {
		final String family = resource.getStringValue(Config.FONT_FAMILY);
		final Font font = new Font(family, Font.PLAIN, 0);
		// 這個動作很慢
		getFontMetrics(font);
	}

	private void configMemberField() {

		// 設定主畫面 Layout
		getContentPane().setLayout(new BorderLayout());

		makeMenu();
		makePopupMenu();
		makeTabbedPane();
		makeLanguageMenu();

		// 更新畫面上的文字
		updateText();

		// 顯示此元件
		setVisible(true);

		// 設定事件控制器的目標主題
		actionController.setView(this);
		actionController.setModel(model);
		changeController.setView(this);
		changeController.setModel(model);
		keyEventController.setView(this);
		keyEventController.setModel(model);
		componentController.setView(this);
		componentController.setModel(model);
		mouseController.setView(this);
		mouseController.setModel(model);

		// 設定系統核心的目標介面
		model.setView(this);

		// 接收視窗大小改變的事件
		addComponentListener(componentController);

		// 攔截鍵盤 event 以處理快速鍵
		KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(
			keyEventController);

		// 在程式啟動時就先讀一次字型，讓使用者開第一個連線視窗時不會感覺太慢。
		cacheFont();

		// 自動連線
		model.autoconnect();
	}

	private void makeLanguageMenu() {
		final int languageAmount = 2;
		languageItems = new JMenuItem[languageAmount];
		languageItems[0] = new JMenuItem(
			InternationalMessages.getString("ZTerm.Language_English_Item"));
		languageItems[0].setToolTipText(InternationalMessages.getString("ZTerm.Language_English_ToolTip"));
		languageItems[0].addActionListener(actionController);
		languageItems[1] = new JMenuItem(
			InternationalMessages.getString("ZTerm.Language_TraditionalChinese_Item"));
		languageItems[1].setToolTipText(InternationalMessages.getString("ZTerm.Language_TraditionalChinese_ToolTip"));
		languageItems[1].addActionListener(actionController);

		for (int i = 0; i < languageItems.length; i++) {
			languageMenu.add(languageItems[i]);
		}
	}

	// 建立主選單
	private void makeMenu() {
		menuBar = new JMenuBar();

		fileMenu = new JMenu();
		languageMenu = new JMenu();
		editMenu = new JMenu();
		viewMenu = new JMenu();
		historyMenu = new JMenu();
		toolsMenu = new JMenu();
		helpMenu = new JMenu();
		encodingMenu = new JMenu();

		openItem = new JMenuItem();
		closeItem = new JMenuItem();
		reopenItem = new JMenuItem();
		copyItem = new JMenuItem();
		pasteItem = new JMenuItem();
		colorCopyItem = new JMenuItem();
		colorPasteItem = new JMenuItem();
		preferenceItem = new JMenuItem();
		siteManagerItem = new JMenuItem();
		usageItem = new JMenuItem();
		faqItem = new JMenuItem();
		aboutItem = new JMenuItem();
		big5Item = new JMenuItem();
		utf8Item = new JMenuItem();
		hideMenuBarItem = new JMenuItem();

		fileMenu.setMnemonic(KeyEvent.VK_F);
		editMenu.setMnemonic(KeyEvent.VK_E);
		viewMenu.setMnemonic(KeyEvent.VK_V);
		historyMenu.setMnemonic(KeyEvent.VK_Y);
		toolsMenu.setMnemonic(KeyEvent.VK_T);
		helpMenu.setMnemonic(KeyEvent.VK_H);

		openItem.addActionListener(actionController);
		closeItem.addActionListener(actionController);
		reopenItem.addActionListener(actionController);
		copyItem.addActionListener(actionController);
		pasteItem.addActionListener(actionController);
		colorCopyItem.addActionListener(actionController);
		colorPasteItem.addActionListener(actionController);
		preferenceItem.addActionListener(actionController);
		siteManagerItem.addActionListener(actionController);
		usageItem.addActionListener(actionController);
		faqItem.addActionListener(actionController);
		aboutItem.addActionListener(actionController);
		big5Item.addActionListener(actionController);
		utf8Item.addActionListener(actionController);
		hideMenuBarItem.addActionListener(actionController);
		
		openItem.setMnemonic(KeyEvent.VK_Q);
		closeItem.setMnemonic(KeyEvent.VK_W);
		reopenItem.setMnemonic(KeyEvent.VK_R);
		copyItem.setMnemonic(KeyEvent.VK_O);
		pasteItem.setMnemonic(KeyEvent.VK_P);
		colorCopyItem.setMnemonic(KeyEvent.VK_U);
		colorPasteItem.setMnemonic(KeyEvent.VK_I);
		preferenceItem.setMnemonic(KeyEvent.VK_COMMA);
		siteManagerItem.setMnemonic(KeyEvent.VK_PERIOD);

		fileMenu.add(openItem);
		fileMenu.add(reopenItem);
		fileMenu.add(closeItem);

		updateFavoriteMenu();

		encodingMenu.add(big5Item);
		encodingMenu.add(utf8Item);

		editMenu.add(copyItem);
		editMenu.add(pasteItem);
		editMenu.add(colorCopyItem);
		editMenu.add(colorPasteItem);

		viewMenu.add(hideMenuBarItem);
		viewMenu.addSeparator();
		viewMenu.add(encodingMenu);
		viewMenu.add(languageMenu);

		toolsMenu.add(preferenceItem);
		toolsMenu.add(siteManagerItem);

		helpMenu.add(usageItem);
		helpMenu.add(faqItem);
		helpMenu.add(aboutItem);

		menuBar.add(fileMenu);
		menuBar.add(editMenu);
		menuBar.add(viewMenu);
		menuBar.add(historyMenu);
		menuBar.add(toolsMenu);
		menuBar.add(helpMenu);

		setJMenuBar(menuBar);
	}

	private void makePopupMenu() {
		popupMenu = new JPopupMenu();

		popupCopyItem = new JMenuItem();
		popupPasteItem = new JMenuItem();
		popupColorCopyItem = new JMenuItem();
		popupColorPasteItem = new JMenuItem();
		popupCopyLinkItem = new JMenuItem();
		showMenuBarItem = new JMenuItem();
		popupCloseItem = new JMenuItem();

		popupCopyItem.addActionListener(actionController);
		popupPasteItem.addActionListener(actionController);
		popupColorCopyItem.addActionListener(actionController);
		popupColorPasteItem.addActionListener(actionController);
		popupCopyLinkItem.addActionListener(actionController);
		showMenuBarItem.addActionListener(actionController);
		popupCloseItem.addActionListener(actionController);
		popupCopyLinkItem.setEnabled(false);

		popupMenu.add(popupCopyItem);
		popupMenu.add(popupPasteItem);
		popupMenu.add(popupColorCopyItem);
		popupMenu.add(popupColorPasteItem);
		popupMenu.add(popupCopyLinkItem);
		popupMenu.add(popupCloseItem);
	}

	private void makeTabbedPane() {
		// tab 擺在上面，太多 tab 時使用捲頁的顯示方式
		tabbedPane = new JTabbedPane(SwingConstants.TOP,
			JTabbedPane.SCROLL_TAB_LAYOUT);
		tabbedPane.addChangeListener(changeController);
		tabbedPane.addMouseListener(mouseController);
		getContentPane().add(tabbedPane, BorderLayout.CENTER);
	}

	public void removeMenuBar() {
		if (menuBar.isVisible()) {
			menuBar.setVisible(false);
			popupMenu.add(showMenuBarItem);
		}
	}

	public void showMenuBar() {
		if (!menuBar.isVisible()) {
			menuBar.setVisible(true);
			popupMenu.remove(showMenuBarItem);
		}
	}
}
