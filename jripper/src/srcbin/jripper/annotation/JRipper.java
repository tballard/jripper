/*
 * srcbin
 */
package srcbin.jripper.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.SOURCE;

/**
 * Strip code from source file and make available to program.
 * For instance, if this was "Fred" all text between comments like 
 *    ///+ Fred ...
 * or
 *    ///+ Fred(3)...
 * and
 *    ///-...
 * would be collected. The "(3)", if given, gives an integer to use
 * to sort the sections by. The default is 0.  The code is put in the
 * file Fred.java.  The ... in the above lines mean you can put whatever 
 * you want.
 * 
 * @author tballard
 */
/** We don't care where in the file the annotation is */
@Target({PACKAGE})
@Retention(SOURCE)
public @interface JRipper {
   /** 
    * Source will be put in files in this directory. Use dots between
    * path parts. Example: "com.srcbin.great.ripped"
    */
   String dirName();
}
