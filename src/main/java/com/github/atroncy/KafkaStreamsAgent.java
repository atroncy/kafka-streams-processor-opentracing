package com.github.atroncy;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;
import org.apache.kafka.streams.processor.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.lang.instrument.Instrumentation;
import java.util.Properties;
import java.util.regex.Pattern;

public class KafkaStreamsAgent {

    private static final Logger LOG = LoggerFactory.getLogger(KafkaStreamsAgent.class);
    private static final String GROUP_ID = "groupId";
    private static final String HEADER_FILTER = "headerFilter";

    public static void premain(String arguments,
                               Instrumentation instrumentation)
    {
        Properties properties = new Properties();
        if (arguments == null) {
            LOG.debug("No properties provided");
        } else {
            try {
                properties.load(new FileReader(arguments));
            } catch (IOException e) {
                LOG.warn("", e);
            }
        }
        ElementMatcher.Junction<TypeDescription> elementMatcher = ElementMatchers.hasSuperType(ElementMatchers.named(Processor.class.getName()));
        if (properties.containsKey(GROUP_ID)) {
            elementMatcher.and(ElementMatchers.nameStartsWith(properties.getProperty(GROUP_ID)));
        }
        if (properties.containsKey(HEADER_FILTER)) {
            final Pattern pattern = Pattern.compile(properties.getProperty(HEADER_FILTER));
            KafkaStreamsProcessorInterceptor.filterKey = pattern.asPredicate();
        }
        AgentBuilder.Identified identified = new AgentBuilder.Default().type(elementMatcher);
        identified.transform((builder, type, classLoader, module) ->
                builder.method(ElementMatchers.named("processor"))
                        .intercept(MethodDelegation.to(KafkaStreamsProcessorInterceptor.class))
            )
            .installOn(instrumentation);
        identified.transform((builder, typeDescription, classLoader, module) ->
                builder.method(ElementMatchers.named("init"))
                        .intercept(MethodDelegation.to(KafkaStreamsProcessorInterceptor.class))
            )
            .installOn(instrumentation);

    }
}
