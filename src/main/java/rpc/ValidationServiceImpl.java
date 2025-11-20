/*
 * Copyright (C) 2025 Invopop Ltd. (https://invopop.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rpc;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.error.level.IErrorLevel;
import com.helger.commons.error.list.ErrorList;
import com.helger.diver.api.coord.DVRCoordinate;
import com.helger.phive.api.execute.ValidationExecutionManager;
import com.helger.phive.api.executorset.IValidationExecutorSet;
import com.helger.phive.api.executorset.ValidationExecutorSetRegistry;
import com.helger.phive.api.result.ValidationResult;
import com.helger.phive.api.result.ValidationResultList;
import com.helger.phive.api.source.IValidationSource;
import com.helger.phive.xml.source.ValidationSourceXML;
import com.helger.xml.serialize.read.DOMReader;
import config.PhiveRulesInitializer;

import io.grpc.stub.StreamObserver;
import jakarta.annotation.Nonnull;

/**
 * gRPC service implementation for Phive validation operations.
 * Provides methods to list available VESIDs and validate XML documents using
 * pre-registered validation rules from phive-rules dependencies.
 *
 * @author Invopop Ltd.
 */
public class ValidationServiceImpl extends ValidationServiceGrpc.ValidationServiceImplBase {
    private static final Logger LOGGER = LoggerFactory.getLogger(ValidationServiceImpl.class);

    private final PhiveRulesInitializer phiveRulesInitializer;

    public ValidationServiceImpl(@Nonnull final PhiveRulesInitializer phiveRulesInitializer) {
        this.phiveRulesInitializer = phiveRulesInitializer;
    }

