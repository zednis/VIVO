/* $This file is distributed under the terms of the license in /doc/license.txt$ */

package edu.cornell.mannlib.vivo.orcid.controller;

import static edu.cornell.mannlib.orcidclient.actions.ApiAction.READ_PROFILE;
import static edu.cornell.mannlib.vivo.orcid.controller.OrcidIntegrationController.TEMPLATE_SUCCESS;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.cornell.mannlib.orcidclient.OrcidClientException;
import edu.cornell.mannlib.orcidclient.actions.ReadProfileAction;
import edu.cornell.mannlib.orcidclient.auth.AuthorizationStatus;
import edu.cornell.mannlib.orcidclient.orcidmessage.OrcidMessage;
import edu.cornell.mannlib.vitro.webapp.controller.VitroRequest;
import edu.cornell.mannlib.vitro.webapp.controller.freemarker.responsevalues.ResponseValues;
import edu.cornell.mannlib.vitro.webapp.controller.freemarker.responsevalues.TemplateResponseValues;

/**
 * We should now be logged in and authorized to read the ORCID profile.
 */
public class OrcidReadProfileHandler extends OrcidAbstractHandler {
	private static final Log log = LogFactory
			.getLog(OrcidReadProfileHandler.class);

	private AuthorizationStatus status;
	private String orcid;

	protected OrcidReadProfileHandler(VitroRequest vreq) {
		super(vreq);
	}

	public ResponseValues exec() throws OrcidClientException {
		status = auth.getAuthorizationStatus(READ_PROFILE);
		if (status.isSuccess()) {
			readProfile();
			return showSuccess();
		} else if (status.isDenied()) {
			return showDeniedAuthorization(READ_PROFILE);
		} else {
			return showFailedAuthorization(READ_PROFILE);
		}
	}

	private void readProfile() throws OrcidClientException {
		@SuppressWarnings("unused")
		OrcidMessage message = new ReadProfileAction(occ).execute(status
				.getAccessToken());

		orcid = status.getAccessToken().getOrcid();
		log.debug("Read profile, orcid=" + orcid);
	}

	private ResponseValues showSuccess() {
		Map<String, Object> map = new HashMap<>();
		map.put("continueUrl", profileUrl());
		map.put("orcid", orcid);
		return new TemplateResponseValues(TEMPLATE_SUCCESS, map);
	}

}
