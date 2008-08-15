package org.zhouer.zterm;

import java.util.Vector;

/**
 * Sessions is collection of Session.
 * 
 * @author h45
 */
public class Sessions extends Vector<Session> {
	private static final long serialVersionUID = -4458258447638659749L;
	private static Sessions sessions = null;

	/**
	 * Getter of instance in singleton pattern
	 * 
	 * @return sessions
	 */
	public static Sessions getInstance() {
		if (Sessions.sessions == null) {
			Sessions.sessions = new Sessions();
		}

		return Sessions.sessions;
	}

	private Sessions() {
		super();
	}
}
