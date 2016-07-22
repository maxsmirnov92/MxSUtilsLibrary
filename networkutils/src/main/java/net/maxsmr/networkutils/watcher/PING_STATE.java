package net.maxsmr.networkutils.watcher;

public enum PING_STATE {

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

    public static PING_STATE fromNativeValue(int value) {
        for (PING_STATE state : PING_STATE.values()) {
            if (state.getValue() == value) {
                return state;
            }
        }
        throw new IllegalArgumentException("Incorrect native value " + value + " for enum type " + PING_STATE.class.getName());
    }
}
