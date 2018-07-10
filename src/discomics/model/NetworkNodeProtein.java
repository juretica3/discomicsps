package discomics.model;

import java.io.Serializable;

/**
 * Created by Jure on 21/03/2017.
 */
public class NetworkNodeProtein implements Serializable {
    private static long serialVersionUID = 3519235755L;

    private Protein protein;
    private double score;

    NetworkNodeProtein(Protein protein, double score, int networkSize) {
        this.protein = protein;

        if (networkSize == 1) //  if network size is 1, then score is 0, since protein doesn't interact
            this.score = 0;
        else // else normalise to log of network size
            this.score = score / (Math.log(networkSize) / Math.log(2));
    }

    public Integer getNrDrugs() {
        return protein.getDrugs().size();
    }

    public Double getScore() {
        return score;
    }

    public String getName() {
        return protein.getMainName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NetworkNodeProtein)) return false;

        NetworkNodeProtein that = (NetworkNodeProtein) o;

        return protein != null ? protein.equals(that.protein) : that.protein == null;
    }

    @Override
    public int hashCode() {
        return protein != null ? protein.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "NetworkNodeProtein{" +
                "protein=" + protein.getMainName() +
                "score=" + getScore() +
                '}';
    }
}
