package ru.practicum.common.exception.controller.api;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import ru.practicum.common.exception.*;

import java.time.format.DateTimeFormatter;

/**
 * API для глобальной обработки ошибок и исключений.
 * Предоставляет централизованные обработчики для всех типов ошибок приложения.
 */
public interface ErrorHandlerApi {

    DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Обработчик исключения "Объект не найден".
     *
     * @param e исключение NotFoundException
     * @return ApiError с деталями ошибки
     */
    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    ApiError handleNotFound(NotFoundException e);

    /**
     * Обработчик исключения "Некорректный запрос".
     *
     * @param e исключение BadRequestException
     * @return ApiError с деталями ошибки
     */
    @ExceptionHandler(BadRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ApiError handleBadRequest(BadRequestException e);

    /**
     * Обработчик исключения "Конфликт данных".
     *
     * @param e исключение ConflictException
     * @return ApiError с деталями ошибки
     */
    @ExceptionHandler(ConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    ApiError handleConflict(ConflictException e);

    /**
     * Обработчик исключения "Доступ запрещен".
     *
     * @param e исключение ForbiddenException
     * @return ApiError с деталями ошибки
     */
    @ExceptionHandler(ForbiddenException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    ApiError handleForbidden(ForbiddenException e);

    /**
     * Обработчик ошибок валидации аргументов метода.
     *
     * @param e исключение MethodArgumentNotValidException
     * @return ApiError с деталями ошибки
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ApiError handleValidation(MethodArgumentNotValidException e);

    /**
     * Обработчик ошибок отсутствия обязательных параметров запроса.
     *
     * @param e исключение MissingServletRequestParameterException
     * @return ApiError с деталями ошибки
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ApiError handleMissingParams(MissingServletRequestParameterException e);

    /**
     * Обработчик всех непредвиденных исключений.
     *
     * @param e исключение Exception
     * @return ApiError с деталями ошибки
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    ApiError handleException(Exception e);

    /**
     * Обработчик ошибок преобразования типов аргументов метода.
     *
     * @param e исключение MethodArgumentTypeMismatchException
     * @return ApiError с деталями ошибки
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ApiError handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException e);

    /**
     * Обработчик ошибок нарушения ограничений валидации.
     *
     * @param e исключение ConstraintViolationException
     * @return ApiError с деталями ошибки
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ApiError handleConstraintViolation(ConstraintViolationException e);
}