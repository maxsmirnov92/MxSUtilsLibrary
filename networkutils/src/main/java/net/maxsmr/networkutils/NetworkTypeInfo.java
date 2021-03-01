package net.maxsmr.networkutils;

import org.jetbrains.annotations.NotNull;

public class NetworkTypeInfo {

    public final int type;
    public final int subtype;
    public final String typeName;
    public final String subtypeName;

    public NetworkTypeInfo(int type, int subtype, String typeName, String subtypeName) {
        this.type = type;
        this.subtype = subtype;
        this.typeName = typeName;
        this.subtypeName = subtypeName;
    }

    @Override
    @NotNull
    public String toString() {
        return "NetworkTypeInfo{" +
                "type=" + type +
                ", subtype=" + subtype +
                ", typeName='" + typeName + '\'' +
                ", subtypeName='" + subtypeName + '\'' +
                '}';
    }
}