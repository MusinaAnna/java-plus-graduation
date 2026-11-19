package ru.practicum.statistics.client.exception;

import ru.practicum.common.exception.ServiceUnavailableException;

public class StatsServerUnavailable extends ServiceUnavailableException {
    public StatsServerUnavailable(String message) {
        super(message);
    }

    public StatsServerUnavailable(String message, Throwable cause) {
        super(message, cause);
    }
}
