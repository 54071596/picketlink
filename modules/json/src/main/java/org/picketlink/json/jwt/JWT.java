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
package org.picketlink.json.jwt;

import static org.picketlink.json.JsonConstants.COMMON.HEADER_CONTENT_TYPE;
import static org.picketlink.json.JsonConstants.COMMON.HEADER_TYPE;
import static org.picketlink.json.JsonConstants.COMMON.PERIOD;
import static org.picketlink.json.JsonConstants.JWT.CLAIM_AUDIENCE;
import static org.picketlink.json.JsonConstants.JWT.CLAIM_EXPIRATION;
import static org.picketlink.json.JsonConstants.JWT.CLAIM_ID;
import static org.picketlink.json.JsonConstants.JWT.CLAIM_ISSUED_AT;
import static org.picketlink.json.JsonConstants.JWT.CLAIM_ISSUER;
import static org.picketlink.json.JsonConstants.JWT.CLAIM_NOT_BEFORE;
import static org.picketlink.json.JsonConstants.JWT.CLAIM_SUBJECT;
import static org.picketlink.json.util.Base64Util.b64Encode;

import java.io.StringWriter;
import java.util.Date;
import java.util.List;

import javax.json.Json;
import javax.json.JsonObject;

import org.picketlink.json.util.JsonUtil;

/**
 * <p>
 * This class represents a JSON Web Token, providing the standard claims set defined by its specification. It is a
 * representation of the claims to be transferred between two parties.
 * </p>
 *
 * <p>
 * Instances must be created by their corresponding {@link JWTBuilder}. Once created, instances are immutable.
 * </p>
 *
 * <p>
 * The JSON representation of a token is obtained via <code>toString()</code> method.
 * </p>
 *
 * @author Pedro Igor
 */
public class JWT {

    /**
     * <p>
     * Holds the headers and their respective values.
     * </p>
     */
    private JsonObject headers;

    /**
     * <p>
     * Holds the claims set and their respective values.
     * </p>
     */
    private final JsonObject claims;

    /**
     * <p>
     * Creates a new instance using the claims set and values from the given {@link javax.json.JsonObject}.
     * </p>
     *
     * @param claims The claims set and their respective values.
     */
    protected JWT(JsonObject headers, JsonObject claims) {
        this.headers = headers;
        this.claims = claims;
    }

    /**
     * <p>
     * Ecodes the JSON representation of a JWT according with the specification.
     * </p>
     *
     * <p>
     * In order to decode, refer to the corresponding {@link JWTBuilder} of this class.
     * </p>
     *
     * @return
     */
    public String encode() {
        return format(b64Encode(getPlainHeader()), b64Encode(getPlainClaims())).toString();
    }

    /**
     * <p>
     * Declares the MIME Media Type [IANA.MediaTypes] of this complete JWT in contexts where this is useful to the application.
     * </p>
     *
     * @return
     */
    public String geType() {
        return getHeader(HEADER_TYPE);
    }

    /**
     * <p>
     * Used by this specification to convey structural information about the JWT.
     * </p>
     *
     * @return
     */
    public String getContentType() {
        return getHeader(HEADER_CONTENT_TYPE);
    }

    /**
     * <p>
     * The unique identifier for a JWT.
     * </p>
     *
     * @return
     */
    public String getId() {
        return getClaim(CLAIM_ID);
    }

    /**
     * <p>
     * The principal that issued the JWT.
     * </p>
     *
     * @return
     */
    public String getIssuer() {
        return getClaim(CLAIM_ISSUER);
    }

    /**
     * <p>
     * Identifies the audience that the JWT is intended for.
     * </p>
     *
     * @return
     */
    public List<String> getAudience() {
        return getClaimValues(CLAIM_AUDIENCE);
    }

    /**
     * <p>
     * Identifies the principal that is the subject of the JWT.
     * </p>
     *
     * @return
     */
    public String getSubject() {
        return getClaim(CLAIM_SUBJECT);
    }

    /**
     * <p>
     * The time at which the JWT was issued.
     * </p>
     *
     * @return
     */
    public Integer getIssuedAt() {
        String claim = getClaim(CLAIM_ISSUED_AT);

        if (claim != null) {
            return Integer.valueOf(claim.toString());
        }

        return null;
    }

