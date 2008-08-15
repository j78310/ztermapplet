package org.zhouer.zterm;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Iterator;

/**
 * KeyHandler is a key controller for ZTerm applet.
 * 
 * @author h45
 */
public class KeyHandler extends KeyAdapter {

	private Model model;

	private ZTerm view;

	@Override
	public void keyPressed(final KeyEvent e) {
		// Mac 下 keyTyped 收不到 ESCAPE 一定要在這裡處理
		if (e.getSource() == this.view.siteText) {
			if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
				this.model.updateTab();
			}
		}
	}

	@Override
	public void keyTyped(final KeyEvent e) {
		if (e.getSource() == this.view.siteText) {

			// 不對快速鍵起反應
			if (e.isMetaDown() || e.isAltDown()) {
				return;
			}

			if (e.getKeyChar() == KeyEvent.VK_ENTER) {
				this.model.openNewTab();
			} else if (e.getKeyChar() == KeyEvent.VK_ESCAPE) {
				// ignore escape
			} else {
				javax.swing.SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						KeyHandler.this.updateCombo();
					}
				});
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

	protected void updateCombo() {
		final int dotPos = this.view.siteText.getCaretPosition();
		final String text = this.view.siteText.getText();
		final Iterator<Site> siteIterator = this.model.getCandidateSites(text)
				.iterator();

		this.view.siteModel.removeAllElements();
		this.view.siteModel.addElement(text);

		while (siteIterator.hasNext()) {
			this.view.siteModel.addElement(siteIterator.next().getURL());
		}

		// 還原輸入游標的位置，否則每次輸入一個字就會跑到最後面
		this.view.siteText.setCaretPosition(dotPos);

		// FIXME: 增加 item 時重繪會有問題
		// 超過一個選項時才顯示 popup
		if (this.view.siteModel.getSize() > 1) {
			this.view.siteField.showPopup();
		} else {
			this.view.siteField.hidePopup();
		}
	}
}
