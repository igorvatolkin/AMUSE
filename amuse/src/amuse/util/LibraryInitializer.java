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
 * Creation date: 15.06.2010
 */
package amuse.util;

import java.io.FileInputStream;

import com.rapidminer.RapidMiner;

/**
 * This class loads on demand libraries which are integrated in AMUSE 
 * @author Igor Vatolkin
 * @version $Id: $
 */
public class LibraryInitializer {
	
	private static boolean rapidMinerInitialized = false;
	
	/**
	 * Initializes RapidMiner as library
	 */
	public static void initializeRapidMiner(String pathToXml) throws Exception {
		if(!rapidMinerInitialized) {
			FileInputStream fis;
			try {
				fis = new FileInputStream(pathToXml);
				RapidMiner.init(fis,true,false,false,false);
			} catch(Exception e) {
				throw e;
			}
			rapidMinerInitialized = true;
		}
	}
	
}