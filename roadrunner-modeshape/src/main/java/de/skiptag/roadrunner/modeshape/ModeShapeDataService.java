package de.skiptag.roadrunner.modeshape;

import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import de.skiptag.roadrunner.core.DataListener;
import de.skiptag.roadrunner.core.DataService;
import de.skiptag.roadrunner.core.authorization.AuthorizationService;
import de.skiptag.roadrunner.core.authorization.RoadrunnerOperation;
import de.skiptag.roadrunner.core.dtos.PushedMessage;

public class ModeShapeDataService implements DataService {

    private Session dataRepo;
    private Node rootNode;

    private AuthorizationService authorizationService;
    private JSONObject auth = new JSONObject();
    private Set<DataListener> listeners = Sets.newHashSet();
    private static Map<String, JSONObject> removedNodes = Maps.newHashMap();

    public static JSONObject transformToJSON(Node node)
	    throws ValueFormatException, RepositoryException, JSONException {
	JSONObject object = new JSONObject();
	PropertyIterator itr = node.getProperties();
	while (itr.hasNext()) {
	    Property property = itr.nextProperty();
	    try {
		if (property.getValue().getType() == PropertyType.BINARY) {
		    // object.put(property, property.getBinary().);
		} else if (property.getValue().getType() == PropertyType.BOOLEAN) {
		    object.put(property.getName(), property.getBoolean());
		} else if (property.getValue().getType() == PropertyType.DATE) {
		    object.put(property.getName(), new Date(property.getDate()
			    .getTimeInMillis()));
		} else if (property.getValue().getType() == PropertyType.DECIMAL) {
		    object.put(property.getName(), property.getDecimal());
		} else if (property.getValue().getType() == PropertyType.DOUBLE) {
		    object.put(property.getName(), property.getDouble());
		} else if (property.getValue().getType() == PropertyType.LONG) {
		    object.put(property.getName(), property.getLong());
		} else if (property.getValue().getType() == PropertyType.NAME) {
		} else if (property.getValue().getType() == PropertyType.PATH) {
		} else if (property.getValue().getType() == PropertyType.REFERENCE) {
		} else if (property.getValue().getType() == PropertyType.STRING) {
		    object.put(property.getName(), property.getString());
		} else if (property.getValue().getType() == PropertyType.UNDEFINED) {
		} else if (property.getValue().getType() == PropertyType.URI) {
		} else if (property.getValue().getType() == PropertyType.WEAKREFERENCE) {
		}
	    } catch (Exception e) {
		// e.printStackTrace();
	    }
	}
	return object;
    }

    public ModeShapeDataService(AuthorizationService authorizationService,
	    Session dataRepo) throws UnsupportedRepositoryOperationException,
	    RepositoryException {
	this.dataRepo = dataRepo;
	this.authorizationService = authorizationService;

	rootNode = dataRepo.getRootNode();
    }

    @Override
    public void addListener(DataListener listener) {
	this.listeners.add(listener);
    }

    private Node addNode(Node node2, String path2) throws ItemExistsException,
	    PathNotFoundException, VersionException,
	    ConstraintViolationException, LockException, RepositoryException {
	if (path2.indexOf("/") > -1) {
	    String subpath = path2.substring(0, path2.indexOf("/"));
	    Node newNode;
	    if (node2.hasNode(subpath)) {
		newNode = node2.getNode(subpath);
	    } else {
		newNode = node2.addNode(subpath);
	    }
	    dataRepo.save();
	    return addNode(newNode, path2.substring(path2.indexOf("/") + 1));
	} else {
	    Node newNode;
	    if (node2.hasNode(path2)) {
		newNode = node2.getNode(path2);
	    } else {
		newNode = node2.addNode(path2);
	    }

	    dataRepo.save();
	    return newNode;
	}
    }

    public void fireChildAdded(String name, String path, String parentName,
	    Object transformToJSON, String prevChildName, boolean hasNodes,
	    long size) {
	for (DataListener listener : listeners) {
	    listener.child_added(name, path, parentName, transformToJSON, prevChildName, hasNodes, size);
	}
    }

    public void fireChildChanged(String name, String path, String parentName,
	    Object transformToJSON, String prevChildName, boolean hasNodes,
	    long size) {
	for (DataListener listener : listeners) {
	    listener.child_changed(name, path, parentName, transformToJSON, prevChildName, hasNodes, size);
	}
    }

