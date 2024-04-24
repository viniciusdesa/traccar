package org.traccar.protocol;

public enum AnjoMessage {
    MSG_TYPE_NULL((byte) 0x00),
    MSG_TYPE_POSITION((byte) 0x01),
    MSG_TYPE_EVENT((byte) 0x02),
    MSG_TYPE_VERSION((byte) 0x03),
    MSG_TYPE_STATUS_HARDWARE((byte) 0x04),
    MSG_TYPE_DRIVER_ID((byte) 0x05),
    MSG_TYPE_UPDATE_FIRMWARE((byte) 0x06),
    MSG_TYPE_ICCID((byte) 0x07),
    MSG_TYPE_UPDATE_DATABASE((byte) 0x08),
    MSG_TYPE_CHILD_DEVICE((byte) 0x79);

    private final byte messageCode;

    AnjoMessage(byte value) {
        messageCode = value;
    }

    public byte getValue() {
        return messageCode;
    }
}
