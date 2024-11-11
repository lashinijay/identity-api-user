/*
 * Copyright (c) 2019-2024, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.rest.api.user.fido2.v1.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.api.user.fido2.common.Constants;
import org.wso2.carbon.identity.api.user.fido2.common.Util;
import org.wso2.carbon.identity.application.authenticator.fido2.core.WebAuthnService;
import org.wso2.carbon.identity.application.authenticator.fido2.dto.RegistrationRequest;
import org.wso2.carbon.identity.application.authenticator.fido2.exception.FIDO2AuthenticatorException;
import org.wso2.carbon.identity.application.authenticator.fido2.exception.FIDO2AuthenticatorServerException;
import org.wso2.carbon.identity.application.authenticator.fido2.util.Either;
import org.wso2.carbon.identity.application.authenticator.fido2.util.FIDOUtil;
import org.wso2.carbon.identity.core.util.IdentityCoreConstants;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.rest.api.user.fido2.v1.MeApiService;
import org.wso2.carbon.identity.rest.api.user.fido2.v1.factories.WebAuthnServiceFactory;

import java.io.IOException;
import java.net.URLDecoder;
import java.text.MessageFormat;
import javax.ws.rs.core.Response;

/**
 * Implementation class of FIDO2 API
 */
public class MeApiServiceImpl extends MeApiService {

    private static final Log log = LogFactory.getLog(MeApiServiceImpl.class);

    private final WebAuthnService webAuthnService;

    public MeApiServiceImpl() {

        this.webAuthnService = WebAuthnServiceFactory.getWebAuthnService();
    }

    @Override
    public Response meWebauthnCredentialIdDelete(String credentialId) {

        if (!isValidAuthenticationType()) {
            throw Util.handleError(Response.Status.FORBIDDEN,
                    Constants.ErrorMessages.ERROR_CODE_ACCESS_DENIED_FOR_BASIC_AUTH);
        }

        try {
            webAuthnService.deregisterCredential(credentialId);
        } catch (IOException e) {
            throw Util.handleError(Response.Status.INTERNAL_SERVER_ERROR, Constants.ErrorMessages
                    .ERROR_CODE_DELETE_CREDENTIALS, credentialId);
        }
        return Response.ok().build();
    }

    @Override
    public Response meWebauthnFinishRegistrationPost(String response) {

        if (!isValidAuthenticationType()) {
            throw Util.handleError(Response.Status.FORBIDDEN,
                    Constants.ErrorMessages.ERROR_CODE_ACCESS_DENIED_FOR_BASIC_AUTH);
        }

        if (log.isDebugEnabled()) {
            log.debug(MessageFormat.format("Received finish registration response: {0}", response));
        }
        try {
            webAuthnService.finishRegistration(response);
        } catch (FIDO2AuthenticatorServerException ex) {
            throw Util.handleError(Response.Status.INTERNAL_SERVER_ERROR, Constants.ErrorMessages
                    .ERROR_CODE_FINISH_REGISTRATION);
        } catch (FIDO2AuthenticatorException | IOException e) {
            throw Util.handleError(Response.Status.BAD_REQUEST, Constants.ErrorMessages
                            .ERROR_CODE_FINISH_REGISTRATION_BY_USER, response);
        }
        return Response.ok().entity(response).build();
    }

    @Override
    public Response meWebauthnGet(String username) {

        if (!isValidAuthenticationType()) {
            throw Util.handleError(Response.Status.FORBIDDEN,
                    Constants.ErrorMessages.ERROR_CODE_ACCESS_DENIED_FOR_BASIC_AUTH);
        }

        if (log.isDebugEnabled()) {
            log.debug(MessageFormat.format("fetching device metadata for the user : {0}", username));
        }
        try {
            if (username.contains(Constants.EQUAL_OPERATOR)) {
                username = URLDecoder.decode(username.split(Constants.EQUAL_OPERATOR)[1], IdentityCoreConstants.UTF_8);
            }
            return Response.ok().entity(FIDOUtil.writeJson(webAuthnService.getDeviceMetaData(username))).build();
        } catch (IOException e) {
            throw Util.handleError(Response.Status.INTERNAL_SERVER_ERROR, Constants.ErrorMessages
                    .ERROR_CODE_FETCH_CREDENTIALS, username);
        }
    }

    @Override
    public Response meWebauthnStartRegistrationPost(String appID) {

        if (!isValidAuthenticationType()) {
            throw Util.handleError(Response.Status.FORBIDDEN,
                    Constants.ErrorMessages.ERROR_CODE_ACCESS_DENIED_FOR_BASIC_AUTH);
        }

        try {
            Either<String, RegistrationRequest> result = webAuthnService.startRegistration(appID);
            if (result.isRight()) {
                return Response.ok().entity(FIDOUtil.writeJson(result.right().get())).build();
            } else {
                throw Util.handleError(Response.Status.INTERNAL_SERVER_ERROR, Constants.ErrorMessages
                        .ERROR_CODE_START_REGISTRATION, appID);
            }
        } catch (FIDO2AuthenticatorException | JsonProcessingException e) {
            throw Util.handleError(Response.Status.INTERNAL_SERVER_ERROR, Constants.ErrorMessages
                    .ERROR_CODE_START_REGISTRATION, appID);
        }
    }

    private boolean isValidAuthenticationType() {

        /*
        Check whether the request is authenticated with basic auth. FIDO endpoint should not be allowed for basic
        authentication. This approach can be improved by providing a Level of Assurance (LOA) and checking that in
        FIDOAdminService.
         */
        if (Boolean.parseBoolean(
                (String) IdentityUtil.threadLocalProperties.get().get(Constants.AUTHENTICATED_WITH_BASIC_AUTH))) {
            if (log.isDebugEnabled()) {
                log.debug("Not a valid authentication method. "
                        + "This method is blocked for the requests with basic authentication.");
            }
            return false;
        }

        return true;
    }
}
