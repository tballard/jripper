package srcbin.jripper.processor;

/**
 * Stores location information of text block ripped from source code,
 * Immutable
 * @author tballard
 */
public class Block {
   private final String _text;
   private final int _priority;
   private final String _info;

   /**
    * 
    * @param text The text from a block of code
    * @param priority The priority indicated in the marker at the start of
    * the block.  
    * @param info Any extra data glommed on the end of the line.
    */
   public Block(String text, String priority, String info) {
      _text = text;
      _priority = priority == null ? 0 : Integer.parseInt(priority);
      _info = info == null ? "" : info;
   }

   /**
    * @param trimmed If true, as long as all lines have a space on the front
    * trim it
    * @return The text from the code block
    */
   public String getText(boolean trimmed) {
      return trimmed ? trim() : _text;
   }

   /** 
    * @return The priority indicated in the marker at the start of
    * the block.  
    */
   public int getPriority() {
      return _priority;
   }

   /** 
    * The start marker has a name, possible priority in prens a space and 
    * then whatever suits you.  This returns whatever part.
    * @return 
    */
   public String getInfo() {
      return _info;
   }

   /** 
    * @return Text convert to html-happy text for display on label or
    * something.
    */
   public String getHtmlText(boolean trimmed) {
      return (trimmed ? trim() : _text).replace("&", "&amp;").
         replace("<", "&lt;").replace(">", "&gt;").replace(" ", "&nbsp;");
   }

   /** 
    * As long as all lines have a space on the front trim it.
    * @return The reassembled code block.
    */
   private String trim() {
      String[] parts = _text.split("\n");
      int leastSpaces = Integer.MAX_VALUE;
      for (String part : parts) {
         int spaces = leadingSpaces(part);
         if (spaces < leastSpaces) {
            leastSpaces = spaces;
         }
      }
      if (leastSpaces == 0) {
         return _text;
      }
      StringBuilder builder = new StringBuilder();
      boolean first = true;
      for (String part : parts) {
         if (first) {
            first = false;
         } else {
            builder.append('\n');
         }
         builder.append(part.substring(leastSpaces));
      }
      return builder.toString();
   }

   /** @ return Number of leading spaces on string */
   private int leadingSpaces(String string) {
      int len = string.length();
      for (int i = 0; i < len; i++) {
         if (string.charAt(i) != ' ') {
            return i;
         }
      }
      return len;
   }
}
