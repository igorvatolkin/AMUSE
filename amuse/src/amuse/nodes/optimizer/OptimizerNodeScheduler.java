/** 
 * This file is part of AMUSE framework (Advanced MUsic Explorer).
 * 
 * Copyright 2006-2010 by code authors
 * 
 * Created at TU Dortmund, Chair of Algorithm Engineering
 * (Contact: <http://ls11-www.cs.tu-dortmund.de>) 
 *
 * AMUSE is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * AMUSE is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with AMUSE. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Creation date: 30.03.2008
 */ 
package amuse.nodes.optimizer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Properties;

import org.apache.log4j.Level;

import amuse.interfaces.nodes.TaskConfiguration;
import amuse.interfaces.nodes.NodeEvent;
import amuse.interfaces.nodes.NodeException;
import amuse.interfaces.nodes.NodeScheduler;
import amuse.interfaces.nodes.methods.AmuseTask;
import amuse.nodes.optimizer.interfaces.OptimizerInterface;
import amuse.preferences.AmusePreferences;
import amuse.util.AmuseLogger;

import weka.core.Attribute;
import weka.core.Instance;
import weka.core.converters.ArffLoader;

/**
 * OptimizerNodeScheduler is responsible for the optimizer node. 
 * 
 * @author Igor Vatolkin
 * @version $Id: OptimizerNodeScheduler.java 1064 2010-07-01 14:04:42Z vatolkin $
 */
public class OptimizerNodeScheduler extends NodeScheduler { 

	/** Optimizer adapter */
	OptimizerInterface oad = null;
	
	/** Parameters for optimization algorithm if required */
	private String requiredParameters = null;
	
	/**
	 * Constructor
	 */
	public OptimizerNodeScheduler(String folderForResults) throws NodeException {
		super(folderForResults);
		requiredParameters = new String();
	}
	
