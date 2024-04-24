package org.traccar.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.BaseProtocolDecoder;
import org.traccar.NetworkMessage;
import org.traccar.Protocol;
import org.traccar.helper.BitUtil;
import org.traccar.helper.DateBuilder;

import java.util.Calendar;

import org.traccar.helper.UnitsConverter;
import org.traccar.model.Device;
import org.traccar.model.Position;

import java.net.SocketAddress;

public class AnjoProtocolDecoder extends BaseProtocolDecoder {
    protected static final Logger LOGGER = LoggerFactory.getLogger(AnjoProtocolDecoder.class);

    private static final byte STX = 0x02;
    private static final byte ETX = 0x03;

    private static final boolean ENABLE_POSITION_FILTER = true;

    public AnjoProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    private ByteBuf createResponse(byte type, byte status) {
        byte[] response = new byte[]{STX, AnjoMessageType.MESSAGE_TYPE_DEVICE_TO_SERVER_RESPONSE.getValue(),
                (AnjoProtocol.PROTOCOL_VERSION >> 8), (AnjoProtocol.PROTOCOL_VERSION & 0xFF), type, status, ETX};
        return Unpooled.wrappedBuffer(response);
    }

    private void sendReply(Channel channel, SocketAddress remoteAddress, byte status) {
        if (channel != null) {
            channel.writeAndFlush(new NetworkMessage(
                    createResponse(AnjoMessage.MSG_TYPE_NULL.getValue(), status), remoteAddress));
        }
    }

