package com.rasitbank.assistant.suggestion.domain.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Öneri listesinin alanlar arası kurallarını (toplam soru sınırı, benzersiz metinler, takip ile soru farkı) işaretler.
 */
@Documented
@Constraint(validatedBy = SuggestionSetValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidSuggestionSet {

    String message() default "invalid suggestion set";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
