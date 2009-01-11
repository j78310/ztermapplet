package org.zhouer.zterm.view;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Locale;

import javax.swing.AbstractButton;

import org.zhouer.utils.InternationalMessages;
import org.zhouer.zterm.model.Model;
import org.zhouer.zterm.model.Resource;
import org.zhouer.zterm.model.Site;

/**
 * ActionHandler is an action controller for ZTerm Applet.
 * 
 * @author Chin-Chang Yang
 */
public class ActionHandler implements ActionListener {

	private Model model;
	private final Resource resource;

	/**
	 * Constructor of ActionHandler which initializes the member field,
	 * resource.
	 */
	public ActionHandler() {
		this.resource = Resource.getInstance(); // 各種設定
	}

	public void actionPerformed(final ActionEvent ae) {
		final Object source = ae.getSource();
		final String command = ae.getActionCommand();
		
		if (InternationalMessages.getString("ZTerm.Open_MenuItem_Text").equals(command)) { 
			this.model.open();
		} else if (InternationalMessages.getString("ZTerm.Reopen_Item_Text").equals(command)) {
			model.reopenSession(model.getCurrentSession());
		} else if (InternationalMessages.getString("ZTerm.Close_MenuItem_Text").equals(command)) {
			model.closeCurrentTab();
		} else if (InternationalMessages.getString("ZTerm.Copy_MenuItem_Text").equals(command)) {
			this.model.copy();
		} else if (InternationalMessages.getString("ZTerm.ColorCopy_MenuItem_Text").equals(command)) {
			this.model.colorCopy();
		} else if (InternationalMessages.getString("ZTerm.Paste_MenuItem_Text").equals(command)) {
			this.model.paste();
		} else if (InternationalMessages.getString("ZTerm.ColorPaste_MenuItem_Text").equals(command)) {
			this.model.colorPaste();
		} else if (InternationalMessages.getString("ZTerm.Popup_CopyLink_MenuItem_Text").equals(command)) {
			this.model.copyLink();
		} else if (InternationalMessages.getString("ZTerm.Preference_MenuItem_Text").equals(command)) {
			this.model.showPreference();
		} else if (InternationalMessages.getString("ZTerm.SiteManager_MenuItem_Text").equals(command)) {
			this.model.showSiteManager();
		} else if (InternationalMessages.getString("ZTerm.Usage_MenuItem_Text").equals(command)) {
			this.model.showUsage();
		} else if (InternationalMessages.getString("ZTerm.FAQ_MenuItem_Text").equals(command)) {
			this.model.showFAQ();
		} else if (InternationalMessages.getString("ZTerm.About_MenuItem_Text").equals(command)) {
			this.model.showAbout();
		} else if (InternationalMessages.getString("ZTerm.Big5_MenuItem_Text").equals(command)) {
			this.model.updateEncoding("Big5"); //$NON-NLS-1$
		} else if (InternationalMessages.getString("ZTerm.UTF8_MenuItem_Text").equals(command)) {
			this.model.updateEncoding("UTF-8"); //$NON-NLS-1$			
		} else if (InternationalMessages.getString("ZTerm.HideMenuBar_MenuItem_Text").equals(command)) {
			model.hideMenuBar();
		} else if (InternationalMessages.getString("ZTerm.ShowMenuBar_MenuItem_Text").equals(command)) {
			model.showMenuBar();
		} else if (InternationalMessages.getString("ZTerm.Language_English_Item").equals(command)) {
			this.model.setLocale(Locale.ENGLISH);
			model.refreshMessages();
			this.resource.writeFile();
		} else if (InternationalMessages.getString("ZTerm.Language_TraditionalChinese_Item").equals(command)) {
			this.model.setLocale(Locale.TAIWAN);
			model.refreshMessages();
			this.resource.writeFile();
		} else if (InternationalMessages.getString("ZTerm.Close_MenuItem_Text").equals(command)) {
			model.closeCurrentTab();
		} else if (ActionCommand.CONNECT_COMMAND.equals(command)) {
			if (source instanceof AbstractButton) {
				final AbstractButton button = (AbstractButton) source;
				final String siteName = button.getText();
				final Site site = resource.getFavorite(siteName);
				model.connect(site, -1);
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
}
