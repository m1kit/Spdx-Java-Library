/**
 * Copyright (c) 2019 Source Auditor Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 * 
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package org.spdx.library.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;

import org.spdx.library.DefaultModelStore;
import org.spdx.library.InvalidSPDXAnalysisException;
import org.spdx.library.SpdxConstants;
import org.spdx.storage.IModelStore;


/**
 * This is an SPDX element which is in an external document.  The ID must be in the form SpdxConstants.EXTERNAL_ELEMENT_REF_PATTERN.pattern()
 * 
 * @author Gary O'Neall
 */
public class ExternalSpdxElement extends SpdxElement implements IndividualUriValue {
	
	// Note: the default empty constructor is not allowed since the element ID must follow a specific pattern

	/**
	 * @param id
	 * @throws InvalidSPDXAnalysisException
	 */
	public ExternalSpdxElement(String id) throws InvalidSPDXAnalysisException {
		this(DefaultModelStore.getDefaultModelStore(), DefaultModelStore.getDefaultDocumentUri(), id, true);
	}

	/**
	 * @param modelStore
	 * @param documentUri
	 * @param id
	 * @param create
	 * @throws InvalidSPDXAnalysisException
	 */
	public ExternalSpdxElement(IModelStore modelStore, String documentUri, String id, boolean create)
			throws InvalidSPDXAnalysisException {
		super(modelStore, documentUri, id, create);
		if (!SpdxConstants.EXTERNAL_ELEMENT_REF_PATTERN.matcher(id).matches()) {
			throw(new InvalidSPDXAnalysisException("Invalid id format for an external document reference.  Must be of the form ExternalSPDXRef:SPDXID"));
		}
		getExternalSpdxElementURI();	//this will check to make sure the external document reference is available
	}
	
	/**
	 * @return external document ID for the external reference
	 * @throws InvalidSPDXAnalysisException
	 */
	public String getExternalDocumentId() throws InvalidSPDXAnalysisException {
		Matcher matcher = SpdxConstants.EXTERNAL_ELEMENT_REF_PATTERN.matcher(this.getId());
		if (!matcher.matches()) {
			throw(new InvalidSPDXAnalysisException("Invalid id format for an external document reference.  Must be of the form ExternalSPDXRef:SPDXID"));
		}
		return matcher.group(1);
	}
	
	/**
	 * @return element ID used in the external document
	 * @throws InvalidSPDXAnalysisException
	 */
	public String getExternalElementId() throws InvalidSPDXAnalysisException {
		Matcher matcher = SpdxConstants.EXTERNAL_ELEMENT_REF_PATTERN.matcher(this.getId());
		if (!matcher.matches()) {
			throw(new InvalidSPDXAnalysisException("Invalid id format for an external document reference.  Must be of the form ExternalSPDXRef:SPDXID"));
		}
		return matcher.group(2);
	}

	/* (non-Javadoc)
	 * @see org.spdx.library.model.ModelObject#getType()
	 */
	@Override
	public String getType() {
		return SpdxConstants.CLASS_EXTERNAL_SPDX_ELEMENT;
	}
	
	@Override
	public List<String> verify() {
		// we don't want to call super.verify since we really don't require those fields
		List<String> retval = new ArrayList<>();
		String id = this.getId();
		Matcher matcher = SpdxConstants.EXTERNAL_ELEMENT_REF_PATTERN.matcher(id);
		if (!matcher.matches()) {				
			retval.add("Invalid id format for an external document reference.  Must be of the form "+SpdxConstants.EXTERNAL_ELEMENT_REF_PATTERN.pattern());
		} else {
			try {
				externalDocumentIdToNamespace(matcher.group(1), getModelStore(), getDocumentUri());
			} catch (InvalidSPDXAnalysisException e) {
				retval.add(e.getMessage());
			}
		}
		return retval;
	}

	public String getExternalSpdxElementURI() throws InvalidSPDXAnalysisException {
		return externalSpdxElementIdToURI(getId(), getModelStore(), getDocumentUri());
	}
	
	public static String externalSpdxElementIdToURI(String externalSpdxElementId,
			IModelStore stModelStore, String stDocumentUri) throws InvalidSPDXAnalysisException {
		Matcher matcher = SpdxConstants.EXTERNAL_ELEMENT_REF_PATTERN.matcher(externalSpdxElementId);
		if (!matcher.matches()) {
			logger.error("Invalid id format for an external document reference.  Must be of the form ExternalSPDXRef:SPDXID");
			throw new InvalidSPDXAnalysisException("Invalid id format for an external document reference.  Must be of the form ExternalSPDXRef:SPDXID");
		}
		String externalDocumentUri;
		externalDocumentUri = externalDocumentIdToNamespace(matcher.group(1), stModelStore, stDocumentUri);
		if (externalDocumentUri.endsWith("#")) {
			return externalDocumentUri + matcher.group(2);
		} else {
			return externalDocumentUri + "#" + matcher.group(2);
		}
	}
	
