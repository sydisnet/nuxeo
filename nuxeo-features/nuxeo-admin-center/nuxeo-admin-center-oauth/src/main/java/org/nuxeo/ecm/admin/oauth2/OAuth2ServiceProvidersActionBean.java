/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *      Nelson Silva
 */
package org.nuxeo.ecm.admin.oauth2;

import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.admin.oauth.DirectoryBasedEditor;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.oauth2.providers.NuxeoOAuth2ServiceProvider;
import org.nuxeo.ecm.platform.oauth2.providers.OAuth2ServiceProvider;
import org.nuxeo.ecm.platform.oauth2.providers.OAuth2ServiceProviderRegistry;
import org.nuxeo.ecm.platform.oauth2.providers.OAuth2ServiceProviderRegistryImpl;
import org.nuxeo.runtime.api.Framework;

import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.AuthorizationCodeRequestUrl;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson.JacksonFactory;

@Name("oauth2ServiceProvidersActions")
@Scope(ScopeType.CONVERSATION)
public class OAuth2ServiceProvidersActionBean extends DirectoryBasedEditor {

    private static final long serialVersionUID = 1L;

    protected static final String DIRECTORY = OAuth2ServiceProviderRegistryImpl.DIRECTORY_NAME;

    protected static final String SCHEMA = NuxeoOAuth2ServiceProvider.SCHEMA;

    @Override
    protected String getDirectoryName() {
        return DIRECTORY;
    }

    @Override
    protected String getSchemaName() {
        return SCHEMA;
    }

    public String getAuthorizationURL(String provider) throws ClientException {

        String url;

        DirectoryService ds = Framework.getService(DirectoryService.class);
        Session session = ds.open(getDirectoryName());
        try {
            OAuth2ServiceProviderRegistry oauth2ProviderRegistry = Framework.getLocalService(OAuth2ServiceProviderRegistry.class);
            OAuth2ServiceProvider serviceProvider = oauth2ProviderRegistry.getProvider(provider);

            HttpTransport HTTP_TRANSPORT = new NetHttpTransport();

            JsonFactory JSON_FACTORY = new JacksonFactory();

            AuthorizationCodeFlow flow = serviceProvider.getAuthorizationCodeFlow(HTTP_TRANSPORT, JSON_FACTORY);

            HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
            String redirectUrl = serviceProvider.getCallbackURL(request);

            // redirect to the authorization flow
            AuthorizationCodeRequestUrl authorizationUrl = flow.newAuthorizationUrl();
            authorizationUrl.setRedirectUri(redirectUrl);

            url = authorizationUrl.build();

        } finally {
            session.close();
        }

        return url;
    }
}
