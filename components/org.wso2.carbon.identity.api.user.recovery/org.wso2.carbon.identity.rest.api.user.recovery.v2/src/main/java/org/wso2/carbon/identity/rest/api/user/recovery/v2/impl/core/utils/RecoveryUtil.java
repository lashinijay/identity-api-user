/*
 * Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com).
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

package org.wso2.carbon.identity.rest.api.user.recovery.v2.impl.core.utils;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.wso2.carbon.identity.api.user.common.error.APIError;
import org.wso2.carbon.identity.api.user.common.error.ErrorResponse;
import org.wso2.carbon.identity.core.ServiceURLBuilder;
import org.wso2.carbon.identity.core.URLBuilderException;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.recovery.IdentityRecoveryClientException;
import org.wso2.carbon.identity.recovery.IdentityRecoveryConstants;
import org.wso2.carbon.identity.recovery.IdentityRecoveryException;
import org.wso2.carbon.identity.recovery.dto.NotificationChannelDTO;
import org.wso2.carbon.identity.rest.api.user.recovery.v2.impl.core.APICalls;
import org.wso2.carbon.identity.rest.api.user.recovery.v2.impl.core.Constants;
import org.wso2.carbon.identity.rest.api.user.recovery.v2.impl.core.exceptions.ConflictException;
import org.wso2.carbon.identity.rest.api.user.recovery.v2.impl.core.exceptions.ForbiddenException;
import org.wso2.carbon.identity.rest.api.user.recovery.v2.impl.core.exceptions.NotAcceptableException;
import org.wso2.carbon.identity.rest.api.user.recovery.v2.impl.core.exceptions.NotFoundException;
import org.wso2.carbon.identity.rest.api.user.recovery.v2.impl.core.exceptions.PreconditionFailedException;

import org.wso2.carbon.identity.rest.api.user.recovery.v2.model.APICall;
import org.wso2.carbon.identity.rest.api.user.recovery.v2.model.Property;
import org.wso2.carbon.identity.rest.api.user.recovery.v2.model.RecoveryChannel;
import org.wso2.carbon.identity.rest.api.user.recovery.v2.model.RetryErrorResponse;
import org.wso2.carbon.identity.rest.api.user.recovery.v2.model.UserClaim;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import static org.wso2.carbon.identity.api.user.common.Constants.TENANT_CONTEXT_PATH_COMPONENT;
import static org.wso2.carbon.identity.application.common.util.IdentityApplicationConstants.Error.UNEXPECTED_SERVER_ERROR;

/**
 * Contains the recovery endpoint utils.
 */
public class RecoveryUtil {

    private static final Log log = LogFactory.getLog(RecoveryUtil.class);
    private static final String LOG_MESSAGE_PREFIX = "INITIATOR";
    private static final String FORBIDDEN_ERROR_CATEGORY = "FORBIDDEN_ERROR_CATEGORY";
    private static final String CONFLICT_REQUEST_ERROR_CATEGORY = "CONFLICT_REQUEST_ERROR_CATEGORY";
    private static final String REQUEST_NOT_FOUND_ERROR_CATEGORY = "REQUEST_NOT_FOUND_ERROR_CATEGORY";
    private static final String REQUEST_NOT_ACCEPTABLE_ERROR_CATEGORY = "REQUEST_NOT_ACCEPTABLE_ERROR_CATEGORY";
    private static final String RETRY_ERROR_CATEGORY = "RETRY_ERROR_CATEGORY";

    // Map with the error codes categorized in to different error groups.
    private static final Map<String, String> clientErrorMap = generateClientErrorMap();

    /**
     * Converts a list of UserClaim in to a UserClaim array.
     *
     * @param userClaimsList UserClaims List.
     * @return Map of user claims.
     */
    public static Map<String, String> buildUserClaimsMap(List<UserClaim> userClaimsList) {

        Map<String, String> userClaims = new HashMap<>();
        for (UserClaim userClaimModel : userClaimsList) {
            userClaims.put(userClaimModel.getUri(), userClaimModel.getValue());
        }
        return userClaims;
    }

    /**
     * Convert the list of Properties in to an array.
     *
     * @param propertyList List of {@link Property} objects.
     * @return Map of properties.
     */
    public static Map<String, String> buildPropertiesMap(List<Property> propertyList) {

        Map<String, String> properties = new HashMap<>();
        if (propertyList == null) {
            return properties;
        }
        for (Property propertyDTO : propertyList) {
            properties.put(propertyDTO.getKey(), propertyDTO.getValue());
        }
        return properties;
    }

