package de.skiptag.roadrunner.disruptor.processor.eventsourcing;

import java.io.File;
import java.io.IOException;

import journal.io.api.ClosedJournalException;
import journal.io.api.CompactedDataFileException;
import journal.io.api.Journal;
import journal.io.api.Journal.ReadType;
import journal.io.api.Journal.WriteType;
import journal.io.api.Location;

import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.lmax.disruptor.EventHandler;

import de.skiptag.roadrunner.disruptor.Roadrunner;
import de.skiptag.roadrunner.disruptor.event.RoadrunnerEvent;

public class EventSourceProcessor implements EventHandler<RoadrunnerEvent> {
    private static final Logger logger = LoggerFactory.getLogger(EventSourceProcessor.class);

    private Journal journal = new Journal();
    private Roadrunner roadrunner;

    private Optional<Location> currentLocation = Optional.absent();

    public EventSourceProcessor(File journal_dir,
	    Roadrunner roadrunner)
	    throws IOException {
	journal.setDirectory(journal_dir);
	journal.open();
	this.roadrunner = roadrunner;
    }

    @Override
    public void onEvent(RoadrunnerEvent event, long sequence, boolean endOfBatch)
	    throws Exception {
	if (!event.isFromHistory()) {
	    currentLocation = Optional.of(journal.write(event.toString()
		    .getBytes(), WriteType.SYNC));
	}
    }

    public void restore() throws ClosedJournalException,
	    CompactedDataFileException, IOException, JSONException {
	Iterable<Location> redo;
	if (currentLocation.isPresent()) {
	    redo = journal.redo(currentLocation.get());
	} else {
	    redo = journal.redo();
	}
	for (Location location : redo) {
	    byte[] record = journal.read(location, ReadType.SYNC);
	    RoadrunnerEvent roadrunnerEvent;
	    try {
		roadrunnerEvent = new RoadrunnerEvent(new String(record));
		roadrunnerEvent.setFromHistory(true);
		Preconditions.checkArgument(roadrunnerEvent.has("type"), "No type defined in Event");
		Preconditions.checkArgument(roadrunnerEvent.has("basePath"), "No basePath defined in Event");
		Preconditions.checkArgument(roadrunnerEvent.has("repositoryName"), "No repositoryName defined in Event");
	    } catch (Exception exp) {
		logger.warn("Error in message (" + exp.getMessage() + "): "
			+ new String(record));
		roadrunnerEvent = null;

	    }
	    roadrunner.handleEvent(roadrunnerEvent);
	}
    }

    public Optional<Location> getCurrentLocation() {
	return currentLocation;
    }

    public void setCurrentLocation(Location currentLocation) {
	this.currentLocation = Optional.fromNullable(currentLocation);

    }

}