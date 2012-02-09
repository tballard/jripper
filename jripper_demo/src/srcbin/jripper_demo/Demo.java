/*
 * srcbin
 */
package srcbin.jripper_demo;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.WindowConstants;
import srcbin.jripper.processor.Block;
import srcbin.jripper.processor.Source;

/**
 * Demo of text ripping. The annotation is in package-info.java
 * 
 * @author tballard
 */
public class Demo extends JFrame {
   private static final long serialVersionUID = 0L;
   public int _fakey;

   /** Create window holding ripped text. */
   private Demo() {
      super("Source Demo");
      setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
      getContentPane().add(new JLabel(get()));
      pack();
      setVisible(true);
   }

   public static void main(String[] args) {
      new Demo();
   }

   /* 
    * The next line marks the start of rip. Weirdo is the file.
    * Priority 1 is used to sort blocks by 
    * Stuff starting ---- is recalled as info field in block.
    */
   ///+ Weirdo(1) ---- priority 1.  Yes, I'm sorta cheating to get this line
   /** Valuable method */
   private static String constructHeaderString(String name, Block block) {
      return "//++ " + name + '(' + block.getPriority() + ") " + 
         block.getInfo() + "\n";
   }
   ///----- nothing in this line matters except leading "//--" ---------

   ///+ Weirdo ------------- priority defaults to 0 (high) for this line
   /** How to get Strings of Source! */
   private static String get() {
      // This is how you grab the blocks
      Block[] blocks = Source.get(Info.DIR, "Weirdo");
      return new StringBuilder().
         append("<html><pre>Code of method getString():\n").
         append(blocks[0].getHtmlText(true)).
         append("\n\n").
         append(constructHeaderString("Weirdo", blocks[1])).
         append(blocks[1].getHtmlText(true)).
         append("\n//--\n").
         append("</pre></html>").
         toString();
   }
   ///----- end cut ----------------------------------------------------
}
