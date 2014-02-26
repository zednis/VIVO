/* $This file is distributed under the terms of the license in /doc/license.txt$ */

package edu.cornell.mannlib.vivo.orcid.controller;

import static edu.cornell.mannlib.orcidclient.actions.ApiAction.ADD_EXTERNAL_ID;
import static edu.cornell.mannlib.orcidclient.orcidmessage.Visibility.PUBLIC;
import static edu.cornell.mannlib.vivo.orcid.controller.OrcidIntegrationController.TEMPLATE_SUCCESS;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.cornell.mannlib.orcidclient.OrcidClientException;
import edu.cornell.mannlib.orcidclient.actions.AddExternalIdAction;
import edu.cornell.mannlib.orcidclient.auth.AuthorizationStatus;
import edu.cornell.mannlib.orcidclient.beans.ExternalId;
import edu.cornell.mannlib.orcidclient.orcidmessage.OrcidMessage;
import edu.cornell.mannlib.vitro.webapp.beans.Individual;
import edu.cornell.mannlib.vitro.webapp.controller.VitroRequest;
import edu.cornell.mannlib.vitro.webapp.controller.freemarker.responsevalues.ResponseValues;
import edu.cornell.mannlib.vitro.webapp.controller.freemarker.responsevalues.TemplateResponseValues;

/**
 * We should now be logged in to ORCID and authorized to add external IDs.
 */
public class OrcidAddExternalIdsHandler extends OrcidAbstractHandler {
	private static final Log log = LogFactory
			.getLog(OrcidAddExternalIdsHandler.class);

	private AuthorizationStatus status;
	private String orcid;

	protected OrcidAddExternalIdsHandler(VitroRequest vreq) {
		super(vreq);
	}

	public ResponseValues exec() throws OrcidClientException {
		status = auth.getAuthorizationStatus(ADD_EXTERNAL_ID);
		if (status.isSuccess()) {
			addExternalIds();
			return showSuccess();
		} else if (status.isDenied()) {
			return showDeniedAuthorization(ADD_EXTERNAL_ID);
		} else {
			return showFailedAuthorization(ADD_EXTERNAL_ID);
		}
	}

	private void addExternalIds() throws OrcidClientException {
		@SuppressWarnings("unused")
		OrcidMessage message = null;

		if (state.isAddVivoId()) {
			message = addVivoId();
		}
		if (state.isAddCornellId()) {
			message = addCornellId();
		}
		orcid = status.getAccessToken().getOrcid();
		recordValidation(orcid);
	}

	private OrcidMessage addVivoId() throws OrcidClientException {
		log.debug("Adding external VIVO ID");
		Individual individual = findIndividual();
		ExternalId externalId = new ExternalId().setCommonName("VIVO Cornell")
				.setReference(individual.getLocalName())
				.setUrl(individual.getURI()).setVisibility(PUBLIC);

		return new AddExternalIdAction().execute(externalId,
				status.getAccessToken());
	}

	private OrcidMessage addCornellId() throws OrcidClientException {
		log.debug("Adding external Cornell ID");
		String netId = cornellNetId();
		if (netId == null) {
			log.error("Can't add a Cornell external ID when the "
					+ "logged in user does not have a netID");
		}
		Individual individual = findIndividual();
		ExternalId externalId = new ExternalId().setCommonName("Cornell NetID")
				.setReference(netId).setUrl(individual.getURI())
				.setVisibility(PUBLIC);
		return new AddExternalIdAction().execute(externalId,
				status.getAccessToken());
	}

	private ResponseValues showSuccess() {
		log.debug("Show success at adding external IDs to " + orcid);
		Map<String, Object> map = new HashMap<>();
		map.put("continueUrl", profileUrl());
		map.put("orcid", orcid);
		map.put("addVivoId", state.isAddVivoId());
		map.put("addCornellId", state.isAddCornellId());
		return new TemplateResponseValues(TEMPLATE_SUCCESS, map);
	}

}
