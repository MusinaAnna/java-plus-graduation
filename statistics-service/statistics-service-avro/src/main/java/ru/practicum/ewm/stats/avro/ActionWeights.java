package ru.practicum.ewm.stats.avro;

/** One shared source of action weights for the aggregator and analyzer. */
public final class ActionWeights {

    private ActionWeights() {
    }

    public static double weight(ActionTypeAvro actionType) {
        if (actionType == null) {
            throw new IllegalArgumentException("Action type cannot be null");
        }
        return switch (actionType) {
            case VIEW -> 0.4;
            case REGISTER -> 0.8;
            case LIKE -> 1.0;
            default -> throw new IllegalArgumentException("Unknown action type: " + actionType);
        };
    }
}
