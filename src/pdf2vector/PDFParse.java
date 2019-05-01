

package pdf2vector;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.font.TextAttribute;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import org.apache.pdfbox.contentstream.PDFGraphicsStreamEngine;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDFontDescriptor;
import org.apache.pdfbox.pdmodel.font.PDSimpleFont;
import org.apache.pdfbox.pdmodel.graphics.image.PDImage;
import org.apache.pdfbox.pdmodel.graphics.shading.PDShading;
import org.apache.pdfbox.pdmodel.graphics.state.PDGraphicsState;
import org.apache.pdfbox.pdmodel.graphics.state.PDTextState;
import org.apache.pdfbox.util.Matrix;
import org.apache.pdfbox.util.Vector;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.function.PDFunction;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDColorSpace;
import org.apache.pdfbox.pdmodel.graphics.pattern.PDAbstractPattern;
import org.apache.pdfbox.pdmodel.graphics.pattern.PDShadingPattern;
import org.apache.pdfbox.pdmodel.graphics.pattern.PDTilingPattern;
import org.apache.pdfbox.pdmodel.graphics.shading.PDShadingType2;
import org.apache.pdfbox.pdmodel.graphics.state.RenderingMode;
import org.apache.xmlgraphics.java2d.CMYKColorSpace;
import org.freehep.graphicsbase.util.UserProperties;

import org.freehep.graphicsio.ps.PSGraphics2D;
import org.freehep.graphicsio.svg.SVGGraphics2D;



public class PDFParse extends PDFGraphicsStreamEngine implements Runnable
{     
    PDDocument document = null;
    int current_page = -1; 
    CountDownLatch latch = null;
    int start_page = -1;
    int end_page = -1;   
    AffineTransform unit_transform;
    
    // from PageDrawer
    private Graphics2D graphics;
    private Graphics2D graphics_eps;
    private OutputStream eps_out;

    private GeneralPath linePath = new GeneralPath();
    private int clipWindingRule = -1;
    private Area lastClip; // last clip path
    // the page box to draw (usually the crop box but may be another)
    private PDRectangle pageSize;  
    
    private AffineTransform xform; // graphic transform before transformed to fit page
    private AffineTransform xform_flip;
    private AffineTransform xform_page;
    

    /**
     * Constructor.
     *
     * @param document
     * @param p_start_page
     * @param p_end_page
     */
    public PDFParse(PDDocument document, int p_start_page, int p_end_page)
    {
        super(document.getPage(0));
        this.document = document;
        this.start_page = p_start_page;
        this.end_page = p_end_page; 
    }
    
    /**
     * We need to do this each time the page dimensions have changed
     * ie aus 03 TR page orientation changes
     */
    private void init_graphics(PDPage page) throws IOException{
        // code from page drawer buffered image code
        // fromrenderImage()
        int page_number = current_page +1;
        pageSize = page.getCropBox();
        int widthPx = (int)pageSize.getWidth();
        int heightPx = (int)pageSize.getHeight();

        // svg
        String svg_name = ".//test//" + String.format("%03d", page_number) + ".svg";
        graphics = new SVGGraphics2D(new File(svg_name), new Dimension(widthPx, heightPx));
        
        UserProperties p = new UserProperties();
        p.put(SVGGraphics2D.TEXT_AS_SHAPES, Boolean.toString(false));
        ((SVGGraphics2D)graphics).setProperties(p);
        
        ((SVGGraphics2D)graphics).startExport();
        graphics.setBackground(Color.WHITE);
        graphics.clearRect(0, 0, widthPx, heightPx);

        // from from drawPage()
        xform = graphics.getTransform(); // store original
        
        graphics.translate(0, pageSize.getHeight());
        graphics.scale(1, -1);
        graphics.translate(-pageSize.getLowerLeftX(), -pageSize.getLowerLeftY());
        
        // store flipped version 
        xform_flip = graphics.getTransform();  
        
        // eps
        String eps_name = ".//test//" + String.format("%03d", page_number) + ".eps";
        graphics_eps = new PSGraphics2D(new File(eps_name), new Dimension(widthPx, heightPx));
        
        p = new UserProperties();
        p.put(PSGraphics2D.TEXT_AS_SHAPES, Boolean.toString(false));
        ((PSGraphics2D)graphics_eps).setProperties(p);
        
        ((PSGraphics2D)graphics_eps).startExport();
        graphics_eps.setBackground(Color.WHITE);
        graphics_eps.clearRect(0, 0, widthPx, heightPx);
        
        graphics_eps.translate(0, pageSize.getHeight());
        graphics_eps.scale(1, -1);  
        graphics_eps.translate(-pageSize.getLowerLeftX(), -pageSize.getLowerLeftY());    
    }
 

    
    public void close() throws IOException{
        this.document.close();

        if (graphics != null)
            graphics.dispose();
    }
    
