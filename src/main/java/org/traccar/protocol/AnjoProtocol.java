package org.traccar.protocol;

import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import org.traccar.BaseProtocol;
import org.traccar.PipelineBuilder;
import org.traccar.TrackerServer;
import org.traccar.config.Config;
import org.traccar.model.Command;

import jakarta.inject.Inject;

public class AnjoProtocol extends BaseProtocol {
    static final short PROTOCOL_VERSION = 0x06;
    static final int MAX_FRAME_LENGTH = 256;
    static final int LENGTH_FIELD_OFFSET = 8;
    static final int LENGTH_FIELD_LENGTH = 2;
    static final int LENGTH_ADJUSTMENT = 1;
    static final int INITIAL_BYTES_TO_STRIP = 1;

    @Inject
    public AnjoProtocol(Config config) {
        setSupportedDataCommands(
                Command.TYPE_CUSTOM,
                Command.TYPE_FIRMWARE_UPDATE);
        addServer(new TrackerServer(config, getName(), false) {
            @Override
            protected void addProtocolHandlers(PipelineBuilder pipeline, Config config) {
                pipeline.addLast(new LengthFieldBasedFrameDecoder(MAX_FRAME_LENGTH, LENGTH_FIELD_OFFSET,
                        LENGTH_FIELD_LENGTH, LENGTH_ADJUSTMENT, INITIAL_BYTES_TO_STRIP));
                pipeline.addLast(new AnjoProtocolDecoder(AnjoProtocol.this));
                pipeline.addLast(new AnjoProtocolEncoder(AnjoProtocol.this));
            }
        });
    }
}
