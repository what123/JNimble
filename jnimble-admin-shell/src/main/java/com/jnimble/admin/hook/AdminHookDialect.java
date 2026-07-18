package com.jnimble.admin.hook;

import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.thymeleaf.dialect.AbstractProcessorDialect;
import org.thymeleaf.processor.IProcessor;
import org.thymeleaf.standard.StandardDialect;

/**
 * Thymeleaf dialect for the JNimble admin hook system.
 * Registers the {@code jn:hook} element processor for rendering plugin-contributed
 * views in admin pages.
 */
public class AdminHookDialect extends AbstractProcessorDialect {

    /** The dialect prefix used in Thymeleaf templates (e.g., {@code jn:hook}). */
    public static final String PREFIX = "jn";

    private final ObjectProvider<AdminHookViewService> hookViewServiceProvider;

    public AdminHookDialect(ObjectProvider<AdminHookViewService> hookViewServiceProvider) {
        super("JNimble Admin Hook Dialect", PREFIX, StandardDialect.PROCESSOR_PRECEDENCE);
        this.hookViewServiceProvider = hookViewServiceProvider;
    }

    @Override
    public Set<IProcessor> getProcessors(String dialectPrefix) {
        return Set.of(new AdminHookElementProcessor(dialectPrefix, hookViewServiceProvider));
    }
}
