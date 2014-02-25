/* $This file is distributed under the terms of the license in /doc/license.txt$ */

package edu.cornell.mannlib.vivo.orcid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;

import edu.cornell.mannlib.vitro.webapp.auth.identifier.IdentifierBundle;
import edu.cornell.mannlib.vitro.webapp.auth.identifier.RequestIdentifiers;
import edu.cornell.mannlib.vitro.webapp.auth.identifier.common.HasProfile;
import edu.cornell.mannlib.vitro.webapp.auth.identifier.common.IsRootUser;
import edu.cornell.mannlib.vitro.webapp.controller.VitroRequest;
import edu.cornell.mannlib.vitro.webapp.utils.SparqlQueryRunner;
import edu.cornell.mannlib.vitro.webapp.utils.SparqlQueryRunner.QueryParser;
import edu.cornell.mannlib.vitro.webapp.utils.dataGetter.DataGetter;

/**
 * This data getter should be assigned to the template that renders the list
 * view for ORCID IDs.
 * 
 * Find out whether the user is authorized to validate the ORCID IDs on this
 * page. Find the list of ORCID IDs, and whether each has already been
 * validated.
 * 
 * The information is stored in the values map like this:
 * 
 * <pre>
 *    orcidInfo = map {
 *        authorizedToValidate: boolean
 *        orcids: map of String to boolean [
 *            orcid: String
 *            validated: boolean
 *        ]
 *    }
 * </pre>
 */
public class OrcidIdDataGetter implements DataGetter {
	private static final Log log = LogFactory.getLog(OrcidIdDataGetter.class);

	private static final Map<String, Object> EMPTY_RESULT = Collections
			.emptyMap();
	private static final String ORCID_ID = "http://vivoweb.org/ontology/core#orcidId";
	private static final String ORCID_IS_VALIDATED = "http://vivoweb.org/ontology/core#validatedOrcidId";
	private static final String QUERY_TEMPLATE = "SELECT ?orcid ?validated \n"
			+ "WHERE { \n" //
			+ "    <%s> <%s> ?orcid . \n" //
			+ "    OPTIONAL { \n" //
			+ "       ?orcid <%s> ?validated . \n" //
			+ "       }  \n" //
			+ "}\n";

	private final VitroRequest vreq;

	public OrcidIdDataGetter(VitroRequest vreq) {
		this.vreq = vreq;
	}

	@Override
	public Map<String, Object> getData(Map<String, Object> valueMap) {
		try {
			String individualUri = findIndividualUri(valueMap);
			if (individualUri == null) {
				return EMPTY_RESULT;
			}

			boolean isAuthorizedToValidate = figureIsAuthorizedtoValidate(individualUri);
			List<OrcidInfo> orcids = runSparqlQuery(individualUri);
			return buildMap(isAuthorizedToValidate, orcids);
		} catch (Exception e) {
			log.warn("Failed to get orcID information", e);
			return EMPTY_RESULT;
		}
	}

	private String findIndividualUri(Map<String, Object> valueMap) {
		try {
			String uri = (String) valueMap.get("individualURI");

			if (uri == null) {
				log.warn("valueMap has no individualURI. Keys are: "
						+ valueMap.keySet());
				return null;
			} else {
				return uri;
			}
		} catch (Exception e) {
			log.debug("has a problem finding the individualURI", e);
			return null;
		}

	}

	/**
	 * You are authorized to validate an orcId only if you are a self-editor or
	 * root.
	 */
	private boolean figureIsAuthorizedtoValidate(String individualUri) {
		IdentifierBundle ids = RequestIdentifiers.getIdBundleForRequest(vreq);
		boolean isSelfEditor = HasProfile.getProfileUris(ids).contains(
				individualUri);
		boolean isRoot = IsRootUser.isRootUser(ids);
		return isRoot || isSelfEditor;
	}

	private List<OrcidInfo> runSparqlQuery(String individualUri) {
		String queryStr = String.format(QUERY_TEMPLATE, individualUri,
				ORCID_ID, ORCID_IS_VALIDATED);
		SparqlQueryRunner runner = new SparqlQueryRunner(vreq.getJenaOntModel());
		return runner.executeSelect(new OrcidResultParser(), queryStr);
	}

	private Map<String, Object> buildMap(boolean isAuthorizedToValidate,
			List<OrcidInfo> orcids) {
		Map<String, Boolean> validationMap = new HashMap<>();
		for (OrcidInfo oInfo: orcids) {
			validationMap.put(oInfo.getOrcid(), oInfo.isValidated());
		}
		
		Map<String, Object> orcidInfoMap = new HashMap<>();
		orcidInfoMap.put("authorizedToValidate", isAuthorizedToValidate);
		orcidInfoMap.put("orcids", validationMap);

		Map<String, Object> map = new HashMap<>();
		map.put("orcidInfo", orcidInfoMap);

		log.debug("Returning these values:" + map);
		return map;
	}

	// ----------------------------------------------------------------------
	// Helper classes
	// ----------------------------------------------------------------------

	/**
	 * Parse the results of the SPARQL query.
	 */
	private static class OrcidResultParser extends QueryParser<List<OrcidInfo>> {
		@Override
		protected List<OrcidInfo> defaultValue() {
			return Collections.emptyList();
		}

		@Override
		protected List<OrcidInfo> parseResults(String queryStr,
				ResultSet results) {
			List<OrcidInfo> orcids = new ArrayList<>();

			while (results.hasNext()) {
				try {
					QuerySolution solution = results.next();
					Resource orcid = solution.getResource("orcid");
					RDFNode vNode = solution.get("validated");
					log.debug("Result is orcid=" + orcid + ", validated="
							+ vNode);

					if (orcid != null && orcid.isURIResource()) {
						boolean validated = (vNode != null);
						orcids.add(new OrcidInfo(orcid.getURI(), validated));
					}
				} catch (Exception e) {
					log.warn("Failed to parse the query result: " + queryStr, e);
				}
			}

			return orcids;
		}
	}

	/**
	 * A bean to hold info for each ORCID.
	 */
	static class OrcidInfo {
		private final String orcid;
		private final boolean validated;

		public OrcidInfo(String orcid, boolean validated) {
			this.orcid = orcid;
			this.validated = validated;
		}

		public String getOrcid() {
			return orcid;
		}

		public boolean isValidated() {
			return validated;
		}

		@Override
		public String toString() {
			return "OrcidInfo[orcid=" + orcid + ", validated=" + validated
					+ "]";
		}

	}

}
