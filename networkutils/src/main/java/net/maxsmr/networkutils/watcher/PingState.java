package net.maxsmr.networkutils.watcher;

public enum PingState {

    NONE {
        @Override
        public int getValue() {
            return -1;
        }
    },

    PINGING {
        @Override
        public int getValue() {
            return 0;
        }
    },

    REACHABLE {
        @Override
        public int getValue() {
            return 1;
        }
    },

    NOT_REACHABLE {
        @Override
        public int getValue() {
            return 2;
        }
    };

    public int getValue() {
        return -1;
    }

    public static PingState fromNativeValue(int value) {
        for (PingState state : PingState.values()) {
            if (state.getValue() == value) {
                return state;
            }
        }
        throw new IllegalArgumentException("Incorrect native value " + value + " for enum type " + PingState.class.getName());
    }
}
