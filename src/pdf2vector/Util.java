
package pdf2vector;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.xmlgraphics.java2d.CMYKColorSpace;

/**
 *
 * @author bronson
 */
public class Util {
    // from pdfbox src
    static final int DEFAULT_USER_SPACE_UNIT_DPI = 72;
    public static final float MM_TO_UNITS = 1/(10*2.54f)*DEFAULT_USER_SPACE_UNIT_DPI;
    

    public static final Map<String, String> font_map = new HashMap<String, String>() {{
        put("futurabt-extrablack", "Futura XBlk BT");
        put("futurabt-extrablackcondensed", "Futura XBlkCn BT");
        put("futurabt-medium", "Futura Md BT");
        put("futurabt-medium-bold", "Futura Md BT");
        put("futurabt-mediumcondensed", "Futura MdCn BT");
        put("palatino-roman", "Palatino");
        put("palatino-bold", "Palatino");
        put("timesnewromanpsmt-bold", "Times New Roman");
        put("times_pp", "swtimes");
        put("swtimesnormal","swtimes");
        put("timesiitalic", "swtimesi");
        put("swtimesiitalic", "swtimesi");
        put("swtimesbbold", "swtimesb");
        put("timesnewromanpsmt", "Times New Roman");
        put("timesnewromanpsmt-bolditalic", "Times New Roman");
        put("timesnewromanpsmt-normalitalic", "Times New Roman");
        put("timesnewroman", "Times New Roman");
        put("couriernewpsmt", "Courier New");
        put("georgia-bolditalic", "Georgia");
        put("georgia", "Georgia");
        put("frutiger-ultrablack", "Frutiger 95");
        put("geometr415blk", "Geometr415 Blk BT");
        put("geometric415bt-blacka-normalitalic","Geometr415 Blk BT");
        put("arialnarrow-bold", "Arial Narrow");
        put("arialnarrow-normalitalic", "Arial Narrow");
        put("arialnarrow", "Arial Narrow");
        put("arialmt", "Arial");
        put("arialmt-bold", "Arial");
        
        // unchanged
        put("swfrankgh","swfrankgh");
        put("swgeo213h","swgeo213h");
        put("swgeo415md","swgeo415md");
        put("geo415ki","geo415ki");
        put("geo415ki-normalitalic","geo415ki");
        put("line10","line10"); // number line font
        
        // technically maths font, but symbols only
        put("msam10","msam10"); 
        put("ppmath","ppmath"); 
        put("symbolpp","symbolpp"); 
        
        // unsure if these are correct...
        // from Ngoc's test pdf
        put("geometric231bt-heavyc", "Geometr231 Hv BT");
        put("geometric415bt-blacka", "geometr415blk");
        put("timesnewromanps-boldmt", "Times New Roman");
        
        // lm fonts
        put("lmmathsymbols10-regular", "L M Math Symbols10");
        put("lmmathsymbols7-regular", "L M Math Symbols7");
        put("lmsans8-regular", "L M Sans8");
        put("lmroman10-regular", "L M Roman10");
        put("lmroman7-regular", "L M Roman7");
        put("lmmathitalic10-regular", "L M Math Italic10");
        put("lmmathitalic7-regular", "L M Math Italic7");
        put("lmmathitalic5-regular", "L M Math Italic5");
        put("lmmathextension10-regular", "L M Math Extension10");
    }};    

    /**
     * Convert from mm to points
     * @param mm
     * @return 
     */
    public static double mm_to_pt(double mm){
        return mm * MM_TO_UNITS;
    }
    
    /**
     * Convert from points to mm
     * @param pt
     * @return 
     */
    public static double pt_to_mm(double pt){
        return pt / MM_TO_UNITS;
    }  

    /**
     * Rounding function
     * @param d
     * @param decimal_place
     * @return 
     */
    public static float round(float d, int decimal_place) {
         return BigDecimal.valueOf(d).setScale(decimal_place,BigDecimal.ROUND_HALF_UP).floatValue();
    }
    
    /**
     * Rounding function for rectangle
     * @param rec
     * @param decimal_place
     * @return 
     */
    public static Rectangle2D round_rectangle(Rectangle2D rec, int decimal_place) {
        Rectangle2D result = new Rectangle2D.Float (round((float)rec.getMinX(), decimal_place), round((float)rec.getMinY(), decimal_place), round((float)rec.getWidth(), decimal_place), round((float)rec.getHeight(), decimal_place) );
        return result;
    }
    
