package com.coffeeproject.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "잘못된 입력값입니다."),
    INVALID_REQUEST_BODY(HttpStatus.BAD_REQUEST, "요청 본문이 올바르지 않습니다."),
    INVALID_CHARGE_AMOUNT(HttpStatus.BAD_REQUEST, "충전 금액은 0보다 커야 합니다."),
    INSUFFICIENT_POINT(HttpStatus.BAD_REQUEST, "포인트가 부족합니다."),
    MENU_NOT_ORDERABLE(HttpStatus.BAD_REQUEST, "판매 가능한 메뉴가 아닙니다."),

    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    MENU_NOT_FOUND(HttpStatus.NOT_FOUND, "메뉴를 찾을 수 없습니다."),
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "주문을 찾을 수 없습니다."),

    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}
