package org.zhouer.zterm;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.SwingUtilities;

public class MouseHandler implements MouseListener {
	
	private ZTerm view;
	
	private Model model;
	
	public void setView(ZTerm view) {
		this.view = view;
	}
	
	public void setModel(Model model) {
		this.model = model;
	}

	public void mouseClicked(MouseEvent e) {
		// Clicked right button
		if (e.getButton() == MouseEvent.BUTTON3) {
			final int x = e.getX();
			final int y = e.getY();
			
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					view.popupMenu.show(view, x, y);
				}
			});
		}
	}

	public void mouseEntered(MouseEvent e) {
		model.requestFocusToCurrentSession();
	}

	public void mouseExited(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	public void mousePressed(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	public void mouseReleased(MouseEvent e) {
		model.requestFocusToCurrentSession();
	}

}
