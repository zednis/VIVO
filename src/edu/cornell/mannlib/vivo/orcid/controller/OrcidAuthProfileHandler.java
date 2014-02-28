/* $This file is distributed under the terms of the license in /doc/license.txt$ */

package edu.cornell.mannlib.vivo.orcid.controller;

import static edu.cornell.mannlib.orcidclient.actions.ApiAction.READ_PROFILE;
import static edu.cornell.mannlib.vivo.orcid.controller.OrcidConfirmationState.Progress.DENIED_PROFILE;
import static edu.cornell.mannlib.vivo.orcid.controller.OrcidConfirmationState.Progress.FAILED_PROFILE;
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
 * We offered the confirmation screen, and they decided to go ahead. Get
 * authorization to read the profile.
 * 
 * We can't assume that they haven't been here before, so they might already
 * have authorized, or denied authorization.
 */
public class OrcidAuthProfileHandler extends OrcidAbstractHandler {
	private static final Log log = LogFactory
			.getLog(OrcidAuthProfileHandler.class);

	private AuthorizationStatus status;

	public OrcidAuthProfileHandler(VitroRequest vreq) {
		super(vreq);
	}

	public ResponseValues exec() throws URISyntaxException,
			OrcidClientException {
		status = auth.getAuthorizationStatus(READ_PROFILE);
		if (status.isNone()) {
			return seekAuthorizationForReadProfile();
		} else if (status.isSuccess()) {
			return redirectToReadProfile();
		} else if (status.isDenied()) {
			return showConfirmationPage(DENIED_PROFILE);
		} else {
			return showConfirmationPage(FAILED_PROFILE);
		}
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

}