    public void write(){
        ((SVGGraphics2D)graphics).endExport();
        ((PSGraphics2D)graphics_eps).endExport();
    }

    /**
     * Add annotations to process page
     * @param page
     * @throws IOException 
     */
    @Override
    public void processPage(PDPage page) throws IOException {
        init_graphics(page);

        // process super
        super.processPage(page);
        
        // write 
        write();
    }

    /**
     * Images are processed here
     * @param pdImage
     * @throws IOException 
     */
    @Override
    public void drawImage(PDImage pdImage) throws IOException
    {
        BufferedImage image = pdImage.getImage();
        Matrix ctmNew = getGraphicsState().getCurrentTransformationMatrix();
        AffineTransform imageTransform = ctmNew.createAffineTransform();
        imageTransform.scale(1.0 / image.getWidth(), -1.0 / image.getHeight());
        imageTransform.translate(0, -image.getHeight());
        graphics.drawImage(image, imageTransform, null);
        graphics_eps.drawImage(image, imageTransform, null);
    }
 

    /**
     * From PageDrawer.java
     * @param winding_rule
     * @throws IOException 
     */
    @Override
    public void fillAndStrokePath(int winding_rule) throws IOException {
        // TODO can we avoid cloning the path?
        GeneralPath path = (GeneralPath)linePath.clone();
        fillPath(winding_rule);
        linePath = path;
        strokePath();
    }
    
    /**
     * Shape shading fill is processed here
     * @param shadingName
     * @throws IOException 
     */
    @Override
    public void shadingFill(COSName shadingName) throws IOException { 
        PDShading shading = getResources().getShading(shadingName);
        PDAbstractPattern pattern = getResources().getPattern(shadingName);
        
        PDGraphicsState state = getGraphicsState();
        PDColor nonStrokingColor = state.getNonStrokingColor();
        PDColor strokingColor = state.getStrokingColor();
        PDColorSpace colorSpace = nonStrokingColor.getColorSpace();
        
        GeneralPath path = (GeneralPath)linePath.clone();
        Shape shape = path.createTransformedShape(xform);
        
        Matrix ctm = getGraphicsState().getCurrentTransformationMatrix();
        
        // pattern
        if (pattern != null && pattern instanceof PDTilingPattern){
            //PDPattern patternSpace = (PDPattern)colorSpace;
            //PDAbstractPattern pattern = patternSpace.getPattern(nonStrokingColor);
            //PDAbstractPattern pattern2 = getResources().getPattern(shadingName);
            

            PDTilingPattern tilingPattern = (PDTilingPattern) pattern;
            if (tilingPattern.getPaintType() == PDTilingPattern.PAINT_COLORED){
                // colored tiling pattern
                //return tilingPaintFactory.create(tilingPattern, null, null, xform);
            } else {
                // uncolored tiling pattern
                //return tilingPaintFactory.create(tilingPattern, patternSpace.getUnderlyingColorSpace(), color, xform);
            } 
            

            // reset
            linePath.reset(); 
            
            return;
        }
        
        // shading
        if (shading == null){
            // found shading!
            PDShadingPattern shadingPattern = (PDShadingPattern)pattern;
            shading = shadingPattern.getShading();
            
            if (shading == null){
                linePath.reset();
                return;
            }            
        }
        
        Paint paint = shading.toPaint(ctm);

        // Type 2 - axial
        // convert to a gradient paint!
        if (shading instanceof PDShadingType2){
            PDShadingType2 type = (PDShadingType2)shading;
            float[] coords = type.getCoords().toFloatArray();
            COSArray domain = type.getDomain();
            COSArray extend = type.getExtend();
            
            PDFunction function = shading.getFunction();
            int inputs = function.getNumberOfInputParameters();
            // Function has a range? should we be using the range?
            float[] evalFunction1 = shading.evalFunction(0);
            float[] evalFunction2 = shading.evalFunction(1);
            
            Color c1 = Util.to_color(evalFunction1);
            Color c2 = Util.to_color(evalFunction2);
            
            // calculate points
            Rectangle2D bounds = shape.getBounds2D();
            float p1x = (float)(coords[0] + bounds.getMinX());
            float p1y = (float)(coords[1] + bounds.getMaxY());
            float p2x = (float)(coords[2] + p1x);
            float p2y = (float)(coords[3] + p1y);

            paint = new GradientPaint(p1x, p1y, c1, p2x, p2y, c2);
        }
        
        if (!(paint instanceof Color || paint instanceof GradientPaint))
            Util.log("painting as image");        
        
        graphics.setComposite(getGraphicsState().getNonStrokingJavaComposite());
        graphics.setPaint(paint);
        graphics.fill(shape);

        // eps
        graphics_eps.setComposite(getGraphicsState().getNonStrokingJavaComposite());
        graphics_eps.setPaint(paint);
        graphics_eps.fill(shape);
        
        // clean
        graphics.setClip(null); 
        graphics_eps.setClip(null); 
        lastClip = null;    
        
        // reset color as there looks like a bug there last color is applied!
        state.setNonStrokingColor(nonStrokingColor);
        state.setStrokingColor(strokingColor);
        
        // reset
        linePath.reset(); 
    } 

