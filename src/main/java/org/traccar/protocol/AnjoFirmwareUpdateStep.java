package org.traccar.protocol;

public enum AnjoFirmwareUpdateStep {


    STEP_CODE_FIRMWARE_REQUEST_RECEIVED((byte) 0x01, "msgUpFwReceived"),
    STEP_CODE_FIRMWARE_CHECKING_VERSION_API((byte) 0x02, "msgUpFwCheckingApiVersion"),
    STEP_CODE_FIRMWARE_UPDATE_EXECUTION_ERROR((byte) 0x03, "msgUpFwUpdateExecutionError"),
    STEP_CODE_FIRMWARE_DOWNLOAD_STARTED((byte) 0x04, "msgUpFwDownloadStarted"),
    STEP_CODE_FIRMWARE_DOWNLOAD_FAILED((byte) 0x05, "msgUpFwDownloadFailed"),
    STEP_CODE_FIRMWARE_DOWNLOAD_COMPLETE((byte) 0x06, "msgUpFwDownloadComplete"),
    STEP_CODE_FIRMWARE_UPDATE_SUCCESSFULLY((byte) 0x07, "msgUpFwUpdateSuccessfully"),
    STEP_CODE_FIRMWARE_REMOTE_UPDATE_FAILED((byte) 0x08, "msgUpFwRemoteUpdateFailed"),
    STEP_CODE_FIRMWARE_UPDATE_INTERRUPED_BY_DEVICE((byte) 0x09, "msgUpFwUpdateInterrupedByDevice"),
    STEP_CODE_FIRMWARE_UPDATE_RESUMED_BY_DEVICE((byte) 0x0A, "msgUpFwUpdateResumedByDevice"),
    STEP_CODE_FIRMWARE_NO_NEW_VERSION((byte) 0x0B, "msgUpFwNoNewVersion"),
    STEP_CODE_FIRMWARE_UPDATE_PROGRESS((byte) 0x0C, "msgUpFwUpdateProgress");

    private final byte code;
    private String message;

    AnjoFirmwareUpdateStep(byte value, String content) {

        code = value;
        message = content;
    }

    public byte getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
