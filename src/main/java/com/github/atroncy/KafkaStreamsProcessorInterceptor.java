package com.github.atroncy;

import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.log.Fields;
import io.opentracing.tag.Tags;
import io.opentracing.util.GlobalTracer;
import net.bytebuddy.implementation.bind.annotation.Super;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.streams.processor.Processor;
import org.apache.kafka.streams.processor.ProcessorContext;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Predicate;

public class KafkaStreamsProcessorInterceptor {

    private static ProcessorContext context;
    static Predicate<String> filterKey;

    public static void interceptContext(ProcessorContext context, @Super Processor zuper) {
        KafkaStreamsProcessorInterceptor.context = context;
        // give back context to owner
        zuper.init(context);
    }

    public static void intercept(@SuperCall Callable<?> zuper) throws Exception {
        Tracer tracer = GlobalTracer.get();
        Tracer.SpanBuilder spanBuilder = tracer.buildSpan("stream-process");
        for (Header header : context.headers()) {
            if (filterKey != null) {
                if (filterKey.negate().test(header.key())) {
                    continue;
                }
            }
            spanBuilder.withTag(header.key(), new String(header.value(), Charset.defaultCharset()));
        }
        Span span = spanBuilder.start();
        try (Scope ignored = tracer.scopeManager().activate(span, false)) {
            zuper.call();
        } catch (Exception e) {
            Tags.ERROR.set(span, true);
            Map<String, Object> map = new HashMap<>();
            map.put(Fields.EVENT, "error");
            map.put(Fields.ERROR_KIND, e);
            map.put(Fields.MESSAGE, e.getMessage());
            span.log(map);
        } finally {
            span.finish();
        }
    }

}
