/* $This file is distributed under the terms of the license in /doc/license.txt$ */

package edu.cornell.mannlib.vivo.orcid.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * Keep track of what was requested for the Orcid confirmation. 
 */
class OrcidConfirmationState {
	// ----------------------------------------------------------------------
	// The factory
	// ----------------------------------------------------------------------

	private static final String ATTRIBUTE_NAME = OrcidConfirmationState.class
			.getName();

	static OrcidConfirmationState fetch(HttpServletRequest req) {
		HttpSession session = req.getSession();
		Object o = session.getAttribute(ATTRIBUTE_NAME);
		if (o instanceof OrcidConfirmationState) {
			return (OrcidConfirmationState) o;
		} else {
			OrcidConfirmationState ocs = new OrcidConfirmationState();
			session.setAttribute(ATTRIBUTE_NAME, ocs);
			return ocs;
		}
	}

	// ----------------------------------------------------------------------
	// The instance
	// ----------------------------------------------------------------------

	private String individualUri;
	private boolean addCornellId;
	private boolean addVivoId;

	OrcidConfirmationState() {
		reset();
	}

	public OrcidConfirmationState reset() {
		individualUri = "NO_URI";
		addCornellId = false;
		addVivoId = false;
		return this;
	}

	public String getIndividualUri() {
		return individualUri;
	}

	public OrcidConfirmationState setIndividualUri(String individualUri) {
		this.individualUri = individualUri;
		return this;
	}

	public boolean isAddCornellId() {
		return addCornellId;
	}

	public OrcidConfirmationState setAddCornellId(boolean addCornellId) {
		this.addCornellId = addCornellId;
		return this;
	}

	public boolean isAddVivoId() {
		return addVivoId;
	}

	public OrcidConfirmationState setAddVivoId(boolean addVivoId) {
		this.addVivoId = addVivoId;
		return this;
	}

}
