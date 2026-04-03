package com.t1.popcon.ticket.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ConsistentSourceReservationValidator.class)
public @interface ConsistentSourceReservation {

    String message() default "sourceType and reservationNo are inconsistent";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
