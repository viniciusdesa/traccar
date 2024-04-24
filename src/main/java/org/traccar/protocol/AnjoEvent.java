package org.traccar.protocol;

public enum AnjoEvent {
    EVENT_VEHICLE_STARTED((byte) 0x01),
    EVENT_VEHICLE_STOPPED((byte) 0x02),
    EVENT_DOOR_OPENED((byte) 0x03),
    EVENT_DOOR_CLOSED((byte) 0x04),
    EVENT_TAMPER((byte) 0x05),
    EVENT_AXLE_SUSPENDED((byte) 0x06),
    EVENT_AXLE_DOWN((byte) 0x07),
    EVENT_HOOD_OPENED((byte) 0x08),
    EVENT_HOOD_CLOSED((byte) 0x09),
    EVENT_TRAILER_ENGAGED((byte) 0x0A),
    EVENT_TRAILER_DISENGAGED((byte) 0x0B),
    EVENT_BOX_OPENED((byte) 0x0C),
    EVENT_BOX_CLOSED((byte) 0x0D),
    EVENT_PARKING_BRAKE_PULLED((byte) 0x0E),
    EVENT_PARKING_BRAKE_RELEASED((byte) 0x0F),
    EVENT_OVERSPEED((byte) 0x10),
    EVENT_SOS((byte) 0x11),
    EVENT_TURN_SPEEDING((byte) 0x12),
    EVENT_DEVICE_POWER_ON((byte) 0x13),
    EVENT_DEVICE_POWER_OFF((byte) 0x14),
    EVENT_GPS_NO_FIX((byte) 0x15),
    EVENT_GPS_FIX((byte) 0x16),
    EVENT_HARD_BRAKING((byte) 0x17),
    EVENT_FRONT_COLLISION((byte) 0x18),
    EVENT_REAR_COLLISION((byte) 0x19),
    EVENT_TIPPLING((byte) 0x1A),
    EVENT_GPS_JAMMING((byte) 0x1B),
    EVENT_GSM_JAMMING((byte) 0x1C),
    EVENT_DRIVER_ID_MISSING((byte) 0x1D),
    EVENT_OVERSPEED_IN_RESTRICTED_ZONE((byte) 0x1E);

    private final byte eventCode;

    AnjoEvent(byte value) {
        eventCode = value;
    }

    public byte getValue() {
        return eventCode;
    }
}
