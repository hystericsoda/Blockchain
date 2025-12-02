package org.example;

public class Block {

    int id;
    long timestamp;
    Object[] data;
    String prev_hash, hash;

    public Block(int id, long timestamp, Object[] data,
                  String prev_hash, String hash) {

        this.id = id;
        this.timestamp = timestamp;
        this.data = data;
        this.prev_hash = prev_hash;
        this.hash = hash;

    }

}
