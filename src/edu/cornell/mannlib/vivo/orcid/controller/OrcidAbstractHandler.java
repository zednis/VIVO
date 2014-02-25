/* $This file is distributed under the terms of the license in /doc/license.txt$ */

package edu.cornell.mannlib.vivo.orcid.controller;

import static edu.cornell.mannlib.vivo.orcid.controller.OrcidIntegrationController.*;

import java.util.HashMap;
import java.util.Map;

import edu.cornell.mannlib.orcidclient.actions.ApiAction;
import edu.cornell.mannlib.orcidclient.auth.AuthorizationManager;
import edu.cornell.mannlib.orcidclient.context.OrcidClientContext;
import edu.cornell.mannlib.vedit.beans.LoginStatusBean;
import edu.cornell.mannlib.vitro.webapp.beans.Individual;
import edu.cornell.mannlib.vitro.webapp.beans.UserAccount;
import edu.cornell.mannlib.vitro.webapp.controller.VitroRequest;
import edu.cornell.mannlib.vitro.webapp.controller.freemarker.UrlBuilder;
import edu.cornell.mannlib.vitro.webapp.controller.freemarker.responsevalues.ResponseValues;
import edu.cornell.mannlib.vitro.webapp.controller.freemarker.responsevalues.TemplateResponseValues;
import edu.cornell.mannlib.vitro.webapp.dao.IndividualDao;

/**
 * Some utility methods for the handlers.
 */
public abstract class OrcidAbstractHandler {
	protected final VitroRequest vreq;
	protected final OrcidClientContext occ;
	protected final AuthorizationManager auth;
	protected final OrcidConfirmationState state;
	protected final UserAccount currentUser;

	protected OrcidAbstractHandler(VitroRequest vreq) {
		this.vreq = vreq;
		this.occ = OrcidClientContext.getInstance();
		this.auth = this.occ.getAuthorizationManager(vreq);
		this.state = OrcidConfirmationState.fetch(vreq);
		this.currentUser = LoginStatusBean.getCurrentUser(vreq);
	}

	protected Individual findIndividual() {
		String uri = state.getIndividualUri();
		try {
			IndividualDao iDao = vreq.getWebappDaoFactory().getIndividualDao();
			Individual individual = iDao.getIndividualByURI(uri);
			if (individual == null) {
				throw new IllegalStateException("Individual URI not valid: '"
						+ uri + "'");
			}
			return individual;
		} catch (Exception e) {
			throw new IllegalStateException("Individual URI not valid: '" + uri
					+ "'");
		}
	}

	protected String profileUrl() {
		return UrlBuilder.getIndividualProfileUrl(findIndividual(), vreq);
	}

	protected String cornellNetId() {
		if (currentUser == null) {
			return null;
		}
		String externalId = currentUser.getExternalAuthId();
		if (externalId == null) {
			return null;
		}
		if (externalId.trim().isEmpty()) {
			return null;
		}
		return externalId;
	}

	protected ResponseValues showDeniedAuthorization(ApiAction action) {
		Map<String, Object> map = new HashMap<>();
		map.put("requestedScope", action.getScope());
		map.put("continueUrl", profileUrl());
		return new TemplateResponseValues(TEMPLATE_DENIED, map);
	}

	protected ResponseValues showFailedAuthorization(ApiAction action) {
		Map<String, Object> map = new HashMap<>();
		map.put("requestedScope", action.getScope());
		map.put("continueUrl", profileUrl());
		return new TemplateResponseValues(TEMPLATE_FAILED, map);
	}

}