    /**
     * Shape flat fill is processed here
     * @param windingRule
     * @throws IOException 
     */
    @Override
    public void fillPath(int windingRule) throws IOException {
        // this has properties such as line width, etc...
        PDGraphicsState state = getGraphicsState();
        String fill_type = state.getNonStrokingColor().getColorSpace().getName();
        
        // we have a pattern/gradient
        if (state.getNonStrokingColor().isPattern()){
            COSName pattern = state.getNonStrokingColor().getPatternName();
            shadingFill(pattern);
            return;
        }
        
        GeneralPath path = (GeneralPath)getLinePath().clone();
        
        Shape shape = path.createTransformedShape(xform);


        Color fill_color = new Color(state.getNonStrokingColor().toRGB());
        
        // svg rgb
        graphics.setPaint(fill_color);
        graphics.draw(shape);
        
        setClip();
        linePath.setWindingRule(windingRule);
        
        if (fill_type.contains("CMYK")){
            fill_color = new Color(CMYKColorSpace.getInstance(), state.getNonStrokingColor().getComponents(), 1f);
        }

        // eps cmyk
        graphics_eps.setPaint(fill_color);
        graphics_eps.fill(shape);        
        
        // from PageDrawer
        linePath.reset();
    }

    /**
     * Shape with only stroke is processed here
     * @throws IOException 
     */
    @Override
    public void strokePath() throws IOException {
        // this has properties such as line width, etc...
        PDGraphicsState state = getGraphicsState();
        String stroke_type = state.getStrokingColor().getColorSpace().getName();
        if (stroke_type.contains("Separation")){
            linePath.reset();   
            return;
        }
        
        BasicStroke stroke = (BasicStroke) graphics.getStroke();

     
        GeneralPath path = (GeneralPath)getLinePath().clone();
        Shape shape = xform.createTransformedShape(path);

        
        Color stroke_color = new Color(state.getStrokingColor().toRGB());

        graphics.setComposite(getGraphicsState().getStrokingJavaComposite());
        graphics_eps.setComposite(getGraphicsState().getStrokingJavaComposite());
        
        // svg rgb
        graphics.setStroke(stroke);
        graphics.setPaint(stroke_color);
        setClip();
        graphics.draw(shape);
        
        if (stroke_type.contains("CMYK")){
            stroke_color = new Color(CMYKColorSpace.getInstance(), state.getStrokingColor().getComponents(), 1f);
        }

        // eps cmyk
        graphics_eps.setStroke(stroke); 
        graphics_eps.setPaint(stroke_color);
        graphics_eps.draw(shape);          

        // reset path
        linePath.reset();        
    }           


    /**
     * Called when a string of text is to be shown.
     *
     * @param string the encoded text
     * @throws IOException if there was an error showing the text
     */
    @Override
    public void showTextString(byte[] string) throws IOException
    {
        super.showTextString(string);
    }

   /**
     * Called when a string of text with spacing adjustments is to be shown.
     *
     * @param array array of encoded text strings and adjustments
     * @throws IOException if there was an error showing the text
     */
    @Override
    public void showTextStrings(COSArray array) throws IOException
    {
        super.showTextStrings(array);
    }
    

    /**
     * Singular string by string
     * @param string
     * @throws IOException 
     */
    @Override
    protected void showText(byte[] string) throws IOException {
        
        // flip the page graphics
        graphics.setTransform(xform);
        graphics_eps.setTransform(xform);

        // collect info from showGlyph();
        super.showText(string);
        
        // restore page graphics
        graphics.setTransform(xform_flip);
        graphics_eps.setTransform(xform_flip);
    }
    

