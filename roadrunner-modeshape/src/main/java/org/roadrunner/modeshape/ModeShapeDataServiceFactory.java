package org.roadrunner.modeshape;

import java.io.FileNotFoundException;
import java.util.Map;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;

import org.infinispan.schematic.document.ParsingException;
import org.modeshape.common.collection.Problems;
import org.modeshape.jcr.ConfigurationException;
import org.modeshape.jcr.ModeShapeEngine;
import org.modeshape.jcr.RepositoryConfiguration;
import org.roadrunner.core.DataService;
import org.roadrunner.core.DataServiceCreationException;
import org.roadrunner.core.DataServiceFactory;

import com.google.common.collect.Maps;

public class ModeShapeDataServiceFactory implements DataServiceFactory {

	private ModeShapeEngine engine;
	private Map<String, Repository> repositories = Maps.newHashMap();

	public ModeShapeDataServiceFactory() {
		engine = new ModeShapeEngine();
		engine.start();
	}

	private Repository getRepository(String repositoryName)
			throws ParsingException, ConfigurationException,
			RepositoryException, FileNotFoundException {
		if (repositories.containsKey(repositoryName)) {
			return repositories.get(repositoryName);
		}

		Repository repository = null;
		if (!engine.getRepositoryNames().contains(repositoryName)) {
			RepositoryConfiguration config = RepositoryConfiguration
					.read("{'name' : '"
							+ repositoryName
							+ "','jndiName': null,'workspaces' : {'predefined' : ['otherWorkspace'],'default' : 'default','allowCreation' : true},'security' : {'anonymous' : {'roles' : ['readonly','readwrite','admin'],'useOnFailedLogin' : false}}}");
			// We could change the name of the repository programmatically ...
			config = config.withName(repositoryName);

			// Verify the configuration for the repository ...
			Problems problems = config.validate();
			if (problems.hasErrors()) {
				throw new RuntimeException();
			}

			// Deploy the repository ...
			repository = engine.deploy(config);
		} else {
			repository = engine.getRepository(repositoryName);
		}
		repositories.put(repositoryName, repository);
		return repository;
	}

	public DataService getDataService(String repositoryName)
			throws DataServiceCreationException {
		try {
			Repository commonRepo = getRepository("common");
			Repository dataRepo = getRepository(repositoryName);
			return new ModeShapeDataService(commonRepo.login(),
					dataRepo.login());
		} catch (Exception exp) {
			throw new DataServiceCreationException(exp);
		}
	}
}
