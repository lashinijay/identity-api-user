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

package org.wso2.carbon.identity.rest.api.user.idv.v1.impl;

import org.wso2.carbon.identity.rest.api.user.idv.v1.MeApiService;
import org.wso2.carbon.identity.rest.api.user.idv.v1.core.IdentityVerificationService;
import org.wso2.carbon.identity.rest.api.user.idv.v1.factories.IdentityVerificationServiceFactory;
import org.wso2.carbon.identity.rest.api.user.idv.v1.model.VerificationClaimResponse;
import org.wso2.carbon.identity.rest.api.user.idv.v1.model.VerificationPostResponse;
import org.wso2.carbon.identity.rest.api.user.idv.v1.model.VerifyRequest;

import java.util.List;

import javax.ws.rs.core.Response;

import static org.wso2.carbon.identity.api.user.common.ContextLoader.getUserIdFromContext;

/**
 * This class implements the MeApiService interface.
 */
public class MeApiServiceImpl implements MeApiService {

    private final IdentityVerificationService identityVerificationService;

    public MeApiServiceImpl() {

        try {
            this.identityVerificationService = IdentityVerificationServiceFactory.getIdentityVerificationService();
        } catch (IllegalStateException e) {
            throw new RuntimeException("Error occurred while initiating required services for " +
                    "IdentityVerificationService.", e);
        }
    }

    @Override
    public Response meGetIdVClaim(String claimId) {

        VerificationClaimResponse verificationClaimResponse =
                identityVerificationService.getIdVClaim(getUserIdFromContext(), claimId);
        return Response.ok().entity(verificationClaimResponse).build();
    }

    @Override
    public Response meGetIdVClaims(String idvProviderid) {

        List<VerificationClaimResponse> verificationGetResponse =
                identityVerificationService.getIdVClaims(getUserIdFromContext(), idvProviderid);
        return Response.ok().entity(verificationGetResponse).build();
    }

    @Override
    public Response meVerifyIdentity(VerifyRequest verifyRequest) {

        VerificationPostResponse verificationPostResponse =
                identityVerificationService.verifyIdentity(getUserIdFromContext(), verifyRequest);
        return Response.ok().entity(verificationPostResponse).build();
    }
}
