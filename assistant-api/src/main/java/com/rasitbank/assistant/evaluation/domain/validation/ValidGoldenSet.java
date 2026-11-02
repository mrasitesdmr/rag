package com.rasitbank.assistant.evaluation.domain.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Golden set'in alanlar arası kurallarını (kırpılmış cevap cümlesi uzunluğu, benzersiz soru metinleri) işaretler.
 */
@Documented
@Constraint(validatedBy = GoldenSetValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidGoldenSet {

    String message() default "invalid golden set";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
