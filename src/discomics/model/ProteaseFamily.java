package discomics.model;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Jure on 9.9.2016.
 */
public class ProteaseFamily implements Serializable {

    static final transient long serialVersionUID = 426375735473256L;

    private String standardAbbreviation;
    private List<String> namesSearchQuery;
    private List<String> highlightTerms;
    private List<String> badSynonyms;

    ProteaseFamily(String standardAbbreviation, List<String> namesSearchQuery, List<String> badSynonyms, List<String> highlightTerms) {
        this.standardAbbreviation = standardAbbreviation;
        this.namesSearchQuery = namesSearchQuery;
        this.badSynonyms = badSynonyms;
        this.highlightTerms = highlightTerms;
    }

    List<String> getNamesSearchQuery() {
        return new ArrayList<>(namesSearchQuery);
    }

    List<String> getBadSynonyms() {
        return new ArrayList<>(badSynonyms);
    }

    public String getStandardAbbreviation() {
        return standardAbbreviation;
    }

    //no setters, to make sure hashCode() of objects put into Collection never changes.


    public List<String> getHighlightTerms() {
        return new ArrayList<>(highlightTerms);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ProteaseFamily that = (ProteaseFamily) o;

        return standardAbbreviation.equals(that.standardAbbreviation);

    }

    @Override
    public int hashCode() {
        return standardAbbreviation.hashCode();
    }

    @Override
    public String toString() {
        return "ProteaseFamily{" +
                "standardAbbreviation='" + standardAbbreviation + '\'' +
                ", namesSearchQuery=" + namesSearchQuery +
                ", badSynonyms=" + badSynonyms +
                '}';
    }

    // SERIALISATION
    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
        s.defaultReadObject();
    }

    private void writeObject(ObjectOutputStream s) throws IOException {
        s.defaultWriteObject();
    }
}
