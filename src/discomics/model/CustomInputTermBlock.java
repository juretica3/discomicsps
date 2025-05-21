package discomics.model;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by admin on 24/02/2017.
 */
public class CustomInputTermBlock implements Serializable {
    static final transient long serialVersionUID = 2223757321341426L;

    private static String urlEncodedQuotes = "%22";
    private List<CustomInputTerm> customInputTerms;

    public CustomInputTermBlock() {
        this.customInputTerms = new ArrayList<>();
    }

    public boolean addInputTerm(String searchTerm, boolean isPhrase) {
        return this.customInputTerms.add(new CustomInputTerm(searchTerm, isPhrase));
    }

    public List<String> getCustomInputTermsAsStrings() {
        List<String> output = new ArrayList<>();
        for(CustomInputTerm customInputTerm: this.customInputTerms) {
            if(customInputTerm.isPhrase) {
                String termWithQuotes = urlEncodedQuotes + customInputTerm.getSearchTerm() + urlEncodedQuotes;
                output.add(termWithQuotes);
            } else {
                output.add(customInputTerm.getSearchTerm());
            }
        }
        return output;
    }

    public List<CustomInputTerm> getCustomInputTerms() {
        return customInputTerms;
    }

    public class CustomInputTerm implements Serializable {
        static final transient long serialVersionUID = 1233539523L;

        private String searchTerm;
        private boolean isPhrase;

        CustomInputTerm(String searchTerm, boolean isPhrase) {
            this.searchTerm = searchTerm;
            this.isPhrase = isPhrase;
        }

        public String getSearchTerm() {
            return searchTerm;
        }

        public boolean isPhrase() {
            return isPhrase;
        }
    }

    // SERIALISATION
    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
        s.defaultReadObject();
    }

    private void writeObject(ObjectOutputStream s) throws IOException {
        s.defaultWriteObject();
    }
}
