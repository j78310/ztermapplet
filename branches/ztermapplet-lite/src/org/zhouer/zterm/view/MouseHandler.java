package org.zhouer.zterm.view;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.SwingUtilities;

import org.zhouer.zterm.model.Model;

public class MouseHandler extends MouseAdapter {
	
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
					view.popupMenu.show(view.getContentPane(), x, y);
				}
			});
		}
	}

	public void mouseEntered(MouseEvent e) {
		model.requestFocusToCurrentSession();
	}

	public void mouseReleased(MouseEvent e) {
		model.requestFocusToCurrentSession();
	}

}