    /**
     * Build the channel response object list.
     *
     * @param channels Available notification channels list as objects of {@link NotificationChannelDTO}.
     * @return List of RecoveryChannels {@link RecoveryChannel}.
     */
    public static List<RecoveryChannel> buildRecoveryChannelInformation(NotificationChannelDTO[] channels) {

        List<RecoveryChannel> recoveryChannelDTOs = new ArrayList<>();
        if (channels != null) {
            // Create a response object and add the details to each object.
            for (NotificationChannelDTO channel : channels) {
                RecoveryChannel recoveryChannel = new RecoveryChannel();
                recoveryChannel.setId(Integer.toString(channel.getId()));
                recoveryChannel.setType(channel.getType());
                recoveryChannel.setValue(channel.getValue());
                if (StringUtils.isNotEmpty(channel.getValue())) {
                    recoveryChannel.setPreferred(channel.isPreferred());
                }
                recoveryChannelDTOs.add(recoveryChannel);
            }
        }
        return recoveryChannelDTOs;
    }

    /**
     * Handle client errors with specific http codes.
     *
     * @param scenario  Recovery scenario.
     * @param exception IdentityRecoveryClientException.
     * @return WebApplicationException (NOTE: Returns null when the client error is for no user available or for
     * multiple users available.
     */
    public static WebApplicationException handleClientException(IdentityRecoveryClientException exception,
                                                                String tenantDomain, String scenario,
                                                                String correlationId) {

        return handleClientException(exception, tenantDomain, scenario, StringUtils.EMPTY, correlationId);
    }

    /**
     * Handle client errors with specific http codes.
     *
     * @param scenario  Recovery scenario.
     * @param code      Recovery code.
     * @param exception IdentityRecoveryClientException.
     * @return WebApplicationException (NOTE: Returns null when the client error is for no user available or for
     * multiple users available.
     */
    public static WebApplicationException handleClientException(IdentityRecoveryClientException exception,
                                                                String tenantDomain, String scenario, String code,
                                                                String correlationId) {

        if (StringUtils.isEmpty(exception.getErrorCode())) {
            return buildConflictRequestResponseObject(exception, exception.getMessage(), exception.getErrorCode());
        }
        String errorCode = prependOperationScenarioToErrorCode(exception.getErrorCode(), scenario);

        if (clientErrorMap.containsKey(errorCode)) {
            String errorCategory = clientErrorMap.get(errorCode);

            // Throw errors according to exception category.
            switch (errorCategory) {
                case FORBIDDEN_ERROR_CATEGORY:
                    return buildForbiddenRequestResponseObject(exception, exception.getMessage(), errorCode);
                case CONFLICT_REQUEST_ERROR_CATEGORY:
                    if (IdentityRecoveryConstants.ErrorMessages.ERROR_CODE_MULTIPLE_MATCHING_USERS.getCode()
                            .equals(errorCode)) {
                        // If user notify is not enabled, throw a accepted response.
                        if (!Boolean.parseBoolean(IdentityUtil
                                .getProperty(IdentityRecoveryConstants.ConnectorConfig.NOTIFY_USER_EXISTENCE))) {
                            return new WebApplicationException(Response.accepted().build());
                        }
                    }
                    return buildConflictRequestResponseObject(exception, exception.getMessage(),
                            exception.getErrorCode());
                case REQUEST_NOT_FOUND_ERROR_CATEGORY:
                    if (IdentityRecoveryConstants.ErrorMessages.ERROR_CODE_NO_USER_FOUND.getCode().equals(errorCode)) {
                        // If user notify is not enabled, throw a accepted response.
                        if (!Boolean.parseBoolean(IdentityUtil
                                .getProperty(IdentityRecoveryConstants.ConnectorConfig.NOTIFY_USER_EXISTENCE))) {
                            return new WebApplicationException(Response.accepted().build());
                        }
                    }
                    return buildRequestNotFoundResponseObject(exception, errorCode, exception.getMessage());
                case REQUEST_NOT_ACCEPTABLE_ERROR_CATEGORY:
                    return buildRequestNotAcceptableResponseObject(exception, errorCode, exception.getMessage());
                case RETRY_ERROR_CATEGORY:
                    return buildRetryPasswordResetObject(tenantDomain, exception.getMessage(), errorCode,
                            code, correlationId);
                default:
                    return buildConflictRequestResponseObject(exception, exception.getMessage(), errorCode);
            }
        } else {
            return buildConflictRequestResponseObject(exception, exception.getMessage(), errorCode);
        }
    }

