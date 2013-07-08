package de.skiptag.roadrunner.messaging;

import org.json.JSONObject;

import de.skiptag.roadrunner.persistence.Path;

public interface DataListener {

    void child_moved(JSONObject childSnapshot, String prevChildName,
	    boolean hasChildren, long numChildren);

    void child_added(String name, Path path, String parent, Object payload,
	    String prevChildName, boolean hasChildren, long numChildren);

    void child_removed(Path path, Object payload);

    public void child_changed(String name, Path path, String parent,
	    Object payload, String prevChildName, boolean hasChildren,
	    long numChildren);
}