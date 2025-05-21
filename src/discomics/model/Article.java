package discomics.model;

import org.joda.time.DateTime;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by Jure on 4.9.2016.
 */
public class Article implements Serializable {
    static final long serialVersionUID = 973649274256L;

    // empty String if not found online
    private String pmid = "";
    private String pmcid = "";

    private String artAbstract = "";
    private String title = "";

    private String pubDateDay = "";
    private String pubDateMonthName = "";
    private String pubDateMonthNr = "";
    private String pubDateYear = "";

    private String authorString = "";
    private List<ArtAuthor> artAuthors;

    private String journalVol = "";
    private String journalIssue = "";
    private String journalTitle = "";
    private String journalTitleShort = "";
    private String startPage = "";
    private String endPage = "";

    private int citedByCount = -1;

    private boolean isReview;

    private Set<Protein> proteasesMentioned;
    private Set<Biomarker> biomarkersMentioned;

    private String url = "";
    private String doi = "";

    private boolean isFilteringPassed = false;
    private boolean isCompressed = false;

    public Article() {
        proteasesMentioned = new HashSet<>();
        biomarkersMentioned = new HashSet<>();
        artAuthors = new ArrayList<>();
    }

    void setArtAbstract(String artAbstract) {
        this.artAbstract = artAbstract;
    }

    void processAbstractForBiomarkers(Biomarker biomarker) {
        String artAbstract = this.artAbstract.toLowerCase();

        for (String term : biomarker.getSearchTerms()) {
            if (artAbstract.matches(".*\\b" + term + "\\b.*"))
                this.biomarkersMentioned.add(biomarker);
        }
    }

    public String getAuthorString() {
        if (authorString.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < artAuthors.size(); i++) {
                ArtAuthor current = artAuthors.get(i);
                sb.append(current.getLastName()).append(" ");

                String currentFirstName = current.getFirstName();
                String[] currentFirstNameSplit = currentFirstName.split(" ");
                for (int ii = 0; ii < currentFirstNameSplit.length; ii++) {
                    if (currentFirstNameSplit[ii].length() == 1)
                        sb.append(currentFirstNameSplit[ii]);
                    else {
                        if (currentFirstNameSplit[ii].length() != 0)
                            sb.append(currentFirstNameSplit[ii].charAt(0));
                    }
                    if (ii != currentFirstNameSplit.length - 1)
                        sb.append(" ");
                }

                if (i == artAuthors.size() - 1) {
                    sb.append(".");
                } else {
                    sb.append(", ");
                }
            }
            return sb.toString().replaceAll("  ", " ");
        } else {
            return authorString;
        }
    }

    void setAuthorString(String artAuthors) {
        this.authorString = artAuthors;
    }

    public String getArtAbstract() {
        return artAbstract;
    }

    public String getTitle() {
        return title;
    }

    void setTitle(String title) {
        this.title = title;
    }

    public String getPubDateYear() {
        return pubDateYear;
    }

    public String getPubDateDay() {
        return pubDateDay;
    }

    public String getPubDateMonthName() {
        return pubDateMonthName;
    }

    public String getPubDateMonthNr() {
        return pubDateMonthNr;
    }

    public String getStartPage() {
        return startPage;
    }

    public String getEndPage() {
        return endPage;
    }

    public String getPmid() {
        return pmid;
    }

    public String getJournalVol() {
        return journalVol;
    }

    public String getJournalIssue() {
        return journalIssue;
    }

    public String getJournalTitle() {
        return journalTitle;
    }

    public String getJournalTitleShort() {
        return journalTitleShort;
    }

    public String getDoi() {
        return doi;
    }

    void setPmid(String pmid) {
        this.pmid = pmid;
    }

    void setJournalVol(String journalVol) {
        this.journalVol = journalVol;
    }

    void setJournalTitle(String journalTitle) {
        this.journalTitle = journalTitle;
    }

    public Boolean getIsReview() {
        return isReview;
    }

    void setReview(boolean review) {
        isReview = review;
    }

    public String getUrl() {
        return url;
    }

    void setArtURL(String artURL) {
        this.url = artURL;
    }

    void setJournalIssue(String journalIssue) {
        this.journalIssue = journalIssue;
    }

    public List<Protein> getProteasesMentioned() {
        return new ArrayList<>(proteasesMentioned);
    }

    void setPubDateDay(String pubDateDay) {
        this.pubDateDay = pubDateDay;
    }

    void setPubDateMonthNr(String pubDateMonthNr) {
        switch (pubDateMonthNr) {
            case "1":
                this.pubDateMonthName = "Jan";
                this.pubDateMonthNr = "1";
                break;
            case "2":
                this.pubDateMonthName = "Feb";
                this.pubDateMonthNr = "2";
                break;
            case "3":
                this.pubDateMonthName = "Mar";
                this.pubDateMonthNr = "3";
                break;
            case "4":
                this.pubDateMonthName = "Apr";
                this.pubDateMonthNr = "4";
                break;
            case "5":
                this.pubDateMonthName = "May";
                this.pubDateMonthNr = "5";
                break;
            case "6":
                this.pubDateMonthName = "Jun";
                this.pubDateMonthNr = "6";
                break;
            case "7":
                this.pubDateMonthName = "Jul";
                this.pubDateMonthNr = "7";
                break;
            case "8":
                this.pubDateMonthName = "Aug";
                this.pubDateMonthNr = "8";
                break;
            case "9":
                this.pubDateMonthName = "Sep";
                this.pubDateMonthNr = "9";
                break;
            case "10":
                this.pubDateMonthName = "Oct";
                this.pubDateMonthNr = "10";
                break;
            case "11":
                this.pubDateMonthName = "Nov";
                this.pubDateMonthNr = "11";
                break;
            case "12":
                this.pubDateMonthName = "Dec";
                this.pubDateMonthNr = "12";
                break;
        }
    }

    void setPubDateMonthName(String pubDateMonthName) {
        switch (pubDateMonthName) {
            case "Jan":
                this.pubDateMonthName = "Jan";
                this.pubDateMonthNr = "1";
                break;
            case "Feb":
                this.pubDateMonthName = "Feb";
                this.pubDateMonthNr = "2";
                break;
            case "Mar":
                this.pubDateMonthName = "Mar";
                this.pubDateMonthNr = "3";
                break;
            case "Apr":
                this.pubDateMonthName = "Apr";
                this.pubDateMonthNr = "4";
                break;
            case "May":
                this.pubDateMonthName = "May";
                this.pubDateMonthNr = "5";
                break;
            case "Jun":
                this.pubDateMonthName = "Jun";
                this.pubDateMonthNr = "6";
                break;
            case "Jul":
                this.pubDateMonthName = "Jul";
                this.pubDateMonthNr = "7";
                break;
            case "Aug":
                this.pubDateMonthName = "Aug";
                this.pubDateMonthNr = "8";
                break;
            case "Sep":
                this.pubDateMonthName = "Sep";
                this.pubDateMonthNr = "9";
                break;
            case "Oct":
                this.pubDateMonthName = "Oct";
                this.pubDateMonthNr = "10";
                break;
            case "Nov":
                this.pubDateMonthName = "Nov";
                this.pubDateMonthNr = "11";
                break;
            case "Dec":
                this.pubDateMonthName = "Dec";
                this.pubDateMonthNr = "12";
                break;
        }
    }

    void setPubDateYear(String pubDateYear) {
        this.pubDateYear = pubDateYear;
    }

    void setJournalTitleShort(String journalTitleShort) {
        this.journalTitleShort = journalTitleShort;
    }

    void setDoi(String doi) {
        this.doi = doi;
    }

    void setDoiAndUrl(String doi) {
        this.doi = doi;
        setArtURL("http://dx.doi.org/" + doi);
    }

    void setPmcid(String pmcid) {
        this.pmcid = pmcid;
    }

    public String getPmcid() {
        return pmcid;
    }

    public Set<Biomarker> getBiomarkersMentioned() {
        return new HashSet<>(biomarkersMentioned);
    }

    boolean removeBiomarker(Biomarker biomarker) {
        return this.biomarkersMentioned.remove(biomarker);
    }

    void setProteasesMentioned(Set<Protein> proteasesMentioned) {
        this.proteasesMentioned = proteasesMentioned;
    }

    public boolean getFilteringPassed() {
        return isFilteringPassed;
    } // table getter, needs to be public

    void setFilteringPassed(boolean filteringPassed) {
        isFilteringPassed = filteringPassed;
    }

    void addArtAuthor(String firstName, String lastName) {
        this.artAuthors.add(new ArtAuthor(firstName, lastName));
    }

    public List<ArtAuthor> getArtAuthors() {
        return new ArrayList<>(this.artAuthors);
    }

    void setCompressed(boolean compressed) {
        isCompressed = compressed;
    }

    public boolean isCompressed() {
        return isCompressed;
    }

    public int getCitedByCount() {
        return citedByCount;
    }

