/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.picketlink.http.internal;

import org.picketlink.Identity;
import org.picketlink.annotations.PicketLink;
import org.picketlink.authentication.AuthenticationException;
import org.picketlink.authorization.util.AuthorizationUtil;
import org.picketlink.common.reflection.Reflections;
import org.picketlink.config.SecurityConfiguration;
import org.picketlink.config.SecurityConfigurationBuilder;
import org.picketlink.config.http.AuthenticationConfiguration;
import org.picketlink.config.http.AuthenticationSchemeConfiguration;
import org.picketlink.config.http.AuthorizationConfiguration;
import org.picketlink.config.http.BasicAuthenticationConfiguration;
import org.picketlink.config.http.DigestAuthenticationConfiguration;
import org.picketlink.config.http.FormAuthenticationConfiguration;
import org.picketlink.config.http.HttpSecurityConfiguration;
import org.picketlink.config.http.HttpSecurityConfigurationException;
import org.picketlink.config.http.InboundConfiguration;
import org.picketlink.config.http.LogoutConfiguration;
import org.picketlink.config.http.OutboundConfiguration;
import org.picketlink.config.http.PathConfiguration;
import org.picketlink.config.http.TokenAuthenticationConfiguration;
import org.picketlink.config.http.X509AuthenticationConfiguration;
import org.picketlink.credential.DefaultLoginCredentials;
import org.picketlink.extension.PicketLinkExtension;
import org.picketlink.http.internal.schemes.BasicAuthenticationScheme;
import org.picketlink.http.internal.schemes.DigestAuthenticationScheme;
import org.picketlink.http.internal.schemes.FormAuthenticationScheme;
import org.picketlink.http.internal.schemes.TokenAuthenticationScheme;
import org.picketlink.http.internal.schemes.X509AuthenticationScheme;
import org.picketlink.idm.PartitionManager;
import org.picketlink.idm.model.Account;
import org.picketlink.internal.el.ELProcessor;
import org.picketlink.web.authentication.HttpAuthenticationScheme;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.picketlink.log.BaseLog.AUTHENTICATION_LOGGER;

/**
 * @author Pedro Igor
 */
public class SecurityFilter implements Filter {

    public static final String AUTHENTICATION_ORIGINAL_PATH = SecurityFilter.class.getName() + ".authc.original.path";

    @Inject
    private PicketLinkExtension picketLinkExtension;

    @Inject
    private Instance<PartitionManager> partitionManager;

    @Inject
    private Instance<Identity> identityInstance;

    @Inject
    private Instance<DefaultLoginCredentials> credentialsInstance;

    @Inject
    @Any
    private Instance<HttpAuthenticationScheme> authenticationSchemesInstances;

    @Inject
    @PicketLink
    private Instance<HttpServletRequest> picketLinkHttpServletRequest;

    @Inject
    private ELProcessor elProcessor;

    private HttpSecurityConfiguration configuration;
    private Map<PathConfiguration, HttpAuthenticationScheme> authenticationSchemes = new HashMap<PathConfiguration, HttpAuthenticationScheme>();
    private PathMatcher pathMatcher;

