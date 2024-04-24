package org.traccar.protocol;

public enum AnjoMessageType {
    MESSAGE_TYPE_DEVICE_TO_SERVER((byte) 0x10),
    MESSAGE_TYPE_DEVICE_TO_SERVER_RESPONSE((byte) 0x20),
    MESSAGE_TYPE_SERVER_TO_DEVICE((byte) 0x30),
    MESSAGE_TYPE_SERVER_TO_DEVICE_RESPONSE((byte) 0x40);

    private final byte typeCode;

    AnjoMessageType(byte value) {
        typeCode = value;
    }

    public byte getValue() {
        return typeCode;
    }
}