    /**
     * <p>
     * Returns a {@link java.util.Date} representation of the {@link org.picketlink.json.jwt.JWT#getIssuedAt()}.
     * </p>
     *
     * @return
     */
    public Date getIssuedAtDate() {
        Integer issuedAt = getIssuedAt();

        if (issuedAt != null) {
            return new Date(issuedAt * 1000L);
        }

        return null;
    }

    /**
     * <p>
     * The expiration time on or after which the token MUST NOT be accepted for processing.
     * </p>
     *
     * @return
     */
    public Integer getExpiration() {
        String claim = getClaim(CLAIM_EXPIRATION);

        if (claim != null) {
            return Integer.valueOf(claim.toString());
        }

        return null;
    }

    /**
     * <p>
     * Returns a {@link java.util.Date} representation of the {@link JWT#getExpiration()}.
     * </p>
     *
     * @return
     */
    public Date getExpirationDate() {
        Integer expiration = getExpiration();

        if (expiration != null) {
            return new Date(expiration * 1000L);
        }

        return null;
    }

    /**
     * <p>
     * The time before which the token MUST NOT be accepted for processing
     * </p>
     *
     * @return
     */
    public Integer getNotBefore() {
        String claim = getClaim(CLAIM_NOT_BEFORE);

        if (claim != null) {
            return Integer.valueOf(claim.toString());
        }

        return null;
    }

    /**
     * <p>
     * Returns a {@link java.util.Date} representation of the {@link JWT#getNotBeforeDate()}.
     * </p>
     *
     * @return
     */
    public Date getNotBeforeDate() {
        Integer notBefore = getNotBefore();

        if (notBefore != null) {
            return new Date(notBefore * 1000L);
        }

        return null;
    }

    @Override
    public String toString() {
        return format(getPlainHeader(), getPlainClaims()).toString();
    }

    /**
     * <p>
     * Subclasses can obtain from this method a {@link javax.json.JsonObject} instance containing the claims set and their
     * respective values.
     * </p>
     *
     * @return
     */
    public JsonObject getClaims() {
        return this.claims;
    }

    /**
     * <p>
     * Subclasses can obtain from this method a {@link javax.json.JsonObject} instance containing headers and their respective
     * values.
     * </p>
     *
     * @return
     */
    public JsonObject getHeaders() {
        return this.headers;
    }

    /**
     * <p>
     * Returns a claim given its name. If the claim represents an array, only the first value is returned.
     * </p>
     *
     * @param name
     * @return
     */
    public String getClaim(String name) {
        return JsonUtil.getValue(name, this.claims);
    }

    /**
     * <p>
     * Returns a claim given its name.
     * </p>
     *
     * @param name
     * @return
     */
    public List<String> getClaimValues(String name) {
        return JsonUtil.getValues(name, this.claims);
    }

    /**
     * <p>
     * Returns a header given its name. If the header represents an array, only the first value is returned.
     * </p>
     *
     * @param name
     * @return
     */
    public String getHeader(String name) {
        return JsonUtil.getValue(name, this.headers);
    }

    /**
     * <p>
     * Returns a header given its name.
     * </p>
     *
     * @param name
     * @return
     */
    public List<String> getHeaderValues(String name) {
        return JsonUtil.getValues(name, this.headers);
    }

    /**
     * <p>
     * Returns a {@link java.lang.StringBuilder} representing a JWT using its encoded format.
     * </p>
     *
     * @param header The string representing the header.
     * @param claimsSet The string representing the claims set.
     * @return
     */
    private StringBuilder format(String header, String claimsSet) {
        return new StringBuilder().append(header).append(PERIOD).append(claimsSet);
    }

    /**
     * Gets the plain claims set as a string representation.
     *
     * @return the plain claims set
     */
    private String getPlainClaims() {
        StringWriter claimsWriter = new StringWriter();

        Json.createWriter(claimsWriter).writeObject(this.claims);

        return claimsWriter.getBuffer().toString();
    }

    /**
     * Gets the plain header as a string representation.
     *
     * @return the plain header set
     */
    private String getPlainHeader() {
        StringWriter headerWriter = new StringWriter();

        Json.createWriter(headerWriter).writeObject(this.headers);

        return headerWriter.getBuffer().toString();
    }
}