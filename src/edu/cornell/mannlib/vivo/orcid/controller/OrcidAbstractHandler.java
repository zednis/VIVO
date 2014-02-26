/* $This file is distributed under the terms of the license in /doc/license.txt$ */

package edu.cornell.mannlib.vivo.orcid.controller;

import static edu.cornell.mannlib.vivo.orcid.OrcidIdDataGetter.ORCID_ID;
import static edu.cornell.mannlib.vivo.orcid.OrcidIdDataGetter.ORCID_IS_VALIDATED;
import static edu.cornell.mannlib.vivo.orcid.controller.OrcidIntegrationController.TEMPLATE_DENIED;
import static edu.cornell.mannlib.vivo.orcid.controller.OrcidIntegrationController.TEMPLATE_FAILED;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.cornell.mannlib.orcidclient.actions.ApiAction;
import edu.cornell.mannlib.orcidclient.auth.AuthorizationManager;
import edu.cornell.mannlib.orcidclient.auth.AuthorizationStatus;
import edu.cornell.mannlib.orcidclient.context.OrcidClientContext;
import edu.cornell.mannlib.vedit.beans.LoginStatusBean;
import edu.cornell.mannlib.vitro.webapp.beans.Individual;
import edu.cornell.mannlib.vitro.webapp.beans.ObjectPropertyStatement;
import edu.cornell.mannlib.vitro.webapp.beans.ObjectPropertyStatementImpl;
import edu.cornell.mannlib.vitro.webapp.beans.UserAccount;
import edu.cornell.mannlib.vitro.webapp.controller.VitroRequest;
import edu.cornell.mannlib.vitro.webapp.controller.freemarker.UrlBuilder;
import edu.cornell.mannlib.vitro.webapp.controller.freemarker.responsevalues.ResponseValues;
import edu.cornell.mannlib.vitro.webapp.controller.freemarker.responsevalues.TemplateResponseValues;
import edu.cornell.mannlib.vitro.webapp.dao.IndividualDao;
import edu.cornell.mannlib.vitro.webapp.dao.ObjectPropertyStatementDao;

/**
 * Some utility methods for the handlers.
 */
public abstract class OrcidAbstractHandler {
	private static final Log log = LogFactory
			.getLog(OrcidAbstractHandler.class);

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

	protected void recordValidation(String orcid) {
		String individualUri = state.getIndividualUri();
		log.debug("Recording validation of ORCID '" + orcid + "' on '"
				+ individualUri + "'");
		ObjectPropertyStatement ops1 = new ObjectPropertyStatementImpl(
				individualUri, ORCID_ID, orcid);
		ObjectPropertyStatement ops2 = new ObjectPropertyStatementImpl(orcid,
				ORCID_IS_VALIDATED, individualUri);

		ObjectPropertyStatementDao opsd = vreq.getWebappDaoFactory()
				.getObjectPropertyStatementDao();
		opsd.insertNewObjectPropertyStatement(ops1);
		opsd.insertNewObjectPropertyStatement(ops2);
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

		AuthorizationStatus status = auth.getAuthorizationStatus(action);
		map.put("errorCode", status.getErrorCode());
		map.put("errorDescription", status.getErrorDescription());
		map.put("exception", status.getException().toString());

		return new TemplateResponseValues(TEMPLATE_FAILED, map);
	}

}
