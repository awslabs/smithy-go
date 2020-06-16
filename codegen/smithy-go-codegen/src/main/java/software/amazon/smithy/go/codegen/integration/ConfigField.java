/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.smithy.go.codegen.integration;

import java.util.Objects;
import java.util.Optional;
import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Represents a config field on a client config struct.
 */
public class ConfigField implements ToSmithyBuilder<ConfigField>, Comparable<ConfigField> {
    private final String name;
    private final Symbol type;
    private final String documentation;

    public ConfigField(Builder builder) {
        this.name = Objects.requireNonNull(builder.name);
        this.type = Objects.requireNonNull(builder.type);
        this.documentation = builder.documentation;
    }

    /**
     * @return Returns the name of the config field.
     */
    public String getName() {
        return name;
    }

    /**
     * @return Returns the type Symbol for the field.
     */
    public Symbol getType() {
        return type;
    }

    /**
     * @return Gets the optional documentation for the field.
     */
    public Optional<String> getDocumentation() {
        return Optional.ofNullable(documentation);
    }

    @Override
    public SmithyBuilder<ConfigField> toBuilder() {
        return builder().type(type).name(name).documentation(documentation);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public int compareTo(ConfigField o) {
        return this.getName().compareTo(o.getName());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ConfigField that = (ConfigField) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    /**
     * Builds a ConfigField.
     */
    public static class Builder implements SmithyBuilder<ConfigField> {
        private String name;
        private Symbol type;
        private String documentation;

        @Override
        public ConfigField build() {
            return new ConfigField(this);
        }

        /**
         * Set the name of the config field.
         *
         * @param name the name of the config field.
         * @return Returns the builder.
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets the type of the config field.
         *
         * @param type A Symbol representing the type of the config field.
         * @return Returns the builder.
         */
        public Builder type(Symbol type) {
            this.type = type;
            return this;
        }

        /**
         * Sets the documentation for the config field.
         *
         * @param documentation The documentation for the config field.
         * @return Returns the builder.
         */
        public Builder documentation(String documentation) {
            this.documentation = documentation;
            return this;
        }
    }
}