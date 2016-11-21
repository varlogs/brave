package com.github.kristofa.brave.httpclient;

import com.github.kristofa.brave.Brave;
import com.github.kristofa.brave.ClientRequestInterceptor;
import com.github.kristofa.brave.ClientRequestAdapter;
import com.github.kristofa.brave.TagExtractor;
import com.github.kristofa.brave.http.HttpClientRequestAdapter;
import com.github.kristofa.brave.http.SpanNameProvider;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.protocol.HttpContext;

import static com.github.kristofa.brave.internal.Util.checkNotNull;

/**
 * Apache http client request interceptor.
 */
public class BraveHttpRequestInterceptor implements HttpRequestInterceptor {

    /** Creates a tracing interceptor with defaults. Use {@link #builder(Brave)} to customize. */
    public static BraveHttpRequestInterceptor create(Brave brave) {
        return new Builder(brave).build();
    }

    public static Builder builder(Brave brave) {
        return new Builder(brave);
    }

    public static final class Builder implements TagExtractor.Config<Builder> {
        final Brave brave;
        final HttpClientRequestAdapter.FactoryBuilder requestFactoryBuilder
            = HttpClientRequestAdapter.factoryBuilder();

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

        public BraveHttpRequestInterceptor build() {
            return new BraveHttpRequestInterceptor(this);
        }
    }

    private final ClientRequestInterceptor requestInterceptor;
    private final ClientRequestAdapter.Factory<HttpClientRequestImpl> requestAdapterFactory;

    BraveHttpRequestInterceptor(Builder b) { // intentionally hidden
        this.requestInterceptor = b.brave.clientRequestInterceptor();
        this.requestAdapterFactory = b.requestFactoryBuilder.build(HttpClientRequestImpl.class);
    }

    /**
     * @deprecated please use {@link #create(Brave)} or {@link #builder(Brave)}
     */
    @Deprecated
    public BraveHttpRequestInterceptor(ClientRequestInterceptor requestInterceptor, SpanNameProvider spanNameProvider) {
        this.requestInterceptor = requestInterceptor;
        this.requestAdapterFactory = HttpClientRequestAdapter.factoryBuilder()
            .spanNameProvider(spanNameProvider)
            .build(HttpClientRequestImpl.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void process(final HttpRequest request, final HttpContext context) {
        ClientRequestAdapter adapter =
            requestAdapterFactory.create(new HttpClientRequestImpl(request));
        requestInterceptor.handle(adapter);
    }
}
