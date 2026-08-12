package dev.catgirlyannick.catitems.pack;

import java.nio.file.Path;

public record PackArtifact(Path file, byte[] sha1, String sha1Hex, long size, PackFormat format) {
    public PackArtifact {
        sha1 = sha1.clone();
    }

    @Override
    public byte[] sha1() {
        return sha1.clone();
    }
}
