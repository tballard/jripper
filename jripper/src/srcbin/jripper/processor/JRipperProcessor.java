package srcbin.jripper.processor;

import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Serializable;
import java.io.StringReader;
import java.io.Writer;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.processing.Filer;
import javax.tools.JavaFileObject;
import srcbin.jripper.annotation.JRipper;

/**
 * Processor for ripping blocks from text files.  The purpose I had in
 * mind was for writing demonstration code with popups that show the code
 * used to create the given functionality.
 * @author tballard
 */
@SupportedSourceVersion(SourceVersion.RELEASE_6)
@SupportedAnnotationTypes("srcbin.jripper.annotation.JRipper")
public class JRipperProcessor extends AbstractProcessor {
   private AtomicInteger _idGen = new AtomicInteger();
   private Trees _trees;
   private Map<String, List<OrderedBlock>> _source =
      new HashMap<String, List<OrderedBlock>>();
   private final Matcher _start =
      Pattern.compile(
      "^\\s*///\\+ ([_A-Za-z][_A-Za-z0-9]*)(?:\\((\\d+)\\))?(?: (.*))?$").
      matcher("");
   private final Matcher _end = Pattern.compile("^\\s*///-.*$").matcher("");

   @Override
   public void init(ProcessingEnvironment pe) {
      super.init(pe);
      _trees = Trees.instance(pe);
   }

   /**
    * @param annotations Annotations of type JRipper
    * @param roundEnvironment environment to get information about
    * current and previous round
    * @return whether or not the set of annotations are claimed by this
    *         processor
    */
   @Override
   public boolean process(Set<? extends TypeElement> annotations,
      RoundEnvironment roundEnvironment) {
      String dirName = null;
      List<JavaFileObject> files = new ArrayList<JavaFileObject>();
      for (Element e : roundEnvironment.getRootElements()) {
         JRipper jripper = e.getAnnotation(JRipper.class);
         if (jripper != null) { // package-info.java
            dirName = jripper.dirName(); // where to put generated files
         } else {
            TreePath path = _trees.getPath(e);
            if (path != null) {
               files.add(path.getCompilationUnit().getSourceFile());
            }
         }
      }
      if (dirName != null) {
         collectSource(files, dirName);
         sortBlockLists();
         createFiles();
      }
      return true;
   }

   /**
    * @param files List of files to check for annotations
    * @param dirName Where to put any files created. For building map
    * of file names to blocks.
    */
   private void collectSource(List<JavaFileObject> files, String dirName) {
      for (JavaFileObject javaFileObject : files) {
         try {
            BufferedReader reader = new BufferedReader(new StringReader(
               javaFileObject.getCharContent(true).toString()));
            try {
               read(reader, dirName, javaFileObject.getName());
            } finally {
               reader.close();
            }
         } catch (IOException ex) {
            throw new RuntimeException(ex);
         }
      }
   }

   /** 
    * Scan file for blocks and pack them into Block objects
    * @param reader
    * @param dirName For building path name of file
    * @param name file name
    * @throws IOException 
    */
   private void read(BufferedReader reader, String dirName, String name)
      throws IOException {
      boolean reading = false;
      boolean firstLine = false;
      String fileName = null;
      String priority = null;
      String info = null;
      StringBuilder builder = new StringBuilder(); // reuse for each block
      for (;;) {
         String line = reader.readLine();
         if (line == null) {
            break;
         }
         if (reading) {
            if (_end.reset(line).matches()) {
               reading = false;
               List<OrderedBlock> blocks = _source.get(fileName);
               if (blocks == null) {
                  blocks = new ArrayList<OrderedBlock>();
                  _source.put(fileName, blocks);
               }
               blocks.add(new OrderedBlock(builder.toString(),
                  priority, info, name));
               builder.setLength(0);
            } else {
               if (firstLine) {
                  firstLine = false;
               } else {
                  builder.append("\n");
               }
               builder.append(line);
            }
         } else if (_start.reset(line).matches()) {
            reading = true;
            firstLine = true;
            fileName = dirName + '.' + _start.group(1);
            priority = _start.group(2);
            info = _start.group(3);
         }
      }
      if (reading) {
         throw new RuntimeException("No end of block found in " + name);
      }
   }

