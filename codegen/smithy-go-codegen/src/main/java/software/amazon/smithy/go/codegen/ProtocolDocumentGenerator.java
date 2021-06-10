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

package software.amazon.smithy.go.codegen;

import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;

import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.go.codegen.integration.ProtocolGenerator;
import software.amazon.smithy.go.codegen.integration.ProtocolGenerator.GenerationContext;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.DocumentShape;

/**
 * Generates the Smithy document type for the service client.
 */
public final class ProtocolDocumentGenerator {
    public static final String DOCUMENT_INTERFACE_NAME = "Interface";
    public static final String NO_DOCUMENT_SERDE_TYPE_NAME = "noSmithyDocumentSerde";
    public static final String NEW_LAZY_DOCUMENT = "NewLazyDocument";
    public static final String INTERNAL_NEW_DOCUMENT_MARSHALER_FUNC = "NewDocumentMarshaler";
    public static final String INTERNAL_NEW_DOCUMENT_UNMARSHALER_FUNC = "NewDocumentUnmarshaler";
    public static final String INTERNAL_IS_DOCUMENT_INTERFACE = "IsInterface";
    public static final String UNMARSHAL_SMITHY_DOCUMENT_METHOD = "UnmarshalSmithyDocument";
    public static final String MARSHAL_SMITHY_DOCUMENT_METHOD = "MarshalSmithyDocument";

    private static final String SERVICE_SMITHY_DOCUMENT_INTERFACE = "smithyDocument";
    private static final String IS_SMITHY_DOCUMENT_METHOD = "isSmithyDocument";


    private final GoSettings settings;
    private final GoDelegator delegator;
    private final Set<DocumentShape> documentShapes = new TreeSet<>();
    private final Model model;

    public ProtocolDocumentGenerator(
            GoSettings settings,
            Model model,
            GoDelegator delegator
    ) {
        this.settings = settings;
        this.model = model;
        this.delegator = delegator;
    }

    /**
     * Get the set of document shapes for the service.
     *
     * @return the service's document shapes.
     */
    public Set<DocumentShape> getDocumentShapes() {
        return documentShapes;
    }

    /**
     * Returns whether the service has any document shapes.
     *
     * @return whether the service has one or more document types.
     */
    public boolean hasDocumentShapes() {
        return getDocumentShapes().size() > 0;
    }

    /**
     * Add a document shape to the generator.
     *
     * @param shape the document shape to add.
     */
    public void addDocumentShape(DocumentShape shape) {
        documentShapes.add(shape);
    }

    /**
     *
     */
    public void generateStandardTypes() {
        generateNoSerdeType();
        generateInternalDocumentInterface();
        generateDocumentPackage();
    }

    private void generateDocumentPackage() {
        if (!hasDocumentShapes()) {
            return;
        }

        writeDocumentPackage("document.go", writer -> {
            writer.write("type $L = $T", DOCUMENT_INTERFACE_NAME,
                            getInternalDocumentSymbol(DOCUMENT_INTERFACE_NAME))
                    .write("");

            writer.openBlock("func $L(v interface{}) $T {", "}", NEW_LAZY_DOCUMENT,
                            getDocumentSymbol(DOCUMENT_INTERFACE_NAME), () -> {
                                writer.write("return $T(v)",
                                        getInternalDocumentSymbol(INTERNAL_NEW_DOCUMENT_MARSHALER_FUNC));
                            })
                    .write("");
        });
    }

    private void generateNoSerdeType() {
        Symbol noSerde = SymbolUtils.createValueSymbolBuilder("NoSerde",
                SmithyGoDependency.SMITHY_DOCUMENT).build();

        delegator.useShapeWriter(settings.getService(model), writer -> {
            writer.write("type $L = $T", NO_DOCUMENT_SERDE_TYPE_NAME, noSerde);
        });

        delegator.useFileWriter("./types/types.go", settings.getModuleName() + "/types", writer -> {
            writer.write("type $L = $T", NO_DOCUMENT_SERDE_TYPE_NAME, noSerde);
        });
    }

