/* $This file is distributed under the terms of the license in /doc/license.txt$ */

package edu.cornell.mannlib.vivo.orcid.controller;

import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.cornell.mannlib.orcidclient.context.OrcidClientContext;
import edu.cornell.mannlib.orcidclient.context.OrcidClientContext.Setting;
import edu.cornell.mannlib.vitro.webapp.controller.VitroRequest;
import edu.cornell.mannlib.vitro.webapp.controller.freemarker.FreemarkerHttpServlet;
import edu.cornell.mannlib.vitro.webapp.controller.freemarker.responsevalues.ExceptionResponseValues;
import edu.cornell.mannlib.vitro.webapp.controller.freemarker.responsevalues.ResponseValues;

/**
 * TODO
 */
public class OrcidIntegrationController extends FreemarkerHttpServlet {
	private static final Log log = LogFactory
			.getLog(OrcidIntegrationController.class);

	private final static String PATHINFO_START_CONFIRMATION = "/start";
	private final static String PATHINFO_CALLBACK = "/callback";
	private final static String PATHINFO_READ_PROFILE = "/readProfile";
	private final static String PATHINFO_ADD_EXTERNAL_IDS = "/addExternalIds";

	final static String PATH_DEFAULT = "/orcid";
	final static String PATH_START_CONFIRMATION = path(PATHINFO_START_CONFIRMATION);
	final static String PATH_READ_PROFILE = path(PATHINFO_READ_PROFILE);
	final static String PATH_ADD_EXTERNAL_IDS = path(PATHINFO_ADD_EXTERNAL_IDS);

	static String path(String pathInfo) {
		return PATH_DEFAULT + pathInfo;
	}

	final static String TEMPLATE_OFFER = "orcidOffer.ftl";
	final static String TEMPLATE_DENIED = "orcidDenied.ftl";
	final static String TEMPLATE_FAILED = "orcidFailed.ftl";
	final static String TEMPLATE_SUCCESS = "orcidSuccess.ftl";

	/**
	 * Get in before FreemarkerHttpServlet for special handling.
	 */
	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException, ServletException {
		if (!isOrcidConfigured()) {
			show404NotFound(resp);
		}
		if (PATHINFO_CALLBACK.equals(req.getPathInfo())) {
			new OrcidCallbackHandler(req, resp).exec();
		}
		super.doGet(req, resp);
	}

	/**
	 * Look at the path info and delegate to a handler.
	 */
	@Override
	protected ResponseValues processRequest(VitroRequest vreq) throws Exception {
		try {
			String pathInfo = vreq.getPathInfo();
			log.debug("Path info: "+ pathInfo);
			switch (pathInfo) {
			case PATHINFO_START_CONFIRMATION:
				return new OrcidStartConfirmationHandler(vreq).exec();
			case PATHINFO_READ_PROFILE:
				return new OrcidReadProfileHandler(vreq).exec();
			case PATHINFO_ADD_EXTERNAL_IDS:
				return new OrcidAddExternalIdsHandler(vreq).exec();
			default:
				return new OrcidDefaultHandler(vreq).exec();
			}
		} catch (Exception e) {
			return new ExceptionResponseValues(e);
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

	private void show404NotFound(HttpServletResponse resp) throws IOException {
		resp.sendError(SC_NOT_FOUND);
	}
}