    @Override
    public void init(FilterConfig config) throws ServletException {
        SecurityConfigurationBuilder configurationBuilder = this.picketLinkExtension.getSecurityConfigurationBuilder();
        SecurityConfiguration securityConfiguration = configurationBuilder.build();

        this.configuration = securityConfiguration.getHttpSecurityConfiguration();

        if (this.configuration == null) {
            throw new HttpSecurityConfigurationException("No configuration provided.");
        }

        for (List<PathConfiguration> configurations : this.configuration.getPaths().values()) {
            for (PathConfiguration pathConfiguration : configurations) {
                if (pathConfiguration.isSecured()) {
                    HttpAuthenticationScheme authenticationScheme = getAuthenticationScheme(pathConfiguration, null);

                    if (authenticationScheme != null) {
                        InboundConfiguration inboundConfig = pathConfiguration.getInboundConfiguration();
                        AuthenticationConfiguration authcConfig = inboundConfig.getAuthenticationConfiguration();

                        try {
                            authenticationScheme.initialize(authcConfig.getAuthenticationSchemeConfiguration());
                        } catch (Exception e) {
                            throw new HttpSecurityConfigurationException("Could not initialize Http Authentication Scheme [" + authenticationScheme + "].", e);
                        }
                    }
                }
            }
        }

        this.pathMatcher = getPathMatcher();
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain) throws IOException,
        ServletException {
        if (!HttpServletRequest.class.isInstance(servletRequest)) {
            throw new ServletException("This filter can only process HttpServletRequest requests.");
        }

        HttpServletRequest request = this.picketLinkHttpServletRequest.get();

        if (AUTHENTICATION_LOGGER.isDebugEnabled()) {
            AUTHENTICATION_LOGGER.debugf("Processing request to URI [%s].", request.getRequestURI());
        }

        HttpServletResponse response = (HttpServletResponse) servletResponse;
        Identity identity = getIdentity();
        PathConfiguration pathConfiguration = null;

        try {
            pathConfiguration = resolvePathConfiguration(request);

            if (pathConfiguration != null) {
                Set<String> methods = pathConfiguration.getInboundConfiguration().getMethods();

                if (!methods.contains(request.getMethod())) {
                    response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
                    return;
                }

                if (!pathConfiguration.isSecured()) {
                    chain.doFilter(request, response);
                    return;
                }
            }

            if (!identity.isLoggedIn()) {
                performAuthenticationIfRequired(pathConfiguration, request, response);
            }

            if (pathConfiguration != null) {
                if (!response.isCommitted()) {
                    if (identity.isLoggedIn()) {
                        boolean authorized = performAuthorization(pathConfiguration, request, response);

                        if (authorized) {
                            processRequest(pathConfiguration, request, response, chain);
                        }
                    }
                }
            }

            if (isLogoutPath(pathConfiguration)) {
                performLogout(request, response, identity, pathConfiguration);
            } else {
                if (!identity.isLoggedIn()) {
                    DefaultLoginCredentials creds = getCredentials();

                    if (pathConfiguration != null || creds.getCredential() != null) {
                        challengeClientForCredentials(pathConfiguration, request, response);

                        if (!response.isCommitted()) {
                            chain.doFilter(request, response);
                        }
                    }
                }
            }
        } catch (Exception e) {
            if (pathConfiguration != null) {
                throw new RuntimeException("Could not process URI [" + pathConfiguration.getUri() + "].", e);
            }

            throw new RuntimeException("Unexpected error while processing requested URI [" + request.getRequestURI() + "].", e);
        } finally {
            performOutboundProcessing(pathConfiguration, request, response, chain);
        }
    }

    private void performOutboundProcessing(PathConfiguration pathConfiguration, HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (pathConfiguration != null && pathConfiguration.isSecured()) {
            String redirectUrl = null;
            OutboundConfiguration outboundConfiguration = pathConfiguration.getOutboundConfiguration();

            if (outboundConfiguration != null) {
                redirectUrl = outboundConfiguration.getRedirectUrl();
            }

            if (redirectUrl == null && isLogoutPath(pathConfiguration)) {
                redirectUrl = request.getContextPath();
            }

            if (redirectUrl != null) {
                if (redirectUrl.startsWith("/")) {
                    if (!redirectUrl.startsWith(request.getContextPath())) {
                        redirectUrl = request.getContextPath() + redirectUrl;
                    }
                }

                if (!response.isCommitted()) {
                    response.sendRedirect(redirectUrl);
                }
            }
        } else {
            if (!response.isCommitted()) {
                if (this.configuration.isPermissive()) {
                    chain.doFilter(request, response);
                }
            }
        }
    }

    private void performLogout(HttpServletRequest request, HttpServletResponse response, Identity identity, PathConfiguration pathConfiguration) throws IOException {
        if (identity.isLoggedIn()) {
            identity.logout();
        }
    }

    private boolean isLogoutPath(PathConfiguration pathConfiguration) {
        if (pathConfiguration != null) {
            LogoutConfiguration logoutConfiguration = pathConfiguration.getInboundConfiguration()
                .getLogoutConfiguration();

            return logoutConfiguration != null;
        }

        return false;
    }

