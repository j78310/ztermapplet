package org.zhouer.zterm;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Locale;

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

		if (source == this.view.openItem) {
			this.model.open();
		} else if (source == this.view.openButton) {
			this.model.openNewTab();
		} else if ((source == this.view.closeItem)
				|| (source == this.view.closeButton)) {
			this.model.closeCurrentTab();
		} else if ((source == this.view.reopenItem)
				|| (source == this.view.reopenButton)) {
			this.model.reopenSession((Session) this.view.tabbedPane
					.getSelectedComponent());
		} else if ((source == this.view.copyItem)
				|| (source == this.view.copyButton)
				|| (source == this.view.popupCopyItem)) {
			this.model.copy();
		} else if ((source == this.view.colorCopyItem)
				|| (source == this.view.colorCopyButton)
				|| (source == this.view.popupColorCopyItem)) {
			this.model.colorCopy();
		} else if ((source == this.view.pasteItem)
				|| (source == this.view.pasteButton)
				|| (source == this.view.popupPasteItem)) {
			this.model.paste();
		} else if ((source == this.view.colorPasteItem)
				|| (source == this.view.colorPasteButton)
				|| (source == this.view.popupColorPasteItem)) {
			this.model.colorPaste();
		} else if (source == this.view.popupCopyLinkItem) {
			this.model.copyLink();
		} else if (source == this.view.telnetButton) {
			this.view.siteText.setText(Messages
					.getString("ActionHandler.TelnetHeader")); //$NON-NLS-1$
			this.view.siteField.requestFocusInWindow();
		} else if (source == this.view.sshButton) {
			this.view.siteText.setText(Messages
					.getString("ActionHandler.SSHHeader")); //$NON-NLS-1$
			this.view.siteField.requestFocusInWindow();
		} else if (source == this.view.preferenceItem) {
			this.model.showPreference();
		} else if (source == this.view.siteManagerItem) {
			this.model.showSiteManager();
		} else if (source == this.view.showToolbarItem) {
			final boolean isShowToolbar = this.resource
					.getBooleanValue(Resource.SHOW_TOOLBAR);
			this.resource.setValue(Resource.SHOW_TOOLBAR, !isShowToolbar);
			this.model.updateToolbar(!isShowToolbar);
			this.model.updateSize();
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
		} else if (source == this.view.languageItems[0]) {
			resource.setLocale(Locale.ENGLISH);
			model.refreshMessages();
			this.resource.writeRcFile();
		} else if (source == this.view.languageItems[1]) {
			resource.setLocale(Locale.TAIWAN);
			model.refreshMessages();
			this.resource.writeRcFile();
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
