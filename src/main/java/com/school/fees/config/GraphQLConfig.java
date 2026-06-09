package com.school.fees.config;

import graphql.schema.Coercing;
import graphql.schema.GraphQLScalarType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

/**
 * GraphQL configuration class registering Federation directive mapping and custom scalar resolver wiring for Fees Service.
 */
@Configuration
public class GraphQLConfig {

    @Bean
    public RuntimeWiringConfigurer runtimeWiringConfigurer() {
        GraphQLScalarType anyScalar = GraphQLScalarType.newScalar()
                .name("_Any")
                .description("Federation _Any scalar")
                .coercing(new Coercing<Object, Object>() {
                    @Override
                    public Object serialize(Object dataFetcherResult) {
                        return dataFetcherResult;
                    }

                    @Override
                    public Object parseValue(Object input) {
                        return input;
                    }

                    @Override
                    public Object parseLiteral(Object input) {
                        return input;
                    }
                })
                .build();

        return wiringBuilder -> wiringBuilder
                .scalar(anyScalar)
                .type("_Entity", typeWiring -> typeWiring.typeResolver(env -> {
                    Object src = env.getObject();
                    if (src instanceof com.school.fees.controller.FeesGraphQLController.StudentRepresentation) {
                        return env.getSchema().getObjectType("Student");
                    }
                    return null;
                }));
    }
}
