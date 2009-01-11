package org.zhouer.zterm.view;

import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.zhouer.zterm.model.Model;

/**
 * ChangeHandler is a change controller for ZTerm Applet.
 * 
 * @author Chin-Chang Yang
 */
public class ChangeHandler implements ChangeListener {

	private Model model;

	/**
	 * Setter of model
	 * 
	 * @param model
	 *            the model to set
	 */
	public void setModel(final Model model) {
		this.model = model;
	}

	public void stateChanged(final ChangeEvent e) {		
		// 切換分頁，更新視窗標題、畫面
		if (e.getSource() instanceof JTabbedPane) {
			this.model.updateTab();
		}
	}
}
