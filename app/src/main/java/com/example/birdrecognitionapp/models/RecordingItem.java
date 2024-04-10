package com.example.birdrecognitionapp.models;

import java.io.Serializable;

public class RecordingItem implements Serializable {
    private String name;
    private String path;
    private long length;
    private long time_added;
    private String user_id;
    private String blob_reference;


    public RecordingItem(String name, String path, long length, long time_added) {
        this.name = name;
        this.path = path;
        this.length = length;
        this.time_added = time_added;
    }

    public RecordingItem(String name, String path, long length, long time_added, String user_id, String blob_reference) {
        this.name = name;
        this.path = path;
        this.length = length;
        this.time_added = time_added;
        this.user_id = user_id;
        this.blob_reference = blob_reference;
    }

    public RecordingItem(){}

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public long getLength() {
        return length;
    }

    public void setLength(long length) {
        this.length = length;
    }

    public long getTime_added() {
        return time_added;
    }

    public String getUser_id() {
        return user_id;
    }

    public void setUser_id(String user_id) {
        this.user_id = user_id;
    }

    public String getBlob_reference() {
        return blob_reference;
    }

    public void setBlob_reference(String blob_reference) {
        this.blob_reference = blob_reference;
    }

    public void setTime_added(long time_added) {
        this.time_added = time_added;
    }

    @Override
    public String toString() {
        return "RecordingItem{" +
                "name='" + name + '\'' +
                ", path='" + path + '\'' +
                ", length=" + length +
                ", time_added=" + time_added +
                ", user_id='" + user_id + '\'' +
                ", blob_reference='" + blob_reference + '\'' +
                '}';
    }
}
