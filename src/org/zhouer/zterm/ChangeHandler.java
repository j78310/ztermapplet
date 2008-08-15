package org.zhouer.zterm;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * ChangeHandler is a change controller for ZTerm Applet.
 * 
 * @author h45
 */
public class ChangeHandler implements ChangeListener {

	private Model model;

	private ZTerm view;

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

	public void stateChanged(final ChangeEvent e) {
		// 切換分頁，更新視窗標題、畫面
		if (e.getSource() == this.view.tabbedPane) {
			this.model.updateTab();
		}
	}
}
