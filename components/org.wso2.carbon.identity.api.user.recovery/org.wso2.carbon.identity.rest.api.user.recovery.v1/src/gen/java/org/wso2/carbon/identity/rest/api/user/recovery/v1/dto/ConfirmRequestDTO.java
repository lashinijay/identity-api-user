/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.identity.rest.api.user.recovery.v1.dto;

import io.swagger.annotations.ApiModel;
import java.util.ArrayList;
import java.util.List;
import org.wso2.carbon.identity.rest.api.user.recovery.v1.dto.PropertyDTO;
import io.swagger.annotations.*;
import com.fasterxml.jackson.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

    /**
    * Request to confirm password recovery
    **/
@ApiModel(description = "Request to confirm password recovery")
public class ConfirmRequestDTO {

    @Valid 
    private String confirmationCode = null;

    @Valid 
    private List<PropertyDTO> properties = new ArrayList<PropertyDTO>();

    /**
    * Confirmation code given to the user
    **/
    @ApiModelProperty(value = "Confirmation code given to the user")
    @JsonProperty("confirmationCode")
    public String getConfirmationCode() {
        return confirmationCode;
    }
    public void setConfirmationCode(String confirmationCode) {
        this.confirmationCode = confirmationCode;
    }

    /**
    * (OPTIONAL) Additional META properties
    **/
    @ApiModelProperty(value = "(OPTIONAL) Additional META properties")
    @JsonProperty("properties")
    public List<PropertyDTO> getProperties() {
        return properties;
    }
    public void setProperties(List<PropertyDTO> properties) {
        this.properties = properties;
    }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();
        sb.append("class ConfirmRequestDTO {\n");
        
        sb.append("    confirmationCode: ").append(confirmationCode).append("\n");
        sb.append("    properties: ").append(properties).append("\n");
        
        sb.append("}\n");
        return sb.toString();
    }
}
