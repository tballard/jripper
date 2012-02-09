/*
 * srcbin
 * $Id$
 */
package srcbin.jripper.processor;

/**
 * Contains the method for retrieving ripped text blocks.
 * @author tballard
 */
public class Source {
   
   /**
    * Retrieves the text that you asked to be ripped 
    * @param dirName The directory used in package-info.java
    * @param blockName The name from the block header
    */
   public static Block[] get(String dirName, String blockName) {
     System.out.println(System.getProperty("java.class.path"));
      try {
         Class clazz = Class.forName(dirName + '.' + blockName);
         return (Block[])clazz.getField("SRC").get(clazz);
      } catch (Exception ex) {
         throw new RuntimeException(ex);
      }
   }
}