    private void generateInternalDocumentInterface() {
        if (!hasDocumentShapes()) {
            return;
        }

        writeInternalDocumentPackage("document.go", writer -> {
            Symbol serviceSmithyDocumentInterface = getInternalDocumentSymbol(SERVICE_SMITHY_DOCUMENT_INTERFACE);

            writer.openBlock("type $T interface {", "}", serviceSmithyDocumentInterface,
                            () -> writer.write("$L()", IS_SMITHY_DOCUMENT_METHOD))
                    .write("");

            Symbol internalDocumentInterface = getInternalDocumentSymbol(DOCUMENT_INTERFACE_NAME);
            Symbol smithyDocumentMarshaler = SymbolUtils.createValueSymbolBuilder("SmithyDocumentMarshaler",
                    SmithyGoDependency.SMITHY_DOCUMENT).build();
            Symbol smithyDocumentUnmarshaler = SymbolUtils.createValueSymbolBuilder("SmithyDocumentUnmarshaler",
                    SmithyGoDependency.SMITHY_DOCUMENT).build();

            writer.openBlock("type $T interface {", "}", internalDocumentInterface, () -> {
                writer.write("$T", serviceSmithyDocumentInterface);
                writer.write("$T", smithyDocumentMarshaler);
                writer.write("$T", smithyDocumentUnmarshaler);
            }).write("");

            writer.write("var _ $T = ($P)(nil)",
                    serviceSmithyDocumentInterface, internalDocumentInterface);
            writer.write("var _ $T = ($P)(nil)",
                    smithyDocumentMarshaler, internalDocumentInterface);
            writer.write("var _ $T = ($P)(nil)",
                    smithyDocumentUnmarshaler,
                    internalDocumentInterface);
            writer.write("");
        });
    }

    /**
     * Generates the internal document Go package for the service client. Delegates the logic for document marshaling
     * and unmarshalling types to the provided protocol generator using the given context.
     *
     * @param protocolGenerator the protocol generator.
     * @param context           the protocol generator context.
     */
    public void generateInternalDocumentTypes(ProtocolGenerator protocolGenerator, GenerationContext context) {
        if (!hasDocumentShapes()) {
            return;
        }

        delegator.useFileWriter(getInternalDocumentFilePath("document.go"), getInternalDocumentPackage(), writer -> {
            Symbol marshalerSymbol = getInternalDocumentSymbol("documentMarshaler",
                    true);
            Symbol unmarshalerSymbol = getInternalDocumentSymbol("documentUnmarshaler", true);

            Symbol isDocumentInterface = getInternalDocumentSymbol(INTERNAL_IS_DOCUMENT_INTERFACE);

            writeInternalDocumentImplementation(
                    writer,
                    marshalerSymbol,
                    () -> {
                        protocolGenerator.generateProtocolDocumentMarshalerUnmarshalDocument(context.toBuilder()
                                .writer(writer)
                                .build());
                    },
                    () -> {
                        protocolGenerator.generateProtocolDocumentMarshalerMarshalDocument(context.toBuilder()
                                .writer(writer)
                                .build());
                    });
            writeInternalDocumentImplementation(writer,
                    unmarshalerSymbol,
                    () -> {
                        protocolGenerator.generateProtocolDocumentUnmarshalerUnmarshalDocument(context.toBuilder()
                                .writer(writer)
                                .build());
                    },
                    () -> {
                        protocolGenerator.generateProtocolDocumentUnmarshalerMarshalDocument(context.toBuilder()
                                .writer(writer)
                                .build());
                    });

            Symbol documentInterfaceSymbol = getInternalDocumentSymbol(DOCUMENT_INTERFACE_NAME);

            writer.openBlock("func $L(v interface{}) $T {", "}", INTERNAL_NEW_DOCUMENT_MARSHALER_FUNC,
                    documentInterfaceSymbol, () -> {
                        protocolGenerator.generateNewDocumentMarshaler(context.toBuilder()
                                .writer(writer)
                                .build(), marshalerSymbol);
                    }).write("");

            writer.openBlock("func $L(v interface{}) $T {", "}", INTERNAL_NEW_DOCUMENT_UNMARSHALER_FUNC,
                    documentInterfaceSymbol, () -> {
                        protocolGenerator.generateNewDocumentUnmarshaler(context.toBuilder()
                                .writer(writer)
                                .build(), unmarshalerSymbol);
                    }).write("");

            writer.openBlock("func $T(v Interface) (ok bool) {", "}", isDocumentInterface, () -> {
                writer.openBlock("defer func() {", "}()", () -> {
                    writer.openBlock("if err := recover(); err != nil {", "}", () -> writer.write("ok = false"));
                });
                writer.write("v.$L()", IS_SMITHY_DOCUMENT_METHOD);
                writer.write("return true");
            }).write("");
        });
    }

