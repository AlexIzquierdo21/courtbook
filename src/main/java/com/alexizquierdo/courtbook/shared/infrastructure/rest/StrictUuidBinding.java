package com.alexizquierdo.courtbook.shared.infrastructure.rest;

import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.InitBinder;

import java.beans.PropertyEditorSupport;
import java.util.UUID;
import java.util.regex.Pattern;

// UUID.fromString is lenient: it accepts "1-2-3-4-5" or "+0000001-0000-0000-0000-000000000000".
// Doc 04 wants a 400 for anything that is not a real UUID in path variables, headers (X-User-Id) and params.
// It is a custom editor, not a Converter: when a Converter fails, Spring retries with its lenient UUIDEditor.
@ControllerAdvice
public class StrictUuidBinding {

    private static final Pattern CANONICAL =
            Pattern.compile("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");

    @InitBinder
    void strictUuids(WebDataBinder binder) {
        binder.registerCustomEditor(UUID.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) {
                if (!CANONICAL.matcher(text).matches()) {
                    throw new IllegalArgumentException("Not a valid UUID: " + text);
                }
                setValue(UUID.fromString(text));
            }
        });
    }
}
