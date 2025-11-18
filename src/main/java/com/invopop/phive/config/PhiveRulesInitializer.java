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
package com.invopop.phive.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.phive.api.executorset.ValidationExecutorSetRegistry;
import com.helger.phive.api.source.IValidationSource;

/**
 * Initializes phive-rules by loading validation rule classes and registering
 * them to a ValidationExecutorSetRegistry instance.
 *
 * @author Invopop Ltd.
 */
public class PhiveRulesInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger(PhiveRulesInitializer.class);

    private ValidationExecutorSetRegistry<IValidationSource> registry;

    public void initialize() {
        LOGGER.info("Initializing Phive validation rules...");

        try {
            // Create the validation executor set registry
            registry = new ValidationExecutorSetRegistry<>();

            // Register phive-rules validation sets to our registry
            // Each phive-rules library has different init methods
            // IMPORTANT: EN16931 must be registered BEFORE ZUGFeRD as ZUGFeRD depends on it

            // EN 16931 (European e-invoicing standard)
            registerRules("com.helger.phive.en16931.EN16931Validation", "initEN16931");

            // Peppol BIS (Business Interoperability Specifications)
            registerRules("com.helger.phive.peppol.PeppolValidation", "initStandard");

            // UBL (Universal Business Language) - registers all UBL versions
            registerRules("com.helger.phive.ubl.UBLValidation", "initUBLAllVersions");

            // CII (Cross Industry Invoice) - registers both D16B and D22B
            registerRules("com.helger.phive.cii.CIIValidation", "initCII");

            // XRechnung (German e-invoicing)
            registerRules("com.helger.phive.xrechnung.XRechnungValidation", "initXRechnung");

            // FacturaE (Spanish e-invoicing)
            registerRules("com.helger.phive.facturae.FacturaeValidation", "initFacturaE");

            // FatturaPA (Italian e-invoicing)
            registerRules("com.helger.phive.fatturapa.FatturaPAValidation", "initFatturaPA");

            // ZUGFeRD (German hybrid invoice format)
            registerRules("com.helger.phive.zugferd.ZugferdValidation", "initZugferd");

            // Log the number of registered validation executor sets
            final int count = registry.getAll().size();

            LOGGER.info("Phive validation rules initialized successfully. {} validation executor sets registered.", count);

        } catch (final Exception ex) {
            LOGGER.error("Failed to initialize Phive validation rules", ex);
            throw new RuntimeException("Failed to initialize Phive validation rules", ex);
        }
    }

    /**
     * Registers validation rules from a phive-rules class by calling its init method.
     * If the class is not found, logs a warning but doesn't fail.
     *
     * @param className The fully qualified class name
     * @param methodName The init method name (e.g., "initStandard", "initUBLAllVersions")
     */
    private void registerRules(final String className, final String methodName) {
        try {
            final Class<?> clazz = Class.forName(className);
            final java.lang.reflect.Method method = clazz.getMethod(methodName,
                    com.helger.phive.api.executorset.IValidationExecutorSetRegistry.class);
            method.invoke(null, registry);
            LOGGER.debug("Registered validation rules from: {} ({})", className, methodName);
        } catch (final ClassNotFoundException ex) {
            LOGGER.warn("Validation rules class not found (dependency may not be included): {}", className);
        } catch (final NoSuchMethodException ex) {
            LOGGER.warn("Class {} does not have {} method", className, methodName);
        } catch (final Exception ex) {
            LOGGER.error("Error registering validation rules from: {}", className, ex);
        }
    }

    /**
     * @return The validation executor set registry
     */
    public ValidationExecutorSetRegistry<IValidationSource> getRegistry() {
        return registry;
    }
}
