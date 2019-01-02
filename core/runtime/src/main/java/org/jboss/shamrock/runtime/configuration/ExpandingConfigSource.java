package org.jboss.shamrock.runtime.configuration;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.wildfly.common.expression.Expression;

/**
 * A value-expanding configuration source, which allows (limited) recursive expansion.
 */
public class ExpandingConfigSource extends AbstractDelegatingConfigSource {
    // this is a cache of compiled expressions, NOT a cache of expanded values
    private final ConcurrentHashMap<String, Expression> exprCache = new ConcurrentHashMap<>();

    /**
     * Construct a new instance.
     *
     * @param delegate the delegate config source (must not be {@code null})
     */
    public ExpandingConfigSource(final ConfigSource delegate) {
        super(delegate);
    }

    public Set<String> getPropertyNames() {
        return delegate.getPropertyNames();
    }

    public String getValue(final String propertyName) {
        return expand(delegate.getValue(propertyName));
    }

    String expand(final String value) {
        final Expression compiled = exprCache.computeIfAbsent(value, str -> Expression.compile(str, Expression.Flag.ESCAPES, Expression.Flag.LENIENT_SYNTAX));
        return compiled.evaluate(ConfigExpander.INSTANCE);
    }

    public void flush() {
        exprCache.clear();
    }
}