	/**
	 * Main method for optimization
	 * @param args Optimization configuration
	 */
	public static void main(String[] args) {
		
		// Create the node scheduler
		OptimizerNodeScheduler thisScheduler = null;
		try {
			thisScheduler = new OptimizerNodeScheduler(args[0] + "/input/task_" + args[1]);
		} catch(NodeException e) {
			AmuseLogger.write(OptimizerNodeScheduler.class.getName(), Level.ERROR,
					"Could not create folder for optimizer node intermediate results: " + e.getMessage());
			return;
		}
		
		// Proceed the task
		thisScheduler.proceedTask(args);
		
		// Remove the folder for input and intermediate results
		try {
			thisScheduler.removeInputFolder();
		} catch(NodeException e) {
				AmuseLogger.write(OptimizerNodeScheduler.class.getClass().getName(), Level.WARN,
					"Could not remove properly the folder with intermediate results '" + 
					thisScheduler.nodeHome + "/input/task_'" + thisScheduler.jobId + 
					"; please delete it manually! (Exception: "+ e.getMessage() + ")");
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see amuse.interfaces.nodes.NodeSchedulerInterface#proceedTask(java.lang.String, long, amuse.interfaces.nodes.TaskConfiguration)
	 */
	public void proceedTask(String nodeHome, long jobId, TaskConfiguration optimizationConfiguration) {

		// -----------------------------------------
		// (I) Configure optimization node scheduler
		// -----------------------------------------
		this.nodeHome = nodeHome;
		if(this.nodeHome.startsWith(System.getenv("AMUSEHOME"))) {
			this.directStart = true;
		}
		this.jobId = new Long(jobId);
		this.taskConfiguration = optimizationConfiguration;
		
		// If this node is started directly, the properties are loaded from AMUSEHOME folder;
		// if this node is started via command line (e.g. in a grid, the properties are loaded from
		// %optimizer home folder%/input
		if(!this.directStart) {
			File preferencesFile = new File(this.nodeHome + "/input/task_" + this.jobId + "/amuse.properties");
			AmusePreferences.restoreFromFile(preferencesFile);
		}
		
		AmuseLogger.write(this.getClass().getName(), Level.INFO, "Optimizer node scheduler for category " + 
				((OptimizationConfiguration)this.taskConfiguration).getTrainingInput() + " started");
		
		// ----------------------------------
		// (II) Configure optimization method
		// ----------------------------------
		try {
			this.configureOptimizationMethod();
		} catch(NodeException e) {
			AmuseLogger.write(this.getClass().getName(), Level.FATAL,  
					"Configuration of optimizer failed: " + e.getMessage()); 
			System.exit(1);
		}
		
		// -------------------------------
		// (III) Start optimization method
		// -------------------------------
		try {
			this.optimize();
		} catch(NodeException e) {
			AmuseLogger.write(this.getClass().getName(), Level.FATAL,  
					"Optimization failed: " + e.getMessage()); 
			System.exit(1);
		}
		
		// ----------------------------------------------------------------------------------
		// (IV) If started directly, remove generated data and fire event for Amuse scheduler
		// ----------------------------------------------------------------------------------
		if(this.directStart) {
			try {
				this.cleanInputFolder();
			} catch(NodeException e) {
				AmuseLogger.write(this.getClass().getName(), Level.ERROR,
					"Could not remove properly the intermediate results '" + 
					this.nodeHome + "/input/task_'" + this.jobId + "; please delete it manually! (Exception: "+ e.getMessage() + ")");
				System.exit(1);
			}
			this.fireEvent(new NodeEvent(NodeEvent.OPTIMIZATION_COMPLETED, this));
		}
		
	}
	
	/*
	 * (non-Javadoc)
	 * @see amuse.interfaces.nodes.NodeSchedulerInterface#proceedTask(java.lang.String[])
	 */
	public void proceedTask(String[] args) {
		if(args.length < 2) {
			AmuseLogger.write(this.getClass().getName(), Level.FATAL, 2 - args.length + 
					" arguments are missing; The usage is 'OptimizerNodeScheduler %1 %2', where: \n" +
					"%1 - Home folder of this node\n" +
					"%2 - Unique (for currently running Amuse instance) task Id\n"); 
			System.exit(1);
		}
		
		// Load the task configuration from %OPTIMIZERHOME%/task.ser
		OptimizationConfiguration optimizerConfig[] = null;
		FileInputStream fis = null;
		ObjectInputStream in = null;
		try {
			fis = new FileInputStream(args[0] + "/task_" + args[1] + ".ser");
			in = new ObjectInputStream(fis);
			Object o = in.readObject();
			optimizerConfig = (OptimizationConfiguration[])o;
			in.close();
		} catch(IOException ex) {
		    ex.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		
		// Proceed the optimization task(s)
		for(int i=0;i<optimizerConfig.length;i++) {
			proceedTask(args[0],new Long(args[1]),optimizerConfig[i]);
			AmuseLogger.write(this.getClass().getName(), Level.INFO, "Optimizer node is going to start job " + 
					(i+1) + "/" + optimizerConfig.length);
		}
	}
	
	/**
	 * Configures the optimization method
	 * @throws NodeException
	 */
	private void configureOptimizationMethod() throws NodeException {
		Integer requiredAlgorithm; 

		// If parameter string for this algorithm exists..
		if(((OptimizationConfiguration)this.taskConfiguration).getAlgorithmDescription().contains("[") && 
				((OptimizationConfiguration)this.taskConfiguration).getAlgorithmDescription().contains("]")) {
			requiredAlgorithm = new Integer(((OptimizationConfiguration)this.taskConfiguration).
					getAlgorithmDescription().substring(0,((OptimizationConfiguration)this.
					taskConfiguration).getAlgorithmDescription().indexOf("[")));
			this.requiredParameters = ((OptimizationConfiguration)this.taskConfiguration).
					getAlgorithmDescription().substring(((OptimizationConfiguration)this.
					taskConfiguration).getAlgorithmDescription().indexOf("[")+1,
					((OptimizationConfiguration)this.taskConfiguration).getAlgorithmDescription().lastIndexOf("]")); 
		} else {
			requiredAlgorithm = new Integer(((OptimizationConfiguration)this.taskConfiguration).getAlgorithmDescription());
			this.requiredParameters = null;
		}
		boolean algorithmFound = false;
		try {
	    	ArffLoader optimizerTableLoader = new ArffLoader();
	    	if(this.directStart) {
	    		optimizerTableLoader.setFile(new File(System.getenv("AMUSEHOME") + "/config/optimizerAlgorithmTable.arff"));
	    	} else {
	    		optimizerTableLoader.setFile(new File(this.nodeHome + "/input/task_" + this.jobId + "/optimizerAlgorithmTable.arff"));
	    	}
			Instance currentInstance = optimizerTableLoader.getNextInstance(optimizerTableLoader.getStructure());
			Attribute idAttribute = optimizerTableLoader.getStructure().attribute("Id");
			Attribute nameAttribute = optimizerTableLoader.getStructure().attribute("Name");
			Attribute optimizerAdapterClassAttribute = optimizerTableLoader.getStructure().attribute("OptimizerAdapterClass");
			while(currentInstance != null) {
				Integer idOfCurrentAlgorithm = new Double(currentInstance.value(idAttribute)).intValue();
				if(idOfCurrentAlgorithm.equals(requiredAlgorithm)) {
					
					// Configure the adapter class
					try {
						Class<?> adapter = Class.forName(currentInstance.stringValue(optimizerAdapterClassAttribute));
						this.oad = (OptimizerInterface)adapter.newInstance();
						Properties optimizerProperties = new Properties();
						Integer id = new Double(currentInstance.value(idAttribute)).intValue();
						optimizerProperties.setProperty("id",id.toString());
						optimizerProperties.setProperty("name",currentInstance.stringValue(nameAttribute));
						((AmuseTask)this.oad).configure(optimizerProperties,this,this.requiredParameters);
						
						AmuseLogger.write(this.getClass().getName(), Level.INFO, 
								"Optimizer is configured: " + currentInstance.stringValue(optimizerAdapterClassAttribute));
					} catch(ClassNotFoundException e) {
						e.printStackTrace();
						AmuseLogger.write(this.getClass().getName(), Level.ERROR, 
								"Optimizer class cannot be located: " + currentInstance.stringValue(optimizerAdapterClassAttribute));
						System.exit(1);
					} catch(IllegalAccessException e) {
						AmuseLogger.write(this.getClass().getName(), Level.ERROR, 
								"Optimizer class or its nullary constructor is not accessible: " + currentInstance.stringValue(optimizerAdapterClassAttribute));
						System.exit(1);
					} catch(InstantiationException e) {
						AmuseLogger.write(this.getClass().getName(), Level.ERROR, 
								"Instantiation failed for optimizer class: " + currentInstance.stringValue(optimizerAdapterClassAttribute));
						System.exit(1);
					} catch(NodeException e) {
						AmuseLogger.write(this.getClass().getName(), Level.ERROR, 
								"Setting of parameters failed for Optimizer class: " + e.getMessage());
						System.exit(1);
					}
					
					algorithmFound = true;
					break;
				} 
				currentInstance = optimizerTableLoader.getNextInstance(optimizerTableLoader.getStructure());
			}
			
			if(!algorithmFound) {
				AmuseLogger.write(this.getClass().getName(), Level.ERROR, 
						"Algorithm with id " + ((OptimizationConfiguration)this.taskConfiguration).getAlgorithmDescription() + 
						" was not found, task aborted");
				System.exit(1);
			}

	    } catch(IOException e) {
	    	throw new NodeException(e.getMessage());
	    }
	}
	
	/**
	 * Starts optimization method
	 * @throws NodeException
	 */
	private void optimize() throws NodeException {
		try {
	    	
	    	// Run the optimization
			this.oad.optimize();
	    } catch(NodeException e) {
			throw e;
	    }
	}

	
}

