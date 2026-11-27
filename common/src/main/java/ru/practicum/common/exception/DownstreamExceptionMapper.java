package ru.practicum.common.exception;

public final class DownstreamExceptionMapper {

    private DownstreamExceptionMapper() {
    }

    public static RuntimeException map(String serviceName, Throwable cause) {
        Throwable current = cause;
        while (current != null) {
            if (current instanceof BadRequestException
                    || current instanceof ConflictException
                    || current instanceof ForbiddenException
                    || current instanceof NotFoundException
                    || current instanceof ValidationException
                    || current instanceof ServiceUnavailableException) {
                return (RuntimeException) current;
            }
            current = current.getCause();
        }

        return new ServiceUnavailableException(
                "Downstream service '" + serviceName + "' is temporarily unavailable",
                cause
        );
    }
}
