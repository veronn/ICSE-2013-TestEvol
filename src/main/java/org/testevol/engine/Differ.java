package org.testevol.engine;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import org.testevol.domain.Version;
import org.testevol.engine.domain.VersionPair;
import org.testevol.engine.util.Utils;

public class Differ extends Task {
	
	private HashMap<VersionPair, JavaDifferencer> testDifferencers;
	private HashMap<VersionPair, JavaDifferencer> codeDifferencers;
	private String configDirectory;
	
	public Differ(List<Version> versions, String configDirectory) {
		super(versions);
		testDifferencers = new HashMap<VersionPair, JavaDifferencer>();
		codeDifferencers = new HashMap<VersionPair, JavaDifferencer>();
		if(!configDirectory.endsWith(File.separator)){
			configDirectory = configDirectory + File.separatorChar;
		}
		this.configDirectory = configDirectory;
	}

    @Override
    protected String[] getGeneratedFiles() {
        // TODO: update list of files
        String files[] = { "emptytrace.txt", "data-testslist-skipped-1.txt",
                "data-testslist-skipped-2.txt", "data-testout-*.txt",
                "data-trace-curr-*.txt", "wala-*.config" };
        return files;
    }

    @Override
    public boolean go(boolean force) throws Exception {
        if (!super.go(force)) {
            return false;
        }
        cleanUp();

        Version oldVersion = null;
        
        for (Version version:versions) {
            
            if (oldVersion == null) {
            	oldVersion = version;
                Utils.println("\nDiffing: Skipping first version: " + version.getName());
                continue;
            }            
            
            Utils.println("\nDiffing pair " + oldVersion.getName() + "-" + version.getName());

            String[] configFilesCode = new String[2];
            String[] configFilesTests = new String[2];

            configFilesCode[0] = generateWalaConfig(oldVersion, "wala-code.config", "code.jar");
            configFilesCode[1] = generateWalaConfig(version, "wala-code.config", "code.jar");
            
            configFilesTests[0] = generateWalaConfig(oldVersion, "wala-tests.config", "tests.jar");
            configFilesTests[1] = generateWalaConfig(version, "wala-tests.config", "tests.jar");

            String exclFile = configDirectory + "diff-SafeClassHierarchyExclusions.wala";


            JavaDifferencer javaDiffCode = new JavaDifferencer(configFilesCode[0], configFilesCode[1], exclFile);
            JavaDifferencer javaDiffTests = new JavaTestDifferencer(configFilesTests[0], configFilesTests[1], exclFile);
            
            VersionPair versionPair = new VersionPair(oldVersion, version);
            
            testDifferencers.put(versionPair, javaDiffTests);
            codeDifferencers.put(versionPair, javaDiffCode);
            
//            Classifier classifier = new Classifier(oldVersion, version, javaDiffTests);
//            
//            
//
//            Results results = classifier.classify();
//            searchClones(oldVersion, version, results);
//            
//            
//            resultsForEachPair.add(results);
            

            
            oldVersion = version;
        }
        
        markAsRun();
        return true;
    }
    
    private String generateWalaConfig(Version version, 
    								  String fileName,
    								  String jarFile) throws IOException{
        
    	String dir =  version.getBuildDir().getAbsolutePath();
    	String configFile =  dir + File.separator + fileName;

        // Creating the wala config file
        // Utils.println("Creating config file: " + configFile);
        FileWriter fwCode = new FileWriter(configFile);
        
        fwCode.write("Primordial,Java,stdlib,none\n");
        fwCode.write("Primordial,Java,jarFile," + configDirectory + File.separator + "primordial.jar.model\n");
        fwCode.write("Primordial,Java,jarFile," + configDirectory + File.separator + "junit-4.4.jar\n");
        fwCode.write("Application,Java,jarFile," + dir + File.separator + jarFile+ "\n");
        fwCode.close();
        
        return configFile;
    }
    
    
    public HashMap<VersionPair, JavaDifferencer> getCodeDifferencers() {
		return codeDifferencers;
	}
    
    public HashMap<VersionPair, JavaDifferencer> getTestDifferencers() {
		return testDifferencers;
	}
    
}