    private boolean performAuthorization(PathConfiguration pathConfiguration, HttpServletRequest request, HttpServletResponse response) {
        InboundConfiguration inboundConfig = pathConfiguration.getInboundConfiguration();
        AuthorizationConfiguration authorizationConfiguration = inboundConfig.getAuthorizationConfiguration();

        if (authorizationConfiguration == null) {
            return true;
        }

        Identity identity = getIdentity();
        String[] allowedRoles = authorizationConfiguration.getAllowedRoles();
        boolean isAuthorized = true;

        if (allowedRoles != null) {
            for (String roneName: allowedRoles) {
                if (!AuthorizationUtil.hasRole(identity, this.partitionManager.get(), roneName)) {
                    isAuthorized = false;
                    break;
                }
            }
        }

        String[] allowedGroups = authorizationConfiguration.getAllowedGroups();

        if (allowedGroups != null) {
            for (String groupName : allowedGroups) {
                if (!AuthorizationUtil.isMember(identity, this.partitionManager.get(), groupName)) {
                    isAuthorized = false;
                    break;
                }
            }
        }

        String[] allowedRealms = authorizationConfiguration.getAllowedRealms();

        if (allowedRealms != null) {
            for (String realmName : allowedRealms) {
                Account validatedAccount = identity.getAccount();

                if (!validatedAccount.getPartition().getName().equals(realmName)) {
                    try {
                        Class<Object> partitionType = Reflections.classForName(realmName);

                        isAuthorized = AuthorizationUtil.hasPartition(identity, partitionType, null);
                    } catch (Exception ignore) {
                    }

                    if (isAuthorized) {
                        break;
                    }

                    isAuthorized = false;
                }
            }
        }

        String[] expressions = authorizationConfiguration.getExpressions();

        if (expressions != null) {
            for (String expression : expressions) {
                try {
                    Object eval = this.elProcessor.eval(expression);

                    if (eval == null || !Boolean.class.isInstance(eval)) {
                        throw new RuntimeException("Authorization expressions must evaluate to a boolean.");
                    }

                    if (!Boolean.valueOf(eval.toString())) {
                        isAuthorized = false;
                        break;
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Failed to process authorization expression [" + expression + "] for URI [" + pathConfiguration
                        .getUri() + "].", e);
                }
            }
        }

        if (!isAuthorized) {
            try {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
            } catch (IOException e) {
                throw new RuntimeException("Could not set forbidden status.", e);
            }
        }

        return isAuthorized;
    }

    private void processRequest(PathConfiguration pathConfiguration, HttpServletRequest request, HttpServletResponse response, FilterChain chain) {
        try {
            if (!isLogoutPath(pathConfiguration)) {
                chain.doFilter(request, response);
            }
        } catch (Exception e) {
            throw new RuntimeException("Could not process request.", e);
        }
    }

    private void challengeClientForCredentials(PathConfiguration pathConfiguration, HttpServletRequest request, HttpServletResponse response) {
        HttpAuthenticationScheme authenticationScheme = getAuthenticationScheme(pathConfiguration, request);

        if (authenticationScheme != null) {
            if (AUTHENTICATION_LOGGER.isDebugEnabled()) {
                AUTHENTICATION_LOGGER
                    .debugf("Challenging client using authentication scheme [%s].", authenticationScheme);
            }

            HttpSession session = request.getSession(false);
            PathConfiguration authenticationOriginalPath = null;

            if (session != null) {
                authenticationOriginalPath = (PathConfiguration) session.getAttribute(AUTHENTICATION_ORIGINAL_PATH);
            }

            try {
                authenticationScheme.challengeClient(request, response);
            } catch (Exception e) {
                throw new RuntimeException("Could not challenge client for credentials.", e);
            }

            if (authenticationOriginalPath == null || !authenticationOriginalPath.equals(pathConfiguration)) {
                session.setAttribute(AUTHENTICATION_ORIGINAL_PATH, pathConfiguration);
            }
        }
    }

    private void performAuthenticationIfRequired(PathConfiguration pathConfiguration, HttpServletRequest request, HttpServletResponse response) throws IOException {
        DefaultLoginCredentials creds = getCredentials();
        Identity identity = getIdentity();
        HttpAuthenticationScheme authenticationScheme = getAuthenticationScheme(pathConfiguration, request);

        if (authenticationScheme != null) {
            creds = extractCredentials(request, authenticationScheme);

            if (AUTHENTICATION_LOGGER.isDebugEnabled()) {
                AUTHENTICATION_LOGGER.debugf("Credentials extracted from request [%s]", creds.getCredential());
            }

            if (creds.getCredential() != null) {
                if (AUTHENTICATION_LOGGER.isDebugEnabled()) {
                    AUTHENTICATION_LOGGER
                        .debugf("Forcing re-authentication. Logging out current user [%s]", identity.getAccount());
                }

                if (identity.isLoggedIn()) {
                    identity.logout();
                }

                creds = extractCredentials(request, authenticationScheme);
            }

            if (creds.getCredential() != null) {
                try {
                    if (AUTHENTICATION_LOGGER.isDebugEnabled()) {
                        AUTHENTICATION_LOGGER.debugf("Authenticating using credentials [%s]", creds.getCredential());
                    }

                    identity.login();

                    authenticationScheme.onPostAuthentication(request, response);
                } catch (AuthenticationException ae) {
                    AUTHENTICATION_LOGGER.authenticationFailed(creds.getUserId(), ae);
                }
            }
        } else {
            if (pathConfiguration != null) {
                if (pathConfiguration.getInboundConfiguration().getAuthorizationConfiguration() != null) {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "This resource [" + pathConfiguration.getUri() + "] requires authentication.");
                }
            }
        }
    }

