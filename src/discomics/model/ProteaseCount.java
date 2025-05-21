package discomics.model;

/**
 * Created by Jure on 4.9.2016.
 */
public class ProteaseCount implements Comparable<ProteaseCount> {

    private Protein protease;
    private int totalHits;
    private int retrieved;

    ProteaseCount(Protein protease, int totalHits, int retrieved) {
        this.protease = protease;
        this.totalHits = totalHits;
        this.retrieved = retrieved;
    }

    public Protein getProtease() {
        return protease;
    }

    public String getGeneName() {
        return protease.getMainName();
    }

    public int getTotalHits() {
        return totalHits;
    }

    public int getRetrieved() {
        return retrieved;
    }

    @Override
    public int compareTo(ProteaseCount o) {
        return Integer.compare(this.totalHits, o.getTotalHits());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ProteaseCount that = (ProteaseCount) o;
        return protease.equals(that.protease);
    }

    @Override
    public int hashCode() {
        return protease.hashCode();
    }
}
