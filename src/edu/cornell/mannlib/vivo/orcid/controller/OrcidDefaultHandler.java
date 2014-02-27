/* $This file is distributed under the terms of the license in /doc/license.txt$ */

package edu.cornell.mannlib.vivo.orcid.controller;

import static edu.cornell.mannlib.vivo.orcid.controller.OrcidIntegrationController.PATH_AUTH_PROFILE;
import static edu.cornell.mannlib.vivo.orcid.controller.OrcidIntegrationController.TEMPLATE_OFFER;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.cornell.mannlib.orcidclient.actions.ApiAction;
import edu.cornell.mannlib.vedit.beans.LoginStatusBean;
import edu.cornell.mannlib.vitro.webapp.auth.identifier.IdentifierBundle;
import edu.cornell.mannlib.vitro.webapp.auth.identifier.RequestIdentifiers;
import edu.cornell.mannlib.vitro.webapp.auth.identifier.common.HasProfile;
import edu.cornell.mannlib.vitro.webapp.beans.Individual;
import edu.cornell.mannlib.vitro.webapp.beans.UserAccount;
import edu.cornell.mannlib.vitro.webapp.controller.VitroRequest;
import edu.cornell.mannlib.vitro.webapp.controller.freemarker.UrlBuilder;
import edu.cornell.mannlib.vitro.webapp.controller.freemarker.responsevalues.NotAuthorizedResponseValues;
import edu.cornell.mannlib.vitro.webapp.controller.freemarker.responsevalues.ResponseValues;
import edu.cornell.mannlib.vitro.webapp.controller.freemarker.responsevalues.TemplateResponseValues;

/**
 * A request came from the "Confirm" button on the individual profile. Get a
 * fresh state object, clear the AuthorizationCache and show the "Offer" page.
 */
public class OrcidDefaultHandler extends OrcidAbstractHandler {
	private static final Log log = LogFactory.getLog(OrcidDefaultHandler.class);

	private Individual individual;

	public OrcidDefaultHandler(VitroRequest vreq) {
		super(vreq);
	}

	public ResponseValues exec() {
		try {
			initializeState();
			initializeAuthorizationCache();
			individual = findIndividual();
		} catch (Exception e) {
			log.error("No proper individual URI on the request", e);
			return show400BadRequest(e);
		}

		if (!isAuthorized()) {
			return showNotAuthorized();
		}

		return showOffer();
	}

	private void initializeState() {
		String uri = vreq.getParameter("individualUri");
		if (uri == null) {
			throw new IllegalStateException(
					"No 'individualUri' parameter on request.");
		}
		state.reset().setIndividualUri(uri);
	}

	private void initializeAuthorizationCache() {
		auth.clearStatus(ApiAction.READ_PROFILE);
		auth.clearStatus(ApiAction.ADD_EXTERNAL_ID);
	}

	private ResponseValues show400BadRequest(Exception e) {
		Map<String, Object> map = new HashMap<>();
		map.put("title", "400 Bad Request");
		map.put("errorMessage", e.getMessage());
		return new TemplateResponseValues("error-titled.ftl", map,
				SC_BAD_REQUEST);
	}

	private boolean isAuthorized() {
		// Only a self-editor is authorized.
		IdentifierBundle ids = RequestIdentifiers.getIdBundleForRequest(vreq);
		Collection<String> profileUris = HasProfile.getProfileUris(ids);
		log.debug("Authorized? individualUri=" + state.getIndividualUri()
				+ ", profileUris=" + profileUris);
		return profileUris.contains(state.getIndividualUri());
	}

	private ResponseValues showNotAuthorized() {
		UserAccount user = LoginStatusBean.getCurrentUser(vreq);
		String userName = (user == null) ? "ANONYMOUS" : user.getEmailAddress();
		return new NotAuthorizedResponseValues(userName
				+ "is not authorized for ORCID operations on '" + individual
				+ "'");
	}

	private ResponseValues showOffer() {
		Map<String, Object> map = new HashMap<>();
		map.put("orcidControllerUrl",
				UrlBuilder.getUrl(PATH_AUTH_PROFILE));
		map.put("cancelUrl", profileUrl());
		return new TemplateResponseValues(TEMPLATE_OFFER, map);
	}

}