	/**
	 * Convert a URI to an ID for an External SPDX Element
	 * @param uri URI with the external document namespace and the external SPDX ref in the form namespace#SPDXRef-XXX
	 * @param stModelStore
	 * @param stDocumentUri
	 * @param createExternalDocRef if true, create the external doc ref if it is not already in the ModelStore
	 * @return external SPDX element ID in the form DocumentRef-XX:SPDXRef-YY
	 * @throws InvalidSPDXAnalysisException
	 */
	public static String uriToExternalSpdxElementId(String uri,
			IModelStore stModelStore, String stDocumentUri, boolean createExternalDocRef) throws InvalidSPDXAnalysisException {
		Objects.requireNonNull(uri);
		Matcher matcher = SpdxConstants.EXTERNAL_SPDX_ELEMENT_URI_PATTERN.matcher(uri);
		if (!matcher.matches()) {
			throw new InvalidSPDXAnalysisException("Invalid URI format: "+uri+".  Expects namespace#SPDXRef-XXX");
		}
		Optional<ExternalDocumentRef> externalDocRef = ExternalDocumentRef.getExternalDocRefByDocNamespace(stModelStore, stDocumentUri, 
				matcher.group(1), createExternalDocRef);
		if (!externalDocRef.isPresent()) {
			logger.error("Could not find or create the external document reference for document namespace "+ matcher.group(1));
			throw new InvalidSPDXAnalysisException("Could not find or create the external document reference for document namespace "+ matcher.group(1));
		}
		return externalDocRef.get().getId() + ":" + matcher.group(2);
	}
	
	/**
	 * Create an ExternalSpdxElement based on a URI of the form externaldocumentnamespace#SPDXRef-XXX
	 * @param uri RI of the form externaldocumentnamespace#SPDXRef-XXX
	 * @param stModelStore
	 * @param stDocumentUri
	 * @return ExternalSpdxRef
	 * @param createExternalDocRef if true, create the external doc ref if it is not already in the ModelStore
	 * @throws InvalidSPDXAnalysisException
	 */
	public static ExternalSpdxElement uriToExternalSpdxElement(String uri,
			IModelStore stModelStore, String stDocumentUri, boolean createExternalDocRef) throws InvalidSPDXAnalysisException {
		return new ExternalSpdxElement(stModelStore, stDocumentUri, uriToExternalSpdxElementId(
				uri, stModelStore, stDocumentUri, createExternalDocRef), true);
	}
	
	private static String externalDocumentIdToNamespace(String externalDocumentId,
			IModelStore stModelStore, String stDocumentUri) throws InvalidSPDXAnalysisException {
		Optional<Object> retval = getObjectPropertyValue(stModelStore, stDocumentUri, 
				externalDocumentId, SpdxConstants.PROP_EXTERNAL_SPDX_DOCUMENT);
		if (!retval.isPresent()) {
			throw(new InvalidSPDXAnalysisException("No external document reference exists for document ID "+externalDocumentId));
		}
		if (!(retval.get() instanceof IndividualUriValue)) {
			logger.error("Invalid type returned for external document.  Expected IndividualValue, actual "+retval.get().getClass().toString());
			throw new InvalidSPDXAnalysisException("Invalid type returned for external document.");
		}
		return ((IndividualUriValue)retval.get()).getIndividualURI();
	}
	
	@Override
	public boolean equivalent(ModelObject compare) {
		if (!(compare instanceof ExternalSpdxElement)) {
			return false;
		}
		return getId().equals(compare.getId());
	}

	@Override
	public String getIndividualURI() {
		try {
			return getExternalSpdxElementURI();
		} catch (InvalidSPDXAnalysisException e) {
			logger.error("Error getting external SPDX element URI",e);
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public boolean addAnnotation(Annotation annotation) throws InvalidSPDXAnalysisException {
		throw new InvalidSPDXAnalysisException("Can not add annotations to an ExternalSpdxElement.  "
				+ "These changes must be done to the local SPDX element in the document which defines the SPDX element.");
	}
	
	@Override
	public boolean addRelationship(Relationship relationship) throws InvalidSPDXAnalysisException {
		throw new InvalidSPDXAnalysisException("Can not add relationshps to an ExternalSpdxElement.  "
				+ "These changes must be done to the local SPDX element in the document which defines the SPDX element.");
	}
	
	@Override
	public void setComment(String comment) throws InvalidSPDXAnalysisException {
		throw new InvalidSPDXAnalysisException("Can not set comment on an ExternalSpdxElement.  "
				+ "These changes must be done to the local SPDX element in the document which defines the SPDX element.");
	}
	
	@Override
	public void setName(String name) throws InvalidSPDXAnalysisException {
		throw new InvalidSPDXAnalysisException("Can not set the name on an ExternalSpdxElement.  "
				+ "These changes must be done to the local SPDX element in the document which defines the SPDX element.");
	}
}