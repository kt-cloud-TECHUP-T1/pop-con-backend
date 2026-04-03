package com.t1.popcon.ticket.validation;

import com.t1.popcon.ticket.domain.TicketSourceType;
import com.t1.popcon.ticket.dto.request.TicketIssueRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ConsistentSourceReservationValidator
    implements ConstraintValidator<ConsistentSourceReservation, TicketIssueRequest> {

    @Override
    public boolean isValid(TicketIssueRequest value, ConstraintValidatorContext context) {
        if (value == null || value.sourceType() == null) {
            return true;
        }

        String reservationNo = value.reservationNo();
        boolean hasReservationNo = reservationNo != null && !reservationNo.isBlank();

        if (value.sourceType() == TicketSourceType.AUCTION && !hasReservationNo) {
            return addViolation(context, "reservationNo is required when sourceType is AUCTION");
        }

        if (value.sourceType() == TicketSourceType.DRAW && hasReservationNo) {
            return addViolation(context, "reservationNo must be blank when sourceType is DRAW");
        }

        return true;
    }

    private boolean addViolation(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message)
            .addPropertyNode("reservationNo")
            .addConstraintViolation();
        return false;
    }
}
