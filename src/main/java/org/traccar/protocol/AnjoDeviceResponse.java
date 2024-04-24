package org.traccar.protocol;

public enum AnjoDeviceResponse {

    MSG_TYPE_DISK_CLEAN((byte) 0x04),
    MSG_TYPE_REQUEST_RESET((byte) 0x05),
    MSG_TYPE_REQUEST_CALIBRE_ACCELEROMETER((byte) 0x06),
    MSG_TYPE_PLAY_AUDIO((byte) 0x07);

    private final byte responseCode;

    AnjoDeviceResponse(byte value) {
        responseCode = value;
    }

    public byte getValue() {
        return responseCode;
    }
}