    /**
     * Overridden from PDFStreamEngine.
     * @param textRenderingMatrix
     * @param font
     * @param code
     * @param unicode
     * @param displacement
     * @throws java.io.IOException
     */
    @Override
    protected void showGlyph(Matrix textRenderingMatrix, PDFont font, int code, String unicode, Vector displacement) throws IOException {  
        if (unicode == null){
            if (font instanceof PDSimpleFont){
                char c = (char) code;
                unicode = new String(new char[] { c });
            }
            else{
                // Acrobat doesn't seem to coerce composite font's character codes, instead it
                // skips them. See the "allah2.pdf" TestTextStripper file.
                System.out.println("no unicode");
                return;
            }
        }  
        
        // we dont want spaces at this stage
        // spaces interfere with the page cleanup code
        // new text item
        if (unicode.equals(" ")){
            return;
        }  
        
        // graphics state
        PDGraphicsState state = getGraphicsState();
        RenderingMode renderingMode = state.getTextState().getRenderingMode();
        PDTextState text_state = state.getTextState();
        
        PDFontDescriptor descriptor = font.getFontDescriptor();
        if (descriptor == null){
            //System.out.println("no font info:" + unicode + " page:"+ this.current_page + " font:" + font.getName());
            return;
        }
              

        // font name
        String[] cur_split = font.getName().toLowerCase().split("\\+");
        String font_name = cur_split[cur_split.length-1]; // after the + symbol

        // get font info
        float font_size_pt = text_state.getFontSize();
        float font_scale = textRenderingMatrix.getScaleX(); // x scale should = y scale

        // replace font size if it = 1.0
        if (font_size_pt == 1)
            font_size_pt = font_scale;  
        
        /*
            // test, do we have a glyph?
            // the code bellow gives a far more accurate bounding box
            // but it looks to cause issues so leave it out for now
            // from page drawer showFontGlyph() && drawGlyph()
            GeneralPath path = null;
            if (font instanceof PDType1CFont)
                path = ((PDType1CFont)font).getPath(unicode);
            else
                System.out.println("add font type!");
            
            if (path == null){
                System.out.println("no font path");
                return;
            }

            // render glyph
            AffineTransform at = textRenderingMatrix.createAffineTransform();
            at.concatenate(font.getFontMatrix().createAffineTransform());            
            Shape glyph = at.createTransformedShape(path);
            
            if (glyph.getBounds2D().getWidth() != 0){
                // convert to document space
                AffineTransform trans = graphics.getTransform();
                shape = trans.createTransformedShape(glyph);
                use_glyph = true;
            }   
        */
        
        // some fonts dont give a cap height
        // this affect mth fonts and caps fonts
        PDRectangle font_bbox = descriptor.getFontBoundingBox();

        float font_bbox_height = font_bbox.getHeight() / 1000;
        float cap_height = descriptor.getCapHeight() / 1000;
        float x_height = descriptor.getXHeight() / 1000;
        float font_width = font.getWidth(code) / 1000;
        float descent = descriptor.getDescent() / 1000 / 2; // this is a negative value, div by 2 to give us a buffer
        float letter_height = cap_height;
        float space_width = font.getSpaceWidth()/ 1000;
        float y_offset = 0; // for special chars
        float x_offset = 0; // for special chars
        
        
        // calulate bounds ourself
        // this is the most standard way
        float font_height = letter_height - descent;

        // apply text matrix
        AffineTransform at = textRenderingMatrix.createAffineTransform();

        //Shape glyph_bounds = new Rectangle2D.Double(x_offset, descent + y_offset, font_width, font_height);
        Shape bbox = new Rectangle2D.Float(0, 0, font.getWidth(code) / 1000, 1);
        Shape shape = at.createTransformedShape(bbox);
        
        String font_map_name = Util.font_map.get(font_name);
        if (font_map_name == null){
            // dont log math fonts
            if (!Util.is_math_font(font_name)){
                Util.log(Util.number_as_formated_string(current_page) + " " + font_name);
            }
            // try use the font name
            font_map_name = font_name;
        }

        // font type
        Map<TextAttribute, Object> font_attributes = new HashMap<>();
        font_attributes.put(TextAttribute.FAMILY, font_map_name);
        font_attributes.put(TextAttribute.SIZE, font_size_pt);
        font_attributes.put(TextAttribute.WEIGHT, TextAttribute.WEIGHT_REGULAR);
        //font_attributes.put(TextAttribute.TRANSFORM, textRenderingMatrix); // experimental!

        if (font_name.contains("-ultrablack"))
            font_attributes.put(TextAttribute.WEIGHT, TextAttribute.WEIGHT_ULTRABOLD);
        else if (font_name.contains("-bolditalic")){
            font_attributes.put(TextAttribute.WEIGHT, TextAttribute.WEIGHT_BOLD);
            font_attributes.put(TextAttribute.POSTURE, TextAttribute.POSTURE_OBLIQUE);
        }
        else if (font_name.contains("-bold"))
           font_attributes.put(TextAttribute.WEIGHT, TextAttribute.WEIGHT_BOLD);
        else if(font_name.contains("-italic"))
            font_attributes.put(TextAttribute.POSTURE, TextAttribute.POSTURE_OBLIQUE);
        
        
        // color
        BasicStroke stroke = (BasicStroke) graphics.getStroke();
        
        String fill_type = state.getNonStrokingColor().getColorSpace().getName();
        Color fill_color = new Color(state.getNonStrokingColor().toRGB()); 
        
        String stroke_type = state.getStrokingColor().getColorSpace().getName();
        Color stroke_color = new Color(state.getStrokingColor().toRGB());  
        
        // TODO: Stroke text??
        
        graphics.setColor(fill_color);
        graphics.setFont(new Font(font_attributes));
        graphics.drawString(unicode, (float)shape.getBounds2D().getMinX(), (float)(pageSize.getHeight() - shape.getBounds2D().getMinY()));
        
        if (fill_type.contains("CMYK")){
            fill_color = new Color(CMYKColorSpace.getInstance(), state.getStrokingColor().getComponents(), 1f);
        }
        
        graphics_eps.setColor(fill_color);
        graphics_eps.setFont(new Font(font_attributes));
        graphics_eps.drawString(unicode, (float)shape.getBounds2D().getMinX(), (float)(pageSize.getHeight() - shape.getBounds2D().getMinY()));        
    }
    

