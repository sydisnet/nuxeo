/*
 * (C) Copyright 2006-2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nelson Silva <nelson.silva@inevo.pt> - initial API and implementation
 *     Nuxeo
 */
package org.nuxeo.ecm.platform.oauth2.providers;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson.JacksonFactory;
import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.core.api.ClientException;

import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.ClientParametersAuthentication;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpExecuteInterceptor;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import org.nuxeo.ecm.platform.oauth2.tokens.NuxeoOAuth2Token;
import org.nuxeo.ecm.platform.oauth2.tokens.OAuth2TokenStore;
import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;

import javax.servlet.http.HttpServletRequest;

public class NuxeoOAuth2ServiceProvider implements OAuth2ServiceProvider,
    AuthorizationCodeFlow.CredentialCreatedListener {

    public static final String SCHEMA = "oauth2ServiceProvider";

    /** Global instance of the HTTP transport. */
    private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();

    /** Global instance of the JSON factory. */
    private static final JsonFactory JSON_FACTORY = new JacksonFactory();

    public static final String CODE_URL_PARAMETER = "code";

    public static final String ERROR_URL_PARAMETER = "error";

    protected String serviceName;

    protected Long id;

    private String tokenServerURL;

    private String authorizationServerURL;

    private String clientId;

    private String clientSecret;

    private List<String> scopes;

    private boolean enabled;

    public String getCallbackURL(HttpServletRequest request) {
        String serverURL = VirtualHostHelper.getBaseURL(request);

        if (serverURL.endsWith("/")) {
            serverURL = serverURL.substring(0, serverURL.length() - 1);
        }

        return serverURL + "/site/oauth2/" + serviceName + "/callback";
    }
    
    public Credential handleAuthorizationCallback(String userId, HttpServletRequest request) throws ClientException {

        // Checking if there was an error such as the user denied access
        String error = getError(request);
        if (error != null) {
            throw new ClientException("There was an error: \"" + error + "\".");
        }

        // Checking conditions on the "code" URL parameter
        String code = getAuthorizationCode(request);
        if (code == null) {
            throw new ClientException("There is not code provided as QueryParam.");
        }

        try {
            AuthorizationCodeFlow flow = getAuthorizationCodeFlow(HTTP_TRANSPORT, JSON_FACTORY);

            String redirectUri = getCallbackURL(request);

            TokenResponse tokenResponse = flow.newTokenRequest(code).setRedirectUri(redirectUri).execute();

            return flow.createAndStoreCredential(tokenResponse, userId);

        } catch (IOException e) {
            throw new ClientException("Failed to retrieve credential", e);
        }

    }

    public AuthorizationCodeFlow getAuthorizationCodeFlow(HttpTransport transport, JsonFactory jsonFactory) {
        return getAuthorizationCodeFlow(transport, jsonFactory, NuxeoOAuth2Token.KEY_NUXEO_LOGIN);
    }


    public AuthorizationCodeFlow getAuthorizationCodeFlow(HttpTransport transport, JsonFactory jsonFactory, String key) {

        Credential.AccessMethod method = BearerToken.authorizationHeaderAccessMethod();
        GenericUrl tokenServerUrl = new GenericUrl(tokenServerURL);
        HttpExecuteInterceptor clientAuthentication = new ClientParametersAuthentication(clientId, clientSecret);
        String authorizationServerUrl = authorizationServerURL;

        AuthorizationCodeFlow flow = new AuthorizationCodeFlow.Builder(method, transport, jsonFactory, tokenServerUrl,
                clientAuthentication, clientId, authorizationServerUrl)
                .setScopes(scopes)
                .setCredentialDataStore(getCredentialDataStore(key))
                .build();
        return flow;
    }

    public OAuth2TokenStore getCredentialDataStore(String key) {
        return new OAuth2TokenStore(serviceName, key);
    }

    protected void onTokenResponse(TokenResponse tokenResponse) {

    }

    protected String getError(HttpServletRequest request) {
        String error = request.getParameter(ERROR_URL_PARAMETER);
        return StringUtils.isBlank(error) ? null : error;
    }

    // Checking conditions on the "code" URL parameter
    protected String getAuthorizationCode(HttpServletRequest request) {
        String code = request.getParameter(CODE_URL_PARAMETER);
        return StringUtils.isBlank(code) ? null : code;
    }
    
    @Override
    public String getServiceName() {
        return serviceName;
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public String getTokenServerURL() {
        return tokenServerURL;
    }

    @Override
    public String getClientId() {
        return clientId;
    }

    @Override
    public String getClientSecret() {
        return clientSecret;
    }

    @Override
    public List<String> getScopes() {
        return scopes;
    }

    @Override
    public String getAuthorizationServerURL() {
        return authorizationServerURL;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public void setTokenServerURL(String tokenServerURL) {
        this.tokenServerURL = tokenServerURL;
    }

    @Override
    public void setAuthorizationServerURL(String authorizationServerURL) {
        this.authorizationServerURL = authorizationServerURL;
    }

    @Override
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    @Override
    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public void setScopes(String... scopes) {
        this.scopes = Arrays.asList(scopes);
    }

    @Override
    public void onCredentialCreated(Credential credential, TokenResponse tokenResponse) throws IOException {

    }
}
