package com.github.kristofa.brave.jaxrs2;

import com.github.kristofa.brave.Brave;
import com.github.kristofa.brave.ClientResponseAdapter;
import com.github.kristofa.brave.ClientResponseInterceptor;
import com.github.kristofa.brave.TagExtractor;
import com.github.kristofa.brave.http.HttpClientResponseAdapter;
import com.github.kristofa.brave.http.SpanNameProvider;
import javax.annotation.Priority;
import javax.inject.Inject;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;

import static com.github.kristofa.brave.internal.Util.checkNotNull;

/**
 * Intercepts JAX-RS 2 client responses and sends cr annotations. Also submits the completed span.
 */
@Provider
@Priority(0)
public class BraveClientResponseFilter implements ClientResponseFilter {

    /** Creates a tracing filter with defaults. Use {@link #builder(Brave)} to customize. */
    public static BraveClientResponseFilter create(Brave brave) {
        return new Builder(brave).build();
    }

    public static Builder builder(Brave brave) {
        return new Builder(brave);
    }

    public static final class Builder implements TagExtractor.Config<Builder> {
        final Brave brave;
        final HttpClientResponseAdapter.FactoryBuilder responseFactoryBuilder
            = HttpClientResponseAdapter.factoryBuilder();

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

        public BraveClientResponseFilter build() {
            return new BraveClientResponseFilter(this);
        }
    }

    private final ClientResponseInterceptor responseInterceptor;
    private final ClientResponseAdapter.Factory<JaxRs2HttpResponse> responseAdapterFactory;

    BraveClientResponseFilter(Builder b) { // intentionally hidden
        this.responseInterceptor = b.brave.clientResponseInterceptor();
        this.responseAdapterFactory = b.responseFactoryBuilder.build(JaxRs2HttpResponse.class);

    }

    @Inject // internal dependency-injection constructor
    BraveClientResponseFilter(Brave brave, SpanNameProvider spanNameProvider) {
        this(builder(brave));
    }

    /**
     * @deprecated please use {@link #create(Brave)} or {@link #builder(Brave)}
     */
    @Deprecated
    public BraveClientResponseFilter(ClientResponseInterceptor responseInterceptor) {
        this.responseInterceptor = responseInterceptor;
        this.responseAdapterFactory = HttpClientResponseAdapter.factoryBuilder()
            .build(JaxRs2HttpResponse.class);
    }

    @Override
    public void filter(ClientRequestContext clientRequestContext, ClientResponseContext clientResponseContext) throws IOException {
        ClientResponseAdapter adapter =
            responseAdapterFactory.create(new JaxRs2HttpResponse(clientResponseContext));
        responseInterceptor.handle(adapter);
    }
}
