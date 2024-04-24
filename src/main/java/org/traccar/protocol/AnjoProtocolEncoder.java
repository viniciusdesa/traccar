/*
 * Author: Vinicius de Sa
 * Copyright 2019 Vido Latino America Ltda.
 */
package org.traccar.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.traccar.BaseProtocolEncoder;
import org.traccar.Protocol;
import org.traccar.model.Command;

public class AnjoProtocolEncoder extends BaseProtocolEncoder {

    private static final byte STX = 0x02;
    private static final byte ETX = 0x03;
    private static final byte MSG_TYPE = 0x30;
    private static final byte MSG_TYPE_NULL = 0x00;
    private static final byte MSG_TYPE_FIRMWARE_UPDATE_REQUEST = 0x01;
    private static final byte MSG_TYPE_DATABASE_UPDATE_REQUEST = 0x02;
    private static final byte MSG_TYPE_ICCID_REQUEST = 0x03;
    private static final byte MSG_TYPE_CLEAN_REQUEST = 0x04;
    private static final byte MSG_TYPE_RESET_REQUEST = 0x05;
    private static final byte MSG_TYPE_CALIBRE_ACCELEROMETER_REQUEST = 0x06;
    private static final byte MSG_TYPE_AUDIO_REQUEST = 0x07;

    public AnjoProtocolEncoder(Protocol protocol) {
        super(protocol);
    }

    private boolean isNumeric(String strNum) {
        return strNum.matches("-?\\d+(\\.\\d+)?");
    }

    private void writeHeader(ByteBuf buf) {
        buf.writeByte(STX);
        buf.writeByte(MSG_TYPE);
        buf.writeShort(AnjoProtocol.PROTOCOL_VERSION);
    }

    private void writeFooter(ByteBuf buf) {
        buf.writeByte(ETX);
    }

    private Object encodeFirmwareUpdate() {
        ByteBuf buf = Unpooled.buffer();
        writeHeader(buf);
        buf.writeShort(1); // tamanho: 1 bytes
        buf.writeByte(MSG_TYPE_FIRMWARE_UPDATE_REQUEST);
        writeFooter(buf);
        return buf;
    }

    private Object encodeDatabaseUpdate() {
        ByteBuf buf = Unpooled.buffer();
        writeHeader(buf);
        buf.writeShort(1); // tamanho: 1 bytes
        buf.writeByte(MSG_TYPE_DATABASE_UPDATE_REQUEST);
        writeFooter(buf);
        return buf;
    }

    private Object encodeICCID() {
        ByteBuf buf = Unpooled.buffer();
        writeHeader(buf);
        buf.writeShort(1); // tamanho: 1 bytes
        buf.writeByte(MSG_TYPE_ICCID_REQUEST);
        writeFooter(buf);
        return buf;
    }

    private Object encodeDiskClean() {
        ByteBuf buf = Unpooled.buffer();
        writeHeader(buf);
        buf.writeShort(1); // tamanho: 1 bytes
        buf.writeByte(MSG_TYPE_CLEAN_REQUEST);
        writeFooter(buf);
        return buf;
    }

    private Object encodeRequestReset() {
        ByteBuf buf = Unpooled.buffer();
        writeHeader(buf);
        buf.writeShort(1); // tamanho: 1 bytes
        buf.writeByte(MSG_TYPE_RESET_REQUEST);
        writeFooter(buf);
        return buf;
    }

    private Object encodeCalibrateAccelerometer() {
        ByteBuf buf = Unpooled.buffer();
        writeHeader(buf);
        buf.writeShort(1); // tamanho: 1 bytes
        buf.writeByte(MSG_TYPE_CALIBRE_ACCELEROMETER_REQUEST);
        writeFooter(buf);
        return buf;
    }

    private Object encodeRequestAudio(String data) {
        ByteBuf buf = Unpooled.buffer();
        writeHeader(buf);
        buf.writeShort(2); // tamanho: 2 bytes
        buf.writeByte(MSG_TYPE_AUDIO_REQUEST);
        buf.writeByte(Byte.parseByte(data));
        writeFooter(buf);
        return buf;
    }

    @Override
    protected Object encodeCommand(Command command) {
        switch (command.getType()) {
            case Command.TYPE_FIRMWARE_UPDATE:
                return encodeFirmwareUpdate();
            case Command.TYPE_CUSTOM:

            default:
                return null;
        }
    }
}
