package org.zhouer.zterm;

import java.awt.Point;
import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Collections;
import java.util.Iterator;
import java.util.Locale;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.zhouer.protocol.Protocol;
import org.zhouer.utils.Convertor;
import org.zhouer.vt.Config;

/**
 * Model is collection of behaviors requested by controllers which registered in
 * view, ZTerm applet.
 * 
 * @author h45
 */
public class Model {

	private volatile static Model model = null;

	/**
	 * Getter of instance in Singleton pattern
	 * 
	 * @return singleton instance of model.
	 */
	public static Model getInstance() {
		if (Model.model == null) {
			synchronized (Model.class) {
				if (Model.model == null) {
					Model.model = new Model();
				}
			}
		}

		return Model.model;
	}

	private final Clip clip;

	private String colorText = null; // 複製下來的彩色文字

	private final Convertor conv;

	private final Resource resource;

	private final Sessions sessions;

	private String copiedLink; // 按滑鼠右鍵時滑鼠下的連結

	private ZTerm view;

	private PreferencePane preferencePane;

	private Model() {
		sessions = Sessions.getInstance(); // 各個連線
		resource = Resource.getInstance(); // 各種設定
		conv = new Convertor(); // 轉碼用
		clip = new Clip(); // 與系統剪貼簿溝通的橋樑
		preferencePane = new PreferencePane();
	}

	/**
	 * Automatically connect to predefined sites according to resource.
	 */
	public void autoconnect() {
		final Vector<Site> favorite = resource.getFavorites();
		final Iterator<Site> favoriteIterator = favorite.iterator();

		while (favoriteIterator.hasNext()) {
			final Site site = favoriteIterator.next();
			if (site.autoconnect) {
				// XXX: here's magic number, -1.
				this.connect(site, -1);
			}
		}
	}

	/**
	 * Sound an alert from certain session.
	 * 
	 * @param session
	 *            sound an alert from this session.
	 */
	public void bell(final Session session) {
		if (resource.getBooleanValue(Resource.USE_CUSTOM_BELL)) {
			try {
				java.applet.Applet.newAudioClip(
						new File(resource
								.getStringValue(Resource.CUSTOM_BELL_PATH))
								.toURI().toURL()).play();
			} catch (final MalformedURLException e) {
				e.printStackTrace();
			}
		} else {
			java.awt.Toolkit.getDefaultToolkit().beep();
		}

		if (!isTabForeground(session)) {
			session.setState(Session.STATE_ALERT);
		}
	}

	/**
	 * Change current session showed on the screen.
	 * 
	 * @param index
	 *            the index of session to be showed on the screen.
	 */
	public void changeSession(final int index) {
		if ((0 <= index) && (index < view.tabbedPane.getTabCount())) {
			
			// 取得焦點的工作
			final Runnable sessionFocuser = new Runnable() {
				public void run() {
					sessions.get(index).requestFocusInWindow();
				}
			};
			
			// 切換分頁的工作 （包含取得焦點的工作）
			final Runnable tabbedSwitcher = new Runnable() {
				public void run() {
					view.tabbedPane.setSelectedIndex(index);
					SwingUtilities.invokeLater(sessionFocuser);
				}
			};
			
			// 啟動切換分頁的工作
			SwingUtilities.invokeLater(tabbedSwitcher);
		}
	}