    private PathConfiguration resolvePathConfiguration(HttpServletRequest request) {
        return getPathMatcher().matches(request);
    }

    private HttpAuthenticationScheme getAuthenticationScheme(PathConfiguration pathConfiguration, HttpServletRequest request) {
        HttpAuthenticationScheme authenticationScheme = null;

        if (pathConfiguration != null) {
            InboundConfiguration inboundConfig = pathConfiguration.getInboundConfiguration();
            AuthenticationConfiguration authcConfiguration = inboundConfig.getAuthenticationConfiguration();

            if (authcConfiguration != null) {
                AuthenticationSchemeConfiguration authSchemeConfiguration = authcConfiguration
                    .getAuthenticationSchemeConfiguration();

                authenticationScheme = this.authenticationSchemes.get(pathConfiguration);

                if (authenticationScheme == null) {
                    if (FormAuthenticationConfiguration.class.isInstance(authSchemeConfiguration)) {
                        authenticationScheme = resolveAuthenticationScheme(FormAuthenticationScheme.class);
                    } else if (DigestAuthenticationConfiguration.class.isInstance(authSchemeConfiguration)) {
                        authenticationScheme = resolveAuthenticationScheme(DigestAuthenticationScheme.class);
                    } else if (BasicAuthenticationConfiguration.class.isInstance(authSchemeConfiguration)) {
                        authenticationScheme = resolveAuthenticationScheme(BasicAuthenticationScheme.class);
                    } else if (X509AuthenticationConfiguration.class.isInstance(authSchemeConfiguration)) {
                        authenticationScheme = resolveAuthenticationScheme(X509AuthenticationScheme.class);
                    } else if (TokenAuthenticationConfiguration.class.isInstance(authSchemeConfiguration)) {
                        authenticationScheme = resolveAuthenticationScheme(TokenAuthenticationScheme.class);
                    }

                    this.authenticationSchemes.put(pathConfiguration, authenticationScheme);
                }
            }
        } else if (request != null) {
            for (Map.Entry<PathConfiguration, HttpAuthenticationScheme> entry : this.authenticationSchemes.entrySet()) {
                DefaultLoginCredentials creds = extractCredentials(request, entry.getValue());

                if (creds.getCredential() != null) {
                    HttpSession session = request.getSession(false);

                    if (session != null) {
                        PathConfiguration originalAuthcPath = (PathConfiguration) session
                            .getAttribute(AUTHENTICATION_ORIGINAL_PATH);

                        if (originalAuthcPath != null && originalAuthcPath.equals(entry.getKey())) {
                            session.removeAttribute(AUTHENTICATION_ORIGINAL_PATH);
                            return entry.getValue();
                        }
                    }
                }
            }
        }

        return authenticationScheme;
    }

    @Override
    public void destroy() {

    }

    private DefaultLoginCredentials extractCredentials(HttpServletRequest request, HttpAuthenticationScheme authenticationScheme) {
        DefaultLoginCredentials creds = getCredentials();

        authenticationScheme.extractCredential(request, creds);

        return creds;
    }

    private DefaultLoginCredentials getCredentials() {
        return resolveInstance(this.credentialsInstance);
    }

    private Identity getIdentity() {
        return resolveInstance(this.identityInstance);
    }

    private <I> I resolveInstance(Instance<I> instance) {
        if (instance.isUnsatisfied()) {
            throw new IllegalStateException("Instance [" + instance + "] not found.");
        } else if (instance.isAmbiguous()) {
            throw new IllegalStateException("Instance [" + instance + "] is ambiguous.");
        }

        try {
            return (I) instance.get();
        } catch (Exception e) {
            throw new IllegalStateException("Could not retrieve instance [" + instance + "].", e);
        }
    }

    private HttpAuthenticationScheme resolveAuthenticationScheme(Class<? extends HttpAuthenticationScheme> authSchemeType) {
        Instance<? extends HttpAuthenticationScheme> configuredAuthScheme = this.authenticationSchemesInstances.select(authSchemeType);

        if (configuredAuthScheme.isAmbiguous()) {
            throw new IllegalStateException("Ambiguous beans found for Http Authentication Scheme type [" + authSchemeType + "].");
        }

        if (configuredAuthScheme.isUnsatisfied()) {
            throw new IllegalStateException("No bean found for Http Authentication Scheme with type [" + authSchemeType + "].");
        }

        return configuredAuthScheme.get();
    }

    protected PathMatcher getPathMatcher() {
        if (this.pathMatcher == null) {
            this.pathMatcher = new PathMatcher(this.configuration.getPaths(), this.elProcessor);
        }

        return this.pathMatcher;
    }

}
