package org.zhouer.zterm.model;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Collections;
import java.util.Iterator;
import java.util.Locale;
import java.util.Vector;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.zhouer.protocol.Protocol;
import org.zhouer.utils.ClipUtils;
import org.zhouer.utils.InternationalMessages;
import org.zhouer.vt.Config;
import org.zhouer.zterm.view.HtmlPane;
import org.zhouer.zterm.view.PasswordPane;
import org.zhouer.zterm.view.PreferencePane;
import org.zhouer.zterm.view.SessionPane;
import org.zhouer.zterm.view.SiteManager;
import org.zhouer.zterm.view.ZTerm;

/**
 * Model is collection of behaviors requested by controllers which registered in
 * view, ZTerm applet.
 * 
 * @author Chin-Chang Yang
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

	private String colorText = null; // 複製下來的彩色文字
	private final Resource resource;
	private final SessionPool sessions;
	private String copiedLink; // 按滑鼠右鍵時滑鼠下的連結
	private ZTerm view;
	private PreferencePane preferencePane;
	private SiteManager siteManager;

	public String getCopiedLink() {
		return copiedLink;
	}

	public void setCopiedLink(String copiedLink) {
		this.copiedLink = copiedLink;
	}

	private Model() {
		sessions = SessionPool.getInstance(); // 各個連線
		resource = Resource.getInstance(); // 各種設定
		preferencePane = new PreferencePane();		
		siteManager = new SiteManager();
	}

	public void setLocale(final Locale locale) {
		Locale.setDefault(locale);
		resource.setValue(Resource.LOCALE_COUNTRY, locale.getCountry());
		resource.setValue(Resource.LOCALE_LANGUAGE, locale.getLanguage());
		resource.setValue(Resource.LOCALE_VARIANT, locale.getVariant());
	}

	/**
	 * Automatically connect to predefined sites according to resource.
	 */
	public void autoconnect() {
		final Vector favorite = resource.getFavorites();
		final Iterator favoriteIterator = favorite.iterator();

		while (favoriteIterator.hasNext()) {
			final Site site = (Site) favoriteIterator.next();
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
	public void bell(final SessionPane session) {
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
			session.setState(SessionPane.STATE_ALERT);
		}
	}

	/**
	 * Change current session showed on the screen.
	 * 
	 * @param index
	 *            the index of session to be showed on the screen.
	 */
	public void changeSession(final int index) {
		view.changeSession(index);
	}
	
	/**
	 * Request focus to the current session.
	 */
	public void requestFocusToCurrentSession() {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				final SessionPane session = getCurrentSession();
				
				if (session != null) {
					getCurrentSession().requestFocusInWindow();
				}
			}
		});
	}
	
	/**
	 * Getter of the current session which is able to be viewed 
	 * 
	 * @return the current session
	 */
	public SessionPane getCurrentSession() {
		return view.getCurrentSession();
	}

	/**
	 * Close current tab which is showing on the screen.
	 */
	public void closeCurrentTab() {
		view.closeCurrentTab();
	}

	/**
	 * Do color copying.
	 */
	public void colorCopy() {
		final SessionPane session = getCurrentSession();

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
		final SessionPane session = getCurrentSession();
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
		view.connect(site, index);
	}

	/**
	 * Do text copying to clip.
	 */
	public void copy() {
		final SessionPane session = getCurrentSession();

		if (session != null) {
			final String str = session.getSelectedText();
			if (str.length() != 0) {
				ClipUtils.setContent(str);
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
			ClipUtils.setContent(copiedLink);
		}
	}

	/**
	 * Search key word for site to be searched from favorite sites.
	 * 
	 * @param keyWordSite
	 *            key word for site to be searched.
	 * @return candidate sites.
	 */
	public Vector getCandidateSites(final String keyWordSite) {
		final Vector candidateSites = new Vector();

		// 如果關鍵字是空字串，那就什麼都不要回
		if (keyWordSite.length() == 0) {
			return candidateSites;
		}

		Iterator siteIterator;
		Site site;

		// 加入站台列表中符合的
		siteIterator = resource.getFavorites().iterator();
		while (siteIterator.hasNext()) {
			site = (Site) siteIterator.next();
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
		return PasswordPane.showPasswordDialog(view, InternationalMessages
				.getString("Model.Password_Text"), InternationalMessages
				.getString("Model.Password_Title")); 
	}

	/**
	 * Getter of sessions
	 * 
	 * @return the sessions
	 */
	public Vector getSessions() {
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
						InternationalMessages.getString("Model.User_Name_Text"), InternationalMessages.getString("Model.User_Name_Title"), //$NON-NLS-1$ //$NON-NLS-2$
						JOptionPane.QUESTION_MESSAGE);
	}

	/**
	 * Check if the session is selected on tab page.
	 * 
	 * @param session
	 * @return true if the session is selected on tab page; false, otherwise.
	 */
	public boolean isTabForeground(final SessionPane session) {
		return view.isTabForeground(session);
	}

	/**
	 * Ask user host and open the site which user enters to dialog.
	 */
	public void open() {
		final String site = JOptionPane.showInputDialog(view, InternationalMessages
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
			showMessage(InternationalMessages
					.getString("ZTerm.Message_Wrong_Explorer_Command")); //$NON-NLS-1$
			return;
		}

		// 把 %u 置換成給定的 url
		final int urlIndex = cmd.indexOf("%u"); //$NON-NLS-1$
		if (urlIndex == -1) {
			showMessage(InternationalMessages
					.getString("ZTerm.Message_Wrong_Explorer_Command")); //$NON-NLS-1$
			return;
		}

		cmd = cmd.substring(0, urlIndex) + url + cmd.substring(urlIndex + 2);
		runExternal(cmd);
	}

	/**
	 * Do paste at current session.
	 */
	public void paste() {
		final SessionPane session = getCurrentSession();
		if (session != null) {
			session.pasteText(ClipUtils.getContent());
		}
	}

	/**
	 * Reopen session which is currently disconnected.
	 * 
	 * @param session
	 *            session to be reopened.
	 */
	public void reopenSession(final SessionPane session) {
		view.reopenSession(session);
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
		showMessage(InternationalMessages.getString("ZTerm.Message_About")); //$NON-NLS-1$
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
		final HtmlPane faqHtmlDialog = new HtmlPane(getClass().getResource(
			"/res/docs/faq.html")); //$NON-NLS-1$ 
		final JDialog dialog = faqHtmlDialog.createDialog(view, InternationalMessages
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
		view.showPopup(x, y, link);
	}

	/**
	 * Show preference dialog.
	 */
	public void showPreference() {
		final JDialog dialog = preferencePane.createDialog(view, InternationalMessages
				.getString("Preference.Title"));
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
		final HtmlPane usageHtmlDialog = new HtmlPane(getClass().getResource(
			"/res/docs/usage.html")); //$NON-NLS-1$ 
		final JDialog dialog = usageHtmlDialog.createDialog(view, InternationalMessages
				.getString("ZTerm.Title_Manual")); //$NON-NLS-1$
		dialog.setSize(640, 400);
		dialog.setVisible(true);
	}

	/**
	 * Update anti idle time to each session.
	 */
	public void updateAntiIdleTime() {
		for (int i = 0; i < sessions.size(); i++) {
			final SessionPane session = (SessionPane) sessions.elementAt(i);
			session.updateAntiIdleTime();
		}
	}

	/**
	 * Update the encode to current session.
	 * 
	 * @param encoding
	 *            the encode name to be updated.
	 */
	public void updateEncoding(final String encoding) {
		final SessionPane session = getCurrentSession();
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
	 * Update size to resource, and also user interface.
	 */
	public void updateSize() {
		view.updateSize();
	}

	/**
	 * Update tab page.
	 */
	public void updateTab() {
		final SessionPane session = getCurrentSession();

		if (session != null) {
			// 切換到 alert 的 session 時設定狀態為 connected, 以取消 bell.
			if (session.getState() == SessionPane.STATE_ALERT) {
				session.setState(SessionPane.STATE_CONNECTED);
			}

			// 因為全部的連線共用一張 BufferedImage, 切換分頁時需重繪內容。
			session.updateScreen();

			// 讓所選的 session 取得 focus
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					session.requestFocusInWindow();
				}
			});
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
	public void updateTabState(final int state, final SessionPane session) {
		view.updateTabState(state, session);
	}

	/**
	 * Update tab title to each tab page.
	 */
	public void updateTabTitle() {
		view.updateTabTitle();
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
				if (url.substring(0, position).equalsIgnoreCase(
						Protocol.TELNET)) {
					protocol = Protocol.TELNET;
				} else {
					showMessage(InternationalMessages
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
		InternationalMessages.restartBundle();
		this.view.updateText();
		this.preferencePane = new PreferencePane();
		siteManager = new SiteManager();
	}
	
	/**
	 * Hide the menu bar.
	 */
	public void hideMenuBar() {
		view.removeMenuBar();
		view.updateSize();
	}
	
	/**
	 * Show the menu bar.
	 */
	public void showMenuBar() {
		view.showMenuBar();
		view.updateSize();
	}
}
