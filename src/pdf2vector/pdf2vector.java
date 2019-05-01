package pdf2vector;

import java.io.File;
import java.io.IOException;
import org.apache.pdfbox.pdmodel.PDDocument;

/**
 *
 * @author Bronson
 */
public class pdf2vector {

    pdf2vector(String filename) throws IOException{
        parse(filename);
    }
        
    /**
     * @param args the command line arguments
     * @throws java.io.IOException
     */
    public static void main(String[] args) throws IOException {
        // https://pdfbox.apache.org/2.0/getting-started.html
        System.setProperty("sun.java2d.cmm", "sun.java2d.cmm.kcms.KcmsServiceProvider");
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");
        
        // example commandline args:
        // pdf2svg -book.pdf
        
        new pdf2vector("test.pdf");
    }       
        

    /**
     * Parse the pdf and store the data
     * @param filename
     * @throws IOException 
     */
    public void parse(String filename) throws IOException{
        File file = new File(".//test//test_print.pdf");
        //file = new File(".//test//sample-kitchen-sink.pdf");
        
        if (!file.exists())
            return;
        
        PDDocument document = PDDocument.load(file);
        int start = 1;
        int end = document.getNumberOfPages();
        
        long start_ms = System.currentTimeMillis();

        // this method uses a custom graphics stream
        // this works great, but the fill/pattern/gradients need fixing?
        PDFParse pdf = new PDFParse(document, start, end);
        
        // process each page
        for (int p = start-1; p <= end-1; p++){
            pdf.current_page = p;
            pdf.processPage(document.getPage(p));
        }
        pdf.close();



        Util.log("pdf parse: " + ((System.currentTimeMillis() - start_ms)/1000f) + "s");
    }    
    
}
