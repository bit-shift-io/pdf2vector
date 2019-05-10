package pdf2vector;

import java.io.File;
import java.io.IOException;
import org.apache.pdfbox.pdmodel.PDDocument;

/**
 *
 * @author Bronson
 */
public class pdf2vector {
    /**
     * @param args the command line arguments
     * @throws java.io.IOException
     */
    public static void main(String[] args) throws IOException {
        // https://pdfbox.apache.org/2.0/getting-started.html
        System.setProperty("sun.java2d.cmm", "sun.java2d.cmm.kcms.KcmsServiceProvider");
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");
        
        // example commandline args:
        // pdf2vector -book.pdf
        String filename = ".//test//test.pdf";
        
        File file = new File(filename);
        if (!file.exists()){
            Util.log(filename + " not found!");
            return;
        }
        
        
        
        long start_ms = System.currentTimeMillis();
        PDDocument document = PDDocument.load(file);
        int start = 1;
        int end = document.getNumberOfPages();
        PDFParse pdf = new PDFParse(document, file, start, end);
        pdf.bdoc = new BasicDocument();
        
        // process each page
        for (int p = start-1; p <= end-1; p++){
            pdf.current_page = p;
            pdf.processPage(document.getPage(p));
        }
        
        pdf.bdoc.write();
        pdf.close();
        Util.log("completed in " + ((System.currentTimeMillis() - start_ms)/1000f) + "s");        
    }       
}