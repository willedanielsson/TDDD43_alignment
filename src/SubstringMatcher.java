import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;

import org.semanticweb.owl.align.Alignment;
import org.semanticweb.owl.align.AlignmentException;
import org.semanticweb.owl.align.AlignmentProcess;

import com.hp.hpl.jena.sparql.function.library.matches;

import fr.inrialpes.exmo.align.impl.method.StringDistAlignment;
import fr.inrialpes.exmo.ontowrap.OntowrapException;

public class SubstringMatcher extends StringDistAlignment implements AlignmentProcess {

	public SubstringMatcher(){
		
	}
	
	@Override
	public void align(Alignment alignment, Properties params) throws AlignmentException {
		for(Object ont2Class : ontology2().getClasses()){
			for(Object ont1Class: ontology1().getClasses()){
				addAlignCell(ont1Class, ont2Class, "=", matchBySubstring(ont1Class,ont2Class));
			}
		}
		
	}
	// Will be tested when results have been obatined
	private void test(String string, ArrayList<String> arrayList){
		for (int i = 0; i < string.length(); i++) {
			arrayList.add(String.valueOf(string.charAt(i)));
			if(i > 1){
				arrayList.add(string.substring(i-2,i));
			}
			
			if(i > 2){
				arrayList.add(string.substring(i-3,i));
			}
			
			if(i == string.length()-1){
				arrayList.add(string.substring(i-1, i+1));
				arrayList.add(string.substring(i-2, i+1));
			}
			
		}
	}

	private double matchBySubstring(Object ont1Class, Object ont2Class) {
		try {
			String string1 = ontology1().getEntityName(ont1Class);
			String string2 = ontology2().getEntityName(ont2Class);
			
			// Matcher of substring
			ArrayList<String> arrayList1 = new ArrayList<String>();
			ArrayList<String> arrayList2 = new ArrayList<String>();
			
			if(string1.equals(string2)){
				return 0;
			}else{
				// TODO: Create a function for this and string 2
				// Get 1, 2 and 3-grams of string1
				for (int i = 0; i < string1.length(); i++) {
					arrayList1.add(String.valueOf(string1.charAt(i)));
					if(i > 1){
						arrayList1.add(string1.substring(i-2,i));
					}
					
					if(i > 2){
						arrayList1.add(string1.substring(i-3,i));
					}
					
					if(i == string1.length()-1){
						arrayList1.add(string1.substring(i-1, i+1));
						arrayList1.add(string1.substring(i-2, i+1));
					}
					
				}
				
				// Get 1, 2 and 3-grams of string2
				for (int i = 0; i < string2.length(); i++) {
					arrayList2.add(String.valueOf(string2.charAt(i)));
					
					if(i > 1){
						arrayList2.add(string2.substring(i-2, i));
					}
					
					if(i > 2){
						arrayList2.add(string2.substring(i-3, i));
					}
					
					if(i == string2.length()-1){
						arrayList2.add(string2.substring(i-1, i+1));
						arrayList2.add(string2.substring(i-2, i+1));
					}
				}
			}
			
			// Calculate the length
			int total = 0;
			for(Iterator iter = arrayList1.iterator(); iter.hasNext();){
				String s = (String) iter.next();
				
				if(arrayList2.contains(s)){
					total += s.length();
				}
			}
			
			double totalLength = Math.abs(1.0 - ((double) total / (((double) string1.length() + (double) string2.length()) * 2)));
			
			return totalLength;
			
		} catch (OntowrapException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return 1.0;		
	}
	
}
