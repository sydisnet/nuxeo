/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Thomas Roger
 */

package org.nuxeo.ecm.platform.publisher.helper;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public class RootSectionsManager {

    public static final String SCHEMA_PUBLISHING = "publishing";

    public static final String SECTIONS_PROPERTY_NAME = "publish:sections";

    protected static final String SECTION_ROOT_DOCUMENT_TYPE = "SectionRoot";

    protected CoreSession coreSession;

    public RootSectionsManager(CoreSession coreSession) {
        this.coreSession = coreSession;
    }

    public boolean canAddSection(DocumentModel section, DocumentModel currentDocument) throws ClientException {
        if (SECTION_ROOT_DOCUMENT_TYPE.equals(section.getType())) {
            return false;
        }
        String sectionId = section.getId();
        if (currentDocument.hasSchema(SCHEMA_PUBLISHING)) {
            String[] sectionIdsArray = (String[]) currentDocument.getPropertyValue(SECTIONS_PROPERTY_NAME);

            List<String> sectionIdsList = new ArrayList<String>();

            if (sectionIdsArray != null && sectionIdsArray.length > 0) {
                sectionIdsList = Arrays.asList(sectionIdsArray);
            }

            if (sectionIdsList.contains(sectionId)) {
                return false;
            }
        }

        return true;
    }

    public String addSection(String sectionId, DocumentModel currentDocument) throws ClientException {

        if (sectionId != null && currentDocument.hasSchema(SCHEMA_PUBLISHING)) {
            String[] sectionIdsArray = (String[]) currentDocument.getPropertyValue(SECTIONS_PROPERTY_NAME);

            List<String> sectionIdsList = new ArrayList<String>();

            if (sectionIdsArray != null && sectionIdsArray.length > 0) {
                sectionIdsList = Arrays.asList(sectionIdsArray);
                // make it resizable
                sectionIdsList = new ArrayList<String>(sectionIdsList);
            }

            sectionIdsList.add(sectionId);
            String[] sectionIdsListIn = new String[sectionIdsList.size()];
            sectionIdsList.toArray(sectionIdsListIn);

            currentDocument.setPropertyValue(SECTIONS_PROPERTY_NAME, sectionIdsListIn);
            coreSession.saveDocument(currentDocument);
            coreSession.save();
        }
        return null;
    }

    public String removeSection(String sectionId, DocumentModel currentDocument) throws ClientException {

        if (sectionId != null && currentDocument.hasSchema(SCHEMA_PUBLISHING)) {
            String[] sectionIdsArray = (String[]) currentDocument.getPropertyValue(SECTIONS_PROPERTY_NAME);

            List<String> sectionIdsList = new ArrayList<String>();

            if (sectionIdsArray != null && sectionIdsArray.length > 0) {
                sectionIdsList = Arrays.asList(sectionIdsArray);
                // make it resizable
                sectionIdsList = new ArrayList<String>(sectionIdsList);
            }

            if (!sectionIdsList.isEmpty()) {
                sectionIdsList.remove(sectionId);

                String[] sectionIdsListIn = new String[sectionIdsList.size()];
                sectionIdsList.toArray(sectionIdsListIn);

                currentDocument.setPropertyValue(SECTIONS_PROPERTY_NAME, sectionIdsListIn);
                coreSession.saveDocument(currentDocument);
                coreSession.save();
            }
        }

        return null;
    }

}
