package discomics.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Jure on 11.9.2016.
 */
public class Biomarker implements Serializable {

    private String biomarkerName;
    private List<String> searchTerms = new ArrayList<>();

    public Biomarker(String biomarkerName) {
        this.biomarkerName = biomarkerName;
    }

    public Biomarker(String biomarkerName, List<String> searchTerms) {
        this.biomarkerName = biomarkerName;
        this.searchTerms = searchTerms;
    }

    public List<String> getSearchTerms() {
        return searchTerms;
    }

    public boolean setSearchTerms(List<String> terms) {
        searchTerms.clear();
        return searchTerms.addAll(terms);
    }

    public void setName(String name) {
        biomarkerName = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Biomarker biomarker = (Biomarker) o;

        return biomarkerName != null ? biomarkerName.equals(biomarker.biomarkerName) : biomarker.biomarkerName == null;

    }

    @Override
    public int hashCode() {
        return biomarkerName != null ? biomarkerName.hashCode() : 0;
    }
}
