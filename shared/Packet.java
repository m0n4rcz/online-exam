package shared;

import java.nio.charset.StandardCharsets;

/**
 * Represents a standard application-level protocol packet.
 * 
 * =========================================================================
 * PROTOCOL FRAME SPECIFICATION (12-BYTE FIXED HEADER + PAYLOAD)
 * =========================================================================
 * Offset  | Size (Bytes) | Field Name    | Description
 * --------+--------------+---------------+---------------------------------
 * 0       | 4            | Magic Bytes   | 0x4558414D ("EXAM" ASCII)
 * 4       | 2            | OpCode        | Operation Code (e.g., 0x1001)
 * 6       | 2            | Status        | Response Status (0 = OK, errors)
 * 8       | 4            | Payload Length| Length of payload in bytes (N)
 * 12      | N            | Payload       | UTF-8 JSON or binary data
 * =========================================================================
 */
public class Packet {
    public static final int MAGIC_NUMBER = 0x4558414D; // "EXAM" in ASCII
    public static final int HEADER_SIZE = 12;

    private int magic = MAGIC_NUMBER;
    private short opCode;
    private short status = OpCodes.STATUS_OK;
    private int length;
    private byte[] payload = new byte[0];

    public Packet() {}

    public Packet(short opCode, short status, byte[] payload) {
        this.opCode = opCode;
        this.status = status;
        this.payload = payload != null ? payload : new byte[0];
        this.length = this.payload.length;
    }

    public Packet(short opCode, short status, String textPayload) {
        this.opCode = opCode;
        this.status = status;
        if (textPayload != null && !textPayload.isEmpty()) {
            this.payload = textPayload.getBytes(StandardCharsets.UTF_8);
        } else {
            this.payload = new byte[0];
        }
        this.length = this.payload.length;
    }

    /**
     * Create an OK packet with empty payload.
     */
    public static Packet ok(short opCode) {
        return new Packet(opCode, OpCodes.STATUS_OK, "");
    }

    /**
     * Create an OK packet with a text / JSON payload.
     */
    public static Packet ok(short opCode, String textPayload) {
        return new Packet(opCode, OpCodes.STATUS_OK, textPayload);
    }

    /**
     * Create an OK packet with an object payload automatically serialized to JSON.
     */
    public static Packet ok(short opCode, Object dto) {
        String json = JsonUtil.toJson(dto);
        return new Packet(opCode, OpCodes.STATUS_OK, json);
    }

    /**
     * Create an ERROR packet with error message string.
     */
    public static Packet error(short opCode, short status, String errorMessage) {
        return new Packet(opCode, status, errorMessage);
    }

    /**
     * Create an ERROR packet with an error DTO serialized to JSON.
     */
    public static Packet error(short opCode, short status, Object errorDto) {
        String json = JsonUtil.toJson(errorDto);
        return new Packet(opCode, status, json);
    }

    public boolean isSuccess() {
        return status == OpCodes.STATUS_OK;
    }

    public boolean isError() {
        return status != OpCodes.STATUS_OK;
    }

    /**
     * Deserializes the JSON payload into the requested DTO class.
     */
    public <T> T getPayloadAs(Class<T> clazz) {
        return JsonUtil.fromJson(getPayloadAsString(), clazz);
    }

    /**
     * Deserializes the JSON payload into a List of the requested DTO class.
     */
    public <T> java.util.List<T> getPayloadAsList(Class<T> clazz) {
        return JsonUtil.fromJsonList(getPayloadAsString(), clazz);
    }

    public int getMagic() { return magic; }
    public void setMagic(int magic) { this.magic = magic; }

    public short getOpCode() { return opCode; }
    public void setOpCode(short opCode) { this.opCode = opCode; }

    public short getStatus() { return status; }
    public void setStatus(short status) { this.status = status; }

    public int getLength() { return length; }
    public void setLength(int length) { this.length = length; }

    public byte[] getPayload() { return payload; }
    public void setPayload(byte[] payload) {
        this.payload = payload != null ? payload : new byte[0];
        this.length = this.payload.length;
    }

    public String getPayloadAsString() {
        if (payload == null || payload.length == 0) return "";
        return new String(payload, StandardCharsets.UTF_8);
    }

    @Override
    public String toString() {
        String content = getPayloadAsString();
        String preview = content.length() > 60 ? content.substring(0, 60) + "..." : content;
        return String.format("Packet[OpCode=%s, Status=%d, Length=%d bytes, Payload=\"%s\"]",
                OpCodes.getName(opCode), status, length, preview);
    }
}