    private void writeInternalDocumentImplementation(
            GoWriter writer,
            Symbol typeSymbol,
            Runnable unmarshalMethodDefinition,
            Runnable marshalMethodDefinition
    ) {
        writer.openBlock("type $T struct {", "}", typeSymbol, () -> {
            writer.write("value interface{}");
        });
        writer.write("");

        writer.openBlock("func (m $P) $L(v interface{}) error {", "}", typeSymbol, UNMARSHAL_SMITHY_DOCUMENT_METHOD,
                unmarshalMethodDefinition);
        writer.write("");

        writer.openBlock("func (m $P) $L() ([]byte, error) {", "}", typeSymbol, MARSHAL_SMITHY_DOCUMENT_METHOD,
                marshalMethodDefinition);
        writer.write("");

        writer.write("func (m $P) $L() {}", typeSymbol, IS_SMITHY_DOCUMENT_METHOD);
        writer.write("");

        writer.write("var _ $T = ($P)(nil)", getInternalDocumentSymbol(DOCUMENT_INTERFACE_NAME, true), typeSymbol);
        writer.write("");
    }

    private void writeDocumentPackage(String fileName, Consumer<GoWriter> writerConsumer) {
        delegator.useFileWriter(getDocumentFilePath(fileName), getDocumentPackage(), writerConsumer);
    }

    private void writeInternalDocumentPackage(String fileName, Consumer<GoWriter> writerConsumer) {
        delegator.useFileWriter(getInternalDocumentFilePath(fileName), getInternalDocumentPackage(), writerConsumer);
    }

    private String getInternalDocumentPackage() {
        return Utilities.getInternalDocumentPackage(settings);
    }

    private String getDocumentPackage() {
        return Utilities.getDocumentPackage(settings);
    }

    private String getInternalDocumentFilePath(String fileName) {
        return "./internal/document/" + fileName;
    }

    private String getDocumentFilePath(String fileName) {
        return "./document/" + fileName;
    }

    private Symbol getDocumentSymbol(String typeName) {
        return getDocumentSymbol(typeName, false);
    }

    private Symbol getDocumentSymbol(String typeName, boolean pointable) {
        return Utilities.getDocumentSymbolBuilder(settings, typeName, pointable).build();
    }

    private Symbol getInternalDocumentSymbol(String typeName) {
        return getInternalDocumentSymbol(typeName, false);
    }

    private Symbol getInternalDocumentSymbol(String typeName, boolean pointable) {
        return Utilities.getInternalDocumentSymbolBuilder(settings, typeName, pointable).build();
    }

    /**
     * Collection of helper utility functions for creating references to the service client's internal
     * and external document package types.
     */
    public static final class Utilities {
        /**
         * Create a non-pointable {@link Symbol.Builder} for typeName in the service's document package.
         *
         * @param settings the Smithy Go settings.
         * @param typeName the name of the Go type.
         * @return the symbol builder.
         */
        public static Symbol.Builder getDocumentSymbolBuilder(GoSettings settings, String typeName) {
            return getDocumentSymbolBuilder(settings, typeName, false);
        }

        /**
         * Create {@link Symbol.Builder} for typeName in the service's document package.
         *
         * @param settings  the Smithy Go settings.
         * @param typeName  the name of the Go type.
         * @param pointable whether typeName is pointable.
         * @return the symbol builder.
         */
        public static Symbol.Builder getDocumentSymbolBuilder(
                GoSettings settings,
                String typeName,
                boolean pointable
        ) {
            return pointable
                    ? SymbolUtils.createPointableSymbolBuilder(typeName, getDocumentPackage(settings))
                    : SymbolUtils.createValueSymbolBuilder(typeName, getDocumentPackage(settings));
        }

        /**
         * Create a non-pointable {@link Symbol.Builder} for typeName in the service's internal document package.
         *
         * @param settings the Smithy Go settings.
         * @param typeName the name of the Go type.
         * @return the symbol builder.
         */
        public static Symbol.Builder getInternalDocumentSymbolBuilder(GoSettings settings, String typeName) {
            return getInternalDocumentSymbolBuilder(settings, typeName, false);
        }

        /**
         * Create {@link Symbol.Builder} for typeName in the service's internal document package.
         *
         * @param settings  the Smithy Go settings.
         * @param typeName  the name of the Go type.
         * @param pointable whether typeName is pointable.
         * @return the symbol builder.
         */
        public static Symbol.Builder getInternalDocumentSymbolBuilder(
                GoSettings settings,
                String typeName,
                boolean pointable
        ) {
            Symbol.Builder builder = pointable
                    ? SymbolUtils.createPointableSymbolBuilder(typeName, getInternalDocumentPackage(settings))
                    : SymbolUtils.createValueSymbolBuilder(typeName, getInternalDocumentPackage(settings));
            builder.putProperty(SymbolUtils.NAMESPACE_ALIAS, "internaldocument");
            return builder;
        }

        private static String getInternalDocumentPackage(GoSettings settings) {
            return settings.getModuleName() + "/internal/document";
        }

        private static String getDocumentPackage(GoSettings settings) {
            return settings.getModuleName() + "/document";
        }
    }
}
