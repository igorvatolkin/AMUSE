package amuse.nodes.annotation;

/**
 * Describes the type of AnnotationAttribute
 * NUMERIC - Attributes hold values of type Double
 * NOMINAL - Attributes hold values of type String and a List of allowed values
 * STRING - Attributes hold values of type String
 * EVENT - Attributes hold values of type Integer and only saves the time stamp
 * @author Frederik Heerde
 * @version $Id$
 */
public enum AnnotationAttributeType {
	NUMERIC, 
	NOMINAL, 
	STRING,
	EVENT;
	
	@Override
	public String toString(){
		switch(this){
		case NUMERIC: return "Numeric";
		case NOMINAL: return "Nominal";
		case STRING: return "String";
		case EVENT: return "Event";
		default: throw new IllegalArgumentException();
		}
	}
}