    /**
     * Build API call information.
     *
     * @param type   Type of the API call.
     * @param rel    API relation.
     * @param apiUrl Url of the API.
     * @param data   Additional data.
     * @return APICall {@link APICall} which encapsulates the API name and the url.
     */
    public static APICall buildApiCall(String type, String rel, String apiUrl, String data) {

        if (StringUtils.isNotEmpty(data)) {
            apiUrl = String.format(apiUrl, data);
        }
        APICall apiCall = new APICall();
        apiCall.setType(type);
        apiCall.setRel(rel);
        apiCall.setHref(apiUrl);
        return apiCall;
    }

    /**
     * Builds URI prepending the user API context with the proxy context path to the endpoint.
     * Ex: /t/<tenant-domain>/api/users/<endpoint>
     *
     * @param endpoint Relative endpoint path.
     * @return Relative URI.
     */
    public static String buildURIForBody(String tenantDomain, String endpoint, String baseUrl) {

        String url;
        String context = getContext(tenantDomain, endpoint, baseUrl);

        try {
            url = ServiceURLBuilder.create().addPath(context).build().getRelativePublicURL();
        } catch (URLBuilderException e) {
            String errorDescription = "Server encountered an error while building URL for response body.";
            ErrorResponse errorResponse =
                    new org.wso2.carbon.identity.api.user.common.error.ErrorResponse.Builder()
                    .withCode(UNEXPECTED_SERVER_ERROR.getCode())
                    .withMessage("Error while building response.")
                    .withDescription(errorDescription)
                    .build(log, e, errorDescription);

            Response.Status status = Response.Status.INTERNAL_SERVER_ERROR;
            throw new APIError(status, errorResponse);
        }
        return url;
    }

    /**
     * Builds API error to be thrown.
     *
     * @param e Identity Recovery Exception.
     * @param errorCode Error code.
     * @param errorMessage Error message.
     * @param errorDescription Error description.
     * @param status HTTP status.
     * @return APIError object which contains the error description.
     */
    public static APIError handleException(IdentityRecoveryException e, String errorCode, String errorMessage,
                                           String errorDescription, Response.Status status) {

        ErrorResponse errorResponse = buildErrorResponse(e, errorCode, errorMessage, errorDescription);
        return new APIError(status, errorResponse);
    }

    /**
     * Builds the API context on whether the tenant qualified url is enabled or not. In tenant qualified mode the
     * ServiceURLBuilder appends the tenant domain to the URI as a path param automatically. But
     * in non tenant qualified mode we need to append the tenant domain to the path manually.
     *
     * @param endpoint Relative endpoint path.
     * @return Context of the API.
     */
    private static String getContext(String tenantDomain, String endpoint, String baseUrl) {

        String context;
        if (IdentityTenantUtil.isTenantQualifiedUrlsEnabled()) {
            context = baseUrl + endpoint;
        } else {
            context = String.format(TENANT_CONTEXT_PATH_COMPONENT, tenantDomain) + baseUrl + endpoint;
        }
        return context;
    }

    /**
     * Builds error response.
     *
     * @param e Identity Recovery Exception.
     * @param errorCode Error code.
     * @param errorMessage Error message.
     * @param errorDescription Error description.
     * @return ErrorResponse.
     */
    private static ErrorResponse buildErrorResponse(IdentityRecoveryException e, String errorCode, String errorMessage,
                                                    String errorDescription) {

        ErrorResponse errorResponse = getErrorBuilder(errorCode, errorMessage, errorDescription).build(log, e,
                errorMessage);
        return errorResponse;
    }

    /**
     * Get ErrorResponse Builder
     *
     * @param errorCode Error code.
     * @param errorMessage Error message.
     * @param errorDescription Error description.
     * @return ErrorResponse.Builder.
     */
    private static ErrorResponse.Builder getErrorBuilder(String errorCode, String errorMessage,
                                                         String errorDescription) {

        return new ErrorResponse.Builder().withCode(errorCode)
                .withMessage(errorMessage)
                .withDescription(errorDescription);
    }

