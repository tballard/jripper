@JRipper(dirName = Info.DIR)    // This is how you annotate a package.
package srcbin.jripper_demo;
import srcbin.jripper.annotation.JRipper;  // Import the code ripper

class Info {
   /** This is the directory to throw the code blocks in. */
   static final String DIR = "srcbin.jripper.code_blocks";
}