package com.personal.finance.account.exception;

import com.personal.finance.common.exception.BaseException;
import com.personal.finance.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

/**
 * Raised when an account lookup yields no row OR the row belongs to a
 * different user. Spec §3.1 explicitly forbids revealing existence to
 * non-owners, so this exception is used in both cases.
 */
public class AccountNotFoundException extends BaseException {

    public AccountNotFoundException(String message) {
        super(ErrorCode.ACCOUNT_NOT_FOUND, HttpStatus.NOT_FOUND, message);
    }
}
