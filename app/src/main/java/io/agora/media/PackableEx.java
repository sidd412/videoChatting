package io.agora.media;

public interface PackableEx {
    void marshal(ByteBuf out);
    void unmarshal(ByteBuf in);
}
