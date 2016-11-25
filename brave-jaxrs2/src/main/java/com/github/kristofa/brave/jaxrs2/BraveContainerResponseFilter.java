package com.github.kristofa.brave.jaxrs2;

import com.github.kristofa.brave.Brave;
import com.github.kristofa.brave.ServerResponseAdapter;
import com.github.kristofa.brave.ServerResponseInterceptor;
import com.github.kristofa.brave.TagExtractor;
import com.github.kristofa.brave.http.HttpServerResponseAdapter;
import com.github.kristofa.brave.http.HttpResponse;
import javax.annotation.Priority;
import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;

import static com.github.kristofa.brave.internal.Util.checkNotNull;

/**
 * Intercepts outgoing container responses and sends ss annotations.
 */
@Provider
@Priority(0)
public class BraveContainerResponseFilter implements ContainerResponseFilter {

    /** Creates a tracing filter with defaults. Use {@link #builder(Brave)} to customize. */
    public static BraveContainerResponseFilter create(Brave brave) {
        return new Builder(brave).build();
    }

    public static Builder builder(Brave brave) {
        return new Builder(brave);
    }

    public static final class Builder implements TagExtractor.Config<Builder> {
        final Brave brave;
        final HttpServerResponseAdapter.FactoryBuilder responseFactoryBuilder
            = HttpServerResponseAdapter.factoryBuilder();

        Builder(Brave brave) { // intentionally hidden
            this.brave = checkNotNull(brave, "brave");
        }

        @Override public Builder addKey(String key) {
            responseFactoryBuilder.addKey(key);
            return this;
        }

        @Override
        public Builder addValueParserFactory(TagExtractor.ValueParserFactory factory) {
            responseFactoryBuilder.addValueParserFactory(factory);
            return this;
        }

        public BraveContainerResponseFilter build() {
            return new BraveContainerResponseFilter(this);
        }
    }

    private final ServerResponseInterceptor responseInterceptor;
    private final ServerResponseAdapter.Factory<HttpResponse> responseAdapterFactory;

    BraveContainerResponseFilter(Builder b) { // intentionally hidden
        this.responseInterceptor = b.brave.serverResponseInterceptor();
        this.responseAdapterFactory = b.responseFactoryBuilder.build(HttpResponse.class);
    }

    @Inject // internal dependency-injection constructor
    BraveContainerResponseFilter(Brave brave) {
        this(builder(brave));
    }

    /**
     * @deprecated please use {@link #create(Brave)} or {@link #builder(Brave)}
     */
    @Deprecated
    public BraveContainerResponseFilter(ServerResponseInterceptor responseInterceptor) {
        this.responseInterceptor = responseInterceptor;
        this.responseAdapterFactory = HttpServerResponseAdapter.factoryBuilder()
            .build(HttpResponse.class);
    }

    @Override
    public void filter(final ContainerRequestContext containerRequestContext, final ContainerResponseContext containerResponseContext) throws IOException {

        HttpResponse httpResponse = new HttpResponse() {

            @Override
            public int getHttpStatusCode() {
                return containerResponseContext.getStatus();
            }
        };

        ServerResponseAdapter adapter = responseAdapterFactory.create(httpResponse);
        responseInterceptor.handle(adapter);
    }
}
