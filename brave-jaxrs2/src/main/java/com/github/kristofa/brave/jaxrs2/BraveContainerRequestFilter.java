package com.github.kristofa.brave.jaxrs2;

import com.github.kristofa.brave.Brave;
import com.github.kristofa.brave.ServerRequestAdapter;
import com.github.kristofa.brave.ServerRequestInterceptor;
import com.github.kristofa.brave.TagExtractor;
import com.github.kristofa.brave.http.HttpServerRequestAdapter;
import com.github.kristofa.brave.http.SpanNameProvider;

import java.io.IOException;

import javax.inject.Inject;
import javax.annotation.Priority;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.ext.Provider;

import static com.github.kristofa.brave.internal.Util.checkNotNull;

/**
 * Intercepts incoming container requests and extracts any trace information from the request header
 * Also sends sr annotations.
 */
@Provider
@PreMatching
@Priority(0)
public class BraveContainerRequestFilter implements ContainerRequestFilter {

    /** Creates a tracing filter with defaults. Use {@link #builder(Brave)} to customize. */
    public static BraveContainerRequestFilter create(Brave brave) {
        return new Builder(brave).build();
    }

    public static Builder builder(Brave brave) {
        return new Builder(brave);
    }

    public static final class Builder implements TagExtractor.Config<Builder> {
        final Brave brave;
        final HttpServerRequestAdapter.FactoryBuilder requestFactoryBuilder
            = HttpServerRequestAdapter.factoryBuilder();

        Builder(Brave brave) { // intentionally hidden
            this.brave = checkNotNull(brave, "brave");
        }

        public Builder spanNameProvider(SpanNameProvider spanNameProvider) {
            requestFactoryBuilder.spanNameProvider(spanNameProvider);
            return this;
        }

        @Override public Builder addKey(String key) {
            requestFactoryBuilder.addKey(key);
            return this;
        }

        @Override
        public Builder addValueParserFactory(TagExtractor.ValueParserFactory factory) {
            requestFactoryBuilder.addValueParserFactory(factory);
            return this;
        }

        public BraveContainerRequestFilter build() {
            return new BraveContainerRequestFilter(this);
        }
    }

    private final ServerRequestInterceptor requestInterceptor;
    private final ServerRequestAdapter.Factory<JaxRs2HttpServerRequest> requestAdapterFactory;

    BraveContainerRequestFilter(Builder b) { // intentionally hidden
        this.requestInterceptor = b.brave.serverRequestInterceptor();
        this.requestAdapterFactory = b.requestFactoryBuilder.build(JaxRs2HttpServerRequest.class);
    }

    @Inject // internal dependency-injection constructor
    BraveContainerRequestFilter(Brave brave, SpanNameProvider spanNameProvider) {
        this(builder(brave).spanNameProvider(spanNameProvider));
    }

    /**
     * @deprecated please use {@link #create(Brave)} or {@link #builder(Brave)}
     */
    @Deprecated
    public BraveContainerRequestFilter(ServerRequestInterceptor interceptor, SpanNameProvider spanNameProvider) {
        this.requestInterceptor = interceptor;
        this.requestAdapterFactory = HttpServerRequestAdapter.factoryBuilder()
            .spanNameProvider(spanNameProvider)
            .build(JaxRs2HttpServerRequest.class);
    }

    @Override
    public void filter(ContainerRequestContext containerRequestContext) throws IOException {
        ServerRequestAdapter adapter =
            requestAdapterFactory.create(new JaxRs2HttpServerRequest(containerRequestContext));
        requestInterceptor.handle(adapter);
    }

}
