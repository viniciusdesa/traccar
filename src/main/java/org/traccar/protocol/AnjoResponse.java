package org.traccar.protocol;

public enum AnjoResponse {
    RSP_SUCCESS((byte) 0x00),
    RSP_ERR_INVALID_LEN((byte) 0x11),
    RSP_ERR_INVALID_MSG((byte) 0x12),
    RSP_ERR_INVALID_PLD((byte) 0x13),
    RSP_ERR_INVALID_COURSE((byte) 0x14),
    RSP_ERR_INVALID_SPEED((byte) 0x15),
    RSP_ERR_INVALID_POSITION((byte) 0x16),
    RSP_ERR_INVALID_STEP_CODE((byte) 0x17),
    RSP_ERR_INVALID_DRIVER_ID((byte) 0X80),
    RSP_ERR_INVALID_GROUP_WITHOUT_ID((byte) 0X81);

    private final byte responseCode;

    AnjoResponse(byte value) {
        responseCode = value;
    }

    public byte getValue() {
        return responseCode;
    }
}