    public static Point2D round_point(Point2D point, int decimal_place){
        return new Point2D.Float(round((float)point.getX(), decimal_place), round((float)point.getY(), decimal_place));
    }

    /** 
     * Check if a value is between min and max
     * @param val
     * @param min
     * @param max
     * @return 
     */
    public static boolean is_between(double val, double min, double max){
        if (max > min && val >= min && val <= max)
            return true;

        if (min > max && val <= min && val >= max)
            return true;

        return false;
    }
    
    /**
     * Check if a value is near another
     * @param val
     * @param val2
     * @param delta
     * @return 
     */
    public static boolean is_near(double val, double val2, double delta) {
        return is_between(val, val2-delta, val2+delta);
    }  
    
    /**
     * Check if a rectangle is similar
     * @param rec
     * @param rec2
     * @param delta
     * @return 
     */
    public static boolean is_rectangle_similar(Rectangle2D rec, Rectangle2D rec2, double delta){
        boolean x_min = false;
        boolean x_max = false;
        boolean y_min = false;
        boolean y_max = false;
        
        if (is_near(rec.getMinX(), rec2.getMinX(), delta))
            x_min = true;
        
        if (is_near(rec.getMaxX(), rec2.getMaxX(), delta))
            x_max = true;

        if (is_near(rec.getMinY(), rec2.getMinY(), delta))
            y_min = true;
                
        if (is_near(rec.getMaxY(), rec2.getMaxY(), delta))
            y_max = true;
        
        return x_min && x_max && y_min && y_max;
    }

    /**
     * Convert PDF Rectangle to Rectangle
     * @param rectangle
     * @return 
     */
    public static Rectangle2D get_rectangle(PDRectangle rectangle){
        return new Rectangle2D.Double(rectangle.getLowerLeftX(), rectangle.getLowerLeftY(), rectangle.getWidth(), rectangle.getHeight());
    }   
   
    /**
     * Grow rectangle by padding amount on all sides
     * @param rec
     * @param padding
     * @return 
     */
    public static Rectangle2D grow_rectangle(Rectangle2D rec, double padding){
        return grow_rectangle(rec, padding, padding, padding, padding);
    }
    
    /**
     * Grow rectangle by padding amount on sides
     * @param rec
     * @param left
     * @param right
     * @param top
     * @param bottom
     * @return 
     */
    public static Rectangle2D grow_rectangle(Rectangle2D rec, double left, double right, double top, double bottom){
        double x = rec.getMinX() - left;
        double y = rec.getMinY() - top;
        double w = rec.getWidth() + left+right;
        double h = rec.getHeight() + top+bottom;
        return new Rectangle2D.Double(x, y, w, h);
    }    
    
    /**
     * Remove accented characters
     * useful before a regex which counts them as extra letters
     * @param str
     * @return 
     */
    public static String remove_diacritics(String str){
        // should only need this when using regex as it stuffs up the string length
        // removing diacritics (Accented character fluff)
        // https://www.drillio.com/en/2011/java-remove-accent-diacritic/
        return str.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }
    

    public static boolean equals(float[] color1, float[] color2){
        if (color1.length != color2.length)
            return false;
        
        // allow for variation of colors
        float max_variation = 0.01f; 
        
        for (int i=0; i<color1.length; i++){
            float a = color1[i]; 
            float b = color2[i];
            if ( Math.abs(a-b) > max_variation )
                return false;
        }
            
        return true;
    }    
    
    public static boolean equals(int[] color1, int[] color2){
        if (color1.length != color2.length)
            return false;
        
        // allow for variation of colors
        int max_variation = 1; 
        
        for (int i=0; i<color1.length; i++){
            int a = color1[i]; 
            int b = color2[i];
            if ( Math.abs(a-b) > max_variation )
                return false;
        }
            
        return true;
    }  
    
    public static void print_character_codes(String str){
        for (int i=0; i<str.length(); i++){
            System.out.println(str.charAt(i) + " = " + str.codePointAt(i));
        }
    }
    