    public void fireChildMoved(JSONObject childSnapshot, String prevChildName,
	    boolean hasNodes, long size) {
	for (DataListener listener : listeners) {
	    listener.child_moved(childSnapshot, prevChildName, hasNodes, size);
	}
    }

    public void fireChildRemoved(String path, JSONObject fromRemovedNodes) {
	for (DataListener listener : listeners) {
	    listener.child_removed(path, fromRemovedNodes);
	}
    }

    @Override
    public JSONObject get(String path) {
	try {
	    return transformToJSON(dataRepo.getNode(path));
	} catch (ValueFormatException e) {
	    throw new RuntimeException(e);
	} catch (PathNotFoundException e) {
	    throw new RuntimeException(e);
	} catch (RepositoryException e) {
	    throw new RuntimeException(e);
	} catch (JSONException e) {
	    throw new RuntimeException(e);
	}
    }

    @Override
    public String getName(String path) {
	try {
	    return dataRepo.getNode(path).getName();
	} catch (ValueFormatException e) {
	    throw new RuntimeException(e);
	} catch (PathNotFoundException e) {
	    throw new RuntimeException(e);
	} catch (RepositoryException e) {
	    throw new RuntimeException(e);
	}
    }

    @Override
    public String getParent(String path) {
	try {
	    return dataRepo.getNode(path).getParent().getPath();
	} catch (ValueFormatException e) {
	    throw new RuntimeException(e);
	} catch (PathNotFoundException e) {
	    return null;
	} catch (RepositoryException e) {
	    throw new RuntimeException(e);
	}
    }

    private String getPrevChildName(Node node) {
	try {
	    String prevChildName;
	    if (node.getIndex() == 1) {
		prevChildName = null;
	    } else {
		prevChildName = node.getParent()
			.getNode("*[" + (node.getIndex() - 1) + "]")
			.getName();
	    }
	    return prevChildName;
	} catch (Exception exp) {
	    return null;
	}
    }

    @Override
    public void query(String expression, final QueryCallback queryCallback) {
	try {
	    // Obtain the query manager for the session via the workspace ...
	    javax.jcr.query.QueryManager queryManager = dataRepo.getWorkspace()
		    .getQueryManager();

	    javax.jcr.query.Query query = queryManager.createQuery(expression, "JCR-SQL2");

	    if (!dataRepo.getRootNode().hasNode("queries")) {
		dataRepo.getRootNode().addNode("queries");
	    }

	    NodeIterator nodeIterator = query.execute().getNodes();
	    while (nodeIterator.hasNext()) {
		Node node = nodeIterator.nextNode();
		if (!node.getName().equals("jcr:system")
			&& !node.getName().equals("mode:repository")
			&& !"mode:root".equals(node.getPrimaryNodeType()
				.getName())) {
		    try {
			queryCallback.change(node.getPath(), transformToJSON(node), node.getDepth() != 0 ? node.getParent()
				.getPath()
				: null, node.getNodes().getSize(), node.getName(), node.hasNodes(), node.getIndex());
		    } catch (JSONException e) {
			e.printStackTrace();
		    }
		}
	    }
	} catch (RepositoryException e) {
	    throw new RuntimeException(e);
	}
    }

    @Override
    public void remove(String path) {
	try {
	    Node node = rootNode.getNode(path.startsWith("/") ? path.substring(1)
		    : path);
	    removedNodes.put(node.getPath(), transformToJSON(node));
	    authorizationService.authorize(RoadrunnerOperation.REMOVE, auth, new ModeshapeRulesDataSnapshot(
		    rootNode), node.getPath(), new ModeshapeRulesDataSnapshot(
		    node));
	    node.remove();
	    dataRepo.save();

	} catch (Exception exp) {
	    throw new RuntimeException(exp);
	}

    }

    @Override
    public void removeListener(DataListener dataListener) {
	this.listeners.remove(dataListener);
    }