//    public double getCitedPerYear() {
//        DateTime todayDate = new DateTime();
//
//        try {
//            int pubDateYear = Integer.parseInt(this.pubDateYear);
//        } catch (NullPointerException | NumberFormatException e) {
//            DateTime artDate = new DateTime(pubDateYear, pubDateMonth, pubDateDay, 0, 0);
//        }
//        try {
//            int pubDateMonth = Integer.parseInt(this.pubDateMonthNr);
//        } catch (NullPointerException | NumberFormatException e){
//
//        }
//        try {
//            int pubDateDay = Integer.parseInt(this.pubDateDay);
//        } catch (NullPointerException | NumberFormatException e){
//
//        }
//    }

    public void setCitedByCount(int citedByCount) {
        this.citedByCount = citedByCount;
    }

    // used for article filtering
    String getTitleAbstractConcat() {
        StringBuilder stringBuilder = new StringBuilder();
        try {
            stringBuilder.append(this.title);
            if (this.title.charAt(this.title.length() - 1) != '.')
                stringBuilder.append(". ");

        } catch (StringIndexOutOfBoundsException e) {
            e.printStackTrace();
        }
        stringBuilder.append(this.artAbstract);

        return stringBuilder.toString();
    }

    public class ArtAuthor implements Serializable {
        String firstName = "";
        String lastName = "";

        public ArtAuthor(String firstName, String lastName) {
            this.firstName = firstName;
            this.lastName = lastName;
        }

        public String getFirstName() {
            return firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public String getAuthorRisLine() {
            StringBuilder sb = new StringBuilder();
            sb.append("AU  - ").append(this.lastName).append(", ");
            sb.append(this.firstName).append("\n");
            return sb.toString();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Article)) return false;

        Article article = (Article) o;

        return pmid.equals(article.pmid);
    }

    @Override
    public int hashCode() {
        return pmid.hashCode();
    }

    @Override
    public String toString() {
        return "Article{" +
                "pmid='" + pmid + '\'' +
                ", pubDateDay='" + pubDateDay + '\'' +
                ", pubDateMonthNr='" + pubDateMonthNr + '\'' +
                ", pubDateYear='" + pubDateYear + '\'' +
                '}';
    }
}
