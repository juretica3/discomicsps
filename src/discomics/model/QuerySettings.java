package discomics.model;

import java.io.Serializable;

public class QuerySettings implements Serializable {
    static final transient long serialVersionUID = 99064523575290L;

    private boolean isProteaseSearch;
    private boolean isBiomarkerSearch;
    private boolean isCustomSearch;

    private boolean nameFilteringEnable;
    private boolean searchFullText;
    private boolean searchOnlyGeneName;
    private boolean supplementPseudogenes;

    private boolean queryPubmed;
    private boolean queryEPmc;

    public QuerySettings(boolean isProteaseSearch, boolean isBiomarkerSearch, boolean isCustomSearch, boolean nameFilteringEnable,
                         boolean searchFullText, boolean searchOnlyGeneName, boolean supplementPseudogenes, boolean queryPubmed) {
        this.isProteaseSearch = isProteaseSearch;
        this.isBiomarkerSearch = isBiomarkerSearch;
        this.isCustomSearch = isCustomSearch;
        this.nameFilteringEnable = nameFilteringEnable;
        this.searchFullText = searchFullText;
        this.searchOnlyGeneName = searchOnlyGeneName;
        this.supplementPseudogenes = supplementPseudogenes;
        this.queryPubmed = queryPubmed;
        this.queryEPmc = true;
    }

    public boolean isProteaseSearch() {
        return isProteaseSearch;
    }

    public boolean isBiomarkerSearch() {
        return isBiomarkerSearch;
    }

    public boolean isCustomSearch() {
        return isCustomSearch;
    }

    public boolean isNameFilteringEnable() {
        return nameFilteringEnable;
    }

    public boolean isSearchFullText() {
        return searchFullText;
    }

    public boolean isSearchOnlyGeneName() {
        return searchOnlyGeneName;
    }

    public boolean isQueryPubmed() {
        return queryPubmed;
    }

    public boolean isSupplementPseudogenes() {
        return supplementPseudogenes;
    }


    @Override
    public String toString() {
        return "QuerySettings{" +
                "isProteaseSearch=" + isProteaseSearch +
                ", isBiomarkerSearch=" + isBiomarkerSearch +
                ", isCustomSearch=" + isCustomSearch +
                ", nameFilteringEnable=" + nameFilteringEnable +
                ", searchFullText=" + searchFullText +
                ", searchOnlyGeneName=" + searchOnlyGeneName +
                ", supplementPseudogenes=" + supplementPseudogenes +
                ", queryPubmed=" + queryPubmed +
                ", queryEPmc=" + queryEPmc +
                '}';
    }
}