    /**
     * Returns a new PreconditionFailedException.
     *
     * @param tenantDomain  Tenant domain.
     * @param description   Description of the exception.
     * @param code          Error code.
     * @param resetCode     Reset code given to the user by confirmation API.
     * @param correlationId Correlation Id.
     * @return A new PreconditionFailedException with the specified details as a response.
     */
    private static PreconditionFailedException buildRetryPasswordResetObject(String tenantDomain, String description,
                                                                             String code, String resetCode,
                                                                             String correlationId) {

        // Build next API calls.
        ArrayList<APICall> apiCallsArrayList = new ArrayList<>();
        apiCallsArrayList.add(RecoveryUtil
                .buildApiCall(APICalls.RESET_PASSWORD_API.getType(), Constants.RelationStates.NEXT_REL,
                        buildURIForBody(tenantDomain, APICalls.RESET_PASSWORD_API.getApiUrl(),
                                Constants.ACCOUNT_RECOVERY_ENDPOINT_BASEPATH), null));
        RetryErrorResponse retryErrorResponse = buildRetryErrorResponse(
                Constants.STATUS_PRECONDITION_FAILED_MESSAGE_DEFAULT, code, description, resetCode, correlationId,
                apiCallsArrayList);
        log.error(description);
        return new PreconditionFailedException(retryErrorResponse);
    }

    /**
     * Build the RetryErrorResponse for not valid password scenario.
     *
     * @param message           Error message.
     * @param description       Error description.
     * @param code              Error code.
     * @param resetCode         Password reset code.
     * @param correlationId     Trace Id.
     * @param apiCallsArrayList Available APIs.
     * @return RetryErrorResponse.
     */
    private static RetryErrorResponse buildRetryErrorResponse(String message, String description, String code,
                                                              String resetCode, String correlationId,
                                                              ArrayList<APICall> apiCallsArrayList) {

        RetryErrorResponse retryErrorResponse = new RetryErrorResponse();
        retryErrorResponse.setCode(code);
        retryErrorResponse.setMessage(message);
        retryErrorResponse.setDescription(description);
        retryErrorResponse.setResetCode(resetCode);
        retryErrorResponse.setTraceId(correlationId);
        retryErrorResponse.setLinks(apiCallsArrayList);
        return retryErrorResponse;
    }

    /**
     * Returns a new NotAcceptableException.
     *
     * @param e             IdentityRecoveryException.
     * @param code          Error code.
     * @param description   Description of the exception.
     * @return A new NotAcceptableException with the specified details as a response.
     */
    private static NotAcceptableException buildRequestNotAcceptableResponseObject(IdentityRecoveryException e,
                                                                                  String code, String description) {

      ErrorResponse errorResponse = buildErrorResponse(e, code, Constants.STATUS_METHOD_NOT_ACCEPTED_MESSAGE_DEFAULT,
                description);
        return new NotAcceptableException(errorResponse);
    }

    /**
     * Returns a new NotAcceptableException.
     *
     * @param e             IdentityRecoveryException.
     * @param code          Error code.
     * @param description   Description of the exception.
     * @return A new NotAcceptableException with the specified details as a response.
     */
    private static NotFoundException buildRequestNotFoundResponseObject(IdentityRecoveryException e, String code,
                                                                        String description) {

        ErrorResponse errorResponse = buildErrorResponse(e, code, Constants.STATUS_NOT_FOUND_MESSAGE_DEFAULT,
                description);
        return new NotFoundException(errorResponse);
    }

    /**
     * Returns a new ConflictException.
     *
     * @param e             IdentityRecoveryException.
     * @param description   Description of the exception
     * @param code          Error code
     * @return A new ConflictException with the specified details as a response
     */
    private static ConflictException buildConflictRequestResponseObject(IdentityRecoveryException e, String description,
                                                                        String code) {

     ErrorResponse errorResponse = buildErrorResponse(e, code, Constants.STATUS_CONFLICT_MESSAGE_DEFAULT,
                description);
        return new ConflictException(errorResponse);
    }

    /**
     * Returns a new ForbiddenException.
     *
     * @param e             IdentityRecoveryException.
     * @param description   Description of the exception.
     * @param code          Error code.
     * @return A new ForbiddenException with the specified details as a response.
     */
    private static ForbiddenException buildForbiddenRequestResponseObject(IdentityRecoveryException e,
                                                                          String description, String code) {

      ErrorResponse errorResponse = buildErrorResponse(e, code, Constants.STATUS_FORBIDDEN_MESSAGE_DEFAULT,
                description);
        return new ForbiddenException(errorResponse);
    }

    /**
     * Prepend the operation scenario to the existing exception error code.
     * (Eg: USR-20045)
     *
     * @param exceptionErrorCode Existing error code.
     * @param scenario           Operation scenario.
     * @return New error code with the scenario prepended.
     */
    private static String prependOperationScenarioToErrorCode(String exceptionErrorCode, String scenario) {

        if (StringUtils.isNotEmpty(exceptionErrorCode)) {
            if (exceptionErrorCode.contains(IdentityRecoveryConstants.EXCEPTION_SCENARIO_SEPARATOR)) {
                return exceptionErrorCode;
            }
            if (StringUtils.isNotEmpty(scenario)) {
                exceptionErrorCode =
                        scenario + IdentityRecoveryConstants.EXCEPTION_SCENARIO_SEPARATOR + exceptionErrorCode;
            }
        }
        return exceptionErrorCode;
    }

