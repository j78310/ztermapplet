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

	private String tmpLink; // 按滑鼠右鍵時滑鼠下的連結

	private ZTerm view;
	
	private PreferencePane preferencePane;

	private Model() {
		this.sessions = Sessions.getInstance(); // 各個連線
		this.resource = Resource.getInstance(); // 各種設定
		this.conv = new Convertor(); // 轉碼用
		this.clip = new Clip(); // 與系統剪貼簿溝通的橋樑
		this.preferencePane = new PreferencePane();
	}
	
	public void setLocale(final Locale locale) {
		Locale.setDefault(locale);
		this.resource.setValue(Resource.LOCALE_COUNTRY, locale.getCountry());
		this.resource.setValue(Resource.LOCALE_LANGUAGE, locale.getLanguage());
		this.resource.setValue(Resource.LOCALE_VARIANT, locale.getVariant());
	}

	/**
	 * Automatically connect to predefined sites according to resource.
	 */
	public void autoconnect() {
		final Vector<Site> favorite = this.resource.getFavorites();
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
	 * @param session sound an alert from this session.
	 */
	public void bell(final Session session) {
		if (this.resource.getBooleanValue(Resource.USE_CUSTOM_BELL)) {
			try {
				java.applet.Applet.newAudioClip(
						new File(this.resource
								.getStringValue(Resource.CUSTOM_BELL_PATH))
								.toURI().toURL()).play();
			} catch (final MalformedURLException e) {
				e.printStackTrace();
			}
		} else {
			java.awt.Toolkit.getDefaultToolkit().beep();
		}

		if (!this.isTabForeground(session)) {
			session.setState(Session.STATE_ALERT);
		}
	}

	/**
	 * Change current session showed on the screen.
	 * 
	 * @param index the index of session to be showed on the screen.
	 */
	public void changeSession(final int index) {
		if ((0 <= index) && (index < this.view.tabbedPane.getTabCount())) {
			this.view.tabbedPane.setSelectedIndex(index);
		}
	}

	/**
	 * Close current tab which is showing on the screen.
	 */
	public void closeCurrentTab() {
		final Session s = (Session) this.view.tabbedPane.getSelectedComponent();

		if (s != null) {

			// 連線中則詢問是否要斷線
			if (!s.isClosed()) {
				if (this
						.showConfirm(
								Messages
										.getString("ZTerm.Message_Confirm_Close"), Messages.getString("ZTerm.Title_Confirm_Close"), JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) { //$NON-NLS-1$ //$NON-NLS-2$
					return;
				}

				// 通知 session 要中斷連線了
				s.close(false);

				if (!this.resource
						.getBooleanValue(Resource.REMOVE_MANUAL_DISCONNECT)) {
					return;
				}
			}

			// 通知 session 要被移除了
			s.remove();

			this.view.tabbedPane.remove(s);
			this.getSessions().remove(s);

			// 刪除分頁會影響分頁編號
			this.updateTabTitle();

			// 讓現在被選取的分頁取得 focus.
			this.updateTab();
		}
	}

	/**
	 * Do color copying.
	 */
	public void colorCopy() {
		final Session s = (Session) this.view.tabbedPane.getSelectedComponent();

		if (s != null) {
			final String str = s.getSelectedColorText();
			if (str.length() != 0) {
				this.colorText = str;
			}
			if (this.resource.getBooleanValue(Config.CLEAR_AFTER_COPY)) {
				s.resetSelected();
				s.repaint();
			}
		}
	}

	/**
	 * Do color pasting.
	 */
	public void colorPaste() {
		final Session s = (Session) this.view.tabbedPane.getSelectedComponent();
		if ((s != null) && (this.colorText != null)) {
			s.pasteColorText(this.colorText);
		}
	}

	/**
	 * Connect to the site at certain index of tab page.
	 * 
	 * @param site site to be connected.
	 * @param index the index of tab page corresponding to this site.
	 */
	public void connect(final Site site, final int index) {
		Session s;

		s = new Session(site, this.resource, this.conv, this.view.bi, this);

		// index 為連線後放在第幾個分頁，若為 -1 表開新分頁。
		if (index == -1) {
			this.getSessions().add(s);

			ImageIcon icon;

			// 一開始預設 icon 是連線中斷
			icon = this.view.closedIcon;

			// chitsaou.070726: 分頁編號
			if (this.resource.getBooleanValue(Resource.TAB_NUMBER)) {
				// 分頁 title 會顯示分頁編號加站台名稱，tip 會顯示 hostname.
				this.view.tabbedPane.addTab(
						(this.view.tabbedPane.getTabCount() + 1)
								+ ". " + site.name, icon, s, site.host); //$NON-NLS-1$
			} else {
				// chitsaou:070726: 不要標號
				this.view.tabbedPane.addTab(site.name, icon, s, site.host);
			}

			this.view.tabbedPane.setSelectedIndex(this.view.tabbedPane
					.getTabCount() - 1);
		} else {
			this.getSessions().setElementAt(s, index);
			this.view.tabbedPane.setComponentAt(index, s);
		}

		// 每個 session 都是一個 thread, 解決主程式被 block 住的問題。
		new Thread(s).start();
	}

	/**
	 * Do text copying to clip.
	 */
	public void copy() {
		final Session s = (Session) this.view.tabbedPane.getSelectedComponent();

		if (s != null) {
			final String str = s.getSelectedText();
			if (str.length() != 0) {
				this.clip.setContent(str);
			}
			if (this.resource.getBooleanValue(Config.CLEAR_AFTER_COPY)) {
				s.resetSelected();
				s.repaint();
			}
		}
	}

	/**
	 * Do link copying to clip.
	 */
	public void copyLink() {
		if (this.tmpLink != null) {
			this.clip.setContent(this.tmpLink);
		}
	}

	/**
	 * Search key word for site to be searched from favorite sites.
	 * 
	 * @param keyWordSite key word for site to be searched.
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
		siteIterator = this.resource.getFavorites().iterator();
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
		final PasswordPane passwordDialog = new PasswordPane(Messages
				.getString("Model.Password_Text")); //$NON-NLS-1$ 
		final JDialog dialog = passwordDialog.createDialog(this.view, Messages
				.getString("Model.Password_Title")); //$NON-NLS-1$

		
		
		Thread focusThread = new Thread() {
			public void run() {
				while (!dialog.isVisible()) {					
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				
				while (passwordDialog.passField.getPassword().length == 0 && dialog.isVisible()) {					
					if (dialog.isFocused()) {
						passwordDialog.passField.requestFocusInWindow();
					}
					
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		};
		
		focusThread.start();
		dialog.setVisible(true);

		if (passwordDialog.getValue() != null) {
			if (passwordDialog.getValue() instanceof Integer) {
				if (passwordDialog.getValue().equals(
						new Integer(JOptionPane.OK_OPTION))) {
					return passwordDialog.getPassword();
				}
			}
		}

		return null;
	}

	/**
	 * Getter of sessions
	 * 
	 * @return the sessions
	 */
	public Vector<Session> getSessions() {
		return this.sessions;
	}

	/**
	 * Prompt a dialog to ask user name.
	 * 
	 * @return user name.
	 */
	public String getUsername() {
		return JOptionPane
				.showInputDialog(
						this.view,
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
		return (this.view.tabbedPane.indexOfComponent(session) == this.view.tabbedPane
				.getSelectedIndex());
	}

	/**
	 * Ask user host and open the site which user enters to dialog.
	 */
	public void open() {
		final String host = JOptionPane.showInputDialog(this.view, Messages
				.getString("ZTerm.Message_Input_Site")); //$NON-NLS-1$
		this.connect(host);
	}

	/**
	 * Open external browser to explore the url.
	 * 
	 * @param url the url to be browsed at external browser.
	 */
	public void openExternalBrowser(final String url) {
		int pos;

		String cmd = this.resource.getStringValue(Resource.EXTERNAL_BROWSER);
		if (cmd == null) {
			this.showMessage(Messages
					.getString("ZTerm.Message_Wrong_Explorer_Command")); //$NON-NLS-1$
			return;
		}

		// 把 %u 置換成給定的 url
		pos = cmd.indexOf("%u"); //$NON-NLS-1$
		if (pos == -1) {
			this.showMessage(Messages
					.getString("ZTerm.Message_Wrong_Explorer_Command")); //$NON-NLS-1$
			return;
		}
		cmd = cmd.substring(0, pos) + url + cmd.substring(pos + 2);
		// System.out.println( "browser command: " + cmd );

		this.runExternal(cmd);
	}

	/**
	 * Open new tab and connect to the site where typed on the text field in user interface.
	 */
	public void openNewTab() {
		final String host = this.view.siteText.getText();
		this.view.siteModel.removeAllElements();
		this.connect(host);
	}

	/**
	 * Do paste at current session.
	 */
	public void paste() {
		final Session s = (Session) this.view.tabbedPane.getSelectedComponent();
		if (s != null) {
			s.pasteText(this.clip.getContent());
		}
	}

	/**
	 * Reopen session which is currently disconnected.
	 * 
	 * @param session session to be reopened.
	 */
	public void reopenSession(final Session session) {
		if (session != null) {
			// 若連線中則開新分頁，已斷線則重連。
			if (session.isClosed()) {
				this.connect(session.getSite(), this.view.tabbedPane
						.indexOfComponent(session));
			} else {
				this.connect(session.getSite(), -1);
			}
		}
	}

	/**
	 * Execute the command.
	 * 
	 * @param command the command to be executed.
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
		this.showMessage(Messages.getString("ZTerm.Message_About")); //$NON-NLS-1$
	}

	/**
	 * Show confirm dialog.
	 * 
	 * @param message the message of confirm dialog.
	 * @param title the title of confirm dialog.
	 * @param option the option of confirm dialog.
	 * @return confirm state.
	 */
	public int showConfirm(final String message, final String title,
			final int option) {

		return JOptionPane.showConfirmDialog(this.view, message, title, option);
	}

	/**
	 * Show FAQ dialog.
	 */
	public void showFAQ() {
		final HtmlPane faqHtmlDialog = new HtmlPane(ZTerm.class
				.getResource("docs/faq.html")); //$NON-NLS-1$ 
		final JDialog dialog = faqHtmlDialog.createDialog(this.view, Messages
				.getString("ZTerm.Title_FAQ")); //$NON-NLS-1$
		dialog.setSize(640, 400);
		dialog.setVisible(true);
	}

	/**
	 * Show message dialog.
	 * 
	 * @param message message to be showed.
	 */
	public void showMessage(final String message) {
		JOptionPane.showMessageDialog(this.view, message);
	}

	/**
	 * Show pop-up menu at current position. 
	 * 
	 * @param x the position x which located mouse.
	 * @param y the position y which located mouse.
	 * @param link the link which is selected.
	 */
	public void showPopup(final int x, final int y, final String link) {
		final Point p = this.view.getLocationOnScreen();

		// TODO: 用 tmpLink 的作法蠻笨的，但暫時想不到好作法
		this.view.popupCopyLinkItem.setEnabled(link != null);
		this.tmpLink = link;

		// 傳進來的是滑鼠相對於視窗左上角的座標，減去主視窗相對於螢幕左上角的座標，可得滑鼠相對於主視窗的座標。
		this.view.popupMenu.show(this.view, x - p.x, y - p.y);
	}

	/**
	 * Show preference dialog.
	 */
	public void showPreference() {
		// FIXME: Linux Firefox 1.5.0.13pre cannot input Chinese after
		// setVisible(true)
		// FIXME: need confirmed.
		final JDialog dialog = preferencePane.createDialog(this.view,
				Messages.getString("Preference.Title"));
		dialog.setSize(620, 300);
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
		// FIXME: Linux Firefox 1.5.0.13pre cannot input Chinese after
		// setVisible(true)
		// FIXME: need confirmed.
		final SiteManager siteManager = new SiteManager();
		final JDialog dialog = siteManager.createDialog(this.view,
				"Site Manager");
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
		final JDialog dialog = usageHtmlDialog.createDialog(this.view, Messages
				.getString("ZTerm.Title_Manual")); //$NON-NLS-1$
		dialog.setSize(640, 400);
		dialog.setVisible(true);
	}

	/**
	 * Update anti idle time to each session.
	 */
	public void updateAntiIdleTime() {
		for (int i = 0; i < this.sessions.size(); i++) {
			(this.sessions.elementAt(i)).updateAntiIdleTime();
		}
	}

	/**
	 * Update bounds to resource, and also set the bounds of user interface.
	 */
	public void updateBounds() {
		// XXX: is this function also worked on applet?
		int locationx, locationy, width, height;
		locationx = this.resource.getIntValue(Resource.GEOMETRY_X);
		locationy = this.resource.getIntValue(Resource.GEOMETRY_Y);
		width = this.resource.getIntValue(Resource.GEOMETRY_WIDTH);
		height = this.resource.getIntValue(Resource.GEOMETRY_HEIGHT);

		this.view.setBounds(locationx, locationy, width, height);
		this.view.validate();
	}

	/**
	 * Update the encode to current session.
	 * 
	 * @param encoding the encode name to be updated.
	 */
	public void updateEncoding(final String encoding) {
		final Session s = (Session) this.view.tabbedPane.getSelectedComponent();
		if (s != null) {
			s.setEncoding(encoding);
		}
	}

	/**
	 * Update favorite menu.
	 */
	public void updateFavoriteMenu() {
		this.view.updateFavoriteMenu();
	}

	/**
	 * Update look and feel which is related to UI manager.
	 */
	public void updateLookAndFeel() {
		this.view.updateLookAndFeel();
	}

	/**
	 * Update size to resource, and also user interface.
	 */
	public void updateSize() {
		final Rectangle r = this.view.getBounds();
		this.resource.setValue(Resource.GEOMETRY_X, (int) r.getX());
		this.resource.setValue(Resource.GEOMETRY_Y, (int) r.getY());
		this.resource.setValue(Resource.GEOMETRY_WIDTH, (int) r.getWidth());
		this.resource.setValue(Resource.GEOMETRY_HEIGHT, (int) r.getHeight());

		this.view.updateSize();
	}

	/**
	 * Update tab page.
	 */
	public void updateTab() {
		// 為了下面 invokeLater 的關係，這邊改成 final
		final Session s = (Session) this.view.tabbedPane.getSelectedComponent();

		if (s != null) {
			// 修改視窗標題列
			// setTitle( "ZTerm - " + s.getWindowTitle() ); //$NON-NLS-1$

			// 修改位置列
			this.view.siteText.setText(s.getURL());
			this.view.siteText.select(0, 0);
			this.view.siteField.hidePopup();

			// 切換到 alert 的 session 時設定狀態為 connected, 以取消 bell.
			if (s.state == Session.STATE_ALERT) {
				s.setState(Session.STATE_CONNECTED);
			}

			// 因為全部的連線共用一張 BufferedImage, 切換分頁時需重繪內容。
			s.updateScreen();

			// 讓所選的 session 取得 focus
			// XXX: PowerPC Linux + IBM JRE 要 invokeLater 才正常
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					s.requestFocusInWindow();
				}
			});
		} else {
			// FIXME: magic number
			// setTitle("ZTerm"); //$NON-NLS-1$
			this.view.siteText.setText(""); //$NON-NLS-1$
		}
	}

	/**
	 * Update tab state at certain session.
	 * 
	 * @param state target state which will be changed to
	 * @param session the session of which tab state to be updated.
	 */
	public void updateTabState(final int state, final Session session) {
		this.view.updateTabState(state, session);
	}

	/**
	 * Update tab title to each tab page.
	 */
	public void updateTabTitle() {
		for (int i = 0; i < this.view.tabbedPane.getTabCount(); i++) {
			this.view.tabbedPane
					.setTitleAt(
							i,
							(i + 1)
									+ ". " + this.getSessions().elementAt(i).getSite().name); //$NON-NLS-1$

			// FIXME: need revise
			this.view.tabbedPane
					.setTitleAt(i, ((this.resource
							.getBooleanValue(Resource.TAB_NUMBER)) ? (i + 1)
							+ ". " : "") //$NON-NLS-1$ //$NON-NLS-2$
							+ this.getSessions().elementAt(i)
									.getSite().name);
		}
	}

	/**
	 * Update tool bar.
	 */
	public void updateToolbar() {
		this.view.updateToolbar();
	}

	private void connect(String h) {
		Site si;
		String host;
		int port, pos;
		String prot;

		// 如果開新連線時按了取消則傳回值為 null
		if ((h == null) || (h.length() == 0)) {
			return;
		}

		do {
			// 透過 name or alias 連線
			si = this.resource.getFavorite(h);
			if (si != null) {
				break;
			}

			pos = h.indexOf("://"); //$NON-NLS-1$
			// Default 就是 telnet
			prot = Protocol.TELNET;
			if (pos != -1) {
				if (h.substring(0, pos).equalsIgnoreCase(Protocol.SSH)) {
					prot = Protocol.SSH;
				} else if (h.substring(0, pos)
						.equalsIgnoreCase(Protocol.TELNET)) {
					prot = Protocol.TELNET;
				} else {
					this.showMessage(Messages
							.getString("ZTerm.Message_Wrong_Protocal")); //$NON-NLS-1$
					return;
				}
				// 將 h 重設為 :// 後的東西
				h = h.substring(pos + 3);
			}

			// 取得 host:port, 或 host(:23)
			pos = h.indexOf(':');
			if (pos == -1) {
				host = h;
				if (prot.equalsIgnoreCase(Protocol.TELNET)) {
					port = 23;
				} else {
					port = 22;
				}
			} else {
				host = h.substring(0, pos);
				port = Integer.parseInt(h.substring(pos + 1));
			}
			si = new Site(host, host, port, prot);
		} while (false);

		// host 長度為零則不做事
		if (h.length() == 0) {
			return;
		}

		this.connect(si, -1);
	}
}