    @Override
    public void appendRectangle(Point2D p0, Point2D p1, Point2D p2, Point2D p3) throws IOException
    {
        // to ensure that the path is created in the right direction, we have to create
        // it by combining single lines instead of creating a simple rectangle
        linePath.moveTo((float) p0.getX(), (float) p0.getY());
        linePath.lineTo((float) p1.getX(), (float) p1.getY());
        linePath.lineTo((float) p2.getX(), (float) p2.getY());
        linePath.lineTo((float) p3.getX(), (float) p3.getY());

        // close the subpath instead of adding the last line so that a possible set line
        // cap style isn't taken into account at the "beginning" of the rectangle
        linePath.closePath();
    }
    
    @Override
    public void clip(int windingRule) throws IOException
    {
        // the clipping path will not be updated until the succeeding painting operator is called
        clipWindingRule = windingRule;
    }

    @Override
    public void moveTo(float x, float y) throws IOException
    {
        linePath.moveTo(x, y);
    }

    @Override
    public void lineTo(float x, float y) throws IOException
    {
        linePath.lineTo(x, y);
    }

    @Override
    public void curveTo(float x1, float y1, float x2, float y2, float x3, float y3) throws IOException
    {
        linePath.curveTo(x1, y1, x2, y2, x3, y3);
    }

    @Override
    public Point2D getCurrentPoint() throws IOException
    {
        return linePath.getCurrentPoint();
    }

    @Override
    public void closePath() throws IOException
    {
        linePath.closePath();
    }

    @Override
    public void endPath() throws IOException
    {
        if (clipWindingRule != -1)
        {
            linePath.setWindingRule(clipWindingRule);
            getGraphicsState().intersectClippingPath(linePath);

            // PDFBOX-3836: lastClip needs to be reset, because after intersection it is still the same 
            // object, thus setClip() would believe that it is cached.
            lastClip = null;

            clipWindingRule = -1;
        }
        linePath.reset();
    }
    
    // from PageDrawer.java
    // sets the clipping path using caching for performance, we track lastClip manually because
    // Graphics2D#getClip() returns a new object instead of the same one passed to setClip
    public void setClip()
    {
        Area clippingPath = getGraphicsState().getCurrentClippingPath();
        if (clippingPath != lastClip)
        {
            graphics.setClip(clippingPath);
            graphics_eps.setClip(clippingPath);
            lastClip = clippingPath;
        }
    }    
    
    /**
     * Returns the underlying Graphics2D. May be null.
     * @return 
     */
    protected final Graphics2D getGraphics()
    {
        return graphics;
    }
    
    /**
     * Returns the current line path. This is reset to empty after each fill/stroke.
     * @return 
     */
    protected final GeneralPath getLinePath()
    {
        return linePath;
    }    
   

    @Override
    public void run() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }  
    
}
