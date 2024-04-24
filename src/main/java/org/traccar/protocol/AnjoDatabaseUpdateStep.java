package org.traccar.protocol;

public enum AnjoDatabaseUpdateStep {

    STEP_CODE_DATABASE_REQUEST_RECEIVED((byte) 0x01, "msgUpDbReceived"),
    STEP_CODE_DATABASE_DOWNLOADING_UPDATE_ID((byte) 0x02, "msgUpDbDownloadingUpdateId"),
    STEP_CODE_DATABASE_DOWNLOADING_FILE_ID_AND_UPDATE_ID((byte) 0x03, "msgUpDbDownloadingFileIdAndUpdateId"),
    STEP_CODE_DATABASE_DOWNLOAD_FILE_ID_SUCCESSFUL((byte) 0x04, "msgUpDbDownloadFileIdSuccessful"),
    STEP_CODE_DATABASE_DOWNLOAD_COMPLETE((byte) 0x05, "msgUpDbDownloadComplete"),

    STEP_CODE_DATABASE_FAILED_GET_API_DATA((byte) 0x06, "msgUpDbFailedGetApiData"),
    STEP_CODE_DATABASE_FAILED_DESERIALIZE_UPDATE((byte) 0x07, "msgUpDbFailedDeserializeUpdate"),
    STEP_CODE_DATABASE_FILEINFO_NOT_FOUND((byte) 0x08, "msgUpDbFileinfoNotFound"),
    STEP_CODE_DATABASE_FULL_MICROSD((byte) 0x09, "msgUpDbFullMicroSD"),

    STEP_CODE_DATABASE_UPDATE_INTERRUPED_BY_DEVICE((byte) 0x0A, "msgUpDbUpdateInterrupedByDevice"),
    STEP_CODE_DATABASE_UPDATE_RESUMED_BY_DEVICE((byte) 0x0B, "msgUpDbUpdateResumedByDevice"),
    STEP_CODE_DATABASE_NO_UPDATE_AVAILABLE((byte) 0x0C, "msgUpDbNoUpdateAvailable"),
    STEP_CODE_DATABASE_UPDATE_PROGRESS((byte) 0x0D, "msgUpDbUpdateProgress");

    private final byte stepCode;
    private String stepMessage;

    AnjoDatabaseUpdateStep(byte value, String message) {

        stepCode = value;
        stepMessage = message;
    }

    public byte getCode() {
        return stepCode;
    }

    public String getMessage() {
        return stepMessage;
    }
}
