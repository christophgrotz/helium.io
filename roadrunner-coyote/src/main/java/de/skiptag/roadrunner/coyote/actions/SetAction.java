package de.skiptag.roadrunner.coyote.actions;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.common.base.Strings;

import de.skiptag.roadrunner.persistence.Persistence;

public class SetAction {

    private Persistence persistence;

    public SetAction(Persistence persistence) {
	this.persistence = persistence;
    }

    public void handle(JSONObject message, String path) throws JSONException {
	JSONObject payload;
	if (message.has("payload")) {
	    Object obj = message.get("payload");
	    if (obj instanceof JSONObject) {
		payload = (JSONObject) obj;
		if (payload instanceof JSONObject) {
		    if (Strings.isNullOrEmpty(path)) {
			persistence.update(null, payload);
		    } else {
			persistence.update(path, payload);
		    }
		}
	    } else if (obj == null || obj == JSONObject.NULL) {
		if (!Strings.isNullOrEmpty(path)) {
		    message.put("oldValue", persistence.get(path));
		    persistence.remove(path);
		}
	    } else {
		if (!Strings.isNullOrEmpty(path)) {
		    persistence.update(path, obj);
		}
	    }
	} else {
	    if (!Strings.isNullOrEmpty(path)) {
		message.put("oldValue", persistence.get(path));
		persistence.remove(path);
	    }
	}

    }

}