    private void sendNode(Node node) throws RepositoryException, JSONException,
	    IOException {
	NodeIterator itr = node.getNodes();
	while (itr.hasNext()) {
	    Node childNode = itr.nextNode();
	    if (authorizationService.isAuthorized(RoadrunnerOperation.READ, auth, new ModeshapeRulesDataSnapshot(
		    rootNode), childNode.getPath(), new ModeshapeRulesDataSnapshot(
		    childNode))) {
		if (!childNode.getName().equals("jcr:system")
			&& !childNode.getName().equals("mode:repository")
			&& !"mode:root".equals(childNode.getPrimaryNodeType()
				.getName())) {
		    fireChildAdded(childNode.getName(), childNode.getPath(), childNode.getParent()
			    .getName(), transformToJSON(childNode), null, childNode.hasNodes(), childNode.getNodes()
			    .getSize());
		}
	    }
	}
    }

    @Override
    public void shutdown() {
	listeners.clear();
	dataRepo.logout();
    }

    @Override
    public void sync(String path) {

	try {
	    String relPath = path.startsWith("/") ? path.substring(1) : path;
	    Node node = retrieveNodeForPath(relPath);
	    sendNode(node);
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

    @Override
    public PushedMessage update(String path, Object payload) {
	if (path.startsWith("/")) {
	    path = path.substring(1);
	}
	try {
	    Node node = retrieveNodeForPath(path);
	    // TODO doesn't work for simple values
	    updateNode((JSONObject) payload, node);
	    dataRepo.save();
	    return new PushedMessage(node.getParent().getName(),
		    getPrevChildName(node), transformToJSON(node),
		    node.hasNodes(), node.getNodes().getSize());
	} catch (Exception exp) {
	    throw new RuntimeException(exp);
	}
    }

    private Node retrieveNodeForPath(String path) throws RepositoryException,
	    PathNotFoundException, ItemExistsException, VersionException,
	    ConstraintViolationException, LockException {
	Node node;
	if (!Strings.isNullOrEmpty(path)) {
	    if (rootNode.hasNode(path)) {
		node = rootNode.getNode(path);
	    } else {
		node = addNode(rootNode, path);
	    }
	} else {
	    node = rootNode;
	}
	return node;
    }

    private void updateNode(JSONObject object, Node node)
	    throws ValueFormatException, RepositoryException, JSONException {
	authorizationService.authorize(RoadrunnerOperation.WRITE, auth, new ModeshapeRulesDataSnapshot(
		rootNode), node.getPath(), new ModeshapeRulesDataSnapshot(node));

	Iterator<?> itr = object.keys();
	while (itr.hasNext()) {
	    String key = (String) itr.next();
	    Object value = object.get(key);
	    if (value instanceof JSONArray) {
		// TODO: not yet handleable
	    } else if (value instanceof JSONObject) {
		Node childNode = node.addNode("key");
		updateNode((JSONObject) value, childNode);
	    } else if (value instanceof Boolean) {
		node.setProperty(key, object.getBoolean(key));
	    } else if (value instanceof Long) {
		node.setProperty(key, object.getLong(key));
	    } else if (value instanceof Integer) {
		node.setProperty(key, object.getInt(key));
	    } else if (value instanceof Double) {
		node.setProperty(key, object.getDouble(key));
	    } else {
		node.setProperty(key, "" + value);
	    }
	}
    }

    @Override
    public void updateSimpleValue(String path, Object value) {
	if (path.startsWith("/")) {
	    path = path.substring(1);
	}
	String key = path.substring(path.lastIndexOf("/") + 1);
	path = path.substring(0, path.lastIndexOf("/"));
	try {
	    Node node;
	    node = retrieveNodeForPath(path);
	    if (value instanceof Boolean) {
		node.setProperty(key, (Boolean) value);
	    } else if (value instanceof Long) {
		node.setProperty(key, (Long) value);
	    } else if (value instanceof Integer) {
		node.setProperty(key, (Integer) value);
	    } else if (value instanceof Double) {
		node.setProperty(key, (Double) value);
	    } else {
		node.setProperty(key, "" + value);
	    }
	    dataRepo.save();
	} catch (Exception exp) {
	    throw new RuntimeException(exp);
	}
    }

    public void setAuth(JSONObject auth) {
	this.auth = auth;
    }

    @Override
    public JSONObject dumpSnapshot() {
	// TODO Auto-generated method stub
	return null;
    }

    @Override
    public void restoreSnapshot(JSONObject payload) throws JSONException {
	// TODO Auto-generated method stub

    }
}