   /** Write java files each containing array of Blocks. */
   private void createFiles() {
      Filer filer = processingEnv.getFiler();
      for (Entry<String, List<OrderedBlock>> entry : _source.entrySet()) {
         try {
            String path = entry.getKey();
            List<OrderedBlock> blocks = entry.getValue();
            JavaFileObject sourceFile = filer.createSourceFile(path);
            Writer writer = sourceFile.openWriter();
            try {
               int dotPos = path.lastIndexOf('.');
               if (dotPos != -1) {
                  writer.append("package " + path.substring(0, dotPos)
                     + ";  \n\n");
               }
               writer.append("\nimport srcbin.jripper.processor.Block;\n\n");
               String className =
                  dotPos == -1 ? path : path.substring(dotPos + 1);
               writer.append("public class ").append(className).append(" {\n").
                  append("   public static final Block[] SRC = {\n");
               for (OrderedBlock block : blocks) {
                  writer.append("     new Block(\n");
                  List<String> lines = new Massager(block.getText(false)).run();
                  int left = lines.size();
                  for (String line : lines) {
                     writer.append(line);
                     if (--left == 0) {
                        writer.append(',');
                     }
                     writer.append('\n');
                  }
                  writer.append("       \"").
                     append(Integer.toString(block.getPriority())).
                     append("\",\n").
                     append("       \"").
                     append(block.getInfo()).
                     append("\"),\n");
               }
               writer.append("   };\n}\n");
            } finally {
               writer.close();
            }
         } catch (IOException ex) {
            throw new RuntimeException(ex);
         }
      }
   }

   /** Class for massaging a block of text into something to print it. */
   private static class Massager {
      private String _text;
      private final String _base = "       \"";
      private final StringBuilder _builder = new StringBuilder(_base);
      private final int _baseLen = _base.length();

      private Massager(String text) {
         _text = text;
      }

      private List<String> run() {
         int len = _text.length();
         List<String> lines = new ArrayList<String>();
         for (int i = 0; i < len; i++) {
            char ch = _text.charAt(i);
            if (ch == '\n') {
               _builder.append("\\n\"");
               if (i < len - 1) {
                  _builder.append(" +");
               }
               lines.add(_builder.toString());
               _builder.setLength(_baseLen);
               continue;
            } else if (ch == '\t') {
               _builder.append("\\t");
            } else if (ch == '"') {
               _builder.append("\\\"");
            } else if (ch == '\\') {
               _builder.append("\\\\");
            } else {
               _builder.append(ch);
            }
         }
         if (_builder.length() > _baseLen) {
            _builder.append("\\n\"");
            lines.add(_builder.toString());
         }
         return lines;
      }
   }

   /** Use the priority and original file order to sort by. */
   private void sortBlockLists() {
      BlockComparator comparator = new BlockComparator();
      for (List<OrderedBlock> list : _source.values()) {
         Collections.sort(list, comparator);
      }
   }

   /** 
    * Class used for collecting block data.  We save file it came from and
    * int giving original file order. 
    */
   private class OrderedBlock extends Block {
      private int _id = _idGen.getAndIncrement();
      private String _fileName;

      public OrderedBlock(String text, String priority, String info,
         String fileName) {
         super(text, priority, info);
         _fileName = fileName;
      }
   }

   /** Sort blocks by priority, file, acquisition order. */
   private static class BlockComparator implements Comparator<OrderedBlock>,
      Serializable {
      private static final long serialVersionUID = 0L;

      @Override
      public int compare(OrderedBlock b1, OrderedBlock b2) {
         int diff = b1.getPriority() - b2.getPriority();
         if (diff != 0) {
            return diff;
         }
         diff = b1._fileName.compareTo(b1._fileName);
         return diff == 0 ? b1._id - b2._id : diff;
      }
   }
}
