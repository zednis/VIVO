/* $This file is distributed under the terms of the license in /doc/license.txt$ */

package edu.cornell.mannlib.vivo.orcid;

import static edu.cornell.mannlib.orcidclient.actions.ApiAction.ADD_EXTERNAL_ID;
import static edu.cornell.mannlib.orcidclient.actions.ApiAction.READ_PROFILE;
import static edu.cornell.mannlib.orcidclient.orcidmessage.Visibility.PUBLIC;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.cornell.mannlib.orcidclient.OrcidClientException;
import edu.cornell.mannlib.orcidclient.actions.AddExternalIdAction;
import edu.cornell.mannlib.orcidclient.actions.ApiAction;
import edu.cornell.mannlib.orcidclient.actions.ReadProfileAction;
import edu.cornell.mannlib.orcidclient.auth.AuthorizationManager;
import edu.cornell.mannlib.orcidclient.auth.AuthorizationStatus;
import edu.cornell.mannlib.orcidclient.beans.ExternalId;
import edu.cornell.mannlib.orcidclient.context.OrcidClientContext;
import edu.cornell.mannlib.orcidclient.context.OrcidClientContext.Setting;
import edu.cornell.mannlib.orcidclient.orcidmessage.OrcidMessage;
import edu.cornell.mannlib.vedit.beans.LoginStatusBean;
import edu.cornell.mannlib.vitro.webapp.auth.identifier.IdentifierBundle;
import edu.cornell.mannlib.vitro.webapp.auth.identifier.RequestIdentifiers;
import edu.cornell.mannlib.vitro.webapp.auth.identifier.common.HasProfile;
import edu.cornell.mannlib.vitro.webapp.beans.Individual;
import edu.cornell.mannlib.vitro.webapp.beans.UserAccount;
import edu.cornell.mannlib.vitro.webapp.controller.VitroRequest;
import edu.cornell.mannlib.vitro.webapp.controller.freemarker.FreemarkerHttpServlet;
import edu.cornell.mannlib.vitro.webapp.controller.freemarker.UrlBuilder;
import edu.cornell.mannlib.vitro.webapp.controller.freemarker.responsevalues.ExceptionResponseValues;
import edu.cornell.mannlib.vitro.webapp.controller.freemarker.responsevalues.NotAuthorizedResponseValues;
import edu.cornell.mannlib.vitro.webapp.controller.freemarker.responsevalues.RedirectResponseValues;
import edu.cornell.mannlib.vitro.webapp.controller.freemarker.responsevalues.ResponseValues;
import edu.cornell.mannlib.vitro.webapp.controller.freemarker.responsevalues.TemplateResponseValues;
import edu.cornell.mannlib.vitro.webapp.dao.IndividualDao;

/**
 * TODO
 */
public class OrcidIntegrationController extends FreemarkerHttpServlet {
	private static final Log log = LogFactory
			.getLog(OrcidIntegrationController.class);

	/**
	 * Get in before FreemarkerHttpServlet for special handling.
	 */
	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException, ServletException {
		if (!isOrcidConfigured()) {
			show404NotFound(resp);
		}
		if ("/callback".equals(req.getPathInfo())) {
			new CallbackHandler(req, resp).exec();
		}
		super.doGet(req, resp);
	}

	/**
	 * Requests from
	 */
	@Override
	protected ResponseValues processRequest(VitroRequest vreq) throws Exception {
		Individual individual = figureIndividual(vreq);
		if (individual == null) {
			return show400BadRequest(vreq);
		}

		if (!isAuthorized(vreq, individual)) {
			return showNotAuthorized(vreq, individual);
		}

		switch (vreq.getPathInfo()) {
		case "/start":
			return new StartConfirmationHandler(vreq, individual).exec();
		case "/denied":
			return new DeniedHandler(vreq, individual).exec();
		case "/authorized":
			return new AuthorizedHandler(vreq, individual).exec();
		default:
			return new DefaultHandler(vreq, individual).exec();
		}
	}

