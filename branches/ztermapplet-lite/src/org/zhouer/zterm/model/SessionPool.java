package org.zhouer.zterm.model;

import java.util.Vector;

/**
 * Sessions is collection of Session.
 * 
 * @author Chin-Chang Yang
 */
public class SessionPool extends Vector {
	private static final long serialVersionUID = -4458258447638659749L;
	private volatile static SessionPool sessions = null;

	/**
	 * Getter of instance in singleton pattern
	 * 
	 * @return sessions
	 */
	public static SessionPool getInstance() {
		if (SessionPool.sessions == null) {
			synchronized (SessionPool.class) {
				if (SessionPool.sessions == null) {
					SessionPool.sessions = new SessionPool();
				}
			}
		}

		return SessionPool.sessions;
	}

	private SessionPool() {
		super();
	}
}
