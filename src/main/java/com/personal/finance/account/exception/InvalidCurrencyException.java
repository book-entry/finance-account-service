package com.personal.finance.account.exception;

import com.personal.finance.common.exception.BaseException;
import com.personal.finance.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

/**
 * Raised when the supplied currency does not parse as a valid ISO 4217 code.
 * Spec §3.1: "validate currency is a valid ISO 4217 code".
 */
public class InvalidCurrencyException extends BaseException {

    public InvalidCurrencyException(String currency) {
        super(ErrorCode.INVALID_CURRENCY, HttpStatus.BAD_REQUEST,
                "Currency '" + currency + "' is not a valid ISO 4217 code");
    }
}