    @Override
    public void listVesIds(
            @Nonnull final ValidationProto.ListVesIdsRequest request,
            @Nonnull final StreamObserver<ValidationProto.ListVesIdsResponse> responseObserver) {

        LOGGER.info("Received request to list VESIDs with filter: {}",
                    request.getFilter().isEmpty() ? "(none)" : request.getFilter());

        try {
            final ValidationProto.ListVesIdsResponse.Builder responseBuilder =
                    ValidationProto.ListVesIdsResponse.newBuilder();

            // Get the registry from the initializer
            final ValidationExecutorSetRegistry<IValidationSource> registry =
                    phiveRulesInitializer.getRegistry();

            final ICommonsList<IValidationExecutorSet<IValidationSource>> allVES = registry.getAll();

            LOGGER.debug("Found {} registered validation executor sets", allVES.size());

            for (final IValidationExecutorSet<IValidationSource> ves : allVES) {
                final DVRCoordinate vesid = ves.getID();
                final String vesidStr = vesid.getAsSingleID();

                // Apply filter if provided
                if (!request.getFilter().isEmpty()) {
                    if (!vesidStr.contains(request.getFilter())) {
                        continue;
                    }
                }

                // Build VesIdInfo
                final ValidationProto.VesIdInfo.Builder vesidInfo =
                        ValidationProto.VesIdInfo.newBuilder()
                        .setVesid(vesidStr)
                        .setVersion(vesid.getVersionString());

                // Add name if available
                if (ves.getDisplayName() != null) {
                    vesidInfo.setName(ves.getDisplayName());
                }

                // Add status if available
                if (ves.getStatus() != null) {
                    vesidInfo.setStatus(ves.getStatus().getType().getID());
                }

                responseBuilder.addVesids(vesidInfo.build());
            }

            LOGGER.info("Returning {} VESIDs", responseBuilder.getVesidsCount());

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();

        } catch (final Exception ex) {
            LOGGER.error("Failed to list VESIDs", ex);

            final ValidationProto.ListVesIdsResponse response =
                    ValidationProto.ListVesIdsResponse.newBuilder()
                    .setErrorMessage("Failed to list VESIDs: " +
                                   (ex.getMessage() != null ? ex.getMessage() : "Unknown error"))
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }

    @Override
    public void validateXml(
            @Nonnull final ValidationProto.ValidateXmlRequest request,
            @Nonnull final StreamObserver<ValidationProto.ValidateXmlResponse> responseObserver) {

        LOGGER.info("Received validation request for VESID: {}, source: {}",
                    request.getVesid(),
                    request.getSourceIdentifier().isEmpty() ? "(none)" : request.getSourceIdentifier());

        try {
            // Parse the VESID
            final DVRCoordinate vesid = DVRCoordinate.parseOrNull(request.getVesid());
            if (vesid == null) {
                throw new IllegalArgumentException("Invalid VESID format: " + request.getVesid());
            }

            // Get the validation executor set from the registry
            final ValidationExecutorSetRegistry<IValidationSource> registry =
                    phiveRulesInitializer.getRegistry();

            final IValidationExecutorSet<IValidationSource> ves = registry.getOfID(vesid);
            if (ves == null) {
                throw new IllegalArgumentException("VESID not found: " + request.getVesid());
            }

            // Parse XML bytes to DOM Node
            final byte[] xmlBytes = request.getXmlContent().toByteArray();
            final Node xmlNode = DOMReader.readXMLDOM(xmlBytes);
            if (xmlNode == null) {
                throw new IllegalArgumentException("Failed to parse XML content");
            }

            // Create validation source from Node
            final IValidationSource validationSource = ValidationSourceXML.create(
                    request.getSourceIdentifier().isEmpty() ? "xml-input" : request.getSourceIdentifier(),
                    xmlNode);

            // Execute validation
            final ValidationResultList resultList = ValidationExecutionManager.executeValidation(
                    ves,
                    validationSource);

            // Build response
            final ValidationProto.ValidateXmlResponse.Builder responseBuilder =
                    ValidationProto.ValidateXmlResponse.newBuilder();

            // Set timestamp
            responseBuilder.setTimestamp(Instant.now().toString());

            // Set resolved VESID
            responseBuilder.setResolvedVesid(vesid.getAsSingleID());

            // Process validation results
            boolean overallSuccess = true;
            if (resultList != null) {
                for (final ValidationResult result : resultList) {
                    final ValidationProto.ValidationLayerResult.Builder layerBuilder =
                            ValidationProto.ValidationLayerResult.newBuilder();

                    // Set validation type and artifact
                    if (result.getValidationArtefact() != null) {
                        final var artefact = result.getValidationArtefact();
                        layerBuilder.setValidationType(artefact.getValidationType().getID());

                        if (artefact.getRuleResource() != null) {
                            layerBuilder.setArtifactId(artefact.getRuleResource().getPath());
                        }
                    }

                    // Check success - result is valid if it has no errors
                    final boolean layerSuccess = !result.getErrorList().containsAtLeastOneError();
                    layerBuilder.setSuccess(layerSuccess);
                    if (!layerSuccess) {
                        overallSuccess = false;
                    }

                    // Process errors and warnings
                    if (result.getErrorList() != null && result.getErrorList().isNotEmpty()) {
                        result.getErrorList().forEach(error -> {
                            final ValidationProto.ValidationError.Builder errorBuilder =
                                    ValidationProto.ValidationError.newBuilder()
                                    .setLevel(error.getErrorLevel().getID())
                                    .setMessage(error.getErrorText(Locale.US));

                            // Add location if available
                            if (error.hasErrorLocation()) {
                                errorBuilder.setLocation(error.getErrorLocation().getAsString());
                            }

                            // Add to appropriate list
                            if (error.isError()) {
                                layerBuilder.addErrors(errorBuilder.build());
                            } else {
                                layerBuilder.addWarnings(errorBuilder.build());
                            }
                        });
                    }

                    responseBuilder.addResults(layerBuilder.build());
                }
            }

            responseBuilder.setSuccess(overallSuccess);

            if (overallSuccess) {
                LOGGER.info("Validation successful for VESID: {}", request.getVesid());
            } else {
                LOGGER.info("Validation failed for VESID: {}", request.getVesid());
            }

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();

        } catch (final Exception ex) {
            LOGGER.error("Failed to validate XML", ex);

            final ValidationProto.ValidateXmlResponse response =
                    ValidationProto.ValidateXmlResponse.newBuilder()
                    .setSuccess(false)
                    .setErrorMessage("Validation failed: " +
                                   (ex.getMessage() != null ? ex.getMessage() : "Unknown error"))
                    .setTimestamp(Instant.now().toString())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }
}
