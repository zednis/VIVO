/* $This file is distributed under the terms of the license in /doc/license.txt$ */

package edu.cornell.mannlib.vivo.orcid.controller;

import static edu.cornell.mannlib.orcidclient.actions.ApiAction.ADD_EXTERNAL_ID;
import static edu.cornell.mannlib.orcidclient.actions.ApiAction.READ_PROFILE;
import static edu.cornell.mannlib.vivo.orcid.controller.OrcidIntegrationController.PATH_ADD_EXTERNAL_IDS;
import static edu.cornell.mannlib.vivo.orcid.controller.OrcidIntegrationController.PATH_READ_PROFILE;

import java.net.URISyntaxException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.cornell.mannlib.orcidclient.OrcidClientException;
import edu.cornell.mannlib.orcidclient.auth.AuthorizationStatus;
import edu.cornell.mannlib.vitro.webapp.controller.VitroRequest;
import edu.cornell.mannlib.vitro.webapp.controller.freemarker.responsevalues.RedirectResponseValues;
import edu.cornell.mannlib.vitro.webapp.controller.freemarker.responsevalues.ResponseValues;

/**
 * We offered the confirmation screen, and they decided to go ahead.
 * 
 * We can't assume that they haven't been here before, so they might already
 * have authorized, or denied authorization.
 */
public class OrcidStartConfirmationHandler extends OrcidAbstractHandler {
	private static final Log log = LogFactory
			.getLog(OrcidStartConfirmationHandler.class);
	
	
	private AuthorizationStatus status;

	public OrcidStartConfirmationHandler(VitroRequest vreq) {
		super(vreq);
	}

	public ResponseValues exec() throws URISyntaxException,
			OrcidClientException {
		updateState();

		if (!isAddingExternalIds()) {
			status = auth.getAuthorizationStatus(READ_PROFILE);
			if (status.isNone()) {
				return seekAuthorizationForReadProfile();
			} else if (status.isSuccess()) {
				return redirectToReadProfile();
			} else if (status.isDenied()) {
				return showDeniedAuthorization(READ_PROFILE);
			} else {
				return showFailedAuthorization(READ_PROFILE);
			}
		} else {
			status = auth.getAuthorizationStatus(ADD_EXTERNAL_ID);
			if (status.isNone()) {
				return seekAuthorizationForExternalIds();
			} else if (status.isSuccess()) {
				return redirectToAddExternalIds();
			} else if (status.isDenied()) {
				return showDeniedAuthorization(ADD_EXTERNAL_ID);
			} else {
				return showFailedAuthorization(ADD_EXTERNAL_ID);
			}
		}
	}

	private void updateState() {
		state.setAddVivoId(vreq.getParameterMap().keySet()
				.contains("addVivoId"));
		state.setAddCornellId(vreq.getParameterMap().keySet()
				.contains("addCornellId"));
	}

	private boolean isAddingExternalIds() {
		return state.isAddVivoId() || state.isAddCornellId();
	}

	private ResponseValues seekAuthorizationForReadProfile()
			throws OrcidClientException, URISyntaxException {
		log.debug("Seeking authorization to read profile.");
		String returnUrl = occ.resolvePathWithWebapp(PATH_READ_PROFILE);
		String seekUrl = auth.seekAuthorization(READ_PROFILE, returnUrl);
		return new RedirectResponseValues(seekUrl);
	}

	private ResponseValues redirectToReadProfile() throws URISyntaxException {
		log.debug("Already authorized to read profile.");
		return new RedirectResponseValues(
				occ.resolvePathWithWebapp(PATH_READ_PROFILE));
	}

	private ResponseValues seekAuthorizationForExternalIds()
			throws OrcidClientException, URISyntaxException {
		log.debug("Seeking authorization to add external IDs");
		String returnUrl = occ.resolvePathWithWebapp(PATH_ADD_EXTERNAL_IDS);
		String seekUrl = auth.seekAuthorization(ADD_EXTERNAL_ID, returnUrl);
		return new RedirectResponseValues(seekUrl);
	}

	private ResponseValues redirectToAddExternalIds() throws URISyntaxException {
		log.debug("Already authorized to add external IDs.");
		return new RedirectResponseValues(
				occ.resolvePathWithWebapp(PATH_ADD_EXTERNAL_IDS));
	}

}