    /**
     * Get the sides of the rectangle
     * top, bottom, left, right
     * @param rect
     * @return 
     */
    public static List<Line2D> get_lines_from_rectangle(Rectangle2D rect){
        Line2D top = new Line2D.Double(rect.getMinX(), rect.getMaxY(), rect.getMaxX(), rect.getMaxY());
        Line2D bottom = new Line2D.Double(rect.getMinX(), rect.getMinY(), rect.getMaxX(), rect.getMinY());
        Line2D left = new Line2D.Double(rect.getMinX(), rect.getMinY(), rect.getMinX(), rect.getMaxY());
        Line2D right = new Line2D.Double(rect.getMaxX(), rect.getMinY(), rect.getMaxX(), rect.getMaxY());
        
        List<Line2D> result = new ArrayList();
        result.add(top);
        result.add(bottom);
        result.add(left);
        result.add(right);
        
        return result;
    }
    
    /**
     * Get the corners of a rectangle
     * TL, TR, BR, BL - clockwise
     * @param rect
     * @return 
     */
    public static List<Point2D> get_points_from_rectangle(Rectangle2D rect){
        Point2D tl = new Point2D.Double(rect.getMinX(), rect.getMinY());
        Point2D tr = new Point2D.Double(rect.getMaxX(), rect.getMinY());
        Point2D br = new Point2D.Double(rect.getMaxX(), rect.getMaxY());
        Point2D bl = new Point2D.Double(rect.getMinX(), rect.getMaxY());
        
        List<Point2D> result = new ArrayList();
        result.add(tl);
        result.add(tr);
        result.add(br);
        result.add(bl);
        
        return result;
    }
    
    /**
     * Gets the largest distance between rectangles
     * @param r1
     * @param r2
     * @return 
     */
    public static double get_largest_distance_between_rectangles(Rectangle2D r1, Rectangle2D r2){
        Rectangle2D intersect = r1.createIntersection(r2);
        double width = Math.abs(intersect.getWidth());
        double height = Math.abs(intersect.getHeight());
        if (width > height)
            return width;
        else
            return height;
    }
    
    /**
     * Log to output window
     */
    public static void log(){
        log("");
    }
    
    /**
     * Log to output window
     * @param log
     */    
    public static void log(Object log){
        String name = log.getClass().getSimpleName();

        switch (name) {
            case "String": 
                System.out.println(log);
                break;
            default: 
                System.out.println(String.valueOf(log));
                break;
        }
        
    }    
    
    /**
     * Log to file
     * @param log
     * @param file_path
     * @param append
     */    
    public static void log_to_file(Object log, String file_path, Boolean append){
        String name = log.getClass().getSimpleName();
        BufferedWriter writer = null;
        
        try {
            writer = new BufferedWriter(new FileWriter(file_path, append));
            
            switch (name) {
                case "String[]":
                    for (String line: (String[])log){
                        writer.write(line);
                        writer.newLine();
                    }
                    break;
                case "String":
                    writer.write(String.valueOf(log));
                    writer.newLine();
                    break;
                default:
                    writer.write(String.valueOf(log));
                    writer.newLine();
                    break;
            }
        } catch (IOException ex) {
            Logger.getLogger(Util.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                // Close the writer regardless of what happens...
                writer.close();
            } catch (Exception e) {
            }
        }        
    }      
    
    /**
     * zero padded string
     * @return 
     */
    public static String number_as_formated_string(int value){
        return String.format("%03d", value);
    }
    
    public static boolean is_math_font(String font){
        // just to hide warnings
        return font.contains("cm") || 
            font.contains("ppfrac") || 
            font.contains("msbm");
    }   
    
    public static AffineTransform flip_y_transform (AffineTransform at){
        double[] flatmatrix = new double[6];
        at.getMatrix(flatmatrix);
        flatmatrix[3] = 1 - flatmatrix[3]; // page height - y coord - y height
        AffineTransform result = new AffineTransform(flatmatrix);
        return result;
    }

    public static Color to_color(float[] array) {
        Color result = null;
        

        // RGB
        if (array.length == 3){
            int[] scaled = new int[array.length];
            for (int i=0; i<array.length; i++){
                float val = array[i];
                int new_val = (int) (val * 255);
                scaled[i] = new_val;
            }            
            result = new Color(scaled[0],scaled[1],scaled[2]);
        }
        
        // CMYK
        if (array.length == 4)
            result = new Color(CMYKColorSpace.getInstance(), array, 1f);       
        
        return result;
    }
    

}
