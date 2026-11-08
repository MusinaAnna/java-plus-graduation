package ru.practicum.common.enums;

/**
 * Действия администратора над статусом события.
 * Используется при модерации событий.
 */
public enum AdminStateAction {

    /**
     * Опубликовать событие.
     * Доступно только для событий в статусе PENDING.
     */
    PUBLISH_EVENT,

    /**
     * Отклонить событие.
     * Доступно только для событий в статусе PENDING.
     */
    REJECT_EVENT
}