    /**
     * Generate the map which categorizes the exceptions for different http error groups.
     *
     * @return Grouped client error map.
     */
    private static Map<String, String> generateClientErrorMap() {

        Map<String, String> clientErrorMap = new HashMap<>();

        // Errors for not enabling account recovery, user account locked, user account disabled.
        clientErrorMap
                .put(IdentityRecoveryConstants.ErrorMessages.ERROR_CODE_PASSWORD_RECOVERY_WITH_NOTIFICATIONS_NOT_ENABLED
                        .getCode(), FORBIDDEN_ERROR_CATEGORY);
        clientErrorMap.put(IdentityRecoveryConstants.ErrorMessages.ERROR_CODE_USERNAME_RECOVERY_NOT_ENABLED.getCode(),
                FORBIDDEN_ERROR_CATEGORY);
        clientErrorMap.put(IdentityRecoveryConstants.ErrorMessages.ERROR_CODE_DISABLED_ACCOUNT.getCode(),
                FORBIDDEN_ERROR_CATEGORY);
        clientErrorMap.put(IdentityRecoveryConstants.ErrorMessages.ERROR_CODE_LOCKED_ACCOUNT.getCode(),
                FORBIDDEN_ERROR_CATEGORY);
        clientErrorMap.put(IdentityRecoveryConstants.ErrorMessages.ERROR_CODE_PASSWORD_RECOVERY_NOT_ENABLED.getCode(),
                FORBIDDEN_ERROR_CATEGORY);

        // Tenant miss match error.
        clientErrorMap.put(IdentityRecoveryConstants.ErrorMessages.ERROR_CODE_USER_TENANT_DOMAIN_MISS_MATCH_WITH_CONTEXT
                .getCode(), CONFLICT_REQUEST_ERROR_CATEGORY);

        // Multiples users found error.
        clientErrorMap.put(IdentityRecoveryConstants.ErrorMessages.ERROR_CODE_MULTIPLE_MATCHING_USERS.getCode(),
                CONFLICT_REQUEST_ERROR_CATEGORY);

        // No user found error.
        clientErrorMap.put(IdentityRecoveryConstants.ErrorMessages.ERROR_CODE_NO_USER_FOUND.getCode(),
                REQUEST_NOT_FOUND_ERROR_CATEGORY);

        // No recovery code found and no verified channels found errors.
        clientErrorMap.put(IdentityRecoveryConstants.ErrorMessages.ERROR_CODE_NO_ACCOUNT_RECOVERY_DATA.getCode(),
                REQUEST_NOT_FOUND_ERROR_CATEGORY);
        clientErrorMap.put(IdentityRecoveryConstants.ErrorMessages.ERROR_CODE_NO_VERIFIED_CHANNELS_FOR_USER.getCode(),
                REQUEST_NOT_FOUND_ERROR_CATEGORY);

        // Invalid recovery codes errors.
        clientErrorMap.put(IdentityRecoveryConstants.ErrorMessages.ERROR_CODE_INVALID_RECOVERY_CODE.getCode(),
                REQUEST_NOT_ACCEPTABLE_ERROR_CATEGORY);
        clientErrorMap.put(IdentityRecoveryConstants.ErrorMessages.ERROR_CODE_INVALID_RESEND_CODE.getCode(),
                REQUEST_NOT_ACCEPTABLE_ERROR_CATEGORY);
        clientErrorMap.put(IdentityRecoveryConstants.ErrorMessages.ERROR_CODE_EXPIRED_RECOVERY_CODE.getCode(),
                REQUEST_NOT_ACCEPTABLE_ERROR_CATEGORY);

        // Password reset password history violation errors and password policy violation errors.
        clientErrorMap.put(IdentityRecoveryConstants.ErrorMessages.ERROR_CODE_PASSWORD_HISTORY_VIOLATION.getCode(),
                RETRY_ERROR_CATEGORY);
        clientErrorMap.put(IdentityRecoveryConstants.ErrorMessages.ERROR_CODE_PASSWORD_POLICY_VIOLATION.getCode(),
                RETRY_ERROR_CATEGORY);
        return clientErrorMap;
    }
}