	/**
	 * If the ORCID interface is configured, it should not throw an exception
	 * when asked for the value of a setting.
	 */
	private boolean isOrcidConfigured() {
		try {
			OrcidClientContext.getInstance().getSetting(Setting.CLIENT_ID);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	private Individual figureIndividual(VitroRequest vreq) {
		String individualUri = vreq.getParameter("individualUri");
		if (individualUri == null) {
			return null;
		}
		try {
			IndividualDao iDao = vreq.getWebappDaoFactory().getIndividualDao();
			return iDao.getIndividualByURI(individualUri);
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Only a self-editor is authorized.
	 */
	private boolean isAuthorized(HttpServletRequest req, Individual individual) {
		IdentifierBundle ids = RequestIdentifiers.getIdBundleForRequest(req);
		Collection<String> profileUris = HasProfile.getProfileUris(ids);
		return profileUris.contains(individual.getURI());
	}

	private void show404NotFound(HttpServletResponse resp) throws IOException {
		resp.sendError(SC_NOT_FOUND);
	}

	private ResponseValues show400BadRequest(HttpServletRequest req) {
		Map<String, Object> map = new HashMap<>();
		map.put("title", "400 Bad Request");

		String individualUri = req.getParameter("individualUri");
		if (individualUri == null) {
			map.put("errorMessage", "Individual URI not provided.");
		} else {
			map.put("errorMessage", "Individual URI not valid: '"
					+ individualUri + "'");
		}

		TemplateResponseValues rv = new TemplateResponseValues(
				"error-titled.ftl", map);
		rv.setStatusCode(SC_BAD_REQUEST);
		return rv;
	}

	private ResponseValues showNotAuthorized(HttpServletRequest req,
			Individual individual) {
		UserAccount user = LoginStatusBean.getCurrentUser(req);
		String userName = (user == null) ? "ANONYMOUS" : user.getEmailAddress();
		return new NotAuthorizedResponseValues(userName
				+ " not authorized for ORCID operations on '" + individual
				+ "'");
	}

	private static class StartConfirmationHandler {
		private final VitroRequest vreq;
		private final Individual individual;
		private final OrcidClientContext occ;
		private final AuthorizationManager auth;

		private boolean addVivoId;
		private boolean addCornellId;
		private AuthorizationStatus status;

		public StartConfirmationHandler(VitroRequest vreq, Individual individual) {
			this.vreq = vreq;
			this.individual = individual;
			this.occ = OrcidClientContext.getInstance();
			this.auth = occ.getAuthorizationManager(vreq);
		}

		public ResponseValues exec() {
			addVivoId = vreq.getParameterMap().keySet().contains("addVivoId");
			addCornellId = vreq.getParameterMap().keySet().contains("addNetId");
			if (!addVivoId && !addCornellId) {
				status = auth.getAuthorizationStatus(ApiAction.READ_PROFILE);
				if (status.isNone()) {
					return seekAuthorizationForReadProfile();
				} else if (status.isSuccess()) {
					return readProfile();
				} else {
					return showFailedAuthorization();
				}
			} else {
				status = auth.getAuthorizationStatus(ApiAction.ADD_EXTERNAL_ID);
				if (status.isNone()) {
					return seekAuthorizationForExternalIds();
				} else if (status.isSuccess()) {
					return addExternalIds();
				} else {
					return showFailedAuthorization();
				}
			}
		}

		private ResponseValues seekAuthorizationForReadProfile() {
			try {
				String returnPath = "orcid/start?individualUri="
						+ individual.getURI();
				String returnUrl = occ.resolvePathWithWebapp(returnPath);
				String seekUrl = auth
						.seekAuthorization(READ_PROFILE, returnUrl);
				return new RedirectResponseValues(seekUrl);
			} catch (OrcidClientException | URISyntaxException e) {
				return new ExceptionResponseValues(e);
			}
		}

		private ResponseValues readProfile() {
			try {
				@SuppressWarnings("unused")
				OrcidMessage message = new ReadProfileAction(occ)
						.execute(status.getAccessToken());

				String orcid = status.getAccessToken().getOrcid();

				Map<String, Object> map = new HashMap<>();
				map.put("continueUrl",
						UrlBuilder.getIndividualProfileUrl(individual, vreq));
				map.put("individualUri", individual.getURI());
				map.put("orcid", orcid);

				return new TemplateResponseValues("orcidSuccess.ftl", map);
			} catch (OrcidClientException e) {
				return new ExceptionResponseValues(e);
			}
		}

		/**
		 * @return
		 */
		private ResponseValues seekAuthorizationForExternalIds() {
			try {
				String returnPath = "orcid/start?individualUri="
						+ individual.getURI();
				if (addVivoId) {
					returnPath += "&addVivoId=true";
				}
				if (addCornellId) {
					returnPath += "&addCornellId=true";
				}
				String returnUrl = occ.resolvePathWithWebapp(returnPath);
				String seekUrl = auth.seekAuthorization(ADD_EXTERNAL_ID,
						returnUrl);
				return new RedirectResponseValues(seekUrl);
			} catch (OrcidClientException | URISyntaxException e) {
				return new ExceptionResponseValues(e);
			}
		}

		private ResponseValues addExternalIds() {
			@SuppressWarnings("unused")
			OrcidMessage message = null;
			try {
				if (addVivoId) {
					message = addVivoExternalId();
				}
				if (addCornellId) {
					message = addCornellExternalId();
				}
				
				String orcid = status.getAccessToken().getOrcid();

				Map<String, Object> map = new HashMap<>();
				map.put("continueUrl",
						UrlBuilder.getIndividualProfileUrl(individual, vreq));
				map.put("individualUri", individual.getURI());
				map.put("addVivoId", addVivoId);
				map.put("orcid", orcid);

				return new TemplateResponseValues("orcidSuccess.ftl", map);

			} catch (OrcidClientException e) {
				return new ExceptionResponseValues(e);
			}
		}

		private OrcidMessage addVivoExternalId() throws OrcidClientException {
			ExternalId externalId = new ExternalId()
					.setCommonName("VIVO Cornell").setReference(individual.getLocalName())
					.setUrl(individual.getURI()).setVisibility(PUBLIC);
			return new AddExternalIdAction().execute(externalId,
					status.getAccessToken());
		}

		private OrcidMessage addCornellExternalId() throws OrcidClientException {
			// TODO Need to fetch the externalID from the currently logged in user. If none, don't offer to add the Cornell ID in the first place. 
			ExternalId externalId = new ExternalId()
			.setCommonName("Cornell NetID").setReference(individual.getcalName())
			.setUrl(individual.getURI()).setVisibility(PUBLIC);
	return new AddExternalIdAction().execute(externalId,
			status.getAccessToken());
		}

		/**
		 * @return
		 */
		private ResponseValues showFailedAuthorization() {
			// TODO Auto-generated method stub
			throw new RuntimeException(
					"StartConfirmationHandler.showFailedAuthorization() not implemented.");
		}

	}

	private static class DeniedHandler {
		private final VitroRequest vreq;
		private final Individual individual;

		public DeniedHandler(VitroRequest vreq, Individual individual) {
			this.vreq = vreq;
			this.individual = individual;
		}

		public ResponseValues exec() {
			// TODO Auto-generated method stub
			throw new RuntimeException(
					"StartConfirmationHandler.exec() not implemented.");
		}

	}

	private static class AuthorizedHandler {
		private final VitroRequest vreq;
		private final Individual individual;

		public AuthorizedHandler(VitroRequest vreq, Individual individual) {
			this.vreq = vreq;
			this.individual = individual;
		}

		public ResponseValues exec() {
			// TODO Auto-generated method stub
			throw new RuntimeException(
					"StartConfirmationHandler.exec() not implemented.");
		}

	}

	private static class DefaultHandler {
		private final VitroRequest vreq;
		private final Individual individual;

		public DefaultHandler(VitroRequest vreq, Individual individual) {
			this.vreq = vreq;
			this.individual = individual;
		}

		public ResponseValues exec() {
			Map<String, Object> map = new HashMap<>();
			map.put("orcidControllerUrl", UrlBuilder.getUrl("/orcid/start"));
			map.put("cancelUrl",
					UrlBuilder.getIndividualProfileUrl(individual, vreq));
			map.put("individualUri", individual.getURI());
			return new TemplateResponseValues("orcidOffer.ftl", map);
		}
	}

	private static class CallbackHandler {
		private final HttpServletRequest req;
		private final HttpServletResponse resp;

		public CallbackHandler(HttpServletRequest req, HttpServletResponse resp) {
			this.req = req;
			this.resp = resp;
		}

		public void exec() throws IOException {
			OrcidClientContext occ = OrcidClientContext.getInstance();
			AuthorizationManager authManager = occ.getAuthorizationManager(req);
			try {
				AuthorizationStatus auth = authManager
						.processAuthorizationResponse();
				if (auth.isSuccess()) {
					resp.sendRedirect(auth.getSuccessUrl());
				} else {
					resp.sendRedirect(auth.getFailureUrl());
				}
			} catch (OrcidClientException e) {
				log.error("Invalid authorization response", e);
				resp.sendError(SC_INTERNAL_SERVER_ERROR);
			}
		}
	}

	/**
	 * Workflow:
	 * 
	 * <pre>
	 * Request from the "validate" link must have the individualUri.
	 * If not authorized for the individual, complain.
	 * Else, show the explain page, with individualUri in a hidden field.
	 * 
	 * On /startConfirm
	 * Request from the explain page must have individualUri. Check auth.
	 * Record flags for external IDs.
	 * If externalIDs, request scope accordingly. Otherwise, different scope.
	 * Start the dance, successUrl and failureUrl will have the individualUri in it.
	 * 
	 * On /callback, continue the dance.
	 * 
	 * On /denied, show the disappointment page, with link back to individual page.
	 * On /authorized, show the result page, with link back to individual page.
	 * </pre>
	 */

}
