/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pdf2vector;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Stack;
import org.yaml.snakeyaml.Yaml;

/**
 *
 * @author Bronson
 * LinkedHashMap maintains order, which is nicer for formatting
 */
public class BasicDocument {
    public Map<String,Object> document = new LinkedHashMap<>();
    public ArrayList<Object> pages = new ArrayList<>();
    
    // to quickly access last item
    //public Stack item_stack = new Stack();
    
    BasicDocument() throws IOException{
        // create document
        document.put("name", "");
        document.put("author", "");
        document.put("time", "");
        document.put("date", "");
        document.put("version", "1.0");
        document.put("url", "");
        document.put("pages", pages); 
    }
    
    
    /**
     * Add a new page to the document
     * Includes some default settings
     * @return 
     */
    public Map<String, Object> add_page(){
        Map<String, Object> page = new LinkedHashMap<>();
        pages.add(page);
        // defaults
        page.put("number", pages.size());
        page.put("dimensions", new float[]{210, 297});
        page.put("units", "millimeters");
        page.put("items", new ArrayList<>());        
        return page;
    }
    
    
    /**
     * Get page by number
     * Zero based
     * @param p_page
     * @return 
     */
    public Map<String, Object> get_page(int p_page){
        Map<String, Object> page = (Map<String, Object>) pages.get(p_page);
        return page;
    }
    
    
    /**
     * Get last page
     * @return 
     */
    public Map<String, Object> get_last_page(){
        Map<String, Object> page = (Map<String, Object>) pages.get(pages.size()-1);
        return page;
    }
    
    
    /**
     * Get last page index
     * @return 
     */
    public int get_last_page_index(){
        return pages.size()-1;
    }    
    
    
    /**
     * Get page items list
     * Zero based
     * @param p_page
     * @return 
     */
    public ArrayList<Object> get_page_items(int p_page){
        Map<String, Object> page = get_page(p_page);
        // loop keys
        for(Map.Entry<String, Object> entry: page.entrySet()) {
            if (entry.getKey() == "items")
                return (ArrayList<Object>) entry.getValue();
        }
        return null;
    }
    
    
    /**
     * Add item to page items list
     * @param p_page
     * @return 
     */
    public Map<String, Object> add_item(int p_page){
        Map<String, Object> item = new LinkedHashMap<>();
        ArrayList<Object> items = get_page_items(p_page);
        items.add(item);
        return item;
    }
    
    
    /**
     * Gets the last item from the stack
     * @return 
     */
    public Map<String, Object> get_last_item(int p_page){
        ArrayList<Object> items = get_page_items(p_page);
        return (Map<String, Object>) items.get(items.size()-1);
    }
    
    
    /**
     * Write to file
     * @throws IOException 
     */
    public void write() throws IOException{
        // write
        Yaml yaml = new Yaml();
        FileWriter writer = new FileWriter(".//test//test.yml");
        yaml.dump(document, writer);        
    }
    
}
