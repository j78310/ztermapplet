package org.zhouer.zterm.view;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Locale;

import org.zhouer.zterm.model.Model;
import org.zhouer.zterm.model.Resource;
import org.zhouer.zterm.model.Site;

/**
 * ActionHandler is an action controller for ZTerm Applet.
 * 
 * @author h45
 */
public class ActionHandler implements ActionListener {

	private Model model;
	private final Resource resource;
	private ZTerm view;

	/**
	 * Constructor of ActionHandler which initializes the member field,
	 * resource.
	 */
	public ActionHandler() {
		this.resource = Resource.getInstance(); // 各種設定
	}

	public void actionPerformed(final ActionEvent ae) {
		final Object source = ae.getSource();
		model.requestFocusToCurrentSession();
		
		// TODO 減少 IF-ELSE 結構以提升效能，如：多個 Handler 實體。
		if (source == this.view.openItem) {
			this.model.open();
		} else if (source == this.view.reopenItem) {
			model.reopenSession(model.getCurrentSession());
		} else if (source == this.view.closeItem) {
			model.closeCurrentTab();
		} else if ((source == this.view.copyItem)
				|| (source == this.view.popupCopyItem)) {
			this.model.copy();
		} else if ((source == this.view.colorCopyItem)
				|| (source == this.view.popupColorCopyItem)) {
			this.model.colorCopy();
		} else if ((source == this.view.pasteItem)
				|| (source == this.view.popupPasteItem)) {
			this.model.paste();
		} else if ((source == this.view.colorPasteItem)
				|| (source == this.view.popupColorPasteItem)) {
			this.model.colorPaste();
		} else if (source == this.view.popupCopyLinkItem) {
			this.model.copyLink();
		} else if (source == this.view.preferenceItem) {
			this.model.showPreference();
		} else if (source == this.view.siteManagerItem) {
			this.model.showSiteManager();
		} else if (source == this.view.usageItem) {
			this.model.showUsage();
		} else if (source == this.view.faqItem) {
			this.model.showFAQ();
		} else if (source == this.view.aboutItem) {
			this.model.showAbout();
		} else if (source == this.view.big5Item) {
			this.model.updateEncoding("Big5"); //$NON-NLS-1$
		} else if (source == this.view.utf8Item) {
			this.model.updateEncoding("UTF-8"); //$NON-NLS-1$
		} else if (source == this.view.hideMenuBarItem) {
			model.hideMenuBar();
		} else if (source == this.view.showMenuBarItem) {
			model.showMenuBar();
		} else if (source == this.view.languageItems[0]) {
			this.model.setLocale(Locale.ENGLISH);
			model.refreshMessages();
			this.resource.writeFile();
		} else if (source == this.view.languageItems[1]) {
			this.model.setLocale(Locale.TAIWAN);
			model.refreshMessages();
			this.resource.writeFile();
		} else if (source == this.view.popupCloseItem) {
			model.closeCurrentTab();
		} else {
			// 我的最愛列表
			for (int i = 0; i < this.view.favoriteItems.length; i++) {
				if (source == this.view.favoriteItems[i]) {
					final Site f = this.resource
							.getFavorite(this.view.favoriteItems[i].getText());
					this.model.connect(f, -1);
					break;
				}
			}
		}
	}

	/**
	 * Setter of model
	 * 
	 * @param model
	 *            the model to set
	 */
	public void setModel(final Model model) {
		this.model = model;
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
}
