package discomics.model;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class CustomInputQuery implements Serializable {
    static final transient long serialVersionUID = 2223757321341426L;

    List<CustomInputTermBlock> customQuery1;
    List<CustomInputTermBlock> customQuery2;
    List<CustomInputTermBlock> customQuery3;
    List<CustomInputTermBlock> customQuery4;
    List<CustomInputTermBlock> customQuery5;

    public CustomInputQuery() {
        customQuery1 = new ArrayList<>();
        customQuery2 = new ArrayList<>();
        customQuery3 = new ArrayList<>();
        customQuery4 = new ArrayList<>();
        customQuery5 = new ArrayList<>();
    }



    class CustomInputTermBlock implements Serializable {
        static final transient long serialVersionUID = 7223752562346L;

        private String urlEncodedQuotes = "%22";
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

}