    @Override
    protected Object decode(Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {
        ByteBuf buf = (ByteBuf) msg;

        if (buf.readableBytes() < 2) {
            LOGGER.error("[MSG_GAPOS_DECODER] Invalid Message.");
            this.sendReply(channel, remoteAddress, AnjoResponse.RSP_ERR_INVALID_MSG.getValue());
            return null;
        }

        byte messageType = buf.readByte();

        if (messageType == AnjoMessageType.MESSAGE_TYPE_DEVICE_TO_SERVER.getValue()) {
            return handleDeviceToServerMessage(buf, channel, remoteAddress);
        } else if (messageType == AnjoMessageType.MESSAGE_TYPE_SERVER_TO_DEVICE_RESPONSE.getValue()) {
            return handleServerToDeviceReponse(buf, channel, remoteAddress);
        }

        return null;
    }

    public Position handleServerToDeviceReponse(ByteBuf buf, Channel channel, SocketAddress remoteAddress) {
        return null;
    }

    private DateBuilder dateBuilder(int unixTime) {

        DateBuilder dateBuilder = new DateBuilder()
                .setDate(1970, 1, 1)
                .setTime(0, 0, 0)
                .addSeconds(unixTime);

        return dateBuilder;
    }

    private boolean checkTime(DateBuilder dateBuilder) {
        long filterFuture = (System.currentTimeMillis() + (86400 * 1000)); //24hrs

        Calendar calendarDevice = Calendar.getInstance();
        calendarDevice.setTime(dateBuilder.getDate());
        int yearDevice = calendarDevice.get(Calendar.YEAR);
        int yearSystem = Calendar.getInstance().get(Calendar.YEAR);

        return yearDevice >= (yearSystem - 1) && calendarDevice.getTime().getTime() < filterFuture;
    }

    private boolean decodeInitialFields(ByteBuf buf, Position position) {
        int unixTime = buf.readInt();

        DateBuilder dateBuilder = dateBuilder(unixTime);
        position.setTime(dateBuilder.getDate());

        double latitude = buf.readUnsignedInt() / 1000000d;
        double longitude = buf.readUnsignedInt() / 1000000d;

        byte flags = buf.readByte();
        if (!BitUtil.check(flags, 7)) {
            latitude = -latitude;
        }

        if (!BitUtil.check(flags, 6)) {
            longitude = -longitude;
        }

        position.setLatitude(latitude);
        position.setLongitude(longitude);

        return true;
    }

    private boolean checkCourse(double course) {
        return (course >= 0) && (course <= 360);
    }

    private boolean checkSpeed(double speed) {
        return (speed >= 0) && (speed <= 160);
    }

    private void decodeDigitalStatus(Position position, short digitalStatus, int protocolVersion) {
        for (int i = 0; i < 8; i++) {
            boolean status = BitUtil.check(digitalStatus, i);
            switch (i) {
                case 0:
                    // position.set(Position.KEY_PARKING_BRAKE, status);
                    break;
                case 1:
                    // position.set(Position.KEY_BOX, status);
                    break;
                case 2:
                    position.set(Position.KEY_MOTION, status);
                    break;
                case 3:
                    // position.set(Position.KEY_HOOD, status);
                    break;
                case 4:
                    if (status) {
                        position.set(Position.KEY_ALARM, Position.ALARM_ACCIDENT);
                    }
                    break;
                case 5:
                    position.set(Position.KEY_DOOR, status);
                    break;
                case 6:
                    if (status && position.getString(Position.KEY_ALARM) == null) {
                        position.set(Position.KEY_ALARM, Position.ALARM_TAMPERING);
                    }
                    break;
                case 7:
                    position.set(Position.KEY_IGNITION, status);
                    break;
                default:
                    break;
            }
        }
    }

    private int getRSSI(short rssiIndex) {
        switch (rssiIndex) {
            case 0:
                return -115;
            case 1:
                return -111;
            default:
                if (rssiIndex <= 30) {
                    return -110 + (2 * (rssiIndex - 2));
                } else {
                    return -120;
                }
        }
    }

    public Position handleDeviceToServerMessage(ByteBuf buf, Channel channel, SocketAddress remoteAddress) {
        int protocolVersion = (buf.readByte() << 8) + buf.readByte();
        int deviceId = buf.readInt();

        Device device = getCacheManager().getObject(Device.class, deviceId);
        if (device == null) {
            LOGGER.error("[MSG_ANJO_DECODER] Invalid UniqueId. (Device ID:" + Integer.toString(deviceId) + ").");
            return null;
        }

        int length = (buf.readByte() << 8) + buf.readByte();

        if ((length <= 0) || ((buf.readableBytes() - 1) != length)) {
            LOGGER.error("[MSG_ANJO_DECODER] Invalid message length or payload different of length. Length:"
                    + Integer.toString(length) + ". (Device ID:" + Integer.toString(deviceId) + ").");
            this.sendReply(channel, remoteAddress, AnjoResponse.RSP_ERR_INVALID_LEN.getValue());
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceId);

        byte type = buf.readByte();
        short digitalStatus, rssiIndex, hdop, pdop, satellites;
        double course = 0, speed = 0, altitude;
        boolean isValidPosition = false;

        if ((type == AnjoMessage.MSG_TYPE_POSITION.getValue())
                || (type == AnjoMessage.MSG_TYPE_EVENT.getValue())
                || (type == AnjoMessage.MSG_TYPE_STATUS_HARDWARE.getValue())
                || (type == AnjoMessage.MSG_TYPE_DRIVER_ID.getValue())) {
            isValidPosition = decodeInitialFields(buf, position);

            course = buf.readUnsignedShort();
            speed = buf.readUnsignedShort();

            if (!checkCourse(course)) {
                LOGGER.error("[MSG_GAPOS_DECODER] Invalid Course. (Type:" + type
                        + " / Device ID:" + Integer.toString(deviceId) + ").");
                this.sendReply(channel, remoteAddress, AnjoResponse.RSP_ERR_INVALID_COURSE.getValue());
                return null;
            }

            if (!checkSpeed(speed)) {
                LOGGER.error("[MSG_GAPOS_DECODER] Invalid Speed. (Type:" + type
                        + " / Device ID:" + Integer.toString(deviceId) + ").");
                this.sendReply(channel, remoteAddress, AnjoResponse.RSP_ERR_INVALID_SPEED.getValue());
                return null;
            }

            position.setCourse(course);
            position.setSpeed(UnitsConverter.knotsFromKph(speed));
        }

        if (type == AnjoMessage.MSG_TYPE_POSITION.getValue()) {
            digitalStatus = buf.readUnsignedByte();
            this.decodeDigitalStatus(position, digitalStatus, protocolVersion);
            decodeAdditionalData(buf, position);

            position.setValid(isValidPosition);
            position.set(Position.KEY_GPS, Boolean.TRUE);

            LOGGER.info("[MSG_TYPE_POSITION_0x01] Device ID: " + Integer.toString(deviceId)
                    + " - Prot. Version: " + Integer.toString(protocolVersion)
                    + " - Length: " + Integer.toString(length)
                    + " - Date: " + position.getDeviceTime().toString()
                    + " - Latitude: " + Double.toString(position.getLatitude())
                    + " - Longitude: " + Double.toString(position.getLongitude())
                    + " - Course: " + Double.toString(course)
                    + " - Speed (km/h): " + Double.toString(speed)
                    + " - Digital Status: " + Short.toString(digitalStatus));
        } else if (type == AnjoMessage.MSG_TYPE_EVENT.getValue()) {
            this.decodeAdditionalData(buf, position);
            int eventType = buf.readByte();

            /*if (eventType == AnjoEvent.EVENT_DRIVER_ID_MISSING.getValue()) {
                if (!checkGroup(device.getGroupId())) {
                    LOGGER.error("[MSG_TYPE_EVENT_0x02]: Invalid Group Without RFID.
                    (Device ID:" + Integer.toString(deviceId) + ").");
                    this.sendReply(channel, remoteAddress, RSP_ERR_INVALID_GROUP_WITHOUT_ID);
                    return null;
                }
            }*/
            position.setValid(isValidPosition);
            this.decodeEventType(position, buf, eventType, protocolVersion);
            LOGGER.info("[MSG_TYPE_EVENT_0x02] Device ID: " + Integer.toString(deviceId)
                    + " - Event Type: " + Integer.toHexString(eventType)
                    + " - Prot. Version: " + Integer.toString(protocolVersion)
                    + " - Length: " + Integer.toString(length)
                    + " - Date: " + position.getDeviceTime().toString()
                    + " - Latitude: " + Double.toString(position.getLatitude())
                    + " - Longitude: " + Double.toString(position.getLongitude())
                    + " - Course: " + Double.toString(course)
                    + " - Speed (km/h): " + Double.toString(speed));
        } else if (type == AnjoMessage.MSG_TYPE_VERSION.getValue()) {
            buf.readInt();

            int firmwareVersion = buf.readUnsignedShort();
            int databaseVersion = buf.readUnsignedShort();

            position = null;
//            Event event = new Event(Event.TYPE_DEVICE_TYPE_VERSION, device.getId());
//            event.set(Position.KEY_VERSION_FW, firmwareVersion);
//            event.set(Position.KEY_VERSION_DB, databaseVersion);
//            this.createEvent(event, false);

            LOGGER.info("[MSG_TYPE_VERSION_0x03] Device ID: " + Integer.toString(deviceId)
                    + " - Prot. Version: " + Integer.toString(protocolVersion)
                    + " - Length: " + Integer.toString(length)
                    + " - Firmware Version: [" + Integer.toString(firmwareVersion) + "]"
                    + " - Database Version: [" + Integer.toString(databaseVersion) + "]");
        } else if (type == AnjoMessage.MSG_TYPE_STATUS_HARDWARE.getValue()) {
            this.decodeAdditionalData(buf, position);
            short hwStatus = buf.readUnsignedByte();

            String key = "";
            int bitInit = 1;
            for (int i = bitInit; i <= 7; i++) {
                if (!BitUtil.check(hwStatus, i)) {
                    key += i + "-";
                }
            }

            if (!key.equals("")) {
                position.set(Position.KEY_STATUS, key.substring(0, key.length() - 1));
            }
            // else {
                // position.set(Position.KEY_STATUS, Position.MSG_STATUS_HARDWARE_OK);
            // }

            int freeHeap = buf.readInt();
            // position.set(Position.KEY_FREE_HEAP, freeHeap);

            short temp = buf.readUnsignedByte();
            position.set(Position.KEY_DEVICE_TEMP, temp);

            short extv = 0;
            float intv = 0;
            float vl3v4 = 0;
            float vl4v4 = 0;
            short supplySrc = 0;
            if (length == 0x000F || length == 0x0010) {
                extv = buf.readUnsignedByte();
                // position.set(Position.KEY_DEVICE_EXT_VOLTAGE, extv);

                intv = (float) (buf.readUnsignedByte() / 10.0);
                // position.set(Position.KEY_DEVICE_INT_VOLTAGE, intv);

                vl3v4 = (float) (buf.readUnsignedByte() / 10.0);
                // position.set(Position.KEY_DEVICE_3V4_VOLTAGE, vl3v4);

                vl4v4 = (float) (buf.readUnsignedByte() / 10.0);
                // position.set(Position.KEY_DEVICE_4V4_VOLTAGE, vl4v4);

                if (length == 0x0010 || protocolVersion >= 3) {
                    supplySrc = buf.readUnsignedByte();
                    // position.set(Position.KEY_DEVICE_SUPLLY_SRC, supplySrc);
                }
            }
            position.setValid(isValidPosition);

            LOGGER.info("[MSG_TYPE_STATUS_HARDWARE_0x04] Device ID: " + Integer.toString(deviceId)
                    + " - Prot. Version: " + Integer.toString(protocolVersion)
                    + " - Length: " + Integer.toString(length)
                    + " - Date: " + position.getDeviceTime().toString()
                    + " - Latitude: " + Double.toString(position.getLatitude())
                    + " - Longitude: " + Double.toString(position.getLongitude())
                    + " - Course: " + Double.toString(course)
                    + " - Speed (km/h): " + Double.toString(speed)
                    + " - Hw Status: " + Short.toString(hwStatus)
                    + " - Free Heap: " + Integer.toString(freeHeap)
                    + " - Temperature: " + Short.toString(temp)
                    + " - External Voltage: " + Short.toString(extv)
                    + " - Internal Voltage: " + Float.toString(intv)
                    + " - Voltage 3V4: " + Float.toString(vl3v4)
                    + " - Voltage 4V4: " + Float.toString(vl4v4)
                    + " - Supply Src: " + Short.toString(supplySrc));
        }

        if (channel != null) {
            channel.writeAndFlush(new NetworkMessage(createResponse(type, AnjoResponse.RSP_SUCCESS.getValue()),
                    remoteAddress));
        }

        return position;
    }


    private void decodeEventType(Position position, ByteBuf buf, int eventType, int protocolVersion) {
        int firmwareVersion;
        int freeStorage;
        if (eventType == AnjoEvent.EVENT_VEHICLE_STARTED.getValue()) {
            position.set(Position.KEY_IGNITION, true);
        } else if (eventType == AnjoEvent.EVENT_VEHICLE_STOPPED.getValue()) {
            position.set(Position.KEY_IGNITION, false);
        } else if (eventType == AnjoEvent.EVENT_DOOR_OPENED.getValue()) {
            position.set(Position.KEY_DOOR, true);
        } else if (eventType == AnjoEvent.EVENT_DOOR_CLOSED.getValue()) {
            position.set(Position.KEY_DOOR, false);
        }  else if (eventType == AnjoEvent.EVENT_TAMPER.getValue()) {
            position.set(Position.KEY_ALARM, Position.ALARM_TAMPERING);
        /*} else if (eventType == AnjoEvent.EVENT_AXLE_SUSPENDED.getValue()) {
            // position.set(Position.KEY_AXLE, true);
        } else if (eventType == AnjoEvent.EVENT_AXLE_DOWN.getValue()) {
            // position.set(Position.KEY_AXLE, false);
        } else if (eventType == AnjoEvent.EVENT_HOOD_OPENED.getValue()) {
            // position.set(Position.KEY_HOOD, true);
        }  else if (eventType == AnjoEvent.EVENT_HOOD_CLOSED.getValue()) {
            // position.set(Position.KEY_HOOD, false);
        }  else if (eventType == AnjoEvent.EVENT_TRAILER_ENGAGED.getValue()) {
            // position.set(Position.KEY_TRAILER_ENGAGED, true);
        }  else if (eventType == AnjoEvent.EVENT_TRAILER_DISENGAGED.getValue()) {
            // position.set(Position.KEY_TRAILER_ENGAGED, false);
        } else if (eventType == AnjoEvent.EVENT_BOX_OPENED.getValue()) {
            // position.set(Position.KEY_BOX, true);
        }  else if (eventType == AnjoEvent.EVENT_BOX_CLOSED.getValue()) {
            // position.set(Position.KEY_BOX, false);
        }  else if (eventType == AnjoEvent.EVENT_PARKING_BRAKE_PULLED.getValue()) {
            // position.set(Position.KEY_PARKING_BRAKE, true);
        }  else if (eventType == AnjoEvent.EVENT_PARKING_BRAKE_RELEASED.getValue()) {
            // position.set(Position.KEY_PARKING_BRAKE, false);*/
        } else if (eventType == AnjoEvent.EVENT_OVERSPEED.getValue()) {
            position.set(Position.KEY_ALARM, Position.ALARM_OVERSPEED);
        } else if (eventType == AnjoEvent.EVENT_SOS.getValue()) {
            position.set(Position.KEY_ALARM, Position.ALARM_SOS);
        } else if (eventType == AnjoEvent.EVENT_TURN_SPEEDING.getValue()) {
            position.set(Position.KEY_ALARM, Position.ALARM_CORNERING);
            String turnSpeedingId = "";
            for (int i = 0; i < 8; i++) {
                byte var = buf.readByte();
                turnSpeedingId += (char) var;
            }

            int turnSequence = buf.readUnsignedShort();
            int turnAlarmDistance = buf.readUnsignedShort();
            short turnSpeedLimit = buf.readUnsignedByte();

//                position.set(Position.KEY_TURN_ID, turnSpeedingId.trim());
//                position.set(Position.KEY_TURN_SEQUENCE, turnSequence);
//                position.set(Position.KEY_TURN_ALARM_DISTANCE, turnAlarmDistance);
//                position.set(Position.KEY_TURN_SPEED_LIMIT, UnitsConverter.knotsFromKph(turnSpeedLimit));

            LOGGER.info("[MSG_TYPE_EVENT_0x02][TURN] Turn ID: " + turnSpeedingId.trim()
                    + " - Sequence: " + Integer.toString(turnSequence)
                    + " - Alarm Distance: " + Integer.toString(turnAlarmDistance)
                    + " - Speed Limit (km/h): " + Short.toString(turnSpeedLimit));

        }  else if (eventType == AnjoEvent.EVENT_DEVICE_POWER_ON.getValue()) {
            position.set(Position.KEY_ALARM, Position.ALARM_POWER_ON);

            firmwareVersion = buf.readUnsignedShort();
            position.set(Position.KEY_VERSION_FW, firmwareVersion);
            freeStorage = buf.readUnsignedShort();
            // position.set(Position.KEY_FREE_STORAGE, freeStorage);
            short watchdogReasonReset = buf.readUnsignedByte();
            // position.set(Position.KEY_WATCHDOG_REASON_RESET, watchdogReasonReset);

            short espReasonReset = buf.readUnsignedByte();
            // position.set(Position.KEY_ESP_REASON_RESET, espReasonReset);
        }  else if (eventType == AnjoEvent.EVENT_DEVICE_POWER_OFF.getValue()) {
            position.set(Position.KEY_ALARM, Position.ALARM_POWER_OFF);
        }  else if (eventType == AnjoEvent.EVENT_GPS_NO_FIX.getValue()) {
            position.setValid(false);
            position.set(Position.KEY_GPS, Boolean.FALSE);
        }  else if (eventType == AnjoEvent.EVENT_GPS_FIX.getValue()) {
            position.set(Position.KEY_GPS, Boolean.TRUE);
        }  else if (eventType == AnjoEvent.EVENT_HARD_BRAKING.getValue()) {
            position.set(Position.KEY_ALARM, Position.ALARM_BRAKING);
        /*}  else if (eventType == AnjoEvent.EVENT_FRONT_COLLISION.getValue()) {
            // position.set(Position.KEY_ALARM, Position.ALARM_FRONT_COLLISION);
        }  else if (eventType == AnjoEvent.EVENT_REAR_COLLISION.getValue()) {
            // position.set(Position.KEY_ALARM, Position.ALARM_REAR_COLLISION);
        }  else if (eventType == AnjoEvent.EVENT_TIPPLING.getValue()) {
            // position.set(Position.KEY_ALARM, Position.ALARM_TIPPLING);
        }  else if (eventType == AnjoEvent.EVENT_GPS_JAMMING.getValue()) {
            // position.set(Position.KEY_ALARM, Position.ALARM_GPS_JAMMING);
        }  else if (eventType == AnjoEvent.EVENT_GSM_JAMMING.getValue()) {
            // position.set(Position.KEY_ALARM, Position.ALARM_GSM_JAMMING);
        }  else if (eventType == AnjoEvent.EVENT_DRIVER_ID_MISSING.getValue()) {
            // position.set(Position.KEY_DRIVER_UNIQUE_ID, Position.DRIVER_ID_MISSING_DEFAULT);
            // position.set(Position.KEY_ALARM, Position.ALARM_DRIVER_ID_MISSING);*/
        }  else if (eventType == AnjoEvent.EVENT_OVERSPEED_IN_RESTRICTED_ZONE.getValue()) {
            String restrictedZoneId = "";
            for (int i = 0; i < 10; i++) {
                byte var = buf.readByte();
                restrictedZoneId += (char) var;
            }
            short limitSpeed = buf.readUnsignedByte();

//            position.set(Position.KEY_RESTRICTED_ZONE_ID, restrictedZoneId.trim());
//            position.set(Position.KEY_RESTRICTED_ZONE_LIMIT_SPEED, UnitsConverter.knotsFromKph(limitSpeed));
//            position.set(Position.KEY_ALARM, Position.ALARM_OVERSPEED_IN_RESTRICTED_ZONE);

            LOGGER.info("[MSG_TYPE_EVENT_0x02][RESTRICTED_ZONE] ID: " + restrictedZoneId.trim()
                    + " - Limit Speed (km/h): " + Double.toString(limitSpeed));
        }
    }

    private void decodeAdditionalData(ByteBuf buf, Position position) {
        position.set(Position.KEY_RSSI, getRSSI(buf.readUnsignedByte()));
        position.set(Position.KEY_HDOP, buf.readUnsignedByte());
        position.set(Position.KEY_PDOP, buf.readUnsignedByte());
        position.set(Position.KEY_SATELLITES, buf.readUnsignedByte());
        position.setAltitude(buf.readUnsignedShort());
    }
}