	/**
	 * Close current tab which is showing on the screen.
	 */
	public void closeCurrentTab() {
		final Session session = (Session) view.tabbedPane
				.getSelectedComponent();

		if (session != null) {

			// 連線中則詢問是否要斷線
			if (!session.isClosed()) {
				if (showConfirm(
						Messages.getString("ZTerm.Message_Confirm_Close"), Messages.getString("ZTerm.Title_Confirm_Close"), JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) { //$NON-NLS-1$ //$NON-NLS-2$
					return;
				}

				// 通知 session 要中斷連線了
				session.close();

				if (!resource
						.getBooleanValue(Resource.REMOVE_MANUAL_DISCONNECT)) {
					return;
				}
			}

			// 通知 session 要被移除了
			session.remove();

			view.tabbedPane.remove(session);
			getSessions().remove(session);

			// 刪除分頁會影響分頁編號
			updateTabTitle();

			// 讓現在被選取的分頁取得 focus.
			updateTab();
		}
	}
	
	/**
	 * Disconnect the session
	 * @param session the session to disconnect
	 */
	public void disconnectSession(Session session) {
		session.disconnect(false);
	}

	/**
	 * Do color copying.
	 */
	public void colorCopy() {
		final Session session = (Session) view.tabbedPane
				.getSelectedComponent();

		if (session != null) {
			final String str = session.getSelectedColorText();
			if (str.length() != 0) {
				colorText = str;
			}
			if (resource.getBooleanValue(Config.CLEAR_AFTER_COPY)) {
				session.resetSelected();
				session.repaint();
			}
		}
	}

	/**
	 * Do color pasting.
	 */
	public void colorPaste() {
		final Session session = (Session) view.tabbedPane
				.getSelectedComponent();
		if ((session != null) && (colorText != null)) {
			session.pasteColorText(colorText);
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
		Session session;

		session = new Session(site, resource, conv, view.bi, this);

		// index 為連線後放在第幾個分頁，若為 -1 表開新分頁。
		if (index == -1) {
			getSessions().add(session);

			// 一開始預設 icon 是連線中斷
			final ImageIcon icon = view.closedIcon;

			// chitsaou.070726: 分頁編號
			if (resource.getBooleanValue(Resource.TAB_NUMBER)) {
				// 分頁 title 會顯示分頁編號加站台名稱，tip 會顯示 hostname.
				view.tabbedPane.addTab((view.tabbedPane.getTabCount() + 1)
						+ ". " + site.name, icon, session, site.host); //$NON-NLS-1$
			} else {
				// chitsaou:070726: 不要標號
				view.tabbedPane.addTab(site.name, icon, session, site.host);
			}

			view.tabbedPane.setSelectedIndex(view.tabbedPane.getTabCount() - 1);
		} else {
			getSessions().setElementAt(session, index);
			view.tabbedPane.setComponentAt(index, session);
		}

		// 每個 session 都是一個 thread, 解決主程式被 block 住的問題。
		new Thread(session).start();
	}

	/**
	 * Do text copying to clip.
	 */
	public void copy() {
		final Session session = (Session) view.tabbedPane
				.getSelectedComponent();

		if (session != null) {
			final String str = session.getSelectedText();
			if (str.length() != 0) {
				clip.setContent(str);
			}
			if (resource.getBooleanValue(Config.CLEAR_AFTER_COPY)) {
				session.resetSelected();
				session.repaint();
			}
		}
	}

	/**
	 * Do link copying to clip.
	 */
	public void copyLink() {
		if (copiedLink != null) {
			clip.setContent(copiedLink);
		}
	}

	/**
	 * Search key word for site to be searched from favorite sites.
	 * 
	 * @param keyWordSite
	 *            key word for site to be searched.
	 * @return candidate sites.
	 */
	public Vector<Site> getCandidateSites(final String keyWordSite) {
		final Vector<Site> candidateSites = new Vector<Site>();

		// 如果關鍵字是空字串，那就什麼都不要回
		if (keyWordSite.length() == 0) {
			return candidateSites;
		}

		Iterator<Site> siteIterator;
		Site site;

		// 加入站台列表中符合的
		siteIterator = resource.getFavorites().iterator();
		while (siteIterator.hasNext()) {
			site = siteIterator.next();
			if ((site.name.indexOf(keyWordSite) != -1)
					|| (site.alias.indexOf(keyWordSite) != -1)
					|| (site.getURL().indexOf(keyWordSite) != -1)) {
				candidateSites.addElement(site);
			}
		}

		// 把結果排序後再輸出
		Collections.sort(candidateSites);

		return candidateSites;
	}

	/**
	 * Prompt a dialog to ask user password.
	 * 
	 * @return password from user.
	 */
	public String getPassword() {
		return PasswordPane.showPasswordDialog(view, Messages
				.getString("Model.Password_Text"), Messages
				.getString("Model.Password_Title")); 
	}

	/**
	 * Getter of sessions
	 * 
	 * @return the sessions
	 */
	public Vector<Session> getSessions() {
		return sessions;
	}

	/**
	 * Prompt a dialog to ask user name.
	 * 
	 * @return user name.
	 */
	public String getUsername() {
		return JOptionPane
				.showInputDialog(
						view,
						Messages.getString("Model.User_Name_Text"), Messages.getString("Model.User_Name_Title"), //$NON-NLS-1$ //$NON-NLS-2$
						JOptionPane.QUESTION_MESSAGE);
	}

	/**
	 * Check if the session is selected on tab page.
	 * 
	 * @param session
	 * @return true if the session is selected on tab page; false, otherwise.
	 */
	public boolean isTabForeground(final Session session) {
		return (view.tabbedPane.indexOfComponent(session) == view.tabbedPane
				.getSelectedIndex());
	}

	/**
	 * Ask user host and open the site which user enters to dialog.
	 */
	public void open() {
		final String site = JOptionPane.showInputDialog(view, Messages
				.getString("ZTerm.Message_Input_Site")); //$NON-NLS-1$
		this.connect(site);
	}

	/**
	 * Open external browser to explore the url.
	 * 
	 * @param url
	 *            the url to be browsed at external browser.
	 */
	public void openExternalBrowser(final String url) {
		String cmd = resource.getStringValue(Resource.EXTERNAL_BROWSER);
		if (cmd == null) {
			showMessage(Messages
					.getString("ZTerm.Message_Wrong_Explorer_Command")); //$NON-NLS-1$
			return;
		}

		// 把 %u 置換成給定的 url
		final int urlIndex = cmd.indexOf("%u"); //$NON-NLS-1$
		if (urlIndex == -1) {
			showMessage(Messages
					.getString("ZTerm.Message_Wrong_Explorer_Command")); //$NON-NLS-1$
			return;
		}

		cmd = cmd.substring(0, urlIndex) + url + cmd.substring(urlIndex + 2);
		runExternal(cmd);
	}

	/**
	 * Open new tab and connect to the site where typed on the text field in
	 * user interface.
	 */
	public void openNewTab() {
		final String site = view.siteText.getText();
		view.siteModel.removeAllElements();
		this.connect(site);
	}

	/**
	 * Do paste at current session.
	 */
	public void paste() {
		final Session session = (Session) view.tabbedPane
				.getSelectedComponent();
		if (session != null) {
			session.pasteText(clip.getContent());
		}
	}

	/**
	 * Reopen session which is currently disconnected.
	 * 
	 * @param session
	 *            session to be reopened.
	 */
	public void reopenSession(final Session session) {
		if (session != null) {
			// 若連線中則開新分頁，已斷線則重連。
			if (session.isClosed()) {
				this.connect(session.getSite(), view.tabbedPane
						.indexOfComponent(session));
			} else {
				this.connect(session.getSite(), -1);
			}
		}
	}

	/**
	 * Execute the command.
	 * 
	 * @param command
	 *            the command to be executed.
	 */
	public void runExternal(final String command) {
		try {
			Runtime.getRuntime().exec(command);
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Setter of view
	 * 
	 * @param view
	 *            the view to set
	 */
	public void setView(final ZTerm view) {
		this.view = view;
	}

	/**
	 * Show about dialog.
	 */
	public void showAbout() {
		showMessage(Messages.getString("ZTerm.Message_About")); //$NON-NLS-1$
	}

	/**
	 * Show confirm dialog.
	 * 
	 * @param message
	 *            the message of confirm dialog.
	 * @param title
	 *            the title of confirm dialog.
	 * @param option
	 *            the option of confirm dialog.
	 * @return confirm state.
	 */
	public int showConfirm(final String message, final String title,
			final int option) {

		return JOptionPane.showConfirmDialog(view, message, title, option);
	}

	/**
	 * Show FAQ dialog.
	 */
	public void showFAQ() {
		final HtmlPane faqHtmlDialog = new HtmlPane(ZTerm.class
				.getResource("docs/faq.html")); //$NON-NLS-1$ 
		final JDialog dialog = faqHtmlDialog.createDialog(view, Messages
				.getString("ZTerm.Title_FAQ")); //$NON-NLS-1$
		dialog.setSize(640, 400);
		dialog.setVisible(true);
	}

	/**
	 * Show message dialog.
	 * 
	 * @param message
	 *            message to be showed.
	 */
	public void showMessage(final String message) {
		JOptionPane.showMessageDialog(view, message);
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
		final Point viewLocation = view.getLocationOnScreen();

		view.popupCopyLinkItem.setEnabled(link != null);
		copiedLink = link;

		// 傳進來的是滑鼠相對於視窗左上角的座標，減去主視窗相對於螢幕左上角的座標，可得滑鼠相對於主視窗的座標。
		view.popupMenu.show(view, x - viewLocation.x, y - viewLocation.y);
	}

	/**
	 * Show preference dialog.
	 */
	public void showPreference() {
		final JDialog dialog = preferencePane.createDialog(view, Messages
				.getString("Preference.Title"));
		dialog.setSize(620, 350);
		dialog.setVisible(true);

		if (preferencePane.getValue() != null) {
			if (preferencePane.getValue() instanceof Integer) {
				if (preferencePane.getValue().equals(
						new Integer(JOptionPane.OK_OPTION))) {
					preferencePane.submit();
				}
			}
		}
	}

	/**
	 * Show site manager dialog.
	 */
	public void showSiteManager() {
		final SiteManager siteManager = new SiteManager();
		final JDialog dialog = siteManager.createDialog(view, "Site Manager");
		dialog.setSize(600, 350);
		dialog.setVisible(true);

		if (siteManager.getValue() != null) {
			if (siteManager.getValue() instanceof Integer) {
				if (siteManager.getValue().equals(
						new Integer(JOptionPane.OK_OPTION))) {
					siteManager.submit();
				}
			}
		}
	}

	/**
	 * Show usage dialog.
	 */
	public void showUsage() {
		final HtmlPane usageHtmlDialog = new HtmlPane(ZTerm.class
				.getResource("docs/usage.html")); //$NON-NLS-1$ 
		final JDialog dialog = usageHtmlDialog.createDialog(view, Messages
				.getString("ZTerm.Title_Manual")); //$NON-NLS-1$
		dialog.setSize(640, 400);
		dialog.setVisible(true);
	}

	/**
	 * Update anti idle time to each session.
	 */
	public void updateAntiIdleTime() {
		for (int i = 0; i < sessions.size(); i++) {
			(sessions.elementAt(i)).updateAntiIdleTime();
		}
	}

	/**
	 * Update bounds to resource, and also set the bounds of user interface.
	 */
	public void updateBounds() {
		int locationx, locationy, width, height;
		locationx = resource.getIntValue(Resource.GEOMETRY_X);
		locationy = resource.getIntValue(Resource.GEOMETRY_Y);
		width = resource.getIntValue(Resource.GEOMETRY_WIDTH);
		height = resource.getIntValue(Resource.GEOMETRY_HEIGHT);

		view.setBounds(locationx, locationy, width, height);
		view.validate();
	}

	/**
	 * Update the encode to current session.
	 * 
	 * @param encoding
	 *            the encode name to be updated.
	 */
	public void updateEncoding(final String encoding) {
		final Session session = (Session) view.tabbedPane
				.getSelectedComponent();
		if (session != null) {
			session.setEncoding(encoding);
		}
	}

	/**
	 * Update favorite menu.
	 */
	public void updateFavoriteMenu() {
		view.updateFavoriteMenu();
	}

	/**
	 * Update look and feel which is related to UI manager.
	 */
	public void updateLookAndFeel() {
		view.updateLookAndFeel();
	}
	
	/**
	 * Reload resource and update the settings displayed in preference panel.
	 */
	public void updatePreferencePane() {
		preferencePane.reloadSettings();
	}

	/**
	 * Update size to resource, and also user interface.
	 */
	public void updateSize() {
		final Rectangle bounds = view.getBounds();
		resource.setValue(Resource.GEOMETRY_X, (int) bounds.getX());
		resource.setValue(Resource.GEOMETRY_Y, (int) bounds.getY());
		resource.setValue(Resource.GEOMETRY_WIDTH, (int) bounds.getWidth());
		resource.setValue(Resource.GEOMETRY_HEIGHT, (int) bounds.getHeight());
		
		preferencePane.apperancePanel.widthSpinner.setValue((int) bounds.getWidth());
		preferencePane.apperancePanel.heightSpinner.setValue((int) bounds.getHeight());

		view.updateSize();
	}

	/**
	 * Update tab page.
	 */
	public void updateTab() {
		// 為了下面 invokeLater 的關係，這邊改成 final
		final Session session = (Session) view.tabbedPane
				.getSelectedComponent();

		if (session != null) {
			// 修改視窗標題列
			// setTitle( "ZTerm - " + s.getWindowTitle() ); //$NON-NLS-1$

			// 修改位置列
			view.siteText.setText(session.getURL());
			view.siteText.select(0, 0);
			view.siteField.hidePopup();

			// 切換到 alert 的 session 時設定狀態為 connected, 以取消 bell.
			if (session.state == Session.STATE_ALERT) {
				session.setState(Session.STATE_CONNECTED);
			}

			// 因為全部的連線共用一張 BufferedImage, 切換分頁時需重繪內容。
			session.updateScreen();

			// 讓所選的 session 取得 focus
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					session.requestFocusInWindow();
				}
			});
		} else {
			view.siteText.setText(""); //$NON-NLS-1$
		}
	}

	/**
	 * Update tab state at certain session.
	 * 
	 * @param state
	 *            target state which will be changed to
	 * @param session
	 *            the session of which tab state to be updated.
	 */
	public void updateTabState(final int state, final Session session) {
		view.updateTabState(state, session);
	}

	/**
	 * Update tab title to each tab page.
	 */
	public void updateTabTitle() {
		for (int i = 0; i < view.tabbedPane.getTabCount(); i++) {
			view.tabbedPane.setTitleAt(i, (i + 1)
					+ ". " + getSessions().elementAt(i).getSite().name); //$NON-NLS-1$

			// FIXME: need revise
			view.tabbedPane.setTitleAt(i,
					((resource.getBooleanValue(Resource.TAB_NUMBER)) ? (i + 1)
							+ ". " : "") //$NON-NLS-1$ //$NON-NLS-2$
							+ getSessions().elementAt(i).getSite().name);
		}
	}

	/**
	 * Update tool bar.
	 * 
	 * @param isShowToolbar
	 *            true, show tool bar; false, hide tool bar.
	 */
	public void updateToolbar(final boolean isShowToolbar) {
		preferencePane.apperancePanel.showToolbarCheckBox.setSelected(isShowToolbar);
		view.updateToolbar(isShowToolbar);
	}

	private void connect(String url) {
		Site site;
		String host;
		int port, position;
		String protocol;

		// 如果開新連線時按了取消則傳回值為 null
		if ((url == null) || (url.length() == 0)) {
			return;
		}

		do {
			// 透過 name or alias 連線
			site = resource.getFavorite(url);
			if (site != null) {
				break;
			}

			position = url.indexOf("://"); //$NON-NLS-1$
			// Default 就是 telnet
			protocol = Protocol.TELNET;
			if (position != -1) {
				if (url.substring(0, position).equalsIgnoreCase(Protocol.SSH)) {
					protocol = Protocol.SSH;
				} else if (url.substring(0, position).equalsIgnoreCase(
						Protocol.TELNET)) {
					protocol = Protocol.TELNET;
				} else {
					showMessage(Messages
							.getString("ZTerm.Message_Wrong_Protocal")); //$NON-NLS-1$
					return;
				}
				// 將 h 重設為 :// 後的東西
				url = url.substring(position + 3);
			}

			// 取得 host:port, 或 host(:23)
			position = url.indexOf(':');
			if (position == -1) {
				host = url;
				if (protocol.equalsIgnoreCase(Protocol.TELNET)) {
					port = 23;
				} else {
					port = 22;
				}
			} else {
				host = url.substring(0, position);
				port = Integer.parseInt(url.substring(position + 1));
			}

			site = new Site(host, host, port, protocol);
		} while (false);

		// host 長度為零則不做事
		if (url.length() == 0) {
			return;
		}

		this.connect(site, -1);
	}
	
	/**
	 * Refresh messages on the user interface.
	 */
	public void refreshMessages() {
		Locale.setDefault(resource.getLocale());
		Messages.restartBundle();
		this.view.updateText();
		this.preferencePane.refreshText();
	}
}
