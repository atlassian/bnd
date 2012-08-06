package test.repository;

import java.io.*;
import java.util.*;

import junit.framework.*;
import test.lib.*;
import aQute.bnd.deployer.repository.*;
import aQute.bnd.osgi.*;
import aQute.bnd.service.*;
import aQute.bnd.service.RepositoryPlugin.PutResult;
import aQute.lib.io.*;

public class TestLocalIndexGeneration extends TestCase {

	private Processor				reporter;
	private LocalIndexedRepo		repo;
	private File					outputDir;
	private HashMap<String,String>	config;

	protected void setUp() throws Exception {
		// Ensure output directory exists and is empty
		outputDir = new File("generated/testoutput");
		IO.delete(outputDir);
		if (!outputDir.exists() && !outputDir.mkdirs()) {
			throw new IOException("Could not create directory " + outputDir);
		}

		// Setup the repo
		reporter = new Processor();
		repo = new LocalIndexedRepo();
		config = new HashMap<String,String>();
		config.put("local", outputDir.getAbsolutePath());
		config.put("type", "R5");
		repo.setProperties(config);
		repo.setReporter(reporter);
	}

	@Override
	protected void tearDown() throws Exception {
		IO.delete(outputDir);
		assertEquals(0, reporter.getErrors().size());
		assertEquals(0, reporter.getWarnings().size());
	}

	public void testInitiallyEmpty() throws Exception {
		List<String> list = repo.list(".*");
		assertNotNull(list);
		assertEquals(0, list.size());
	}

	public void testDeployBundle() throws Exception {
		PutResult r = repo.put(new BufferedInputStream(new FileInputStream("testdata/bundles/name.njbartlett.osgi.emf.minimal-2.6.1.jar")), new RepositoryPlugin.PutOptions());
		File deployedFile = new File(r.artifact);

		assertEquals(IO.getFile("generated/testoutput/name.njbartlett.osgi.emf.minimal/name.njbartlett.osgi.emf.minimal-2.6.1.jar")
			.getAbsolutePath(), deployedFile.getAbsolutePath());

		File indexFile = IO.getFile("generated/testoutput/index.xml.gz");
		assertTrue(indexFile.exists());

		AbstractIndexedRepo repo2 = createRepoForIndex(indexFile);
		File[] files = repo2.get("name.njbartlett.osgi.emf.minimal", null);
		assertNotNull(files);
		assertEquals(1, files.length);
		assertEquals(deployedFile.getAbsoluteFile(), files[0]);
	}
	
	public void testOverwrite() throws Exception {
		config.put("overwrite", "false");
		repo.setProperties(config);
		
		PutResult r = repo.put(new BufferedInputStream(new FileInputStream("testdata/bundles/name.njbartlett.osgi.emf.minimal-2.6.1.jar")), new RepositoryPlugin.PutOptions());
		File originalFile = new File(r.artifact);
		assertEquals(IO.getFile("generated/testoutput/name.njbartlett.osgi.emf.minimal/name.njbartlett.osgi.emf.minimal-2.6.1.jar")
				.getAbsolutePath(), originalFile.getAbsolutePath());
		
		Jar newJar = new Jar(IO.getFile("testdata/bundles/name.njbartlett.osgi.emf.minimal-2.6.1.jar"));
		Jar dummyJar = new Jar(IO.getFile("testdata/bundles/dummybundle.jar"));
		newJar.putResource("testOverwrite/dummybundle.jar", new JarResource(dummyJar));
		newJar.write("testdata/bundles/name.njbartlett.osgi.emf.minimal-2.6.1-testOverwrite.jar");
		r = repo.put(new BufferedInputStream(new FileInputStream("testdata/bundles/name.njbartlett.osgi.emf.minimal-2.6.1-testOverwrite.jar")), new RepositoryPlugin.PutOptions());
		IO.delete(new File("testdata/bundles/name.njbartlett.osgi.emf.minimal-2.6.1-testOverwrite.jar"));
		assertNull(r.artifact);
	}

	public void testInvalidContentProvider() throws Exception {
		LocalIndexedRepo repo = new LocalIndexedRepo();
		Map<String,String> config = new HashMap<String,String>();
		config.put("local", outputDir.getAbsolutePath());
		config.put("type", "Rubbish");
		repo.setProperties(config);
		repo.setReporter(reporter);

		repo.put(new BufferedInputStream(new FileInputStream("testdata/bundles/name.njbartlett.osgi.emf.minimal-2.6.1.jar")), new RepositoryPlugin.PutOptions());

		assertEquals(0, reporter.getErrors().size());
		assertTrue(reporter.getWarnings().size() > 0);
		reporter.clear();
	}

	public void testNonGeneratingProvider() throws Exception {
		MockRegistry registry = new MockRegistry();
		registry.addPlugin(new NonGeneratingProvider());

		LocalIndexedRepo repo = new LocalIndexedRepo();
		repo.setRegistry(registry);
		repo.setReporter(reporter);

		Map<String,String> config = new HashMap<String,String>();
		config.put("local", outputDir.getAbsolutePath());
		config.put("type", "Nongenerating");
		repo.setProperties(config);

		repo.put(new BufferedInputStream(new FileInputStream("testdata/bundles/name.njbartlett.osgi.emf.minimal-2.6.1.jar")), new RepositoryPlugin.PutOptions());

		assertEquals(0, reporter.getErrors().size());
		assertTrue(reporter.getWarnings().size() > 0);
		reporter.clear();
	}

	public void testFailToGenerate() throws Exception {
		MockRegistry registry = new MockRegistry();
		registry.addPlugin(new FailingGeneratingProvider());

		LocalIndexedRepo repo = new LocalIndexedRepo();
		repo.setRegistry(registry);
		repo.setReporter(reporter);

		Map<String,String> config = new HashMap<String,String>();
		config.put("local", outputDir.getAbsolutePath());
		config.put("type", "Fail");
		repo.setProperties(config);

		repo.put(new BufferedInputStream(new FileInputStream("testdata/bundles/name.njbartlett.osgi.emf.minimal-2.6.1.jar")), new RepositoryPlugin.PutOptions());

		assertTrue(reporter.getErrors().size() > 0);
		assertEquals(0, reporter.getWarnings().size());
		reporter.clear();
	}

	// UTILS

	private static AbstractIndexedRepo createRepoForIndex(File index) {
		FixedIndexedRepo newRepo = new FixedIndexedRepo();

		Map<String,String> config = new HashMap<String,String>();
		config.put("locations", index.getAbsoluteFile().toURI().toString());
		newRepo.setProperties(config);

		return newRepo;
	}
}
