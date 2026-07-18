package com.jnimble.admin.hook;

import java.util.stream.Collectors;
import org.springframework.beans.factory.ObjectProvider;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.processor.element.AbstractElementTagProcessor;
import org.thymeleaf.processor.element.IElementTagStructureHandler;
import org.thymeleaf.templatemode.TemplateMode;

/**
 * Thymeleaf element processor that handles the {@code jn:hook} element.
 * Renders plugin-contributed views for the specified hook name.
 */
class AdminHookElementProcessor extends AbstractElementTagProcessor {

    private static final String ELEMENT_NAME = "hook";
    private static final int PRECEDENCE = 1000;

    private final ObjectProvider<AdminHookViewService> hookViewServiceProvider;

    AdminHookElementProcessor(
            String dialectPrefix,
            ObjectProvider<AdminHookViewService> hookViewServiceProvider
    ) {
        super(TemplateMode.HTML, dialectPrefix, ELEMENT_NAME, true, null, false, PRECEDENCE);
        this.hookViewServiceProvider = hookViewServiceProvider;
    }

    /**
     * Processes the {@code jn:hook} element and replaces it with rendered hook content.
     *
     * <p>Extracts the hook name from the element's {@code name} attribute, resolves
     * and renders all matching hook views, and replaces the element with the
     * concatenated HTML output.</p>
     *
     * @param context          the Thymeleaf template context
     * @param tag              the processable element tag
     * @param structureHandler the structure handler for replacing the element
     */
    @Override
    protected void doProcess(
            ITemplateContext context,
            IProcessableElementTag tag,
            IElementTagStructureHandler structureHandler
    ) {
        String hookName = tag.getAttributeValue("name");
        if (hookName == null || hookName.isBlank()) {
            structureHandler.removeElement();
            return;
        }

        String html = hookViewServiceProvider.getObject().render(hookName.trim(), context).stream()
                .map(AdminHookViewService.RenderedAdminHookView::html)
                .collect(Collectors.joining());
        structureHandler.replaceWith(html, false);
    }
}
