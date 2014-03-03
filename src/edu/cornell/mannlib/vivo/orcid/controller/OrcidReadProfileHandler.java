/* $This file is distributed under the terms of the license in /doc/license.txt$ */

package edu.cornell.mannlib.vivo.orcid.controller;

import static edu.cornell.mannlib.orcidclient.actions.ApiAction.READ_PROFILE;
import static edu.cornell.mannlib.vivo.orcid.controller.OrcidConfirmationState.Progress.DENIED_PROFILE;
import static edu.cornell.mannlib.vivo.orcid.controller.OrcidConfirmationState.Progress.FAILED_PROFILE;
import static edu.cornell.mannlib.vivo.orcid.controller.OrcidConfirmationState.Progress.GOT_PROFILE;
import static edu.cornell.mannlib.vivo.orcid.controller.OrcidConfirmationState.Progress.ID_ALREADY_PRESENT;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.cornell.mannlib.orcidclient.OrcidClientException;
import edu.cornell.mannlib.orcidclient.actions.ReadProfileAction;
import edu.cornell.mannlib.orcidclient.auth.AuthorizationStatus;
import edu.cornell.mannlib.orcidclient.orcidmessage.OrcidMessage;
import edu.cornell.mannlib.vitro.webapp.controller.VitroRequest;
import edu.cornell.mannlib.vitro.webapp.controller.freemarker.responsevalues.ResponseValues;

/**
 * We should now be logged in and authorized to read the ORCID profile.
 */
public class OrcidReadProfileHandler extends OrcidAbstractHandler {
	private static final Log log = LogFactory
			.getLog(OrcidReadProfileHandler.class);

	private AuthorizationStatus status;
	private OrcidMessage profile;

	protected OrcidReadProfileHandler(VitroRequest vreq) {
		super(vreq);
	}

	public ResponseValues exec() throws OrcidClientException {
		status = auth.getAuthorizationStatus(READ_PROFILE);
		if (status.isSuccess()) {
			readProfile();
			state.progress(GOT_PROFILE, profile);

			recordConfirmation();
			
			if (state.getVivoId() != null) {
				state.progress(ID_ALREADY_PRESENT);
			}
			
			return showConfirmationPage();
		} else if (status.isDenied()) {
			return showConfirmationPage(DENIED_PROFILE);
		} else {
			return showConfirmationPage(FAILED_PROFILE);
		}
	}

	private void readProfile() throws OrcidClientException {
		profile = new ReadProfileAction(occ).execute(status.getAccessToken());
		log.debug("Read profile");
	}

}
